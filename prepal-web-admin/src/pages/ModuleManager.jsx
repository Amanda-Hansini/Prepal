import React, { useState, useEffect } from 'react';
import toast from "react-hot-toast";
import { motion } from 'framer-motion';
import { Search, Plus, Check, X, Edit2, Trash2, Home, Box, Upload, GraduationCap } from 'lucide-react';
import * as XLSX from 'xlsx';
import { collectionGroup, getDocs, doc, deleteDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { db } from '../firebase';
import { logActivity } from '../utils/activityLogger';
import ConfirmModal from '../components/ConfirmModal';

const ModuleManager = ({ setPage }) => {
  const [modules, setModules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isSaving, setIsSaving] = useState(false);

  // Filters
  const [degreeFilter, setDegreeFilter] = useState('All');
  const [uniqueDegrees, setUniqueDegrees] = useState([]);
  const [semesterFilter, setSemesterFilter] = useState('All');

  // Editing state
  const [editingModId, setEditingModId] = useState(null);
  const [editingModData, setEditingModData] = useState({});

  const [confirmConfig, setConfirmConfig] = useState({ isOpen: false, title: '', message: '', onConfirm: null });

  // Bulk Upload states
  const [isBulkOpen, setIsBulkOpen] = useState(false);
  const [parsedModules, setParsedModules] = useState([]);
  const [targetDegree, setTargetDegree] = useState('');

  const fetchModules = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collectionGroup(db, 'Modules'));
      const fetched = snap.docs.map(doc => {
        const data = doc.data();
        const pathParts = doc.ref.path.split('/');
        const degId = pathParts[1] || '';
        return { 
          docId: doc.id, 
          docPath: doc.ref.path, 
          degreeId: degId,
          ...data 
        };
      });
      setModules(fetched);

      const degs = Array.from(new Set(fetched.map(m => m.degreeId).filter(Boolean)));
      setUniqueDegrees(degs);
    } catch (error) {
      console.error("Error fetching modules:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchModules();
  }, []);

  const handleEditModule = (mod) => {
    setEditingModId(mod.docPath);
    setEditingModData({ ...mod });
  };

  const handleSaveModule = async () => {
    if (!editingModData.moduleCode || !editingModData.moduleName || !editingModData.credits) {
      toast.error("All fields are required!")
      return;
    }
    const creditVal = parseFloat(editingModData.credits);
    if (isNaN(creditVal) || creditVal < 1) {
      toast.error("Module credits must be a valid number and cannot be less than 1!")
      return;
    }
    setIsSaving(true);
    try {
      const updateData = {
        moduleCode: editingModData.moduleCode,
        moduleName: editingModData.moduleName,
        credits: parseInt(editingModData.credits, 10),
        semesterId: editingModData.semesterId
      };

      await updateDoc(doc(db, editingModData.docPath), updateData);
      await logActivity("Updated Module", `${updateData.moduleCode}: ${updateData.moduleName}`);

      setModules(prev => prev.map(m => m.docPath === editingModId ? { ...m, ...updateData } : m));
      setEditingModId(null);
      toast.success("Module updated successfully!")
    } catch (error) {
      console.error("Error updating module:", error);
      toast.error("Update failed: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteModule = (mod) => {
    setConfirmConfig({
      isOpen: true,
      title: 'Delete Module',
      message: `Are you sure you want to delete module ${mod.moduleCode}?`,
      onConfirm: async () => {
        setConfirmConfig(prev => ({ ...prev, isOpen: false }));
        setIsSaving(true);
        try {
          await deleteDoc(doc(db, mod.docPath));
          await logActivity("Deleted Module", `${mod.moduleCode}: ${mod.moduleName}`);
          setModules(prev => prev.filter(m => m.docPath !== mod.docPath));
          toast.success("Module deleted successfully!")
        } catch (error) {
          console.error("Error deleting module:", error);
          toast.error("Delete failed: " + error.message)
        } finally {
          setIsSaving(false);
        }
      }
    });
  };

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const bstr = evt.target.result;
        const wb = XLSX.read(bstr, { type: 'binary' });
        const wsname = wb.SheetNames[0];
        const ws = wb.Sheets[wsname];

        // Dynamic header row detection for module sheets
        const rawRows = XLSX.utils.sheet_to_json(ws, { header: 1 });
        let headerRowIndex = 0;
        
        const semKeys = ['semesterid', 'semester id', 'sem id', 'sem_id', 'semester', 'sem', 'semid'];
        const codeKeys = ['modulecode', 'module code', 'module_code', 'course code', 'coursecode', 'course_code', 'code', 'subjectcode', 'subject code', 'subject_code'];
        const nameKeys = ['modulename', 'module name', 'module_name', 'course title', 'coursetitle', 'course_title', 'name', 'title', 'subjectname', 'subject name', 'subject_name'];
        
        for (let i = 0; i < rawRows.length; i++) {
          const row = rawRows[i];
          if (!row || !Array.isArray(row)) continue;
          const hasSem = row.some(cell => cell && semKeys.includes(String(cell).toLowerCase().trim().replace(/[\s_-]/g, '')));
          const hasCode = row.some(cell => cell && codeKeys.includes(String(cell).toLowerCase().trim().replace(/[\s_-]/g, '')));
          const hasName = row.some(cell => cell && nameKeys.includes(String(cell).toLowerCase().trim().replace(/[\s_-]/g, '')));
          
          if (hasSem || (hasCode && hasName)) {
            headerRowIndex = i;
            break;
          }
        }

        const data = XLSX.utils.sheet_to_json(ws, { range: headerRowIndex });

        // Helper function for case-insensitive, space/underscore-resilient header mapping
        const getValueByKeys = (row, keys) => {
          const rowKeys = Object.keys(row);
          for (const k of keys) {
            if (row[k] !== undefined && row[k] !== null) return row[k];
            const normalizedK = k.toLowerCase().replace(/[\s_-]/g, '');
            const foundKey = rowKeys.find(rk => rk.toLowerCase().replace(/[\s_-]/g, '') === normalizedK);
            if (foundKey && row[foundKey] !== undefined && row[foundKey] !== null) return row[foundKey];
          }
          return '';
        };

        const resolveSemesterId = (rowSemId, moduleCode, lastValidSemId) => {
          let sem = String(rowSemId).trim();
          if (sem && sem !== 'undefined') {
            const digits = sem.match(/\d+/);
            if (digits) {
              const num = parseInt(digits[0], 10);
              return `SEM${num.toString().padStart(2, '0')}`;
            }
            return sem;
          }
          if (moduleCode) {
            const digits = moduleCode.match(/\d+/);
            if (digits && digits[0].length >= 3) {
              const codeStr = digits[0];
              const year = parseInt(codeStr[0], 10);
              const term = parseInt(codeStr[1], 10);
              if (year >= 1 && year <= 4 && term >= 1 && term <= 2) {
                const semNum = (year - 1) * 2 + term;
                return `SEM${semNum.toString().padStart(2, '0')}`;
              }
            }
          }
          return lastValidSemId || '';
        };

        let lastValidSemId = '';
        const parsed = data.map(row => {
          const rawSemId = getValueByKeys(row, ['semesterId', 'SemesterId', 'SemesterID', 'semesterid', 'Semester ID', 'semester id', 'semester_id', 'Semester_ID', 'semester', 'sem', 'semid', 'sem_id', 'Sem_ID', 'Semester No', 'semester no', 'semesternumber', 'SemesterNumber']);
          const rawCode = getValueByKeys(row, ['moduleCode', 'ModuleCode', 'modulecode', 'Module Code', 'module code', 'module_code', 'Module_Code', 'code', 'Code', 'courseCode', 'CourseCode', 'course code', 'Course Code', 'course_code', 'Course_Code', 'moduleid', 'module id', 'module_id', 'ModuleID', 'Module ID', 'Module_ID', 'subjectcode', 'subject code', 'subject_code', 'Subject Code', 'SubjectCode', 'subject', 'Subject', 'module', 'Module', 'modNo', 'mod no', 'mod_no', 'subjectid', 'subject id', 'subject_id', 'Subject ID', 'SubjectID']);
          const rawName = getValueByKeys(row, ['moduleName', 'ModuleName', 'modulename', 'Module Name', 'module name', 'module_name', 'Module_Name', 'name', 'Name', 'title', 'Title', 'courseTitle', 'CourseTitle', 'course title', 'Course Title', 'course_title', 'Course_Title', 'subjectname', 'subject name', 'subject_name', 'Subject Name', 'SubjectName', 'moduleTitle', 'module title', 'Module Title', 'ModuleTitle', 'subject', 'Subject']);
          const rawCredits = getValueByKeys(row, ['credits', 'Credits', 'credit', 'Credit', 'credit hours', 'credithours', 'Credit Hours', 'CreditHours', 'credit_hours', 'Credit_Hours', 'hrs', 'Hrs', 'hours', 'Hours']);

          const resolvedSemId = resolveSemesterId(rawSemId, String(rawCode).trim(), lastValidSemId);
          if (resolvedSemId) {
            lastValidSemId = resolvedSemId;
          }

          return {
            semesterId: resolvedSemId,
            moduleCode: String(rawCode).trim(),
            moduleName: String(rawName).trim(),
            credits: Math.max(parseInt(String(rawCredits || '3').trim(), 10) || 3, 1)
          };
        }).filter(m => m.moduleCode && m.semesterId && !m.moduleCode.toLowerCase().startsWith('total') && !m.moduleName.toLowerCase().startsWith('total'));

        setParsedModules(parsed);
        toast.success(`Parsed ${parsed.length} modules successfully.`)
      } catch (error) {
        console.error("Error parsing file:", error);
        toast.error("Failed to parse file. Please upload a valid Excel or CSV.")
      }
    };
    reader.readAsBinaryString(file);
  };

  const handleSaveBulkModules = async () => {
    if (!targetDegree || parsedModules.length === 0) {
      toast.error("Please select a target Programme first!")
      return;
    }
    setIsSaving(true);
    try {
      const batchCommit = writeBatch(db);
      const newModulesList = [];

      for (const mod of parsedModules) {
        const moduleDocId = `${targetDegree}_${mod.semesterId}_${mod.moduleCode}`;
        const moduleDocRef = doc(db, 'Degrees', targetDegree, 'Modules', moduleDocId);
        
        const moduleData = {
          ...mod,
          degreeId: targetDegree
        };

        batchCommit.set(moduleDocRef, moduleData);
        newModulesList.push({
          docId: moduleDocId,
          docPath: moduleDocRef.path,
          ...moduleData
        });
      }

      await batchCommit.commit();
      await logActivity("Imported Modules", `Bulk imported ${parsedModules.length} modules for ${targetDegree}`);
      toast.success(`Bulk imported ${parsedModules.length} modules!`)
      setModules(prev => [...prev, ...newModulesList]);
      setIsBulkOpen(false);
      setParsedModules([]);
    } catch (error) {
      console.error("Error bulk uploading modules:", error);
      toast.error("Failed: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const filteredModules = modules.filter(m => {
    const matchesSearch = (m.moduleCode || '').toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (m.moduleName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (m.degreeId || '').toLowerCase().includes(searchQuery.toLowerCase());
    const matchesDegree = degreeFilter === 'All' || m.degreeId === degreeFilter;
    const matchesSemester = semesterFilter === 'All' || m.semesterId === semesterFilter;
    return matchesSearch && matchesDegree && matchesSemester;
  });

  const uniqueSemesters = Array.from(new Set(modules.filter(m => degreeFilter === 'All' || m.degreeId === degreeFilter).map(m => m.semesterId).filter(Boolean)));

  const itemsPerPage = 8;
  const totalPages = Math.ceil(filteredModules.length / itemsPerPage) || 1;
  const currentModules = filteredModules.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  return (
    <div className="module-manager">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        className="dashboard-tab"
      >
        <ConfirmModal 
          {...confirmConfig} 
          onCancel={() => setConfirmConfig(prev => ({ ...prev, isOpen: false }))} 
        />
        <div className="view-container">
        <div className="page-header-row">
          <div>
            <button className="btn-back-home" onClick={() => setPage('dashboard')}>
              <Home size={16} /> Back to Dashboard
            </button>
            <h1 className="page-title">Global Module Management</h1>
            <p className="page-subtitle">Search, review, and import modules across academic plans.</p>
          </div>
        </div>

        <div className="glass-panel table-container">
          <div className="table-controls" style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
            <div className="search-box" style={{ flex: 2, minWidth: '250px' }}>
              <Search size={18} color="var(--text-muted)" />
              <input 
                type="text" 
                placeholder="Search modules globally by Code, Name, or Programme..." 
                value={searchQuery}
                onChange={e => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
            <div className="filter-group" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: '700' }}>Degree:</label>
              <select className="form-input" style={{ padding: '8px 12px' }} value={degreeFilter} onChange={e => { setDegreeFilter(e.target.value); setSemesterFilter('All'); setCurrentPage(1); }}>
                <option value="All">All Degrees</option>
                {uniqueDegrees.map(deg => <option key={deg} value={deg}>{deg}</option>)}
              </select>
            </div>
            <div className="filter-group" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: '700' }}>Semester:</label>
              <select className="form-input" style={{ padding: '8px 12px' }} value={semesterFilter} onChange={e => { setSemesterFilter(e.target.value); setCurrentPage(1); }}>
                <option value="All">All Semesters</option>
                {uniqueSemesters.map(sem => <option key={sem} value={sem}>{sem}</option>)}
              </select>
            </div>
          </div>

          <table className="modern-table">
            <thead>
              <tr>
                <th>Degree Programme</th>
                <th>Module Code</th>
                <th>Module Name</th>
                <th>Semester</th>
                <th>Credits</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading Modules...
                  </td>
                </tr>
              ) : currentModules.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No modules found.
                  </td>
                </tr>
              ) : currentModules.map((mod) => (
                <tr key={mod.docPath}>
                  {editingModId === mod.docPath ? (
                    <>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', backgroundColor: '#f1f5f9'}} value={editingModData.degreeId || ''} disabled />
                      </td>
                      <td className="id-cell">
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingModData.moduleCode || ''} onChange={e => setEditingModData({...editingModData, moduleCode: e.target.value})} />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingModData.moduleName || ''} onChange={e => setEditingModData({...editingModData, moduleName: e.target.value})} />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingModData.semesterId || ''} onChange={e => setEditingModData({...editingModData, semesterId: e.target.value})} />
                      </td>
                      <td>
                        <input type="number" className="form-input" style={{padding: '4px', fontSize: '0.9rem', width: '70px'}} value={editingModData.credits || ''} onChange={e => setEditingModData({...editingModData, credits: e.target.value})} />
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={handleSaveModule} disabled={isSaving} title="Save"><Check size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => setEditingModId(null)} title="Cancel"><X size={16} /></button>
                        </div>
                      </td>
                    </>
                  ) : (
                    <>
                      <td>
                        <span className="count-badge" style={{ backgroundColor: 'var(--shape-light)', color: 'var(--primary)' }}>
                          <GraduationCap size={12} style={{ marginRight: '4px', display: 'inline' }} /> {mod.degreeId}
                        </span>
                      </td>
                      <td className="id-cell">{mod.moduleCode}</td>
                      <td>{mod.moduleName}</td>
                      <td>
                        <span className="count-badge" style={{ backgroundColor: 'rgba(5, 123, 254, 0.1)', color: 'var(--accent)' }}>
                          {mod.semesterId}
                        </span>
                      </td>
                      <td><strong>{mod.credits}</strong> credits</td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={() => handleEditModule(mod)} title="Edit"><Edit2 size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => handleDeleteModule(mod)} title="Delete"><Trash2 size={16} /></button>
                        </div>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>

          <div className="table-footer">
            <p>Showing {currentModules.length} of {filteredModules.length} modules</p>
            {totalPages > 1 && (
              <div className="pagination">
                <button disabled={currentPage === 1} onClick={() => setCurrentPage(prev => prev - 1)}>Prev</button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(pageNum => (
                  <button 
                    key={pageNum} 
                    className={currentPage === pageNum ? 'active' : ''}
                    onClick={() => setCurrentPage(pageNum)}
                  >
                    {pageNum}
                  </button>
                ))}
                <button disabled={currentPage === totalPages} onClick={() => setCurrentPage(prev => prev + 1)}>Next</button>
              </div>
            )}
          </div>
        </div>
        </div>
      </motion.div>

      {/* Bulk Upload Modal */}
      {isBulkOpen && (
        <div className="modal-overlay">
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="glass-panel modal-content"
            style={{ maxWidth: '600px', width: '90%' }}
          >
            <div className="modal-header" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px', alignItems: 'center' }}>
              <div>
                <h2 style={{ fontSize: '1.5rem', fontWeight: '800' }}>Bulk Import Modules</h2>
                <p className="text-muted">Import a list of modules into an existing programme.</p>
              </div>
              <button className="icon-action-btn" onClick={() => { setIsBulkOpen(false); setParsedModules([]); }}><X size={24} /></button>
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Target Programme *</label>
              <select className="form-input" style={{ width: '100%', padding: '10px' }} value={targetDegree} onChange={e => setTargetDegree(e.target.value)}>
                <option value="">Select a Programme</option>
                {uniqueDegrees.map(deg => <option key={deg} value={deg}>{deg}</option>)}
              </select>
            </div>

            <div className="excel-upload-panel" style={{ border: '2px dashed var(--border-color)', borderRadius: '16px', padding: '32px', textAlign: 'center', background: 'rgba(242, 244, 248, 0.5)', marginBottom: '24px' }}>
              <input type="file" accept=".xlsx, .xls, .csv" id="module-upload-modal" style={{ display: 'none' }} onChange={handleFileUpload} />
              <label htmlFor="module-upload-modal" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                <Upload size={32} color="var(--accent)" />
                <p style={{ fontWeight: '700' }}>Click to upload module Excel/CSV file</p>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: SemesterID, ModuleCode (or Module ID), ModuleName, Credits</p>
              </label>
            </div>

            {parsedModules.length > 0 && (
              <>
                <h3 style={{ fontSize: '1rem', fontWeight: '700', marginBottom: '12px' }}>Parsed Modules Preview ({parsedModules.length})</h3>
                <div className="preview-table-wrapper" style={{ maxHeight: '180px', overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)', marginBottom: '24px' }}>
                  <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                    <thead><tr><th>Sem ID</th><th>Code</th><th>Name</th><th>Credits</th></tr></thead>
                    <tbody>
                      {parsedModules.map((m, idx) => (
                        <tr key={idx}><td>{m.semesterId}</td><td>{m.moduleCode}</td><td>{m.moduleName}</td><td>{m.credits}</td></tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <button className="btn-primary" style={{ width: '100%', padding: '12px' }} onClick={handleSaveBulkModules} disabled={isSaving}>
                  {isSaving ? 'Importing...' : 'Save Imported Modules'}
                </button>
              </>
            )}
          </motion.div>
        </div>
      )}
      <style dangerouslySetInnerHTML={{ __html: `
        .loader {
          width: 32px;
          height: 32px;
          border: 3px solid #e2e8f0;
          border-top: 3px solid var(--accent);
          border-radius: 50%;
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }

        .view-container {
          display: flex;
          flex-direction: column;
          gap: 24px;
        }

        .page-header-row {
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          margin-bottom: 8px;
        }

        .page-title {
          font-size: 2rem;
          font-weight: 800;
          color: var(--text-main);
          margin: 0;
        }

        .page-subtitle {
          color: var(--text-muted);
          font-weight: 500;
          margin: 8px 0 0 0;
        }

        .btn-back-home {
          background: none;
          border: none;
          color: var(--accent);
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;
          cursor: pointer;
        }

        .btn-back-home:hover {
          text-decoration: underline;
        }

        .btn-primary {
          background: var(--btn-gradient);
          color: white;
          padding: 14px 24px;
          border-radius: 12px;
          font-weight: 700;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          gap: 10px;
          box-shadow: 0 8px 20px rgba(0, 97, 255, 0.25);
        }

        .btn-primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 12px 25px rgba(0, 97, 255, 0.35);
        }

        .btn-secondary {
          background: var(--shape-light);
          color: var(--accent);
          padding: 12px 24px;
          border-radius: 12px;
          font-weight: 700;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
        }

        .table-container {
          padding: 0;
          overflow: hidden;
          background: white;
        }

        .table-controls {
          padding: 24px;
          display: flex;
          justify-content: space-between;
          border-bottom: 1px solid var(--border-color);
        }

        .search-box {
          display: flex;
          align-items: center;
          gap: 12px;
          background: var(--bg-body);
          border: 1px solid var(--border-color);
          padding: 12px 16px;
          border-radius: 12px;
          width: 320px;
        }

        .search-box input {
          background: transparent;
          border: none;
          padding: 0;
          font-size: 0.95rem;
          width: 100%;
          outline: none;
        }

        .modern-table {
          width: 100%;
          border-collapse: collapse;
          text-align: left;
        }

        .modern-table th {
          padding: 16px 24px;
          font-size: 0.85rem;
          color: var(--text-muted);
          font-weight: 600;
          border-bottom: 1px solid var(--border-color);
          background: rgba(242, 244, 248, 0.5);
        }

        .modern-table td {
          padding: 20px 24px;
          font-size: 0.95rem;
          border-bottom: 1px solid var(--border-color);
          vertical-align: middle;
        }

        .id-cell { font-weight: 700; color: var(--text-main); }
        .name-cell { font-weight: 600; color: var(--text-main); }

        .count-badge {
          background: var(--shape-light);
          color: var(--accent);
          padding: 6px 12px;
          border-radius: 8px;
          font-size: 0.85rem;
          font-weight: 700;
          display: inline-flex;
          align-items: center;
        }

        .status-pill {
          padding: 6px 16px;
          border-radius: 20px;
          font-size: 0.85rem;
          font-weight: 700;
        }

        .status-pill.active, .status-pill.Active { background: #ECFDF5; color: #10B981; border: 1px solid rgba(16, 185, 129, 0.2); }
        .status-pill.warning, .status-pill.Warning { background: #FFFBEB; color: #F59E0B; border: 1px solid rgba(245, 158, 11, 0.2); }
        .status-pill.inactive, .status-pill.Inactive { background: #FEF2F2; color: #EF4444; border: 1px solid rgba(239, 68, 68, 0.2); }

        .action-btns {
          display: flex;
          gap: 8px;
        }

        .icon-action-btn {
          background: var(--shape-light);
          color: var(--accent);
          padding: 8px;
          border-radius: 8px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          border: none;
          cursor: pointer;
          transition: all 0.2s;
        }

        .icon-action-btn:hover {
          background: var(--accent);
          color: white;
        }

        .icon-action-btn.edit:hover {
          background: #DBEAFE;
          color: #2563EB;
        }

        .icon-action-btn.delete:hover {
          background: #FEE2E2;
          color: #DC2626;
        }

        .table-footer {
          padding: 20px 24px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 0.9rem;
          color: var(--text-muted);
          font-weight: 500;
        }

        .pagination {
          display: flex;
          gap: 8px;
        }

        .pagination button {
          background: white;
          border: 1px solid var(--border-color);
          color: var(--text-muted);
          padding: 6px 14px;
          border-radius: 8px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;
        }

        .pagination button.active {
          background: var(--accent);
          color: white;
          border-color: var(--accent);
        }

        .pagination button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .form-input {
          border: 1px solid var(--border-color);
          border-radius: 10px;
          padding: 10px 14px;
          font-size: 0.95rem;
          color: var(--text-main);
          background: white;
          outline: none;
          transition: all 0.2s;
        }

        .form-input:focus {
          border-color: var(--accent);
          box-shadow: 0 0 0 3px rgba(5, 123, 254, 0.1);
        }

        .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(13, 47, 90, 0.4);
          backdrop-filter: blur(8px);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .modal-content {
          background: white;
          border-radius: var(--radius-lg);
          padding: 32px;
          box-shadow: var(--shadow-card);
          border: 1px solid var(--border-color);
        }

        .excel-upload-panel {
          border: 2px dashed var(--border-color);
          border-radius: 16px;
          padding: 32px;
          text-align: center;
          background: rgba(242, 244, 248, 0.5);
          margin-bottom: 24px;
          transition: all 0.2s;
        }

        .excel-upload-panel:hover {
          border-color: var(--accent);
          background: rgba(5, 123, 254, 0.02);
        }

        .preview-table-wrapper {
          max-height: 250px;
          overflow-y: auto;
          border-radius: 12px;
          border: 1px solid var(--border-color);
          margin-bottom: 24px;
        }
      ` }} />
    </div>
  );
};

export default ModuleManager;

import React, { useState, useEffect } from 'react';
import toast from "react-hot-toast";
import { motion } from 'framer-motion';
import { Search, Plus, Check, X, Edit2, Trash2, Home, Calendar, GraduationCap } from 'lucide-react';
import { collectionGroup, getDocs, doc, deleteDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { db } from '../firebase';
import { logActivity } from '../utils/activityLogger';
import ConfirmModal from '../components/ConfirmModal';

const getSemesterStatus = (sem) => {
  if (sem.status) return sem.status;
  if (!sem.startDate || !sem.endDate || sem.startDate === 'Not Set' || sem.endDate === 'Not Set' || sem.startDate === '' || sem.endDate === '') return 'Inactive';
  const today = new Date();
  const sDate = new Date(sem.startDate);
  const eDate = new Date(sem.endDate);
  if (today >= sDate && today <= eDate) return 'Active';
  if (today > eDate) return 'Completed';
  return 'Inactive';
};

const SemesterManager = ({ setPage }) => {
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isSaving, setIsSaving] = useState(false);

  // Filters
  const [degreeFilter, setDegreeFilter] = useState('All');
  const [uniqueDegrees, setUniqueDegrees] = useState([]);
  const [batchFilter, setBatchFilter] = useState('All');
  const [uniqueBatches, setUniqueBatches] = useState([]);

  // Editing state
  const [editingSemId, setEditingSemId] = useState(null);
  const [editingSemData, setEditingSemData] = useState({});

  const [confirmConfig, setConfirmConfig] = useState({ isOpen: false, title: '', message: '', onConfirm: null });

  const fetchSemesters = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collectionGroup(db, 'Semesters'));
      const fetched = snap.docs.map(doc => {
        const data = doc.data();
        // Extract parent degreeId from path if not present in fields
        const pathParts = doc.ref.path.split('/');
        const degId = pathParts[1] || '';
        return { 
          docId: doc.id, 
          docPath: doc.ref.path, 
          degreeId: degId,
          ...data 
        };
      });
      setSemesters(fetched);

      // Extract unique degree IDs for filtering
      const degs = Array.from(new Set(fetched.map(s => s.degreeId).filter(Boolean)));
      setUniqueDegrees(degs);

      const batches = Array.from(new Set(fetched.map(s => s.batchId).filter(Boolean)));
      setUniqueBatches(batches);
    } catch (error) {
      console.error("Error fetching semesters:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSemesters();
  }, []);

  const handleEditSemester = (sem) => {
    setEditingSemId(sem.docPath);
    setEditingSemData({ ...sem });
  };

  const handleSaveSemester = async () => {
    if (!editingSemData.academicYear || !editingSemData.semesterNo) {
      toast.error("Academic Year and Semester Number are required!")
      return;
    }
    setIsSaving(true);
    try {
      const updateData = {
        academicYear: editingSemData.academicYear,
        semesterNo: editingSemData.semesterNo,
        startDate: editingSemData.startDate || '',
        endDate: editingSemData.endDate || '',
        status: editingSemData.status || getSemesterStatus(editingSemData)
      };

      // Since semesters are inside a subcollection Degrees/{degreeId}/Semesters/{docId}
      await updateDoc(doc(db, editingSemData.docPath), updateData);

      await logActivity("Updated Semester", `${editingSemData.semesterId} (${updateData.academicYear} ${updateData.semesterNo})`);

      setSemesters(prev => prev.map(s => s.docPath === editingSemId ? { ...s, ...updateData } : s));
      setEditingSemId(null);
      toast.success("Semester updated successfully!")
    } catch (error) {
      console.error("Error updating semester:", error);
      toast.error("Update failed: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteSemester = (sem) => {
    setConfirmConfig({
      isOpen: true,
      title: 'Delete Semester',
      message: `Are you sure you want to delete semester ${sem.semesterId}? This might affect nested modules.`,
      onConfirm: async () => {
        setConfirmConfig(prev => ({ ...prev, isOpen: false }));
        setIsSaving(true);
        try {
          await deleteDoc(doc(db, sem.docPath));
          await logActivity("Deleted Semester", `${sem.semesterId} of ${sem.degreeId}`);
          setSemesters(prev => prev.filter(s => s.docPath !== sem.docPath));
          toast.success("Semester deleted successfully!")
        } catch (error) {
          console.error("Error deleting semester:", error);
          toast.error("Delete failed: " + error.message)
        } finally {
          setIsSaving(false);
        }
      }
    });
  };

  const filteredSemesters = semesters.filter(s => {
    const matchesSearch = (s.semesterId || '').toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (s.degreeId || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (s.academicYear || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (s.semesterNo || '').toLowerCase().includes(searchQuery.toLowerCase());
    const matchesDegree = degreeFilter === 'All' || s.degreeId === degreeFilter;
    const matchesBatch = batchFilter === 'All' || s.batchId === batchFilter;
    return matchesSearch && matchesDegree && matchesBatch;
  });

  const itemsPerPage = 8;
  const totalPages = Math.ceil(filteredSemesters.length / itemsPerPage) || 1;
  const currentSemesters = filteredSemesters.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  return (
    <div className="semester-manager">
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
            <h1 className="page-title">Global Semester Management</h1>
            <p className="page-subtitle">Configure, view, and organize semesters across academic tracks.</p>
          </div>
        </div>

        <div className="glass-panel table-container">
          <div className="table-controls" style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
            <div className="search-box" style={{ flex: 1 }}>
              <Search size={18} color="var(--text-muted)" />
              <input 
                type="text" 
                placeholder="Search semesters (e.g. Year I, Sem I, IT)..." 
                value={searchQuery}
                onChange={e => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
            <div className="filter-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: '700', color: 'var(--text-main)' }}>Programme:</label>
              <select 
                className="form-input" 
                style={{ padding: '8px 12px', minWidth: '150px' }}
                value={degreeFilter}
                onChange={e => { setDegreeFilter(e.target.value); setCurrentPage(1); }}
              >
                <option value="All">All Programmes</option>
                {uniqueDegrees.map(deg => <option key={deg} value={deg}>{deg}</option>)}
              </select>

              <label style={{ fontSize: '0.85rem', fontWeight: '700', color: 'var(--text-main)', marginLeft: '16px' }}>Batch:</label>
              <select 
                className="form-input" 
                style={{ padding: '8px 12px', minWidth: '150px' }}
                value={batchFilter}
                onChange={e => { setBatchFilter(e.target.value); setCurrentPage(1); }}
              >
                <option value="All">All Batches</option>
                {uniqueBatches.map(batch => <option key={batch} value={batch}>{batch}</option>)}
              </select>
            </div>
          </div>

          <table className="modern-table">
            <thead>
              <tr>
                <th>Degree Programme</th>
                  <th>Batch</th>
                  <th>Semester ID</th>
                <th>Academic Year</th>
                <th>Semester Name</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading Semesters...
                  </td>
                </tr>
              ) : currentSemesters.length === 0 ? (
                <tr>
                  <td colSpan="8" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No semesters found.
                  </td>
                </tr>
              ) : currentSemesters.map((sem) => (
                <tr key={sem.docPath}>
                  {editingSemId === sem.docPath ? (
                    <>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', backgroundColor: '#f1f5f9'}} value={editingSemData.degreeId || ''} disabled />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', backgroundColor: '#f1f5f9'}} value={editingSemData.batchId || ''} disabled />
                      </td>
                      <td className="id-cell">{sem.semesterId}</td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingSemData.academicYear || ''} onChange={e => setEditingSemData({...editingSemData, academicYear: e.target.value})} />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingSemData.semesterNo || ''} onChange={e => setEditingSemData({...editingSemData, semesterNo: e.target.value})} />
                      </td>
                      <td>
                        <input type="date" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingSemData.startDate || ''} onChange={e => setEditingSemData({...editingSemData, startDate: e.target.value})} />
                      </td>
                      <td>
                        <input type="date" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingSemData.endDate || ''} onChange={e => setEditingSemData({...editingSemData, endDate: e.target.value})} />
                      </td>
                      <td>
                        <select
                          className="form-input"
                          style={{ padding: '4px', fontSize: '0.85rem', width: '110px' }}
                          value={editingSemData.status || getSemesterStatus(sem)}
                          onChange={e => setEditingSemData({ ...editingSemData, status: e.target.value })}
                        >
                          <option value="Active">Active</option>
                          <option value="Inactive">Inactive</option>
                          <option value="Completed">Completed</option>
                        </select>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={handleSaveSemester} disabled={isSaving} title="Save"><Check size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => setEditingSemId(null)} title="Cancel"><X size={16} /></button>
                        </div>
                      </td>
                    </>
                  ) : (
                    <>
                      <td>
                        <span className="count-badge" style={{ backgroundColor: 'var(--shape-light)', color: 'var(--primary)' }}>
                          <GraduationCap size={12} style={{ marginRight: '4px', display: 'inline' }} /> {sem.degreeId}
                        </span>
                      </td>
                      <td style={{ fontWeight: 600 }}>{sem.batchId || 'N/A'}</td>
                      <td className="id-cell">{sem.semesterId}</td>
                      <td>{sem.academicYear}</td>
                      <td>
                        <span className="count-badge" style={{ backgroundColor: 'rgba(5, 123, 254, 0.1)', color: 'var(--accent)' }}>
                          <Calendar size={12} style={{ marginRight: '4px', display: 'inline' }} /> {sem.semesterNo}
                        </span>
                      </td>
                      <td>
                        {sem.startDate || <span style={{ color: 'var(--text-muted)' }}>Not Set</span>}
                      </td>
                      <td>
                        {sem.endDate || <span style={{ color: 'var(--text-muted)' }}>Not Set</span>}
                      </td>
                      <td>
                        {(() => {
                          const st = getSemesterStatus(sem);
                          const bg = st === 'Active' ? '#e6f4ea' : st === 'Completed' ? '#e8f0fe' : '#fef7e0';
                          const col = st === 'Active' ? '#137333' : st === 'Completed' ? '#1a73e8' : '#b06000';
                          const dot = st === 'Active' ? '● ' : st === 'Completed' ? '✓ ' : '○ ';
                          return (
                            <span style={{
                              display: 'inline-block',
                              padding: '3px 10px',
                              borderRadius: '12px',
                              fontSize: '0.78rem',
                              fontWeight: 600,
                              backgroundColor: bg,
                              color: col,
                              border: `1px solid ${col}40`
                            }}>
                              {dot}{st}
                            </span>
                          );
                        })()}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={() => handleEditSemester(sem)} title="Edit"><Edit2 size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => handleDeleteSemester(sem)} title="Delete"><Trash2 size={16} /></button>
                        </div>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>

          <div className="table-footer">
            <p>Showing {currentSemesters.length} of {filteredSemesters.length} semesters</p>
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
      ` }} />
    </div>
  );
};

export default SemesterManager;

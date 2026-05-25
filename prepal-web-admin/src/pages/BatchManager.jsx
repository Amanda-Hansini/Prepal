import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Search, Plus, ChevronRight, Check, X, ArrowLeft,
  Users, Edit2, Trash2, Home, Upload, UserPlus, GraduationCap
} from 'lucide-react';
import * as XLSX from 'xlsx';
import { collection, getDocs, doc, deleteDoc, setDoc, writeBatch, query, where } from 'firebase/firestore';
import { db } from '../firebase';
import { logActivity } from '../utils/activityLogger';

const BatchManager = ({ setPage }) => {
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isSaving, setIsSaving] = useState(false);

  // Editing state for Batches
  const [editingBatchId, setEditingBatchId] = useState(null);
  const [editingBatchData, setEditingBatchData] = useState({});

  // Enrollment Modal States
  const [isEnrollmentModalOpen, setIsEnrollmentModalOpen] = useState(false);
  const [enrollmentBatch, setEnrollmentBatch] = useState(null);
  const [studentList, setStudentList] = useState([]); // Uploaded students
  const [existingStudents, setExistingStudents] = useState([]);
  const [isLoadingStudents, setIsLoadingStudents] = useState(false);
  const [enrollmentTab, setEnrollmentTab] = useState('view'); // 'view', 'upload', 'single'
  const [editingStudentId, setEditingStudentId] = useState(null);
  const [editingStudentData, setEditingStudentData] = useState({});
  const [singleStudent, setSingleStudent] = useState({ studentId: '', fullName: '', email: '', password: '', status: 'active' });

  const fetchBatches = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, 'Batches'));
      const fetched = snap.docs.map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }));
      setBatches(fetched);
    } catch (error) {
      console.error("Error fetching batches:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBatches();
  }, []);

  const handleEditBatch = (batch) => {
    setEditingBatchId(batch.docId);
    setEditingBatchData({ ...batch });
  };

  const handleSaveBatch = async () => {
    if (!editingBatchData.batchId || !editingBatchData.batchName) {
      alert("Batch ID and Name are required!");
      return;
    }
    setIsSaving(true);
    try {
      const dataToSave = { ...editingBatchData };
      delete dataToSave.docId;
      delete dataToSave.docPath;

      const programId = editingBatchData.programId;
      const batchDocPath = `${programId}(${editingBatchData.batchName})`;

      // Update both global and nested collections
      const batchCommit = writeBatch(db);
      batchCommit.set(doc(db, 'Batches', batchDocPath), dataToSave, { merge: true });
      batchCommit.set(doc(db, 'Degrees', programId, 'Batches', batchDocPath), dataToSave, { merge: true });

      await batchCommit.commit();
      await logActivity("Updated Batch", `${editingBatchData.batchId}: ${editingBatchData.batchName}`);
      
      setBatches(prev => prev.map(b => b.docId === editingBatchId ? { ...b, ...editingBatchData } : b));
      setEditingBatchId(null);
      alert("Batch updated successfully!");
    } catch (error) {
      console.error("Error updating batch:", error);
      alert("Update failed: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteBatch = async (batch) => {
    if (!window.confirm(`Are you sure you want to delete batch ${batch.batchName}? This action cannot be undone.`)) return;
    setIsSaving(true);
    try {
      const programId = batch.programId;
      const batchDocPath = `${programId}(${batch.batchName})`;

      const batchCommit = writeBatch(db);
      batchCommit.delete(doc(db, 'Batches', batchDocPath));
      batchCommit.delete(doc(db, 'Degrees', programId, 'Batches', batchDocPath));

      await batchCommit.commit();
      await logActivity("Deleted Batch", `${batch.batchId}: ${batch.batchName}`);

      setBatches(prev => prev.filter(b => b.batchId !== batch.batchId));
      alert("Batch deleted successfully!");
    } catch (error) {
      console.error("Error deleting batch:", error);
      alert("Delete failed: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  // Student Enrollment Logic
  const handleOpenEnrollment = async (batch) => {
    setEnrollmentBatch(batch);
    setStudentList([]);
    setExistingStudents([]);
    setIsEnrollmentModalOpen(true);
    setIsLoadingStudents(true);
    
    try {
      const studentDocPath = `${batch.programId}(${batch.batchName})`;
      const studentsSnap = await getDocs(collection(db, 'Students', studentDocPath, 'Student IDs'));
      const students = studentsSnap.docs.map(doc => doc.data());
      setExistingStudents(students);
    } catch (error) {
      console.error("Error fetching existing students:", error);
    } finally {
      setIsLoadingStudents(false);
    }
  };

  const checkStudentIdsUnique = async (ids) => {
    const existingIds = [];
    for (let i = 0; i < ids.length; i += 30) {
      const chunk = ids.slice(i, i + 30);
      const q = query(collection(db, 'AllStudents'), where('studentId', 'in', chunk));
      const snap = await getDocs(q);
      snap.forEach(doc => existingIds.push(doc.data().studentId));
    }
    return existingIds;
  };

  const hashPassword = async (password) => {
    const encoder = new TextEncoder();
    const data = encoder.encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
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
        const data = XLSX.utils.sheet_to_json(ws);

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

        const parsedStudents = data.map(row => ({
          studentId: String(getValueByKeys(row, ['studentId', 'studentid', 'student id', 'student_id', 'id', 'student no', 'studentno', 'student_no'])).trim(),
          fullName: String(getValueByKeys(row, ['fullName', 'fullname', 'full name', 'full_name', 'name', 'studentname', 'student name'])).trim(),
          email: String(getValueByKeys(row, ['email', 'Email', 'University Email', 'university email', 'universityemail', 'university_email', 'universityEmail', 'UniversityEmail', 'email address', 'emailaddress', 'email_address', 'mail'])).trim(),
          password: String(getValueByKeys(row, ['password', 'pass']) || '123456').trim(),
          status: String(getValueByKeys(row, ['status']) || 'active').toLowerCase().trim()
        })).filter(s => s.studentId);
        setStudentList(parsedStudents);
        alert(`Successfully parsed ${parsedStudents.length} students.`);
      } catch (error) {
        console.error("Error parsing student file:", error);
        alert("Failed to parse file. Please ensure it is a valid Excel or CSV.");
      }
    };
    reader.readAsBinaryString(file);
  };

  const handleSaveSingleStudent = async () => {
    const { studentId, fullName, email, password, status } = singleStudent;
    if (!studentId || !fullName || !email || !password) {
      alert("Please fill all student fields.");
      return;
    }

    setIsSaving(true);
    try {
      const existingIds = await checkStudentIdsUnique([studentId]);
      if (existingIds.length > 0) {
        alert(`Error: Student ID "${studentId}" is already taken!`);
        setIsSaving(false);
        return;
      }

      const batchCommit = writeBatch(db);
      const studentDocPath = `${enrollmentBatch.programId}(${enrollmentBatch.batchName})`;
      const hashedPassword = await hashPassword(password);
      
      const studentData = {
        studentId,
        fullName,
        email,
        status,
        batchId: enrollmentBatch.batchId,
        hashed_password: hashedPassword,
        initial_password: password,
        isFirstLogin: true
      };

      batchCommit.set(doc(db, 'Students', studentDocPath, 'Student IDs', studentId), studentData);
      batchCommit.set(doc(db, 'AllStudents', studentId), studentData);

      await batchCommit.commit();
      await logActivity("Enrolled Student", `${studentId} (${fullName}) in Batch ${enrollmentBatch.batchName}`);
      
      alert("Student enrolled successfully!");
      setExistingStudents(prev => [...prev, studentData]);
      setSingleStudent({ studentId: '', fullName: '', email: '', password: '', status: 'active' });
      setEnrollmentTab('view');
    } catch (error) {
      console.error("Error saving single student:", error);
      alert("Failed to enroll student: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveEnrollment = async () => {
    if (!enrollmentBatch || studentList.length === 0) return;
    setIsSaving(true);
    try {
      const newIds = studentList.map(s => s.studentId);
      const existingIds = await checkStudentIdsUnique(newIds);
      
      if (existingIds.length > 0) {
        alert(`Duplicate Student IDs Detected!\n\nIDs already in use: ${existingIds.join(', ')}.`);
        setIsSaving(false);
        return;
      }

      const batchCommit = writeBatch(db);
      const studentDocPath = `${enrollmentBatch.programId}(${enrollmentBatch.batchName})`;
      
      for (const student of studentList) {
        const hashedPassword = await hashPassword(student.password);
        const studentData = {
          studentId: student.studentId,
          fullName: student.fullName,
          email: student.email,
          status: student.status,
          batchId: enrollmentBatch.batchId,
          hashed_password: hashedPassword,
          initial_password: student.password,
          isFirstLogin: true
        };
        
        batchCommit.set(doc(db, 'Students', studentDocPath, 'Student IDs', student.studentId), studentData);
        batchCommit.set(doc(db, 'AllStudents', student.studentId), studentData);
      }
      
      await batchCommit.commit();
      await logActivity("Enrolled Students", `Bulk enrolled ${studentList.length} students into Batch ${enrollmentBatch.batchName}`);
      alert(`Successfully enrolled ${studentList.length} students into ${enrollmentBatch.batchName}.`);
      setIsEnrollmentModalOpen(false);
      setStudentList([]);
    } catch (error) {
      console.error("Error enrolling students:", error);
      alert("Failed to enroll students: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleEditStudent = (student) => {
    setEditingStudentId(student.studentId);
    setEditingStudentData({ ...student });
  };

  const handleSaveStudentEdit = async () => {
    if (!editingStudentId || !enrollmentBatch) return;
    setIsSaving(true);
    try {
      const batchCommit = writeBatch(db);
      const studentDocPath = `${enrollmentBatch.programId}(${enrollmentBatch.batchName})`;
      
      const studentData = {
        fullName: editingStudentData.fullName,
        email: editingStudentData.email,
        status: editingStudentData.status
      };

      batchCommit.update(doc(db, 'Students', studentDocPath, 'Student IDs', editingStudentId), studentData);
      batchCommit.update(doc(db, 'AllStudents', editingStudentId), studentData);

      await batchCommit.commit();
      await logActivity("Updated Student", `${editingStudentId} (${studentData.fullName}) details`);
      
      setExistingStudents(prev => prev.map(s => s.studentId === editingStudentId ? { ...s, ...studentData } : s));
      setEditingStudentId(null);
      alert("Student updated successfully.");
    } catch (error) {
      console.error("Error updating student:", error);
      alert("Failed to update student: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteStudent = async (studentId) => {
    if (!window.confirm(`Are you sure you want to delete student ${studentId}?`)) return;
    setIsSaving(true);
    try {
      const batchCommit = writeBatch(db);
      const studentDocPath = `${enrollmentBatch.programId}(${enrollmentBatch.batchName})`;

      batchCommit.delete(doc(db, 'Students', studentDocPath, 'Student IDs', studentId));
      batchCommit.delete(doc(db, 'AllStudents', studentId));

      await batchCommit.commit();
      await logActivity("Removed Student", `${studentId} from Batch ${enrollmentBatch.batchName}`);

      setExistingStudents(prev => prev.filter(s => s.studentId !== studentId));
      alert("Student deleted successfully.");
    } catch (error) {
      console.error("Error deleting student:", error);
      alert("Failed to delete student: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const filteredBatches = batches.filter(b => {
    const matchesSearch = (b.batchId || '').toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (b.batchName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (b.programId || '').toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSearch;
  });

  const itemsPerPage = 6;
  const totalPages = Math.ceil(filteredBatches.length / itemsPerPage) || 1;
  const currentBatches = filteredBatches.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  return (
    <div className="batch-manager">
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="view-container"
      >
        <div className="page-header-row">
          <div>
            <button className="btn-back-home" onClick={() => setPage('dashboard')}>
              <Home size={16} /> Back to Dashboard
            </button>
            <h1 className="page-title">Global Batch Management</h1>
            <p className="page-subtitle">View and manage all batches across all academic programmes.</p>
          </div>
        </div>

        <div className="glass-panel table-container">
          <div className="table-controls">
            <div className="search-box">
              <Search size={18} color="var(--text-muted)" />
              <input 
                type="text" 
                placeholder="Search by ID, Name, or Programme..." 
                value={searchQuery}
                onChange={e => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
          </div>

          <table className="modern-table">
            <thead>
              <tr>
                <th>Batch ID</th>
                <th>Batch Name</th>
                <th>Degree Programme</th>
                <th>Intake Year</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading Batches...
                  </td>
                </tr>
              ) : currentBatches.length === 0 ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No batches found.
                  </td>
                </tr>
              ) : currentBatches.map((batch) => (
                <tr key={batch.docId}>
                  {editingBatchId === batch.docId ? (
                    <>
                      <td className="id-cell">
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingBatchData.batchId || ''} onChange={e => setEditingBatchData({...editingBatchData, batchId: e.target.value})} />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingBatchData.batchName || ''} onChange={e => setEditingBatchData({...editingBatchData, batchName: e.target.value})} />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', backgroundColor: '#f1f5f9'}} value={editingBatchData.programId || ''} disabled />
                      </td>
                      <td>
                        <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingBatchData.intakeYear || ''} onChange={e => setEditingBatchData({...editingBatchData, intakeYear: e.target.value})} />
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={handleSaveBatch} disabled={isSaving} title="Save"><Check size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => setEditingBatchId(null)} title="Cancel"><X size={16} /></button>
                        </div>
                      </td>
                    </>
                  ) : (
                    <>
                      <td className="id-cell">{batch.batchId}</td>
                      <td>{batch.batchName}</td>
                      <td>
                        <span className="count-badge" style={{ backgroundColor: 'var(--shape-light)', color: 'var(--primary)' }}>
                          <GraduationCap size={12} style={{ marginRight: '4px', display: 'inline' }} /> {batch.programId}
                        </span>
                      </td>
                      <td>{batch.intakeYear}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="icon-action-btn edit" onClick={() => handleEditBatch(batch)} title="Edit"><Edit2 size={16} /></button>
                          <button className="icon-action-btn delete" onClick={() => handleDeleteBatch(batch)} title="Delete"><Trash2 size={16} /></button>
                          <button className="icon-action-btn" style={{ color: 'var(--accent)' }} onClick={() => handleOpenEnrollment(batch)} title="Manage Students"><Users size={16} /></button>
                        </div>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>

          <div className="table-footer">
            <p>Showing {currentBatches.length} of {filteredBatches.length} batches</p>
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
      </motion.div>

      {/* Student Enrollment Modal */}
      {isEnrollmentModalOpen && (
        <div className="modal-overlay">
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="glass-panel modal-content"
            style={{ maxWidth: '800px', width: '90%', padding: '32px' }}
          >
            <div className="modal-header" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px', alignItems: 'center' }}>
              <div>
                <h2 style={{ fontSize: '1.5rem', fontWeight: '800', color: 'var(--text-main)' }}>Enroll Students</h2>
                <p className="text-muted">Batch: <strong>{enrollmentBatch?.batchName}</strong> ({enrollmentBatch?.batchId})</p>
              </div>
              <button className="icon-action-btn" onClick={() => {setIsEnrollmentModalOpen(false); setStudentList([]);}}><X size={24} /></button>
            </div>

            <div className="modal-tabs" style={{ display: 'flex', gap: '20px', borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
              <button 
                onClick={() => setEnrollmentTab('view')}
                style={{ 
                  padding: '12px 16px', 
                  background: 'none', 
                  border: 'none', 
                  fontWeight: '700',
                  color: enrollmentTab === 'view' ? 'var(--accent)' : 'var(--text-muted)',
                  borderBottom: enrollmentTab === 'view' ? '2px solid var(--accent)' : '2px solid transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
              >
                Enrolled Students ({existingStudents.length})
              </button>
              <button 
                onClick={() => setEnrollmentTab('upload')}
                style={{ 
                  padding: '12px 16px', 
                  background: 'none', 
                  border: 'none', 
                  fontWeight: '700',
                  color: enrollmentTab === 'upload' ? 'var(--accent)' : 'var(--text-muted)',
                  borderBottom: enrollmentTab === 'upload' ? '2px solid var(--accent)' : '2px solid transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
              >
                Bulk Upload
              </button>
              <button 
                onClick={() => setEnrollmentTab('single')}
                style={{ 
                  padding: '12px 16px', 
                  background: 'none', 
                  border: 'none', 
                  fontWeight: '700',
                  color: enrollmentTab === 'single' ? 'var(--accent)' : 'var(--text-muted)',
                  borderBottom: enrollmentTab === 'single' ? '2px solid var(--accent)' : '2px solid transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
              >
                Add Single Student
              </button>
            </div>

            {enrollmentTab === 'view' && (
              <div className="tab-view-content">
                {isLoadingStudents ? (
                  <div className="loading-state" style={{ padding: '40px' }}>
                    <div className="loader" />
                    <p>Loading enrolled students...</p>
                  </div>
                ) : existingStudents.length > 0 ? (
                  <div className="preview-table-wrapper" style={{ maxHeight: '350px', overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                    <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                      <thead style={{ position: 'sticky', top: 0, background: 'white', zIndex: 1 }}>
                        <tr>
                          <th>ID</th>
                          <th>Name</th>
                          <th>Email</th>
                          <th>Initial Password</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {existingStudents.map((s, idx) => (
                          <tr key={idx}>
                            {editingStudentId === s.studentId ? (
                              <>
                                <td className="id-cell">{s.studentId}</td>
                                <td>
                                  <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.8rem'}} value={editingStudentData.fullName || ''} onChange={e => setEditingStudentData({...editingStudentData, fullName: e.target.value})} />
                                </td>
                                <td>
                                  <input type="email" className="form-input" style={{padding: '4px', fontSize: '0.8rem'}} value={editingStudentData.email || ''} onChange={e => setEditingStudentData({...editingStudentData, email: e.target.value})} />
                                </td>
                                <td>
                                  <code style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>{s.initial_password || '-'}</code>
                                </td>
                                <td>
                                  <select className="form-input" style={{padding: '4px', fontSize: '0.8rem'}} value={editingStudentData.status || 'active'} onChange={e => setEditingStudentData({...editingStudentData, status: e.target.value})}>
                                    <option value="active">Active</option>
                                    <option value="warning">Warning</option>
                                    <option value="inactive">Inactive</option>
                                  </select>
                                </td>
                                <td>
                                  <div style={{ display: 'flex', gap: '6px' }}>
                                    <button className="icon-action-btn edit" onClick={handleSaveStudentEdit} disabled={isSaving}><Check size={14} /></button>
                                    <button className="icon-action-btn delete" onClick={() => setEditingStudentId(null)}><X size={14} /></button>
                                  </div>
                                </td>
                              </>
                            ) : (
                              <>
                                <td className="id-cell">{s.studentId}</td>
                                <td>{s.fullName}</td>
                                <td>{s.email}</td>
                                <td>
                                  <code style={{ fontSize: '0.85rem', color: 'var(--primary)' }}>{s.initial_password || '-'}</code>
                                </td>
                                <td>
                                  <span className={`status-pill ${s.status?.toLowerCase() || 'active'}`}>{s.status || 'Active'}</span>
                                </td>
                                <td>
                                  <div style={{ display: 'flex', gap: '6px' }}>
                                    <button className="icon-action-btn edit" onClick={() => handleEditStudent(s)}><Edit2 size={14} /></button>
                                    <button className="icon-action-btn delete" onClick={() => handleDeleteStudent(s.studentId)} disabled={isSaving}><Trash2 size={14} /></button>
                                  </div>
                                </td>
                              </>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>No students enrolled in this batch yet.</p>
                )}
              </div>
            )}

            {enrollmentTab === 'upload' && (
              <div className="tab-view-content">
                <div className="excel-upload-panel" style={{ border: '2px dashed var(--border-color)', borderRadius: '16px', padding: '32px', textAlign: 'center', background: 'rgba(242, 244, 248, 0.5)', marginBottom: '24px' }}>
                  <input type="file" accept=".xlsx, .xls, .csv" id="student-upload-modal" style={{ display: 'none' }} onChange={handleFileUpload} />
                  <label htmlFor="student-upload-modal" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                    <Upload size={32} color="var(--accent)" />
                    <p style={{ fontWeight: '700' }}>Click to upload student Excel/CSV file</p>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: ID, Name, Email (or University Email), Password, Status</p>
                  </label>
                </div>

                {studentList.length > 0 && (
                  <>
                    <h3 style={{ fontSize: '1rem', fontWeight: '700', marginBottom: '12px', color: 'var(--text-main)' }}>Parsed Students List ({studentList.length})</h3>
                    <div className="preview-table-wrapper" style={{ maxHeight: '200px', overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)', marginBottom: '24px' }}>
                      <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                        <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Initial Password</th><th>Status</th><th>Actions</th></tr></thead>
                        <tbody>
                          {studentList.map((st, idx) => (
                            <tr key={idx}>
                              <td>{st.studentId}</td>
                              <td>{st.fullName}</td>
                              <td>{st.email}</td>
                              <td>
                                <code style={{ fontSize: '0.85rem', color: 'var(--primary)' }}>{st.password}</code>
                              </td>
                              <td><span className={`status-pill ${st.status}`}>{st.status}</span></td>
                              <td><button className="icon-action-btn delete" onClick={() => setStudentList(studentList.filter((_, i) => i !== idx))}><X size={14} /></button></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <button className="btn-primary" style={{ width: '100%', padding: '12px' }} onClick={handleSaveEnrollment} disabled={isSaving}>
                      {isSaving ? 'Enrolling...' : 'Enroll Students'}
                    </button>
                  </>
                )}
              </div>
            )}

            {enrollmentTab === 'single' && (
              <div className="tab-view-content" style={{ maxWidth: '500px', margin: '0 auto' }}>
                <div className="form-group" style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Student ID *</label>
                  <input type="text" className="form-input" style={{ width: '100%', padding: '10px' }} value={singleStudent.studentId} onChange={e => setSingleStudent({...singleStudent, studentId: e.target.value})} placeholder="e.g. IT210045" />
                </div>
                <div className="form-group" style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Full Name *</label>
                  <input type="text" className="form-input" style={{ width: '100%', padding: '10px' }} value={singleStudent.fullName} onChange={e => setSingleStudent({...singleStudent, fullName: e.target.value})} placeholder="Kasun Perera" />
                </div>
                <div className="form-group" style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Email Address *</label>
                  <input type="email" className="form-input" style={{ width: '100%', padding: '10px' }} value={singleStudent.email} onChange={e => setSingleStudent({...singleStudent, email: e.target.value})} placeholder="kasun@example.com" />
                </div>
                <div className="form-group" style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Default Password *</label>
                  <input type="password" className="form-input" style={{ width: '100%', padding: '10px' }} value={singleStudent.password} onChange={e => setSingleStudent({...singleStudent, password: e.target.value})} placeholder="••••••••" />
                </div>
                <div className="form-group" style={{ marginBottom: '24px' }}>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '700', marginBottom: '6px' }}>Status</label>
                  <select className="form-input" style={{ width: '100%', padding: '10px' }} value={singleStudent.status} onChange={e => setSingleStudent({...singleStudent, status: e.target.value})}>
                    <option value="active">Active</option>
                    <option value="warning">Warning</option>
                    <option value="inactive">Inactive</option>
                  </select>
                </div>
                <button className="btn-primary" style={{ width: '100%', padding: '12px' }} onClick={handleSaveSingleStudent} disabled={isSaving}>
                  {isSaving ? 'Enrolling...' : 'Enroll Student'}
                </button>
              </div>
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

export default BatchManager;

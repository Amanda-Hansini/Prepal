import React, { useState, useEffect } from 'react';
import toast from "react-hot-toast";
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Search, Filter, Plus, ChevronRight, Check, X, ArrowLeft,
  GraduationCap, Users, Calendar, Box, Settings, MoreVertical, Eye, Edit2, Trash2, Home,
  Upload, UserPlus, Download
} from 'lucide-react';
import * as XLSX from 'xlsx';
import { collection, getDocs, getDoc, doc, deleteDoc, setDoc, updateDoc, writeBatch, query, where, getDocsFromServer } from 'firebase/firestore';
import { db } from '../firebase';
import { logActivity } from '../utils/activityLogger';
import ConfirmModal from '../components/ConfirmModal';

const ProgrammeManager = ({ setPage, initialTab = 'batches', initialView = 'list' }) => {
  const [view, setView] = useState(initialView); // 'list', 'wizard', 'details'
  const [selectedProgramme, setSelectedProgramme] = useState(null);
  const [wizardStep, setWizardStep] = useState(1);
  const [activeTab, setActiveTab] = useState(initialTab === 'programmes' ? 'batches' : initialTab);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [currentPage, setCurrentPage] = useState(1);
  const [confirmConfig, setConfirmConfig] = useState({ isOpen: false, title: '', message: '', onConfirm: null });

  useEffect(() => {
    setView(initialView);
    if (initialView === 'wizard') {
      setWizardStep(1);
    }
  }, [initialView]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, statusFilter]);

  useEffect(() => {
    if (initialTab && initialTab !== 'programmes') {
      setActiveTab(initialTab);
    } else {
      setActiveTab('batches');
    }
  }, [initialTab]);
  
  // Form States
  const [newProgId, setNewProgId] = useState('');
  const [newProgName, setNewProgName] = useState('');
  const [newProgDuration, setNewProgDuration] = useState('N/A');
  const [newBatchId, setNewBatchId] = useState('');
  const [newBatchName, setNewBatchName] = useState('');
  const [newBatchIntakeYear, setNewBatchIntakeYear] = useState('');
  const [studentList, setStudentList] = useState([]); // Array of students for the new batch
  const [isEnrollmentModalOpen, setIsEnrollmentModalOpen] = useState(false);
  const [enrollmentBatch, setEnrollmentBatch] = useState(null);
  const [existingStudents, setExistingStudents] = useState([]);
  const [isLoadingStudents, setIsLoadingStudents] = useState(false);
  const [enrollmentTab, setEnrollmentTab] = useState('view'); // 'view' or 'upload'
  const [editingStudentId, setEditingStudentId] = useState(null);
  const [editingStudentData, setEditingStudentData] = useState({});
  const [singleStudent, setSingleStudent] = useState({ studentId: '', fullName: '', email: '', password: '', status: 'active' });
  const [studentTab, setStudentTab] = useState('upload'); // 'upload' or 'single'
  const [manualStudent, setManualStudent] = useState({ studentId: '', fullName: '', email: '', password: 'password123', status: 'active' });

  const [isAddBatchModalOpen, setIsAddBatchModalOpen] = useState(false);
  const [addBatchWizardStep, setAddBatchWizardStep] = useState(1);
  const [addBatchData, setAddBatchData] = useState({ batchId: '', batchName: '', intakeYear: '' });
  const [isAutoGeneratingSemesters, setIsAutoGeneratingSemesters] = useState(true);
  const [moduleList, setModuleList] = useState([]); // Array of modules for the new batch
  const [manualModule, setManualModule] = useState({ semesterId: '', moduleCode: '', moduleName: '', credits: '' });

  const [editingProg, setEditingProg] = useState(null);
  const [isSaving, setIsSaving] = useState(false);

  // Hub Data States
  const [hubBatches, setHubBatches] = useState([]);
  const [hubSemesters, setHubSemesters] = useState([]);
  const [hubModules, setHubModules] = useState([]);
  const [hubLoading, setHubLoading] = useState(false);

  const [hubFilterBatch, setHubFilterBatch] = useState('All');
  const [hubFilterSemester, setHubFilterSemester] = useState('All');

  const generateSemesters = (durationStr) => {
    const years = parseInt(durationStr?.charAt(0)) || 3;
    const romanYears = ['Year I', 'Year II', 'Year III', 'Year IV', 'Year V'];
    const romanSems = ['Semester I', 'Semester II'];
    let sems = [];
    let semCounter = 1;
    for (let y = 0; y < years; y++) {
      for (let s = 0; s < 2; s++) {
        const semId = `SEM${semCounter.toString().padStart(2, '0')}`;
        sems.push({
          id: semId,
          academicYear: romanYears[y],
          semesterNo: romanSems[s]
        });
        semCounter++;
      }
    }
    return sems;
  };

  const [programmes, setProgrammes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Editing state for Details Hub items
  const [editingHubItemId, setEditingHubItemId] = useState(null);
  const [editingHubItemType, setEditingHubItemType] = useState('');
  const [editingHubItemData, setEditingHubItemData] = useState({});

  const fetchProgrammes = async () => {
    setLoading(true);
    try {
      // Fetch batches to count them
      const batchesSnapshot = await getDocs(collection(db, 'Batches'));
      const batchCounts = {};
      batchesSnapshot.forEach(doc => {
        const pId = doc.data().programId;
        if (pId) {
          batchCounts[pId] = (batchCounts[pId] || 0) + 1;
        }
      });

      const querySnapshot = await getDocs(collection(db, 'Degrees'));
      const progs = [];
      querySnapshot.forEach((doc) => {
        const data = doc.data();
        const progId = data.id || doc.id;
        progs.push({
          docId: doc.id, // Track actual Firestore document ID
          id: progId,
          name: data.name || 'Unnamed',
          duration: data.duration || 'N/A',
          batches: batchCounts[progId] || 0,
          status: data.status || 'Active'
        });
      });
      setProgrammes(progs);
    } catch (error) {
      console.error("Error fetching programmes: ", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (view === 'list') {
      fetchProgrammes();
    }
  }, [view]);

  const handleStartWizard = () => {
    setView('wizard');
    setWizardStep(1);
  };

  const handleOpenDetails = (prog) => {
    setSelectedProgramme(prog);
    setView('details');
    setActiveTab(initialTab === 'programmes' ? 'batches' : initialTab);
    setHubFilterBatch('All');
    setHubFilterSemester('All');
  };

  const fetchHubData = async () => {
    if (!selectedProgramme || view !== 'details') return;
    setHubLoading(true);
    const normalize = (str) => String(str || '').replace(/[^a-z0-9]/gi, '').toLowerCase();
    const pIdNormalized = normalize(selectedProgramme.id);
    const pNameNormalized = normalize(selectedProgramme.name);

    try {
      // 1. Fetch Batches for this programme
      const batchesQ = query(collection(db, 'Batches'), where('programId', '==', selectedProgramme.id));
      const batchesSnap = await getDocs(batchesQ);
      const batches = batchesSnap.docs.map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }));
      setHubBatches(batches);

      // 2. Fetch Semesters (Check both new 'Semesters' and legacy 'Semester IDs')
      const directSemSnap = await getDocs(collection(db, 'Degrees', selectedProgramme.id, 'Semesters'));
      const directSems = directSemSnap.docs.map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }));

      const legacySemSnap = await getDocs(collectionGroup(db, 'Semester IDs'));
      const legacySems = legacySemSnap.docs
        .map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }))
        .filter(s => {
          const sDeg = normalize(s.degreeId);
          return sDeg === pIdNormalized || sDeg === pNameNormalized;
        });

      setHubSemesters([...directSems, ...legacySems]);

      // 3. Fetch Modules (Check both new 'Modules' and legacy 'Module IDs')
      const directModSnap = await getDocs(collection(db, 'Degrees', selectedProgramme.id, 'Modules'));
      const directMods = directModSnap.docs.map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }));

      const legacyModSnap = await getDocs(collectionGroup(db, 'Module IDs'));
      const legacyMods = legacyModSnap.docs
        .map(doc => ({ docId: doc.id, docPath: doc.ref.path, ...doc.data() }))
        .filter(m => {
          const mDeg = normalize(m.degreeId);
          return mDeg === pIdNormalized || mDeg === pNameNormalized;
        });

      setHubModules([...directMods, ...legacyMods]);
    } catch (error) {
      console.error("Error fetching hub data:", error);
    } finally {
      setHubLoading(false);
    }
  };

  useEffect(() => {
    fetchHubData();
  }, [selectedProgramme, view]);

  const handleDeleteHubItem = (type, item) => {
    if (selectedProgramme?.status?.toLowerCase() === 'inactive') {
      toast.error(`Action Blocked: The degree programme "${selectedProgramme.name}" is currently Inactive. You cannot delete items.`)
      return;
    }
    
    let message = `Are you sure you want to delete this ${type.slice(0, -1)}?`;
    if (type === 'batches') {
      message = `Are you sure you want to delete this batch? All semesters, modules, and enrolled students for this batch will also be deleted. This action cannot be undone.`;
    }

    setConfirmConfig({
      isOpen: true,
      title: `Delete ${type.slice(0, -1).charAt(0).toUpperCase() + type.slice(1, -1)}`,
      message: message,
      onConfirm: async () => {
        setConfirmConfig(prev => ({ ...prev, isOpen: false }));
        try {
          if (!item.docPath) {
            toast.error("Cannot delete: Document path not found.")
            return;
          }
      
      const deletePromises = [];
      deletePromises.push(deleteDoc(doc(db, item.docPath)));

      if (type === 'batches') {
          const safeProgId = String(item.programId || selectedProgramme.id).replace(/\//g, '-');
          const safeBatchName = String(item.batchName || '').replace(/\//g, '-');
          const batchDocPathFlat = `${safeProgId}(${safeBatchName})`;
          deletePromises.push(deleteDoc(doc(db, 'Batches', batchDocPathFlat)));

          const semsQ = query(collection(db, 'Degrees', selectedProgramme.id, 'Semesters'), where('batchId', '==', item.batchId));
          const semsSnap = await getDocs(semsQ);
          semsSnap.forEach(d => deletePromises.push(deleteDoc(d.ref)));

          const modsQ = query(collection(db, 'Degrees', selectedProgramme.id, 'Modules'), where('batchId', '==', item.batchId));
          const modsSnap = await getDocs(modsQ);
          modsSnap.forEach(d => deletePromises.push(deleteDoc(d.ref)));

          const studentsQ = query(collection(db, 'AllStudents'), where('batchId', '==', item.batchId));
          const studentsSnap = await getDocs(studentsQ);
          studentsSnap.forEach(d => {
             deletePromises.push(deleteDoc(d.ref));
             deletePromises.push(deleteDoc(doc(db, 'Students', batchDocPathFlat, 'Student IDs', d.id)));
          });
      } else if (type === 'semesters') {
          const modsQ = query(collection(db, 'Degrees', selectedProgramme.id, 'Modules'), where('semesterId', '==', item.semesterId), where('batchId', '==', item.batchId));
          const modsSnap = await getDocs(modsQ);
          modsSnap.forEach(d => deletePromises.push(deleteDoc(d.ref)));
      }

      await Promise.all(deletePromises);
      
      if (type === 'batches') {
        setHubBatches(prev => prev.filter(b => b.docPath !== item.docPath));
      } else if (type === 'semesters') {
        setHubSemesters(prev => prev.filter(s => s.docPath !== item.docPath));
      } else if (type === 'modules') {
        setHubModules(prev => prev.filter(m => m.docPath !== item.docPath));
      }
      toast.success(`${type.slice(0, -1).charAt(0).toUpperCase() + type.slice(1, -1)} deleted successfully!`);
    } catch (error) {
      console.error("Delete failed:", error);
      toast.error("Delete failed: " + error.message)
    }
      }
    });
  };

  const handleEditHubItem = (type, item) => {
    setEditingHubItemId(item.docPath);
    setEditingHubItemType(type);
    setEditingHubItemData({ ...item });
  };

  const handleSaveHubItem = async () => {
    try {
      setIsSaving(true);
      const dataToSave = { ...editingHubItemData };
      delete dataToSave.docId;
      delete dataToSave.docPath;

      await setDoc(doc(db, editingHubItemId), dataToSave, { merge: true });

      if (editingHubItemType === 'batches') {
        const batchDocPathFlat = editingHubItemId.split('/').pop();
        await setDoc(doc(db, 'Batches', batchDocPathFlat), dataToSave, { merge: true });
      }

      // Update local state
      if (editingHubItemType === 'batches') {
        setHubBatches(prev => prev.map(b => b.docPath === editingHubItemId ? { ...b, ...editingHubItemData } : b));
      } else if (editingHubItemType === 'semesters') {
        setHubSemesters(prev => prev.map(s => s.docPath === editingHubItemId ? { ...s, ...editingHubItemData } : s));
      } else if (editingHubItemType === 'modules') {
        setHubModules(prev => prev.map(m => m.docPath === editingHubItemId ? { ...m, ...editingHubItemData } : m));
      }

      setEditingHubItemId(null);
      setEditingHubItemType('');
      setEditingHubItemData({});
      toast.success("Updated successfully!")
    } catch (error) {
      console.error("Update failed:", error);
      toast.error("Update failed: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const cancelEditHubItem = () => {
    setEditingHubItemId(null);
    setEditingHubItemType('');
    setEditingHubItemData({});
  };

  const handleOpenEdit = (prog) => {
    setEditingProg({ ...prog });
    setView('edit');
  };

  const handleSaveNewBatch = async () => {
    if (!addBatchData.batchId || !addBatchData.batchName) {
      toast.error("Batch ID and Name are required!")
      return;
    }

    setIsSaving(true);
    try {
      // 1. Check for duplicate Student IDs system-wide
      if (studentList.length > 0) {
        const newIds = studentList.map(s => s.studentId);
        const existingIds = await checkStudentIdsUnique(newIds);
        if (existingIds.length > 0) {
          toast.error(`Duplicate IDs: ${existingIds.join(', ')} already exist in the system.`);
          setIsSaving(false);
          return;
        }
      }

      const batchCommit = writeBatch(db);
      const programId = selectedProgramme.id;
      
      // 2. Create Batch Document in nested and flat collections
      const batchDocPath = `${programId}(${addBatchData.batchName})`;
      const batchPayload = {
        batchId: addBatchData.batchId,
        batchName: addBatchData.batchName,
        intakeYear: addBatchData.intakeYear,
        programId: programId
      };
      
      batchCommit.set(doc(db, 'Degrees', programId, 'Batches', batchDocPath), batchPayload);
      batchCommit.set(doc(db, 'Batches', batchDocPath), batchPayload);

      // 3. Auto-generate Semesters if enabled
      if (isAutoGeneratingSemesters) {
        const generatedSemesters = generateSemesters(selectedProgramme.duration);
        for (const sem of generatedSemesters) {
          const semDocPath = `${batchDocPath}_${sem.id}`;
          batchCommit.set(doc(db, 'Degrees', programId, 'Semesters', semDocPath), {
            batchId: addBatchData.batchId,
            degreeId: programId,
            semesterId: sem.id,
            academicYear: sem.academicYear,
            semesterNo: sem.semesterNo
          });
        }
      }

      // 4. Enroll Students
      for (const student of studentList) {
        const hashedPassword = await hashPassword(student.password);
        const studentData = {
          studentId: student.studentId,
          fullName: student.fullName,
          email: student.email,
          status: student.status,
          batchId: addBatchData.batchId,
          hashed_password: hashedPassword,
          initial_password: student.password,
          isFirstLogin: true
        };
        
        batchCommit.set(doc(db, 'Students', batchDocPath, 'Student IDs', student.studentId), studentData);
        batchCommit.set(doc(db, 'AllStudents', student.studentId), studentData);
      }

      // 5. Save Modules
      for (const mod of moduleList) {
        const modDocPath = `${batchDocPath}_${mod.semesterId}_${mod.moduleCode}`;
        batchCommit.set(doc(db, 'Degrees', programId, 'Modules', modDocPath), {
          degreeId: programId,
          batchId: addBatchData.batchId,
          semesterId: mod.semesterId,
          moduleCode: mod.moduleCode,
          moduleName: mod.moduleName,
          credits: mod.credits
        });
      }

      await batchCommit.commit();
      await logActivity("Created Batch", `${addBatchData.batchId}: ${addBatchData.batchName} for ${programId}`);
      toast.success(`Batch "${addBatchData.batchName}" created with ${studentList.length} students and ${moduleList.length} modules!`)
      
      // Refresh Both Hub Data and Global Programme List (for batch counts)
      await fetchHubData(); 
      await fetchProgrammes();

      setIsAddBatchModalOpen(false);
      setAddBatchWizardStep(1);
      setStudentList([]);
      setModuleList([]);
      setAddBatchData({ batchId: '', batchName: '', intakeYear: '' });
    } catch (error) {
      console.error("Error creating new batch:", error);
      toast.error("Failed to create batch: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleOpenEnrollment = async (batch) => {
    setEnrollmentBatch(batch);
    setStudentList([]);
    setExistingStudents([]);
    setIsEnrollmentModalOpen(true);
    setIsLoadingStudents(true);
    
    try {
      const studentDocPath = `${selectedProgramme.id}(${batch.batchName})`;
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
    
    // 1. Check for duplicates within the uploaded list itself
    const uniqueIds = new Set();
    const duplicatesInList = new Set();
    ids.forEach(id => {
      if (uniqueIds.has(id)) duplicatesInList.add(id);
      uniqueIds.add(id);
    });
    duplicatesInList.forEach(id => existingIds.push(`${id} (duplicate inside file)`));

    // 2. Check Firestore (using getDocsFromServer to bypass any stuck local cache)
    // Firestore 'in' query limit is 30, so we chunk the ids
    for (let i = 0; i < ids.length; i += 30) {
      const chunk = ids.slice(i, i + 30);
      const q = query(collection(db, 'AllStudents'), where('studentId', 'in', chunk));
      const snap = await getDocsFromServer(q);
      snap.forEach(doc => existingIds.push(doc.data().studentId));
    }
    
    return [...new Set(existingIds)];
  };

  const handleSaveSingleStudent = async () => {
    const { studentId, fullName, email, password, status } = singleStudent;
    if (!studentId || !fullName || !email || !password) {
      toast.error("Please fill all student fields.")
      return;
    }

    setIsSaving(true);
    try {
      // Check for uniqueness
      const existingIds = await checkStudentIdsUnique([studentId]);
      if (existingIds.length > 0) {
        toast.error(`Error: Student ID "${studentId}" is already taken!`)
        setIsSaving(false);
        return;
      }

      const batchCommit = writeBatch(db);
      const studentDocPath = `${selectedProgramme.id}(${enrollmentBatch.batchName})`;
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
      
      toast.success("Student enrolled successfully!")
      setExistingStudents(prev => [...prev, studentData]);
      setSingleStudent({ studentId: '', fullName: '', email: '', password: '', status: 'active' });
      setEnrollmentTab('view');
    } catch (error) {
      console.error("Error saving single student:", error);
      toast.error("Failed to enroll student: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveEnrollment = async () => {
    if (!enrollmentBatch || studentList.length === 0) return;
    setIsSaving(true);
    try {
      // Check for duplicate Student IDs system-wide
      const newIds = studentList.map(s => s.studentId);
      const existingIds = await checkStudentIdsUnique(newIds);
      
      if (existingIds.length > 0) {
        toast.error(`Duplicate Student IDs Detected!\n\nThe following IDs are already in use: ${existingIds.join(', ')}.\n\nEach student must have a unique ID across the entire system. Please update your file and try again.`);
        setIsSaving(false);
        return;
      }

      const batchCommit = writeBatch(db);
      // Construct docPath based on program ID and batch name (matching mobile app logic)
      const studentDocPath = `${selectedProgramme.id}(${enrollmentBatch.batchName})`;
      
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
        
        // Save to nested collection (for Admin views)
        batchCommit.set(doc(db, 'Students', studentDocPath, 'Student IDs', student.studentId), studentData);
        // Save to flat collection (for Login system)
        batchCommit.set(doc(db, 'AllStudents', student.studentId), studentData);
      }
      
      await batchCommit.commit();
      await logActivity("Enrolled Students", `Bulk enrolled ${studentList.length} students into Batch ${enrollmentBatch.batchName}`);
      toast.success(`Successfully enrolled ${studentList.length} students into ${enrollmentBatch.batchName}.`)
      setIsEnrollmentModalOpen(false);
      setStudentList([]);
    } catch (error) {
      console.error("Error enrolling students:", error);
      toast.error("Failed to enroll students: " + error.message)
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
      const studentDocPath = `${selectedProgramme.id}(${enrollmentBatch.batchName})`;
      
      const studentData = {
        fullName: editingStudentData.fullName,
        email: editingStudentData.email,
        status: editingStudentData.status
      };

      // Update in nested collection
      batchCommit.update(doc(db, 'Students', studentDocPath, 'Student IDs', editingStudentId), studentData);
      // Update in flat collection
      batchCommit.update(doc(db, 'AllStudents', editingStudentId), studentData);

      await batchCommit.commit();
      await logActivity("Updated Student", `${editingStudentId} (${studentData.fullName}) details`);
      
      // Update local state
      setExistingStudents(prev => prev.map(s => s.studentId === editingStudentId ? { ...s, ...studentData } : s));
      setEditingStudentId(null);
      toast.success("Student updated successfully.")
    } catch (error) {
      console.error("Error updating student:", error);
      toast.error("Failed to update student: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteStudent = (studentId) => {
    setConfirmConfig({
      isOpen: true,
      title: 'Delete Student',
      message: `Are you sure you want to delete student ${studentId}?`,
      onConfirm: async () => {
        setConfirmConfig(prev => ({ ...prev, isOpen: false }));
        setIsSaving(true);
        try {
          const batchCommit = writeBatch(db);
          const studentDocPath = `${selectedProgramme.id}(${enrollmentBatch.batchName})`;

      // Delete from nested collection
      batchCommit.delete(doc(db, 'Students', studentDocPath, 'Student IDs', studentId));
      // Delete from flat collection
      batchCommit.delete(doc(db, 'AllStudents', studentId));

      await batchCommit.commit();
      await logActivity("Removed Student", `${studentId} from Batch ${enrollmentBatch.batchName}`);

      // Update local state
      setExistingStudents(prev => prev.filter(s => s.studentId !== studentId));
      toast.success("Student deleted successfully.")
    } catch (error) {
      console.error("Error deleting student:", error);
      toast.error("Failed to delete student: " + error.message)
    } finally {
      setIsSaving(false);
    }
      }
    });
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

        // Detect if we are in a Modules step
        const isModuleStep = (view === 'wizard' && wizardStep === 5) || (isAddBatchModalOpen && addBatchWizardStep === 2);

        if (isModuleStep) {
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
          const parsedModules = data.map(row => {
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
              credits: String(rawCredits || '3').trim()
            };
          }).filter(m => m.moduleCode && !m.moduleCode.toLowerCase().startsWith('total') && !m.moduleName.toLowerCase().startsWith('total'));

          if (parsedModules.length === 0) {
            toast.error((t) => (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <span><b>Error:</b> No valid modules found. Upload the Expected columns format include: Semester ID, Module Code, Module Name, Credits.</span>
                <button 
                  onClick={() => toast.dismiss(t.id)}
                  style={{ alignSelf: 'flex-end', padding: '6px 16px', background: '#ef4444', color: 'white', borderRadius: '6px', border: 'none', cursor: 'pointer', fontWeight: '500' }}
                >
                  OK
                </button>
              </div>
            ), { duration: Infinity });
          } else {
            setModuleList(parsedModules);
            toast.success(`Successfully parsed ${parsedModules.length} modules.`);
          }
        } else {
          const parsedStudents = data.map(row => ({
            studentId: String(getValueByKeys(row, ['studentId', 'studentid', 'student id', 'student_id', 'id', 'student no', 'studentno', 'student_no'])).trim(),
            fullName: String(getValueByKeys(row, ['fullName', 'fullname', 'full name', 'full_name', 'name', 'studentname', 'student name'])).trim(),
            email: String(getValueByKeys(row, ['email', 'Email', 'University Email', 'university email', 'universityemail', 'university_email', 'universityEmail', 'UniversityEmail', 'email address', 'emailaddress', 'email_address', 'mail'])).trim(),
            password: String(getValueByKeys(row, ['password', 'pass']) || '123456').trim(),
            status: String(getValueByKeys(row, ['status']) || 'active').toLowerCase().trim()
          })).filter(s => s.studentId);
          if (parsedStudents.length === 0) {
            toast.error((t) => (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <span><b>Error:</b> No valid students found. Upload the Expected columns format include: Student ID, Full Name, Email.</span>
                <button 
                  onClick={() => toast.dismiss(t.id)}
                  style={{ alignSelf: 'flex-end', padding: '6px 16px', background: '#ef4444', color: 'white', borderRadius: '6px', border: 'none', cursor: 'pointer', fontWeight: '500' }}
                >
                  OK
                </button>
              </div>
            ), { duration: Infinity });
          } else {
            setStudentList(parsedStudents);
            toast.success(`Successfully parsed ${parsedStudents.length} students.`);
          }
        }
        
        // Clear the file input so the same file can be selected again
        e.target.value = '';
      } catch (error) {
        console.error("Error parsing file:", error);
        toast.error("Failed to parse file. Please ensure it is a valid Excel or CSV.")
      }
    };
    reader.readAsBinaryString(file);
  };

  const handleUpdateProgramme = async () => {
    if(!editingProg.id || !editingProg.name || !editingProg.duration) {
      toast.error("Please fill all fields")
      return;
    }
    setIsSaving(true);
    try {
      // Use docId to update the exact Firestore document, fallback to id if docId is missing (e.g. from HMR state)
      const targetDocId = editingProg.docId || editingProg.id;
      await setDoc(doc(db, 'Degrees', targetDocId), {
        id: editingProg.id,
        name: editingProg.name,
        duration: editingProg.duration,
        status: editingProg.status || 'Active'
      }, { merge: true });
      
      toast.success("Programme updated successfully!")
      
      // Update local state manually to ensure the table reflects changes even if cache is slightly behind
      setProgrammes(prev => prev.map(p => 
        (p.docId === targetDocId || p.id === targetDocId) 
        ? { ...p, name: editingProg.name, duration: editingProg.duration } 
        : p
      ));

      setView('list');
      setEditingProg(null);
    } catch (error) {
      console.error("Error updating programme:", error);
      toast.error("Failed to update programme: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleNextStep = () => {
    if (wizardStep === 1) {
      if (!newProgId || !newProgName || newProgDuration === 'N/A') {
        toast.error("Please fill in all core details (ID, Name, Duration) in Step 1.");
        return;
      }
      if (newProgId.includes('/')) {
        toast.error("Programme ID cannot contain slashes ('/'). Use hyphens instead.");
        return;
      }
    } else if (wizardStep === 2) {
      if (!newBatchId || !newBatchName || !newBatchIntakeYear) {
        toast.error("Please provide a Batch Name, Batch ID, and Intake Year to continue.");
        return;
      }
      if (newBatchId.includes('/') || newBatchName.includes('/')) {
        toast.error("Batch ID and Name cannot contain slashes ('/'). Use hyphens instead.");
        return;
      }
    } else if (wizardStep === 4) {
      if (studentList.length === 0) {
        toast.error("Please enroll at least one student before continuing.");
        return;
      }
    }
    setWizardStep(prev => prev + 1);
  };

  const handleCompleteSetup = async () => {
    if(!newProgId || !newProgName || newProgDuration === 'N/A') {
      toast.error("Programme ID, Name, and Duration are required!")
      return;
    }
    if (newProgId.includes('/')) {
      toast.error("Programme ID cannot contain slashes ('/'). Use hyphens instead.");
      return;
    }
    if (!newBatchId || !newBatchName || !newBatchIntakeYear) {
      toast.error("Batch ID, Name, and Intake Year are required!");
      return;
    }
    if (newBatchId.includes('/') || newBatchName.includes('/')) {
      toast.error("Batch ID and Name cannot contain slashes ('/'). Use hyphens instead.");
      return;
    }
    if (studentList.length === 0) {
      toast.error("Please enroll at least one student before completing setup.");
      return;
    }
    if (moduleList.length === 0) {
      toast.error("Please add at least one module before completing setup.");
      return;
    }
    setIsSaving(true);
    try {
      // Check for duplicate Student IDs system-wide if list is provided
      if (studentList.length > 0) {
        const newIds = studentList.map(s => s.studentId);
        const existingIds = await checkStudentIdsUnique(newIds);
        if (existingIds.length > 0) {
          toast.error(`Setup Blocked: Duplicate Student IDs Detected (v2)!\n\nIDs already in use: ${existingIds.join(', ')}.\n\nPlease fix the student list in Step 4 before completing setup.`);
          setIsSaving(false);
          return;
        }
      }

      // Check if Programme ID is already taken
      const progDoc = await getDoc(doc(db, 'Degrees', newProgId));
      if (progDoc.exists()) {
        toast.error(`Programme ID "${newProgId}" is already taken! Please use a different ID.`)
        setIsSaving(false);
        return;
      }

      const batchCommit = writeBatch(db);

      // 1. Save Programme
      batchCommit.set(doc(db, 'Degrees', newProgId), {
        id: newProgId,
        name: newProgName,
        duration: newProgDuration,
        status: 'Active'
      });

      // 2. Save Initial Batch if provided
      if (newBatchId && newBatchName) {
        const batchDocPath = `${newProgId}(${newBatchName})`;
        const batchPayload = {
          programId: newProgId,
          batchId: newBatchId,
          batchName: newBatchName,
          intakeYear: newBatchIntakeYear
        };
        
        batchCommit.set(doc(db, 'Batches', batchDocPath), batchPayload);
        batchCommit.set(doc(db, 'Degrees', newProgId, 'Batches', batchDocPath), batchPayload);

        // 3. Auto-generate and save semesters
        const generatedSemesters = generateSemesters(newProgDuration);
        for (const sem of generatedSemesters) {
          const semDocPath = `${batchDocPath}_${sem.id}`;
          batchCommit.set(doc(db, 'Degrees', newProgId, 'Semesters', semDocPath), {
            degreeId: newProgId,
            batchId: newBatchId,
            semesterId: sem.id,
            academicYear: sem.academicYear,
            semesterNo: sem.semesterNo,
            name: `${sem.academicYear} ${sem.semesterNo}`
          });
        }

        // 4. Save Students if provided
        for (const student of studentList) {
          const hashedPassword = await hashPassword(student.password);
          const studentData = {
            studentId: student.studentId,
            fullName: student.fullName,
            email: student.email,
            status: student.status,
            batchId: newBatchId,
            hashed_password: hashedPassword,
            initial_password: student.password,
            isFirstLogin: true
          };
          const safeStudentId = student.studentId.replace(/\//g, '-');
          batchCommit.set(doc(db, 'Students', batchDocPath, 'Student IDs', safeStudentId), studentData);
          batchCommit.set(doc(db, 'AllStudents', safeStudentId), studentData);
        }

        // 5. Save Modules
        for (const mod of moduleList) {
          const safeModCode = mod.moduleCode.replace(/\//g, '-');
          const modDocPath = `${batchDocPath}_${mod.semesterId}_${safeModCode}`;
          batchCommit.set(doc(db, 'Degrees', newProgId, 'Modules', modDocPath), {
            degreeId: newProgId,
            batchId: newBatchId,
            semesterId: mod.semesterId,
            moduleCode: mod.moduleCode,
            moduleName: mod.moduleName,
            credits: mod.credits
          });
        }
      }

      await batchCommit.commit();
      await logActivity("Created Programme", `${newProgId}: ${newProgName}`);
      if (newBatchId) {
        await logActivity("Created Batch", `${newBatchId}: ${newBatchName} for ${newProgId}`);
      }
      toast.success("Programme setup completed successfully!")

      // Refresh and reset
      await fetchProgrammes();
      setNewProgId('');
      setNewProgName('');
      setNewBatchId('');
      setNewBatchName('');
      setNewBatchIntakeYear('');
      setStudentList([]);
      setModuleList([]);
      setView('list');
    } catch (error) {
      console.error("Error saving setup:", error);
      toast.error("Failed to complete setup: " + error.message)
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteProgramme = (docId, displayId) => {
    setConfirmConfig({
      isOpen: true,
      title: 'Delete Programme',
      message: `Are you sure you want to delete programme ${displayId}? This will also delete all associated batches, semesters, and modules. This action cannot be undone.`,
      onConfirm: async () => {
        setConfirmConfig(prev => ({ ...prev, isOpen: false }));
        try {
          const targetDocId = docId || displayId;
          const progId = displayId;
          
          const deletePromises = [];

          // 1. Fetch and delete top-level Batches, and their Students
          const batchesQ = query(collection(db, 'Batches'), where('programId', '==', progId));
          const batchesSnap = await getDocs(batchesQ);
          for (const bDoc of batchesSnap.docs) {
             const bData = bDoc.data();
             deletePromises.push(deleteDoc(bDoc.ref));
             
             // Fetch and delete students for this batch
             if (bData.batchId) {
                 const studentsQ = query(collection(db, 'AllStudents'), where('batchId', '==', bData.batchId));
                 const studentsSnap = await getDocs(studentsQ);
                 const safeProgId = String(bData.programId || progId).replace(/\//g, '-');
                 const safeBatchName = String(bData.batchName || '').replace(/\//g, '-');
                 const batchDocPathFlat = `${safeProgId}(${safeBatchName})`;
                 
                 studentsSnap.forEach(sDoc => {
                    deletePromises.push(deleteDoc(sDoc.ref)); // flat
                    deletePromises.push(deleteDoc(doc(db, 'Students', batchDocPathFlat, 'Student IDs', sDoc.id))); // nested
                 });
             }
          }

          // 2. Fetch and delete Degrees/{targetDocId}/Batches
          const subBatchesSnap = await getDocs(collection(db, 'Degrees', targetDocId, 'Batches'));
          subBatchesSnap.forEach(doc => deletePromises.push(deleteDoc(doc.ref)));

          // 3. Fetch and delete Degrees/{targetDocId}/Semesters
          const subSemsSnap = await getDocs(collection(db, 'Degrees', targetDocId, 'Semesters'));
          subSemsSnap.forEach(doc => deletePromises.push(deleteDoc(doc.ref)));

          // 4. Fetch and delete Degrees/{targetDocId}/Modules
          const subModsSnap = await getDocs(collection(db, 'Degrees', targetDocId, 'Modules'));
          subModsSnap.forEach(doc => deletePromises.push(deleteDoc(doc.ref)));

          // 5. Delete the main Degree document
          deletePromises.push(deleteDoc(doc(db, 'Degrees', targetDocId)));

        await Promise.all(deletePromises);
        await logActivity("Deleted Programme", `${displayId} (and all associated data)`);

        // Update local state
        setProgrammes(prev => prev.filter(p => p.docId !== targetDocId && p.id !== targetDocId));
        toast.success(`Programme ${displayId} and all associated data deleted successfully.`)
        
      } catch (error) {
        console.error("Error during cascading delete:", error);
        toast.error("Failed to delete programme and its data: " + error.message)
      }
    }});
  };

  const filteredProgrammes = programmes.filter(prog => {
    const matchesSearch = prog.id.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          prog.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'All' || prog.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const itemsPerPage = 5;
  const totalPages = Math.ceil(filteredProgrammes.length / itemsPerPage) || 1;
  const currentProgrammes = filteredProgrammes.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  const getHeaderTitle = () => {
    switch (initialTab) {
      case 'batches':
        return {
          title: 'Batch Management',
          subtitle: 'Select a programme below to view, add, or manage academic batches.'
        };
      case 'semesters':
        return {
          title: 'Semester Management',
          subtitle: 'Select a programme below to configure and organize semesters.'
        };
      case 'modules':
        return {
          title: 'Module Management',
          subtitle: 'Select a programme below to customize, review, or import modules.'
        };
      default:
        return {
          title: 'Programme Management',
          subtitle: 'Manage all academic programmes, batches, semesters, and modules.'
        };
    }
  };

  const renderProgrammeList = () => {
    const headerInfo = getHeaderTitle();
    return (
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
            <h1 className="page-title">{headerInfo.title}</h1>
            <p className="page-subtitle">{headerInfo.subtitle}</p>
          </div>
          <button className="btn-primary" onClick={handleStartWizard}>
            <Plus size={18} />
            Add New Programme
          </button>
        </div>

      <div className="glass-panel table-container">
        <div className="table-controls">
          <div className="search-box">
            <Search size={18} color="var(--text-muted)" />
            <input 
              type="text" 
              placeholder="Search by ID or Name..." 
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="filter-group">
            <select 
              className="filter-btn" 
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              style={{ outline: 'none', cursor: 'pointer', appearance: 'none', paddingRight: '32px', backgroundImage: 'url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%236C7B8E%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E")', backgroundRepeat: 'no-repeat', backgroundPosition: 'right 12px center', backgroundSize: '12px auto' }}
            >
              <option value="All">All Statuses</option>
              <option value="Active">Active</option>
              <option value="Warning">Warning</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>
        </div>

        <table className="modern-table">
          <thead>
            <tr>
              <th>Programme ID</th>
              <th>Programme Name</th>
              <th>Duration</th>
              <th>Total Batches</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  Loading Programmes...
                </td>
              </tr>
            ) : currentProgrammes.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  No Programmes match your search.
                </td>
              </tr>
            ) : currentProgrammes.map((prog, i) => (
              <motion.tr 
                key={prog.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
              >
                <td className="id-cell">{prog.id}</td>
                <td className="name-cell">{prog.name}</td>
                <td>{prog.duration}</td>
                <td>
                  <span className="count-badge">{prog.batches} Batches</span>
                </td>
                <td>
                  <span className={`status-pill ${prog.status.toLowerCase()}`}>
                    {prog.status}
                  </span>
                </td>
                <td>
                  <div className="action-btns">
                    <button className="icon-action-btn" title="Open Details Hub" onClick={() => handleOpenDetails(prog)}>
                      <Settings size={18} />
                    </button>
                    <button className="icon-action-btn edit" title="Edit Programme" onClick={() => handleOpenEdit(prog)}>
                      <Edit2 size={18} />
                    </button>
                    <button className="icon-action-btn delete" title="Delete Programme" onClick={() => handleDeleteProgramme(prog.docId, prog.id)}>
                      <Trash2 size={18} />
                    </button>
                  </div>
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>

        <div className="table-footer">
          <p>Showing {currentProgrammes.length} of {filteredProgrammes.length} programmes</p>
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
  );
};

  const renderWizard = () => (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className="view-container wizard-container"
    >
      <div className="wizard-header">
        <button className="btn-back" onClick={() => setView('list')}>
          <ArrowLeft size={18} /> Back to Programmes
        </button>
        <h2>Programme Setup Wizard</h2>
        <p>Follow the steps to configure a new academic structure.</p>
      </div>

      <div className="wizard-stepper">
        {['Details', 'Batches', 'Semesters', 'Students', 'Modules'].map((stepName, i) => {
          const stepNum = i + 1;
          const isActive = wizardStep === stepNum;
          const isCompleted = wizardStep > stepNum;
          
          return (
            <div key={stepNum} className={`stepper-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}>
              <div className="step-circle">
                {isCompleted ? <Check size={16} /> : stepNum}
              </div>
              <span className="step-label">{stepName}</span>
              {stepNum < 5 && <div className="step-line" />}
            </div>
          );
        })}
      </div>

      <div className="wizard-content-panel glass-panel">
        {wizardStep === 1 && (
          <div className="step-content">
            <h3>Step 1: Core Details</h3>
            <div className="form-group">
              <label>Programme ID *</label>
              <input type="text" placeholder="e.g. BSc-IT" className="form-input" value={newProgId} onChange={e => setNewProgId(e.target.value)} />
            </div>
            <div className="form-group">
              <label>Programme Name *</label>
              <input type="text" placeholder="e.g. B.Sc. in Information Technology" className="form-input" value={newProgName} onChange={e => setNewProgName(e.target.value)} />
            </div>
            <div className="form-group">
              <label>Duration (Years)</label>
              <select className="form-input" value={newProgDuration} onChange={e => setNewProgDuration(e.target.value)}>
                <option value="N/A" disabled>Select Duration</option>
                <option value="3 Years">3 Years</option>
                <option value="4 Years">4 Years</option>
              </select>
            </div>
          </div>
        )}

        {wizardStep === 2 && (
          <div className="step-content">
            <h3>Step 2: Add Initial Batches</h3>
            <p className="text-muted mb-4">You can add the first batch for this programme now. More can be added later from the Details Hub.</p>
            <div className="form-group">
              <label>Batch Name / Intake</label>
              <input type="text" placeholder="e.g. 2026 Intake" className="form-input" value={newBatchName} onChange={e => setNewBatchName(e.target.value)} />
            </div>
            <div className="form-group">
              <label>Batch ID</label>
              <input type="text" placeholder="e.g. BATCH-26" className="form-input" value={newBatchId} onChange={e => setNewBatchId(e.target.value)} />
            </div>
            <div className="form-group">
              <label>Intake Year</label>
              <input type="number" placeholder="e.g. 2026" className="form-input" value={newBatchIntakeYear} onChange={e => setNewBatchIntakeYear(e.target.value)} />
            </div>
          </div>
        )}

        {wizardStep === 3 && (
          <div className="step-content">
            <h3>Step 3: Semester Configuration</h3>
            <p className="text-muted mb-4">Based on your selection of <strong>{newProgDuration}</strong>, the following <strong>{parseInt(newProgDuration.charAt(0))*2} semesters</strong> will be automatically generated and linked to the initial batch ({newBatchId || 'Pending Batch'}).</p>
            
            <div className="semester-preview-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginTop: '20px' }}>
              {generateSemesters(newProgDuration).map((sem, idx) => (
                <div key={idx} style={{ padding: '12px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', fontWeight: '600', color: '#1e293b', fontSize: '0.9rem' }}>
                  {sem.id}
                </div>
              ))}
            </div>
            {!newBatchId && (
              <p style={{ color: '#ef4444', fontSize: '0.85rem', marginTop: '16px' }}>Note: Semesters will only be saved if an Initial Batch is provided in Step 2.</p>
            )}
          </div>
        )}

        {wizardStep === 4 && (
          <div className="step-content">
            <h3>Step 4: Student Enrollment</h3>
            <p className="text-muted mb-4">Enroll students into <strong>{newBatchId || 'the initial batch'}</strong>. You can upload a file or enter them manually.</p>
            
            <div className="modal-tabs" style={{ display: 'flex', gap: '20px', borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
              <button onClick={() => setStudentTab('upload')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: studentTab === 'upload' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: studentTab === 'upload' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Bulk Upload</button>
              <button onClick={() => setStudentTab('single')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: studentTab === 'single' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: studentTab === 'single' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Manual Entry</button>
            </div>

            {studentTab === 'upload' ? (
              <div className="upload-section glass-panel" style={{ border: '2px dashed var(--border-color)', padding: '40px', textAlign: 'center', marginBottom: '24px' }}>
                <input type="file" id="student-upload" hidden accept=".xlsx, .xls, .csv" onChange={handleFileUpload} />
                <label htmlFor="student-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                  <div style={{ padding: '16px', background: 'var(--shape-light)', borderRadius: '50%', color: 'var(--accent)' }}>
                    <Upload size={32} />
                  </div>
                  <div>
                    <p style={{ fontWeight: '700', marginBottom: '4px' }}>Click to upload student list</p>
                    <p className="text-muted" style={{ fontSize: '0.85rem' }}>Supports .xlsx, .xls, and .csv</p>
                  </div>
                </label>
              </div>
            ) : (
              <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr 120px', gap: '12px', marginBottom: '24px' }}>
                <input 
                  type="text" 
                  placeholder="Student ID *" 
                  className="form-input" 
                  style={{ padding: '10px' }} 
                  value={manualStudent.studentId} 
                  onChange={e => setManualStudent({...manualStudent, studentId: e.target.value})} 
                />
                <input 
                  type="text" 
                  placeholder="Full Name *" 
                  className="form-input" 
                  style={{ padding: '10px' }} 
                  value={manualStudent.fullName} 
                  onChange={e => setManualStudent({...manualStudent, fullName: e.target.value})} 
                />
                <input 
                  type="email" 
                  placeholder="Email *" 
                  className="form-input" 
                  style={{ padding: '10px' }} 
                  value={manualStudent.email} 
                  onChange={e => setManualStudent({...manualStudent, email: e.target.value})} 
                />
                <input 
                  type="password" 
                  placeholder="Password" 
                  className="form-input" 
                  style={{ padding: '10px' }} 
                  value={manualStudent.password} 
                  onChange={e => setManualStudent({...manualStudent, password: e.target.value})} 
                />
                <select 
                  className="form-input" 
                  style={{ padding: '10px' }} 
                  value={manualStudent.status} 
                  onChange={e => setManualStudent({...manualStudent, status: e.target.value})}
                >
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                  <option value="warning">Warning</option>
                </select>
                <button 
                  className="btn-primary" 
                  style={{ gridColumn: 'span 5' }} 
                  onClick={() => {
                    if (manualStudent.studentId && manualStudent.fullName && manualStudent.email) {
                      // Check duplicate locally
                      if (studentList.some(s => s.studentId === manualStudent.studentId)) {
                        toast.error("Duplicate Student ID detected locally!")
                        return;
                      }
                      setStudentList([...studentList, {
                        studentId: manualStudent.studentId,
                        fullName: manualStudent.fullName,
                        email: manualStudent.email,
                        password: manualStudent.password || '123456',
                        status: manualStudent.status || 'active'
                      }]);
                      setManualStudent({ studentId: '', fullName: '', email: '', password: 'password123', status: 'active' });
                    } else {
                      toast.error("Please fill in Student ID, Full Name, and Email.")
                    }
                  }}
                >
                  Add Student
                </button>
              </div>
            )}

            {studentList.length > 0 && (
              <div className="preview-table-wrapper" style={{ maxHeight: '300px', overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                  <thead style={{ position: 'sticky', top: 0, background: 'white' }}>
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
                    {studentList.map((s, idx) => (
                      <tr key={idx}>
                        <td className="id-cell">{s.studentId}</td>
                        <td>{s.fullName}</td>
                        <td>{s.email}</td>
                        <td>
                          <code style={{ fontSize: '0.85rem', color: 'var(--primary)' }}>{s.password}</code>
                        </td>
                        <td>
                          <span className={`status-pill ${s.status}`}>
                            {s.status}
                          </span>
                        </td>
                        <td>
                          <button className="icon-action-btn delete" onClick={() => setStudentList(studentList.filter((_, i) => i !== idx))}>
                            <X size={14} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {wizardStep === 5 && (
          <div className="step-content">
            <h3>Step 5: Module Configuration</h3>
            <p className="text-muted mb-4">Add modules for the initial batch. You can upload a file or enter them manually.</p>
            
            <div className="modal-tabs" style={{ display: 'flex', gap: '20px', borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
              <button onClick={() => setEnrollmentTab('upload')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'upload' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'upload' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Bulk Upload</button>
              <button onClick={() => setEnrollmentTab('single')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'single' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'single' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Manual Entry</button>
            </div>

            {enrollmentTab === 'upload' ? (
              <div className="upload-section glass-panel" style={{ border: '2px dashed var(--border-color)', padding: '32px', textAlign: 'center', marginBottom: '24px', borderRadius: '16px', background: 'rgba(0,0,0,0.02)' }}>
                <input type="file" id="module-upload" hidden accept=".xlsx, .xls, .csv" onChange={handleFileUpload} />
                <label htmlFor="module-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                  <Upload size={32} color="var(--accent)" />
                  <p style={{ fontWeight: '700' }}>Click to upload module Excel/CSV file</p>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: SemesterID, ModuleCode (or Module ID), ModuleName, Credits</p>
                </label>
              </div>
            ) : (
              <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 100px', gap: '12px', marginBottom: '24px' }}>
                <select className="form-input" style={{ padding: '10px' }} value={manualModule.semesterId} onChange={e => setManualModule({...manualModule, semesterId: e.target.value})}>
                  <option value="">Select Sem</option>
                  {generateSemesters(newProgDuration).map(s => <option key={s.id} value={s.id}>{s.id}</option>)}
                </select>
                <input type="text" placeholder="Code" className="form-input" style={{ padding: '10px' }} value={manualModule.moduleCode} onChange={e => setManualModule({...manualModule, moduleCode: e.target.value})} />
                <input type="text" placeholder="Name" className="form-input" style={{ padding: '10px' }} value={manualModule.moduleName} onChange={e => setManualModule({...manualModule, moduleName: e.target.value})} />
                <input type="number" placeholder="Cred" className="form-input" style={{ padding: '10px' }} value={manualModule.credits} onChange={e => setManualModule({...manualModule, credits: e.target.value})} />
                <button className="btn-primary" style={{ gridColumn: 'span 4' }} onClick={() => {
                   if(manualModule.moduleCode && manualModule.semesterId) {
                     setModuleList([...moduleList, {...manualModule}]);
                     setManualModule({ semesterId: '', moduleCode: '', moduleName: '', credits: '' });
                   } else {
                     toast.error("Fill required module fields.")
                   }
                }}>Add Module</button>
              </div>
            )}

            {moduleList.length > 0 && (
              <div className="preview-table-wrapper" style={{ maxHeight: '250px', overflowY: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                  <thead><tr><th>Sem</th><th>Code</th><th>Name</th><th>Credits</th><th>Actions</th></tr></thead>
                  <tbody>
                    {moduleList.map((m, idx) => (
                      <tr key={idx}><td>{m.semesterId}</td><td>{m.moduleCode}</td><td>{m.moduleName}</td><td>{m.credits}</td><td><button className="icon-action-btn delete" onClick={() => setModuleList(moduleList.filter((_, i) => i !== idx))}><X size={14} /></button></td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="wizard-actions">
          {wizardStep > 1 && (
            <button className="btn-secondary" onClick={() => setWizardStep(prev => prev - 1)} disabled={isSaving}>
              Previous
            </button>
          )}
          <div className="spacer" />
          {wizardStep < 5 ? (
            <button className="btn-primary" onClick={handleNextStep}>
              Next Step <ChevronRight size={18} />
            </button>
          ) : (
            <button className="btn-success" onClick={handleCompleteSetup} disabled={isSaving}>
              <Check size={18} /> {isSaving ? 'Saving...' : 'Complete Setup'}
            </button>
          )}
        </div>
    </motion.div>
  );

  const renderDetailsHub = () => {
    const TabIcon = [
      { id: 'batches', icon: Users },
      { id: 'semesters', icon: Calendar },
      { id: 'modules', icon: Box },
    ].find(t => t.id === activeTab)?.icon || Box;

    return (
      <motion.div 
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        className="view-container"
      >
        <div className="hub-header glass-panel">
          <button className="btn-back" onClick={() => setView('list')}>
            <ArrowLeft size={18} /> Back to List
          </button>
          <div className="hub-title-row">
            <div>
              <h1>{selectedProgramme?.name}</h1>
              <span className="hub-id-badge">{selectedProgramme?.id}</span>
            </div>
            {activeTab === 'batches' && (
              <button className="btn-primary" onClick={() => setIsAddBatchModalOpen(true)}>
                <Plus size={18} /> Add Batch
              </button>
            )}
            {activeTab === 'semesters' && (
              <button className="btn-primary" onClick={() => toast.error("Please add semesters by creating a new batch or using the edit options.")}>
                <Plus size={18} /> Add Semester
              </button>
            )}
             {activeTab === 'modules' && (
              <button className="btn-primary" onClick={() => toast.error("Please add modules via the edit icon in the modules table.")}>
                <Plus size={18} /> Add Module
              </button>
            )}
          </div>

          <div className="hub-tabs">
            {[
              { id: 'batches', label: 'Batches', icon: Users },
              { id: 'semesters', label: 'Semesters', icon: Calendar },
              { id: 'modules', label: 'Modules', icon: Box },
            ].map(tab => (
              <button 
                key={tab.id}
                className={`hub-tab ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                <tab.icon size={18} /> {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="hub-content glass-panel">
          {hubLoading ? (
            <div className="loading-state">
              <div className="loader" />
              <p>Fetching {activeTab}...</p>
            </div>
          ) : (
            <div className="hub-table-wrapper">
              {/* Filter Controls for Semesters and Modules */}
              {activeTab === 'semesters' && hubBatches.length > 0 && (
                <div className="hub-filter-row">
                  <label>Filter by Batch:</label>
                  <select className="filter-select" value={hubFilterBatch} onChange={e => setHubFilterBatch(e.target.value)}>
                    <option value="All">All Batches</option>
                    {hubBatches.map(b => <option key={b.batchId} value={b.batchId}>{b.batchName} ({b.batchId})</option>)}
                  </select>
                </div>
              )}

              {activeTab === 'modules' && (
                <div className="hub-filter-row">
                  <div className="filter-item">
                    <label>Batch:</label>
                    <select className="filter-select" value={hubFilterBatch} onChange={e => setHubFilterBatch(e.target.value)}>
                      <option value="All">All Batches</option>
                      {hubBatches.map(b => <option key={b.batchId} value={b.batchId}>{b.batchName} ({b.batchId})</option>)}
                    </select>
                  </div>
                  <div className="filter-item">
                    <label>Semester:</label>
                    <select className="filter-select" value={hubFilterSemester} onChange={e => setHubFilterSemester(e.target.value)}>
                      <option value="All">All Semesters</option>
                      {/* Unique semesters across filtered batches or all */}
                      {[...new Set(hubSemesters.filter(s => hubFilterBatch === 'All' || s.batchId === hubFilterBatch).map(s => s.semesterId))].map(sId => (
                        <option key={sId} value={sId}>{sId}</option>
                      ))}
                    </select>
                  </div>
                </div>
              )}

              <table className="modern-table">
                {activeTab === 'batches' && (
                  <>
                    <thead>
                      <tr>
                        <th>Batch ID</th>
                        <th>Batch Name</th>
                        <th>Intake Year</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {hubBatches.length === 0 ? (
                        <tr><td colSpan="4" className="text-center">No batches found.</td></tr>
                      ) : hubBatches.map(batch => (
                        <tr key={batch.docId}>
                          {editingHubItemId === batch.docPath ? (
                            <>
                              <td className="id-cell">
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.batchId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, batchId: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.batchName || ''} onChange={e => setEditingHubItemData({...editingHubItemData, batchName: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.intakeYear || ''} onChange={e => setEditingHubItemData({...editingHubItemData, intakeYear: e.target.value})} />
                              </td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={handleSaveHubItem} title="Save"><Check size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={cancelEditHubItem} title="Cancel"><X size={16} /></button>
                                </div>
                              </td>
                            </>
                          ) : (
                            <>
                              <td className="id-cell">{batch.batchId}</td>
                              <td>{batch.batchName}</td>
                              <td>{batch.intakeYear}</td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={() => handleEditHubItem('batches', batch)} title="Edit"><Edit2 size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={() => handleDeleteHubItem('batches', batch)} title="Delete"><Trash2 size={16} /></button>
                                  <button className="icon-action-btn" style={{ color: 'var(--accent)' }} onClick={() => handleOpenEnrollment(batch)} title="Manage Students"><Users size={16} /></button>
                                </div>
                              </td>
                            </>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </>
                )}

                {activeTab === 'semesters' && (
                  <>
                    <thead>
                      <tr>
                        <th>Batch</th>
                        <th>Semester ID</th>
                        <th>Year</th>
                        <th>Sem No</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {hubSemesters.filter(s => {
                        const bMatch = hubFilterBatch === 'All' || 
                          String(s.batchId || '').trim().toLowerCase() === hubFilterBatch.trim().toLowerCase();
                        return bMatch;
                      }).length === 0 ? (
                        <tr><td colSpan="5" className="text-center">No semesters match this filter.</td></tr>
                      ) : hubSemesters.filter(s => {
                        const bMatch = hubFilterBatch === 'All' || 
                          String(s.batchId || '').trim().toLowerCase() === hubFilterBatch.trim().toLowerCase();
                        return bMatch;
                      }).map(sem => (
                        <tr key={sem.docId}>
                          {editingHubItemId === sem.docPath ? (
                            <>
                              <td className="id-cell">
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.batchId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, batchId: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.semesterId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, semesterId: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.academicYear || ''} onChange={e => setEditingHubItemData({...editingHubItemData, academicYear: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', width: '60px'}} value={editingHubItemData.semesterNo || ''} onChange={e => setEditingHubItemData({...editingHubItemData, semesterNo: e.target.value})} />
                              </td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={handleSaveHubItem} title="Save"><Check size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={cancelEditHubItem} title="Cancel"><X size={16} /></button>
                                </div>
                              </td>
                            </>
                          ) : (
                            <>
                              <td className="id-cell">{sem.batchId}</td>
                              <td>{sem.semesterId}</td>
                              <td>{sem.academicYear}</td>
                              <td>{sem.semesterNo}</td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={() => handleEditHubItem('semesters', sem)} title="Edit"><Edit2 size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={() => handleDeleteHubItem('semesters', sem)} title="Delete"><Trash2 size={16} /></button>
                                </div>
                              </td>
                            </>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </>
                )}

                {activeTab === 'modules' && (
                  <>
                    <thead>
                      <tr>
                        <th>Batch / Sem</th>
                        <th>Module Code</th>
                        <th>Module Name</th>
                        <th>Credits</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {hubModules.filter(m => {
                        const selectedBatchObj = hubBatches.find(b => b.batchId === hubFilterBatch);
                        const selectedSemObj = hubSemesters.find(s => s.semesterId === hubFilterSemester);

                        const normalize = (str) => String(str || '').replace(/[^a-z0-9]/gi, '').toLowerCase();

                        const mBatch = normalize(m.batchId);
                        const bMatch = hubFilterBatch === 'All' || 
                          mBatch === normalize(hubFilterBatch) ||
                          (selectedBatchObj && mBatch === normalize(selectedBatchObj.batchName));
                        
                        const mSem = normalize(m.semesterId);
                        const sMatch = hubFilterSemester === 'All' || 
                          mSem === normalize(hubFilterSemester) ||
                          (selectedSemObj && (
                            mSem === normalize(selectedSemObj.semesterNo) ||
                            mSem === normalize(selectedSemObj.name) ||
                            mSem === normalize(`${selectedSemObj.academicYear}${selectedSemObj.semesterNo}`)
                          ));
                        
                        return bMatch && sMatch;
                      }).length === 0 ? (
                        <tr>
                          <td colSpan="5" className="text-center">
                            No modules match this filter.
                            {hubModules.length > 0 && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--accent)', marginTop: '8px', opacity: 0.8 }}>
                                Found {hubModules.length} total modules for this degree. 
                                <br/>Example raw data: Batch="{hubModules[0].batchId}", Semester="{hubModules[0].semesterId}"
                              </div>
                            )}
                          </td>
                        </tr>
                      ) : hubModules.filter(m => {
                        const selectedBatchObj = hubBatches.find(b => b.batchId === hubFilterBatch);
                        const selectedSemObj = hubSemesters.find(s => s.semesterId === hubFilterSemester);

                        const normalize = (str) => String(str || '').replace(/[^a-z0-9]/gi, '').toLowerCase();

                        const mBatch = normalize(m.batchId);
                        const bMatch = hubFilterBatch === 'All' || 
                          mBatch === normalize(hubFilterBatch) ||
                          (selectedBatchObj && mBatch === normalize(selectedBatchObj.batchName));
                        
                        const mSem = normalize(m.semesterId);
                        const sMatch = hubFilterSemester === 'All' || 
                          mSem === normalize(hubFilterSemester) ||
                          (selectedSemObj && (
                            mSem === normalize(selectedSemObj.semesterNo) ||
                            mSem === normalize(selectedSemObj.name) ||
                            mSem === normalize(`${selectedSemObj.academicYear}${selectedSemObj.semesterNo}`)
                          ));
                        
                        return bMatch && sMatch;
                      }).map(mod => (
                        <tr key={mod.docId}>
                          {editingHubItemId === mod.docPath ? (
                            <>
                              <td className="id-cell" style={{ fontSize: '0.8rem' }}>
                                <div style={{display:'flex', flexDirection:'column', gap:'4px'}}>
                                  <input type="text" placeholder="Batch ID" className="form-input" style={{padding: '2px', fontSize: '0.8rem'}} value={editingHubItemData.batchId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, batchId: e.target.value})} />
                                  <input type="text" placeholder="Semester ID" className="form-input" style={{padding: '2px', fontSize: '0.8rem'}} value={editingHubItemData.semesterId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, semesterId: e.target.value})} />
                                </div>
                              </td>
                              <td className="id-cell">
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem', width: '80px'}} value={editingHubItemData.moduleId || ''} onChange={e => setEditingHubItemData({...editingHubItemData, moduleId: e.target.value})} />
                              </td>
                              <td>
                                <input type="text" className="form-input" style={{padding: '4px', fontSize: '0.9rem'}} value={editingHubItemData.moduleName || ''} onChange={e => setEditingHubItemData({...editingHubItemData, moduleName: e.target.value})} />
                              </td>
                              <td>
                                <input type="number" className="form-input" style={{padding: '4px', fontSize: '0.9rem', width: '60px'}} value={editingHubItemData.credits || ''} onChange={e => setEditingHubItemData({...editingHubItemData, credits: e.target.value})} />
                              </td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={handleSaveHubItem} title="Save"><Check size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={cancelEditHubItem} title="Cancel"><X size={16} /></button>
                                </div>
                              </td>
                            </>
                          ) : (
                            <>
                              <td className="id-cell" style={{ fontSize: '0.8rem' }}>{mod.batchId} / {mod.semesterId}</td>
                              <td className="id-cell">{mod.moduleId}</td>
                              <td>{mod.moduleName}</td>
                              <td>{mod.credits}</td>
                              <td>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                  <button className="icon-action-btn edit" onClick={() => handleEditHubItem('modules', mod)} title="Edit"><Edit2 size={16} /></button>
                                  <button className="icon-action-btn delete" onClick={() => handleDeleteHubItem('modules', mod)} title="Delete"><Trash2 size={16} /></button>
                                </div>
                              </td>
                            </>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </>
                )}
              </table>
            </div>
          )}
        </div>
      </motion.div>
    );
  };

  const renderEditView = () => (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className="view-container wizard-container"
    >
      <div className="wizard-header">
        <button className="btn-back" onClick={() => setView('list')}>
          <ArrowLeft size={18} /> Back to Programmes
        </button>
        <h2>Edit Programme</h2>
        <p>Update details for {editingProg?.id}.</p>
      </div>

      <div className="wizard-content-panel glass-panel">
        <div className="step-content">
          <div className="form-group">
            <label>Programme ID</label>
            <input type="text" className="form-input" value={editingProg?.id || ''} disabled style={{ backgroundColor: '#f1f5f9' }} />
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>ID cannot be changed.</span>
          </div>
          <div className="form-group">
            <label>Programme Name *</label>
            <input type="text" className="form-input" value={editingProg?.name || ''} onChange={e => setEditingProg({...editingProg, name: e.target.value})} />
          </div>
          <div className="form-group">
            <label>Duration (Years)</label>
            <select className="form-input" value={editingProg?.duration || '3 Years'} onChange={e => setEditingProg({...editingProg, duration: e.target.value})}>
              <option value="N/A" disabled>Select Duration</option>
              <option value="3 Years">3 Years</option>
              <option value="4 Years">4 Years</option>
            </select>
          </div>
          <div className="form-group">
            <label>Status</label>
            <select className="form-input" value={editingProg?.status || 'Active'} onChange={e => setEditingProg({...editingProg, status: e.target.value})}>
              <option value="Active">Active</option>
              <option value="Warning">Warning</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>
        </div>
        <div className="wizard-actions">
          <div className="spacer" />
          <button className="btn-primary" onClick={handleUpdateProgramme} disabled={isSaving}>
            <Check size={18} /> {isSaving ? 'Updating...' : 'Update Programme'}
          </button>
        </div>
      </div>
    </motion.div>
  );

  return (
    <div className="programme-manager">
      <ConfirmModal 
        {...confirmConfig} 
        onCancel={() => setConfirmConfig(prev => ({ ...prev, isOpen: false }))} 
      />
      <AnimatePresence mode="wait">
        {view === 'list' && renderProgrammeList()}
        {view === 'wizard' && renderWizard()}
        {view === 'edit' && renderEditView()}
        {view === 'details' && renderDetailsHub()}
      </AnimatePresence>

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
                                  <select className="form-input" style={{padding: '4px', fontSize: '0.8rem'}} value={editingStudentData.status || ''} onChange={e => setEditingStudentData({...editingStudentData, status: e.target.value})}>
                                    <option value="active">Active</option>
                                    <option value="inactive">Inactive</option>
                                  </select>
                                </td>
                                <td>
                                  <div style={{ display: 'flex', gap: '4px' }}>
                                    <button className="icon-action-btn edit" onClick={handleSaveStudentEdit} title="Save"><Check size={14} /></button>
                                    <button className="icon-action-btn delete" onClick={() => setEditingStudentId(null)} title="Cancel"><X size={14} /></button>
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
                                <td><span className={`status-pill ${(s.status || '').toLowerCase()}`}>{s.status}</span></td>
                                <td>
                                  <div style={{ display: 'flex', gap: '8px' }}>
                                    <button className="icon-action-btn edit" onClick={() => handleEditStudent(s)} title="Edit"><Edit2 size={14} /></button>
                                    <button className="icon-action-btn delete" onClick={() => handleDeleteStudent(s.studentId)} title="Delete"><Trash2 size={14} /></button>
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
                  <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    <Users size={48} style={{ opacity: 0.2, marginBottom: '16px' }} />
                    <p>No students enrolled in this batch yet.</p>
                    <button className="btn-primary" style={{ marginTop: '16px' }} onClick={() => setEnrollmentTab('upload')}>
                      Upload First Student List
                    </button>
                  </div>
                )}
              </div>
            )}

            {enrollmentTab === 'upload' && (
              <div className="tab-upload-content">
                <div className="upload-section glass-panel" style={{ border: '2px dashed var(--border-color)', padding: '32px', textAlign: 'center', marginBottom: '24px', borderRadius: '16px', background: 'rgba(0,0,0,0.02)' }}>
                  <input type="file" id="modal-upload" hidden accept=".xlsx, .xls, .csv" onChange={handleFileUpload} />
                  <label htmlFor="modal-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                    <div style={{ padding: '12px', background: 'white', borderRadius: '50%', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
                      <Upload size={32} color="var(--accent)" />
                    </div>
                    <div>
                      <p style={{ fontWeight: '700', color: 'var(--text-main)' }}>Click to upload student Excel/CSV file</p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: ID, Name, Email (or University Email), Password, Status</p>
                    </div>
                  </label>
                </div>

                {studentList.length > 0 && (
                  <>
                    <div className="preview-table-wrapper" style={{ maxHeight: '250px', overflowY: 'auto', marginBottom: '24px', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                      <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                        <thead style={{ position: 'sticky', top: 0, background: 'white', zIndex: 1 }}>
                          <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Initial Password</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {studentList.map((s, idx) => (
                            <tr key={idx}>
                              <td className="id-cell">{s.studentId}</td>
                              <td>{s.fullName}</td>
                              <td>{s.email}</td>
                              <td>
                                <code style={{ fontSize: '0.85rem', color: 'var(--primary)' }}>{s.password}</code>
                              </td>
                              <td><span className={`status-pill ${s.status}`}>{s.status}</span></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                      <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setStudentList([])}>Clear List</button>
                      <button className="btn-success" style={{ padding: '10px 24px' }} onClick={handleSaveEnrollment} disabled={isSaving}>
                        <UserPlus size={18} /> {isSaving ? 'Enrolling...' : `Enroll ${studentList.length} Students`}
                      </button>
                    </div>
                  </>
                )}
              </div>
            )}

            {enrollmentTab === 'single' && (
              <div className="tab-single-content">
                <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Student ID</label>
                    <input type="text" placeholder="e.g. STU001" className="form-input" style={{ padding: '10px 14px' }} value={singleStudent.studentId} onChange={e => setSingleStudent({...singleStudent, studentId: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Full Name</label>
                    <input type="text" placeholder="John Doe" className="form-input" style={{ padding: '10px 14px' }} value={singleStudent.fullName} onChange={e => setSingleStudent({...singleStudent, fullName: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Email Address</label>
                    <input type="email" placeholder="john@example.com" className="form-input" style={{ padding: '10px 14px' }} value={singleStudent.email} onChange={e => setSingleStudent({...singleStudent, email: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Initial Password</label>
                    <input type="text" placeholder="Enter password" className="form-input" style={{ padding: '10px 14px' }} value={singleStudent.password} onChange={e => setSingleStudent({...singleStudent, password: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Status</label>
                    <select className="form-input" style={{ padding: '10px 14px' }} value={singleStudent.status} onChange={e => setSingleStudent({...singleStudent, status: e.target.value})}>
                      <option value="active">Active</option>
                      <option value="inactive">Inactive</option>
                    </select>
                  </div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
                  <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setSingleStudent({ studentId: '', fullName: '', email: '', password: '', status: 'active' })}>Clear Fields</button>
                  <button className="btn-success" style={{ padding: '10px 24px' }} onClick={handleSaveSingleStudent} disabled={isSaving}>
                    <UserPlus size={18} /> {isSaving ? 'Enrolling...' : 'Enroll Student'}
                  </button>
                </div>
              </div>
            )}
          </motion.div>
        </div>
      )}

      {isAddBatchModalOpen && (
        <div className="modal-overlay">
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="glass-panel modal-content"
            style={{ maxWidth: addBatchWizardStep === 1 ? '500px' : '800px', width: '90%', padding: '32px', transition: 'max-width 0.3s ease' }}
          >
            <div className="modal-header" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px', alignItems: 'center' }}>
              <div>
                <h2 style={{ fontSize: '1.5rem', fontWeight: '800', color: 'var(--text-main)' }}>
                  {addBatchWizardStep === 1 ? 'Add New Batch: Step 1' : addBatchWizardStep === 2 ? 'Module Config: Step 2' : 'Enroll Students: Step 3'}
                </h2>
                <p className="text-muted">Batch: <strong>{addBatchData.batchName || 'New Batch'}</strong> for {selectedProgramme?.name}</p>
              </div>
              <button className="icon-action-btn" onClick={() => {setIsAddBatchModalOpen(false); setStudentList([]); setModuleList([]); setAddBatchWizardStep(1);}}><X size={24} /></button>
            </div>

            {addBatchWizardStep === 1 ? (
              <div className="step-1-content">
                <div className="form-grid" style={{ display: 'flex', flexDirection: 'column', gap: '20px', marginBottom: '24px' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Batch ID</label>
                    <input type="text" placeholder="e.g. B24-CS" className="form-input" style={{ padding: '10px 14px' }} value={addBatchData.batchId} onChange={e => setAddBatchData({...addBatchData, batchId: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Batch Name</label>
                    <input type="text" placeholder="e.g. Intake 2024" className="form-input" style={{ padding: '10px 14px' }} value={addBatchData.batchName} onChange={e => setAddBatchData({...addBatchData, batchName: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label style={{ fontSize: '0.85rem' }}>Intake Year</label>
                    <input type="number" className="form-input" style={{ padding: '10px 14px' }} value={addBatchData.intakeYear} onChange={e => setAddBatchData({...addBatchData, intakeYear: e.target.value})} />
                  </div>
                  <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer', marginBottom: 0 }} onClick={() => setIsAutoGeneratingSemesters(!isAutoGeneratingSemesters)}>
                    <div style={{ 
                      width: '24px', 
                      height: '24px', 
                      borderRadius: '6px', 
                      border: '2px solid var(--accent)', 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'center',
                      background: isAutoGeneratingSemesters ? 'var(--accent)' : 'transparent',
                      transition: 'all 0.2s'
                    }}>
                      {isAutoGeneratingSemesters && <Check size={16} color="white" />}
                    </div>
                    <span style={{ fontWeight: '600', fontSize: '0.9rem', color: 'var(--text-main)' }}>Auto-generate all semesters ({parseInt(selectedProgramme?.duration || 0) * 2} semesters)</span>
                  </div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
                  <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setIsAddBatchModalOpen(false)}>Cancel</button>
                  <button className="btn-primary" style={{ padding: '10px 24px' }} onClick={() => {
                    if(!addBatchData.batchId || !addBatchData.batchName) { toast.error("Please fill batch details."); return; }
                    setAddBatchWizardStep(2);
                    setEnrollmentTab('upload');
                  }}>
                    Next: Modules <ChevronRight size={18} />
                  </button>
                </div>
              </div>
            ) : addBatchWizardStep === 2 ? (
              <div className="step-2-content">
                <div className="modal-tabs" style={{ display: 'flex', gap: '20px', borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
                  <button onClick={() => setEnrollmentTab('upload')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'upload' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'upload' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Bulk Upload</button>
                  <button onClick={() => setEnrollmentTab('single')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'single' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'single' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Manual Entry</button>
                </div>

                {enrollmentTab === 'upload' ? (
                  <div className="upload-section glass-panel" style={{ border: '2px dashed var(--border-color)', padding: '32px', textAlign: 'center', marginBottom: '24px', borderRadius: '16px', background: 'rgba(0,0,0,0.02)' }}>
                    <input type="file" id="new-batch-modules-upload" hidden accept=".xlsx, .xls, .csv" onChange={handleFileUpload} />
                    <label htmlFor="new-batch-modules-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                      <Upload size={32} color="var(--accent)" />
                      <p style={{ fontWeight: '700' }}>Click to upload module Excel/CSV file</p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: SemesterID, ModuleCode (or Module ID), ModuleName, Credits</p>
                    </label>
                  </div>
                ) : (
                  <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 100px', gap: '12px', marginBottom: '24px' }}>
                    <select className="form-input" style={{ padding: '10px' }} value={manualModule.semesterId} onChange={e => setManualModule({...manualModule, semesterId: e.target.value})}>
                      <option value="">Select Sem</option>
                      {hubSemesters.length > 0 ? (
                        [...new Set(hubSemesters.map(s => s.semesterId))].sort().map(sId => (
                          <option key={sId} value={sId}>{sId}</option>
                        ))
                      ) : (
                        generateSemesters(selectedProgramme?.duration).map(s => (
                          <option key={s.id} value={s.id}>{s.id}</option>
                        ))
                      )}
                    </select>
                    <input type="text" placeholder="Code" className="form-input" style={{ padding: '10px' }} value={manualModule.moduleCode} onChange={e => setManualModule({...manualModule, moduleCode: e.target.value})} />
                    <input type="text" placeholder="Name" className="form-input" style={{ padding: '10px' }} value={manualModule.moduleName} onChange={e => setManualModule({...manualModule, moduleName: e.target.value})} />
                    <input type="number" placeholder="Cred" className="form-input" style={{ padding: '10px' }} value={manualModule.credits} onChange={e => setManualModule({...manualModule, credits: e.target.value})} />
                    <button className="btn-primary" style={{ gridColumn: 'span 4' }} onClick={() => {
                       if(manualModule.moduleCode && manualModule.semesterId) {
                         setModuleList([...moduleList, {...manualModule}]);
                         setManualModule({ semesterId: '', moduleCode: '', moduleName: '', credits: '' });
                       } else {
                         toast.error("Fill required module fields.")
                       }
                    }}>Add Module</button>
                  </div>
                )}

                {moduleList.length > 0 && (
                  <div className="preview-table-wrapper" style={{ maxHeight: '200px', overflowY: 'auto', marginBottom: '24px', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                    <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                      <thead><tr><th>Sem</th><th>Code</th><th>Name</th><th>Credits</th><th>Actions</th></tr></thead>
                      <tbody>
                        {moduleList.map((m, idx) => (
                          <tr key={idx}><td>{m.semesterId}</td><td>{m.moduleCode}</td><td>{m.moduleName}</td><td>{m.credits}</td><td><button className="icon-action-btn delete" onClick={() => setModuleList(moduleList.filter((_, i) => i !== idx))}><X size={14} /></button></td></tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
                  <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setAddBatchWizardStep(1)}>Back</button>
                  <button className="btn-primary" style={{ padding: '10px 24px' }} onClick={() => {
                    setAddBatchWizardStep(3);
                    setEnrollmentTab('upload');
                  }}>
                    Next: Enroll Students <ChevronRight size={18} />
                  </button>
                </div>
              </div>
            ) : (
              <div className="step-3-content">
                <div className="modal-tabs" style={{ display: 'flex', gap: '20px', borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
                  <button onClick={() => setEnrollmentTab('upload')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'upload' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'upload' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Bulk Upload</button>
                  <button onClick={() => setEnrollmentTab('single')} style={{ padding: '12px 16px', background: 'none', border: 'none', fontWeight: '700', color: enrollmentTab === 'single' ? 'var(--accent)' : 'var(--text-muted)', borderBottom: enrollmentTab === 'single' ? '2px solid var(--accent)' : '2px solid transparent', cursor: 'pointer' }}>Single Entry</button>
                </div>

                {enrollmentTab === 'upload' ? (
                  <div className="upload-section glass-panel" style={{ border: '2px dashed var(--border-color)', padding: '32px', textAlign: 'center', marginBottom: '24px', borderRadius: '16px', background: 'rgba(0,0,0,0.02)' }}>
                    <input type="file" id="new-batch-upload" hidden accept=".xlsx, .xls, .csv" onChange={handleFileUpload} />
                    <label htmlFor="new-batch-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                      <Upload size={32} color="var(--accent)" />
                      <p style={{ fontWeight: '700' }}>Click to upload student Excel/CSV file</p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Format: ID, Name, Email (or University Email), Password, Status</p>
                    </label>
                  </div>
                ) : (
                  <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '24px' }}>
                    <input type="text" placeholder="Student ID" className="form-input" style={{ padding: '10px' }} value={singleStudent.studentId} onChange={e => setSingleStudent({...singleStudent, studentId: e.target.value})} />
                    <input type="text" placeholder="Full Name" className="form-input" style={{ padding: '10px' }} value={singleStudent.fullName} onChange={e => setSingleStudent({...singleStudent, fullName: e.target.value})} />
                    <input type="email" placeholder="Email" className="form-input" style={{ padding: '10px' }} value={singleStudent.email} onChange={e => setSingleStudent({...singleStudent, email: e.target.value})} />
                    <input type="text" placeholder="Password" className="form-input" style={{ padding: '10px' }} value={singleStudent.password} onChange={e => setSingleStudent({...singleStudent, password: e.target.value})} />
                    <button className="btn-primary" style={{ gridColumn: 'span 2' }} onClick={() => {
                       if(singleStudent.studentId && singleStudent.fullName) {
                         setStudentList([...studentList, {...singleStudent}]);
                         setSingleStudent({ studentId: '', fullName: '', email: '', password: '', status: 'active' });
                       } else {
                         toast.error("Fill required student fields.")
                       }
                    }}>Add to List</button>
                  </div>
                )}

                {studentList.length > 0 && (
                  <div className="preview-table-wrapper" style={{ maxHeight: '200px', overflowY: 'auto', marginBottom: '24px', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                    <table className="modern-table" style={{ fontSize: '0.85rem' }}>
                      <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Initial Password</th><th>Actions</th></tr></thead>
                      <tbody>
                        {studentList.map((s, idx) => (
                          <tr key={idx}>
                            <td>{s.studentId}</td>
                            <td>{s.fullName}</td>
                            <td>{s.email}</td>
                            <td>
                              <code style={{ fontSize: '0.85rem', color: 'var(--primary)' }}>{s.password}</code>
                            </td>
                            <td>
                              <button className="icon-action-btn delete" onClick={() => setStudentList(studentList.filter((_, i) => i !== idx))}>
                                <X size={14} />
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
                  <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setAddBatchWizardStep(1)}>Back</button>
                  <button className="btn-success" style={{ padding: '10px 24px' }} onClick={handleSaveNewBatch} disabled={isSaving}>
                    <Check size={18} /> {isSaving ? 'Saving Everything...' : `Complete & Enroll ${studentList.length} Students`}
                  </button>
                </div>
              </div>
            )}
          </motion.div>
        </div>
      )}

      <style dangerouslySetInnerHTML={{ __html: `
        .programme-manager {
          width: 100%;
          min-height: 100%;
        }

        .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(15, 23, 42, 0.4);
          backdrop-filter: blur(8px);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .modal-content {
          background: white;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        }

        .hub-content {
          padding: 24px;
          min-height: 400px;
        }

        .hub-filter-row {
          display: flex;
          gap: 20px;
          margin-bottom: 20px;
          padding: 16px;
          background: #f8fafc;
          border-radius: 12px;
          border: 1px solid #e2e8f0;
          align-items: center;
        }

        .filter-item {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .hub-filter-row label {
          font-weight: 700;
          color: var(--text-main);
          font-size: 0.9rem;
        }

        .filter-select {
          padding: 8px 12px;
          border-radius: 8px;
          border: 1px solid var(--border-color);
          background: white;
          font-weight: 600;
          outline: none;
        }

        .loading-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 60px;
          gap: 16px;
          color: var(--text-muted);
        }

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

        .btn-success {
          background: #10B981;
          color: white;
          padding: 14px 24px;
          border-radius: 12px;
          font-weight: 700;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          gap: 10px;
          box-shadow: 0 8px 20px rgba(16, 185, 129, 0.25);
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

        .filter-btn {
          background: white;
          color: var(--text-main);
          border: 1px solid var(--border-color);
          padding: 10px 20px;
          border-radius: 12px;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 8px;
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
          display: flex;
          align-items: center;
          justify-content: center;
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
        }

        .pagination button.active {
          background: var(--accent);
          color: white;
          border-color: var(--accent);
        }

        /* Wizard Styles */
        .wizard-container {
          max-width: 800px;
          margin: 0 auto;
        }

        .wizard-header {
          text-align: center;
          margin-bottom: 24px;
        }

        .btn-back {
          background: none;
          border: none;
          color: var(--text-muted);
          font-weight: 600;
          display: inline-flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 24px;
        }

        .wizard-header h2 {
          font-size: 2rem;
          color: var(--text-main);
          font-weight: 800;
        }

        .wizard-stepper {
          display: flex;
          justify-content: space-between;
          margin-bottom: 32px;
          position: relative;
        }

        .stepper-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
          flex: 1;
          position: relative;
          z-index: 2;
        }

        .step-circle {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: white;
          border: 2px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          color: var(--text-muted);
          transition: all 0.3s;
        }

        .step-label {
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--text-muted);
        }

        .step-line {
          position: absolute;
          top: 20px;
          left: 50%;
          width: 100%;
          height: 2px;
          background: var(--border-color);
          z-index: -1;
        }

        .stepper-item.active .step-circle {
          border-color: var(--accent);
          color: var(--accent);
        }

        .stepper-item.active .step-label {
          color: var(--text-main);
        }

        .stepper-item.completed .step-circle {
          background: var(--accent);
          border-color: var(--accent);
          color: white;
        }

        .stepper-item.completed .step-line {
          background: var(--accent);
        }

        .wizard-content-panel {
          background: white;
          padding: 40px;
        }

        .step-content h3 {
          font-size: 1.5rem;
          margin-bottom: 8px;
          color: var(--text-main);
        }

        .form-group {
          margin-bottom: 24px;
        }

        .form-group label {
          display: block;
          font-weight: 600;
          margin-bottom: 8px;
          color: var(--text-main);
        }

        .form-input {
          width: 100%;
          padding: 14px 16px;
          border: 1px solid var(--border-color);
          border-radius: 12px;
          font-size: 1rem;
          background: var(--bg-body);
          outline: none;
        }

        .form-input:focus {
          border-color: var(--accent);
          background: white;
        }

        .wizard-actions {
          display: flex;
          margin-top: 40px;
          padding-top: 24px;
          border-top: 1px solid var(--border-color);
        }

        .spacer {
          flex: 1;
        }

        .mb-4 { margin-bottom: 24px; }
        .mt-4 { margin-top: 24px; }
        .w-full { width: 100%; }

        .preview-item {
          padding: 16px;
          border: 1px solid var(--border-color);
          border-radius: 12px;
          margin-bottom: 12px;
          font-weight: 600;
        }

        .empty-state {
          text-align: center;
          padding: 48px;
          background: var(--bg-body);
          border-radius: 16px;
          border: 2px dashed var(--border-color);
          color: var(--text-muted);
          font-weight: 500;
        }

        /* Hub Styles */
        .hub-header {
          background: white;
          padding: 32px 40px 0 40px;
        }

        .hub-title-row {
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          margin-bottom: 32px;
        }

        .hub-title-row h1 {
          font-size: 2.5rem;
          font-weight: 800;
          color: var(--text-main);
          margin: 0 0 8px 0;
        }

        .hub-id-badge {
          background: var(--shape-light);
          color: var(--accent);
          padding: 6px 16px;
          border-radius: 8px;
          font-weight: 700;
        }

        .hub-tabs {
          display: flex;
          gap: 32px;
          border-bottom: 2px solid var(--border-color);
        }

        .hub-tab {
          background: none;
          border: none;
          padding: 16px 0;
          font-weight: 600;
          font-size: 1.05rem;
          color: var(--text-muted);
          display: flex;
          align-items: center;
          gap: 10px;
          position: relative;
        }

        .hub-tab.active {
          color: var(--accent);
        }

        .hub-tab.active::after {
          content: '';
          position: absolute;
          bottom: -2px;
          left: 0;
          width: 100%;
          height: 3px;
          background: var(--accent);
          border-radius: 3px 3px 0 0;
        }

        .hub-content {
          background: white;
          padding: 40px;
          min-height: 400px;
        }

        .tab-content-placeholder {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          height: 100%;
          text-align: center;
          padding: 60px;
          color: var(--text-muted);
        }

        .tab-content-placeholder h3 {
          font-size: 1.5rem;
          color: var(--text-main);
          margin-bottom: 12px;
        }
      `}} />
    </div>
  );
};

export default ProgrammeManager;

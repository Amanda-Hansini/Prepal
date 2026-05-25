import { addDoc, collection, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase';

/**
 * Logs a standard activity to the Firestore ActivityLogs collection
 * for real-time display on the Admin Dashboard.
 * 
 * @param {string} action - The action type (e.g. "Created Programme", "Deleted Batch")
 * @param {string} details - Human-readable details (e.g. "Software Engineering", "Batch B01")
 */
export const logActivity = async (action, details) => {
  try {
    await addDoc(collection(db, 'ActivityLogs'), {
      action,
      details,
      timestamp: serverTimestamp()
    });
  } catch (error) {
    console.error("Error logging activity to Firestore: ", error);
  }
};

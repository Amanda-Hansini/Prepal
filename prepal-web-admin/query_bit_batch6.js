import { collectionGroup, getDocs } from 'firebase/firestore';
import { db } from './src/firebase.js';

async function checkBITBatch06() {
  try {
    const snap = await getDocs(collectionGroup(db, 'Semesters'));
    console.log("Total Semesters found:", snap.docs.length);
    snap.docs.forEach(doc => {
      const data = doc.data();
      const path = doc.ref.path;
      // Print anything related to BIT or batch 06
      if (
        path.toLowerCase().includes("bit") ||
        (data.batchId && data.batchId.toLowerCase().includes("06")) ||
        (data.degreeId && data.degreeId.toLowerCase().includes("bit")) ||
        (data.name && data.name.toLowerCase().includes("first")) ||
        (data.semesterName && data.semesterName.toLowerCase().includes("first"))
      ) {
        console.log("--- FOUND MATCH ---");
        console.log("Path:", path);
        console.log("Data:", JSON.stringify(data, null, 2));
      }
    });
    // Also print all unique batchIds and degree paths just in case
    console.log("All Semester Paths and batchIds:");
    snap.docs.forEach(doc => {
      console.log(`Path: ${doc.ref.path} | batchId: ${doc.data().batchId} | name: ${doc.data().name || doc.data().semesterName}`);
    });
    process.exit(0);
  } catch (err) {
    console.error("Error querying Firestore:", err);
    process.exit(1);
  }
}

checkBITBatch06();

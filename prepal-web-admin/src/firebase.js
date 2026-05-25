import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

// Your web app's Firebase configuration from Firebase Console
const firebaseConfig = {
  apiKey: "AIzaSyBgIFE4CWz2vXGUc6lPy0xgPX3W34eu5qs",
  authDomain: "finalyearproject-5ec83.firebaseapp.com",
  projectId: "finalyearproject-5ec83",
  storageBucket: "finalyearproject-5ec83.firebasestorage.app",
  messagingSenderId: "749286195744",
  appId: "1:749286195744:web:2debc735bced87c6567245"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

export { db };

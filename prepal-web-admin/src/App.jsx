import React, { useState, useEffect } from 'react'
import Login from './pages/Login'
import Sidebar from './components/Sidebar'
import Header from './components/Header'
import DashboardHome from './pages/DashboardHome'
import StudentManager from './pages/StudentManager'
import ProgrammeManager from './pages/ProgrammeManager'
import BatchManager from './pages/BatchManager'
import SemesterManager from './pages/SemesterManager'
import ModuleManager from './pages/ModuleManager'
import ReportPage from './pages/ReportPage'
import Profile from './pages/Profile'
import { db } from './firebase'
import { doc, onSnapshot } from 'firebase/firestore'

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [currentAdmin, setCurrentAdmin] = useState(null);

  useEffect(() => {
    if (isLoggedIn && currentAdmin?.adminId) {
      const unsub = onSnapshot(doc(db, "Admins", currentAdmin.adminId), (docSnap) => {
        if (docSnap.exists()) {
          setCurrentAdmin({ adminId: docSnap.id, ...docSnap.data() });
        }
      }, (error) => {
        console.error("Error syncing active admin snapshot: ", error);
      });
      return unsub;
    }
  }, [isLoggedIn, currentAdmin?.adminId]);

  const handleLogout = () => {
    setIsLoggedIn(false);
    setCurrentAdmin(null);
    setCurrentPage('dashboard');
  };

  if (!isLoggedIn) {
    return <Login onLogin={(admin) => {
      setIsLoggedIn(true);
      setCurrentAdmin(admin);
    }} />;
  }

  const renderPage = () => {
    switch(currentPage) {
      case 'students': 
        return <StudentManager />;
      case 'programmes': 
        return <ProgrammeManager setPage={setCurrentPage} initialTab="programmes" initialView="list" />;
      case 'programmes-wizard': 
        return <ProgrammeManager setPage={setCurrentPage} initialTab="programmes" initialView="wizard" />;
      case 'batches': 
        return <BatchManager setPage={setCurrentPage} />;
      case 'semesters': 
        return <SemesterManager setPage={setCurrentPage} />;
      case 'modules': 
        return <ModuleManager setPage={setCurrentPage} />;
      case 'report':
        return <ReportPage setPage={setCurrentPage} />;
      case 'profile':
        return <Profile currentAdmin={currentAdmin} setPage={setCurrentPage} onLogout={handleLogout} />;
      default: 
        return <DashboardHome setPage={setCurrentPage} />;
    }
  };

  return (
    <div className="app-shell">
      <div className="bg-decor-container">
        {/* Top Right Wave */}
        <div className="decor-top-right">
          <svg width="200" height="200" viewBox="0 0 200 200" fill="none">
            <path d="M80 0C120 100 200 80 200 150V0H80Z" fill="var(--shape-light)"/>
            <path d="M130 0C160 60 200 60 200 100V0H130Z" fill="var(--shape-dark)"/>
          </svg>
        </div>

        {/* Top Left Dots */}
        <div className="decor-dots-top">
          <svg width="120" height="120" viewBox="0 0 64 64" fill="var(--primary)" style={{ opacity: 0.3 }}>

            <circle cx="52" cy="12" r="2" />
            <circle cx="40" cy="24" r="2" />
            <circle cx="52" cy="24" r="2" />
            <circle cx="28" cy="36" r="2" />
            <circle cx="40" cy="36" r="2" />
            <circle cx="52" cy="36" r="2" />
            <circle cx="16" cy="48" r="2" />
            <circle cx="28" cy="48" r="2" />
            <circle cx="40" cy="48" r="2" />
            <circle cx="52" cy="48" r="2" />
            <circle cx="4" cy="60" r="2" />
            <circle cx="16" cy="60" r="2" />
            <circle cx="28" cy="60" r="2" />
            <circle cx="40" cy="60" r="2" />
            <circle cx="52" cy="60" r="2" />
          </svg>
        </div>

        {/* Bottom Left Wave */}
        <div className="decor-bottom-left">
          <svg width="180" height="180" viewBox="0 0 180 180" fill="none">
            <path d="M0 60C80 100 80 180 120 180H0V60Z" fill="var(--shape-light)"/>
            <path d="M0 110C50 130 50 180 80 180H0V110Z" fill="var(--shape-dark)"/>
          </svg>
        </div>

        {/* Bottom Right Dots */}
        <div className="decor-dots-bottom">
          <svg width="120" height="120" viewBox="0 0 64 64" fill="var(--primary)" style={{ opacity: 0.3 }}>
            <circle cx="52" cy="12" r="2" />
            <circle cx="40" cy="24" r="2" />
            <circle cx="52" cy="24" r="2" />
            <circle cx="28" cy="36" r="2" />
            <circle cx="40" cy="36" r="2" />
            <circle cx="52" cy="36" r="2" />
            <circle cx="16" cy="48" r="2" />
            <circle cx="28" cy="48" r="2" />
            <circle cx="40" cy="48" r="2" />
            <circle cx="52" cy="48" r="2" />
            <circle cx="4" cy="60" r="2" />
            <circle cx="16" cy="60" r="2" />
            <circle cx="28" cy="60" r="2" />
            <circle cx="40" cy="60" r="2" />
            <circle cx="52" cy="60" r="2" />
          </svg>
        </div>
      </div>

      <Sidebar activePage={currentPage} setPage={setCurrentPage} currentAdmin={currentAdmin} />
      <div className="main-content">
        <div className="page-container">
          {renderPage()}
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .app-shell {
          display: flex;
          min-height: 100vh;
          background: var(--bg-body);
          position: relative;
          overflow: hidden;
        }

        .bg-decor-container {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          pointer-events: none;
          z-index: 0;
        }

        .decor-top-right {
          position: absolute;
          top: 0;
          right: 0;
        }

        .decor-bottom-left {
          position: absolute;
          bottom: 0;
          left: 0;
        }

        .decor-dots-top {
          position: absolute;
          top: 30px;
          right: 30px;
        }

        .decor-dots-bottom {
          position: absolute;
          bottom: 20px;
          right: 20px;
        }


        .main-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          margin-left: 280px;
          min-height: 100vh;
          position: relative;
          z-index: 1;
        }

        .page-container {
          padding: 40px;
          flex: 1;
        }

        @media (max-width: 1024px) {
          .main-content {
            margin-left: 0;
          }
          .decor-dots-top {
            left: 20px;
          }
          .decor-dots-bottom {
            right: 20px;
          }
        }
      `}} />
    </div>
  )
}




export default App

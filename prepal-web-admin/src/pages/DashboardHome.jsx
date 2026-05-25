import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  GraduationCap, 
  Users, 
  Calendar, 
  Box, 
  ChevronRight, 
  Plus, 
  ArrowRight,
  Activity
} from 'lucide-react';
import { collection, onSnapshot, query, orderBy, limit, collectionGroup } from 'firebase/firestore';
import { db } from '../firebase';

const DashboardHome = ({ setPage }) => {
  const [progsCount, setProgsCount] = useState(null);
  const [batchesCount, setBatchesCount] = useState(null);
  const [semsCount, setSemsCount] = useState(null);
  const [modsCount, setModsCount] = useState(null);
  const [activities, setActivities] = useState([]);

  useEffect(() => {
    // 1. Listen to Degrees (Programmes) in real-time
    const unsubProgs = onSnapshot(collection(db, 'Degrees'), (snap) => {
      setProgsCount(snap.size);
    }, (error) => {
      console.error("Error listening to Degrees:", error);
    });

    // 2. Listen to Batches in real-time
    const unsubBatches = onSnapshot(collection(db, 'Batches'), (snap) => {
      setBatchesCount(snap.size);
    }, (error) => {
      console.error("Error listening to Batches:", error);
    });

    // 3. Listen to Semesters subcollections in real-time
    const unsubSems = onSnapshot(collectionGroup(db, 'Semesters'), (snap) => {
      setSemsCount(snap.size);
    }, (error) => {
      console.error("Error listening to Semesters:", error);
    });

    // 4. Listen to Modules subcollections in real-time
    const unsubMods = onSnapshot(collectionGroup(db, 'Modules'), (snap) => {
      setModsCount(snap.size);
    }, (error) => {
      console.error("Error listening to Modules:", error);
    });

    // 5. Listen to Recent Activities in real-time
    const qActs = query(collection(db, 'ActivityLogs'), orderBy('timestamp', 'desc'), limit(3));
    const unsubActs = onSnapshot(qActs, (snap) => {
      const fetchedActs = [];
      snap.forEach(doc => fetchedActs.push({ id: doc.id, ...doc.data() }));
      setActivities(fetchedActs);
    }, (error) => {
      console.error("Error listening to ActivityLogs:", error);
    });

    return () => {
      unsubProgs();
      unsubBatches();
      unsubSems();
      unsubMods();
      unsubActs();
    };
  }, []);

  const stats = [
    { label: 'Programmes', value: progsCount !== null ? progsCount.toString() : '...', icon: GraduationCap, sub: 'Total Programmes' },
    { label: 'Batches', value: batchesCount !== null ? batchesCount.toString() : '...', icon: Users, sub: 'Total Batches' },
    { label: 'Semesters', value: semsCount !== null ? semsCount.toString() : '...', icon: Calendar, sub: 'Total Semesters' },
    { label: 'Modules', value: modsCount !== null ? modsCount.toString() : '...', icon: Box, sub: 'Total Modules' },
  ];

  const managementItems = [
    { title: 'Programme Management', desc: 'Create, update and manage programmes offered in the system.', count: progsCount !== null ? `${progsCount} Programmes` : '... Programmes', icon: GraduationCap, btn: 'Manage Programmes', id: 'programmes' },
    { title: 'Batch Management', desc: 'Organize and manage batches for different degrees.', count: batchesCount !== null ? `${batchesCount} Batches` : '... Batches', icon: Users, btn: 'Manage Batches', id: 'batches' },
    { title: 'Semester Management', desc: 'Create and manage semesters for curriculum structure.', count: semsCount !== null ? `${semsCount} Semesters` : '... Semesters', icon: Calendar, btn: 'Manage Semesters', id: 'semesters' },
    { title: 'Module Management', desc: 'Add, update and manage modules and subjects.', count: modsCount !== null ? `${modsCount} Modules` : '... Modules', icon: Box, btn: 'Manage Modules', id: 'modules' },
  ];

  const formatTimeAgo = (timestamp) => {
    if (!timestamp) return 'Just now';
    const date = timestamp.toDate();
    const now = new Date();
    const diffInSeconds = Math.floor((now - date) / 1000);
    if (diffInSeconds < 60) return `${diffInSeconds} seconds ago`;
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes} minutes ago`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours} hours ago`;
    const diffInDays = Math.floor(diffInHours / 24);
    return `${diffInDays} days ago`;
  };

  const getFormattedDate = () => {
    const today = new Date();
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    return today.toLocaleDateString('en-US', options);
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">

        <div className="header-left">
          <h1>Dashboard</h1>
          <p>Welcome back, Admin!</p>
        </div>
        <div className="header-right">
          <div className="date-badge">
            <Calendar size={16} />
            <span>{getFormattedDate()}</span>
          </div>
        </div>
      </header>

      <section className="stats-grid">
        {stats.map((stat, i) => (
          <motion.div 
            key={i}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            className="stat-card"
          >
            <div className="stat-icon-box">
              <stat.icon size={24} color="white" strokeWidth={2.5} />
            </div>
            <div className="stat-info">
              <span className="stat-label">{stat.label}</span>
              <h2 className="stat-value">{stat.value}</h2>
              <span className="stat-sub">{stat.sub}</span>
            </div>
          </motion.div>
        ))}
      </section>

      <section className="management-overview">
        <div className="section-header">
          <h2>Management Overview</h2>
          <p>Manage and organize academic structure</p>
        </div>
        <div className="management-grid">
          {managementItems.map((item, i) => (
            <motion.div 
              key={i}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.4 + i * 0.1 }}
              className="manage-card"
            >
              <div className="manage-icon-circle">
                <item.icon size={32} color="var(--accent)" strokeWidth={2.5} />
              </div>
              <h3 className="manage-title">{item.title}</h3>
              <p className="manage-desc">{item.desc}</p>
              <div className="manage-badge">{item.count}</div>
              <button className="btn-manage" onClick={() => item.id && setPage(item.id)}>
                {item.btn}
                <ChevronRight size={18} strokeWidth={3} />
              </button>
            </motion.div>
          ))}
        </div>
      </section>

      <div className="bottom-grid">
        <section className="recent-activity">
          <div className="section-header">
            <h2>Recent Activity</h2>
          </div>
          <div className="activity-list-container">
            {activities.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '10px 0' }}>No recent activity to display.</p>
            ) : activities.map((act) => (
              <div className="activity-item-card" key={act.id} style={{ marginBottom: '20px' }}>
                <div className="activity-icon-circle">
                  <Activity size={18} color="var(--accent)" />
                </div>
                <div className="activity-info">
                  <p><strong>{act.action}:</strong> {act.details}</p>
                  <span>{formatTimeAgo(act.timestamp)}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="quick-actions">
          <div className="section-header">
            <h2>Quick Actions</h2>
          </div>
          <div className="quick-action-card" onClick={() => setPage('programmes-wizard')}>
            <div className="action-row">
              <div className="action-icon-circle">
                <Plus size={18} color="var(--accent)" />
              </div>
              <span className="action-text">Add New Programme</span>
              <ChevronRight size={18} color="var(--text-muted)" className="ms-auto" />
            </div>
          </div>
        </section>
      </div>

      <footer className="dashboard-footer">
        <p>© 2025 PrePal. All rights reserved.</p>
      </footer>

      <style dangerouslySetInnerHTML={{ __html: `
        .dashboard-container {
          position: relative;
          z-index: 1;
        }

        .dashboard-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 40px;
        }

        .header-left h1 {
          font-size: 2rem;
          font-weight: 800;
          color: var(--text-main);
          margin: 0;
        }

        .header-left p {
          color: var(--text-muted);
          font-weight: 500;
          margin: 4px 0 0 0;
        }

        .date-badge {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 16px;
          background: white;
          border-radius: 12px;
          box-shadow: var(--shadow-sm);
          color: var(--text-muted);
          font-size: 0.9rem;
          font-weight: 600;
        }

        .stats-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 20px;
          margin-bottom: 40px;
        }

        .stat-card {
          background: white;
          padding: 24px 20px;
          border-radius: 20px;
          display: flex;
          align-items: center;
          gap: 16px;
          box-shadow: 0 10px 30px rgba(13, 47, 90, 0.04);
          border: 1px solid rgba(224, 228, 236, 0.5);
        }

        .stat-icon-box {
          width: 56px;
          height: 56px;
          background: var(--accent);
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 8px 20px rgba(5, 123, 254, 0.25);
        }

        .stat-label {
          display: block;
          font-size: 0.95rem;
          font-weight: 700;
          color: var(--text-main);
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 2rem;
          font-weight: 800;
          color: var(--accent);
          margin: 0;
          line-height: 1.1;
        }

        .stat-sub {
          font-size: 0.8rem;
          color: var(--text-muted);
          font-weight: 500;
        }

        .management-overview {
          margin-bottom: 40px;
        }

        .section-header h2 {
          font-size: 1.5rem;
          font-weight: 800;
          color: var(--text-main);
          margin: 0;
        }

        .management-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
          gap: 20px;
          margin-top: 24px;
        }

        .manage-card {
          background: white;
          padding: 32px 24px;
          border-radius: 20px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          box-shadow: 0 10px 40px rgba(13, 47, 90, 0.05);
          border: 1px solid rgba(224, 228, 236, 0.5);
          height: 100%;
        }

        .manage-icon-circle {
          width: 72px;
          height: 72px;
          background: var(--shape-light);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 20px;
        }

        .manage-title {
          font-size: 1.25rem;
          font-weight: 800;
          color: var(--text-main);
          margin-bottom: 12px;
          min-height: 56px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .manage-desc {
          font-size: 0.9rem;
          color: var(--text-muted);
          line-height: 1.5;
          margin-bottom: 20px;
          min-height: 48px;
          display: flex;
          align-items: flex-start;
          justify-content: center;
        }

        .manage-badge {
          display: inline-block;
          padding: 6px 16px;
          background: #E6F2FF;
          color: var(--accent);
          border-radius: 10px;
          font-size: 0.85rem;
          font-weight: 700;
          margin-bottom: 24px;
        }

        .btn-manage {
          width: 100%;
          padding: 14px;
          background: #0061ff;
          color: white;
          border-radius: 12px;
          font-weight: 700;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          box-shadow: 0 8px 20px rgba(0, 97, 255, 0.25);
          margin-top: auto;
        }

        .btn-manage:hover {
          transform: translateY(-2px);
          box-shadow: 0 12px 25px rgba(0, 97, 255, 0.35);
        }

        .bottom-grid {
          display: grid;
          grid-template-columns: 2fr 1fr;
          gap: 24px;
          margin-bottom: 48px;
        }

        .activity-list-container, .quick-action-card {
          background: white;
          border-radius: 24px;
          padding: 32px;
          box-shadow: 0 10px 30px rgba(13, 47, 90, 0.04);
          border: 1px solid rgba(224, 228, 236, 0.5);
          transition: all 0.2s ease;
        }

        .quick-action-card {
          cursor: pointer;
        }

        .quick-action-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 12px 35px rgba(5, 123, 254, 0.1);
          border-color: rgba(5, 123, 254, 0.3);
        }

        .quick-action-card:active {
          transform: translateY(0);
        }

        .activity-item-card {
          display: flex;
          gap: 20px;
          align-items: center;
        }

        .activity-icon-circle {
          width: 48px;
          height: 48px;
          background: var(--shape-light);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .activity-info p {
          font-size: 1rem;
          font-weight: 600;
          color: var(--text-main);
          margin: 0;
        }

        .activity-info span {
          font-size: 0.85rem;
          color: var(--text-muted);
        }

        .action-row {
          display: flex;
          align-items: center;
          gap: 20px;
          cursor: pointer;
        }

        .action-icon-circle {
          width: 40px;
          height: 40px;
          background: var(--shape-light);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .action-text {
          font-size: 1rem;
          font-weight: 700;
          color: var(--accent);
        }

        .dashboard-footer {
          text-align: center;
          padding: 32px 0;
          border-top: 1px solid var(--border-color);
        }

        .dashboard-footer p {
          font-size: 0.9rem;
          color: var(--text-muted);
          font-weight: 500;
        }

        @media (max-width: 1200px) {
          .bottom-grid {
            grid-template-columns: 1fr;
          }
        }
      `}} />
    </div>
  );
};

export default DashboardHome;


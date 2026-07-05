import React, { useState, useEffect, useRef } from 'react';
import toast from "react-hot-toast";
import { motion, AnimatePresence } from 'framer-motion';
import { 
  BarChart3, FileSpreadsheet, Download, RefreshCw, 
  Calendar, Users, GraduationCap, ArrowUpRight, CheckCircle2, 
  Database, ShieldCheck, Layers, FileText, AlertCircle, ChevronDown
} from 'lucide-react';
import * as XLSX from 'xlsx';
import { collection, onSnapshot, query, orderBy, getDocs } from 'firebase/firestore';
import { db } from '../firebase';
import { logActivity } from '../utils/activityLogger';

const ReportPage = ({ setPage }) => {
  const [batches, setBatches] = useState([]);
  const [students, setStudents] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedBatchId, setSelectedBatchId] = useState('');
  const [hoveredBatch, setHoveredBatch] = useState(null);
  
  // Interactive 3D Chart tilt state
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const chartCardRef = useRef(null);

  // Success alert state for downloads
  const [alertMessage, setAlertMessage] = useState(null);

  useEffect(() => {
    // 1. Listen to Batches in real-time
    const unsubBatches = onSnapshot(collection(db, 'Batches'), (snap) => {
      const fetched = snap.docs.map(doc => ({ docId: doc.id, ...doc.data() }));
      setBatches(fetched);
      if (fetched.length > 0 && !selectedBatchId) {
        setSelectedBatchId(fetched[0].batchId);
      }
    }, (error) => {
      console.error("Error listening to Batches:", error);
    });

    // 2. Listen to All Students in real-time
    const unsubStudents = onSnapshot(collection(db, 'AllStudents'), (snap) => {
      const fetched = snap.docs.map(doc => ({ docId: doc.id, ...doc.data() }));
      setStudents(fetched);
    }, (error) => {
      console.error("Error listening to AllStudents:", error);
    });

    // 3. Listen to all Activity Logs in real-time
    const qActs = query(collection(db, 'ActivityLogs'), orderBy('timestamp', 'desc'));
    const unsubActs = onSnapshot(qActs, (snap) => {
      const fetched = [];
      snap.forEach(doc => fetched.push({ id: doc.id, ...doc.data() }));
      setActivities(fetched);
      setLoading(false);
    }, (error) => {
      console.error("Error listening to ActivityLogs:", error);
      setLoading(false);
    });

    return () => {
      unsubBatches();
      unsubStudents();
      unsubActs();
    };
  }, []);

  // Compute enrollment data for the 3D chart
  const chartData = batches.map((batch, index) => {
    const count = students.filter(s => s.batchId === batch.batchId).length;
    // Map different premium neon gradients
    const gradients = [
      {
        front: 'linear-gradient(to top, rgba(5, 123, 254, 0.2), rgba(1, 201, 255, 0.8))',
        left: 'linear-gradient(to top, rgba(5, 123, 254, 0.15), rgba(1, 201, 255, 0.6))',
        right: 'linear-gradient(to top, rgba(5, 123, 254, 0.3), rgba(1, 201, 255, 0.9))',
        top: '#01C9FF',
        glow: 'rgba(1, 201, 255, 0.8)',
        textColor: '#057BFE'
      },
      {
        front: 'linear-gradient(to top, rgba(124, 58, 237, 0.2), rgba(236, 72, 153, 0.8))',
        left: 'linear-gradient(to top, rgba(124, 58, 237, 0.15), rgba(236, 72, 153, 0.6))',
        right: 'linear-gradient(to top, rgba(124, 58, 237, 0.3), rgba(236, 72, 153, 0.9))',
        top: '#EC4899',
        glow: 'rgba(236, 72, 153, 0.8)',
        textColor: '#7C3AED'
      },
      {
        front: 'linear-gradient(to top, rgba(5, 150, 105, 0.2), rgba(52, 211, 153, 0.8))',
        left: 'linear-gradient(to top, rgba(5, 150, 105, 0.15), rgba(52, 211, 153, 0.6))',
        right: 'linear-gradient(to top, rgba(5, 150, 105, 0.3), rgba(52, 211, 153, 0.9))',
        top: '#34D399',
        glow: 'rgba(52, 211, 153, 0.8)',
        textColor: '#059669'
      },
      {
        front: 'linear-gradient(to top, rgba(234, 88, 12, 0.2), rgba(251, 191, 36, 0.8))',
        left: 'linear-gradient(to top, rgba(234, 88, 12, 0.15), rgba(251, 191, 36, 0.6))',
        right: 'linear-gradient(to top, rgba(234, 88, 12, 0.3), rgba(251, 191, 36, 0.9))',
        top: '#FBBF24',
        glow: 'rgba(251, 191, 36, 0.8)',
        textColor: '#EA580C'
      }
    ];

    const styleTheme = gradients[index % gradients.length];

    return {
      ...batch,
      studentCount: count,
      theme: styleTheme
    };
  });

  const maxStudentCount = Math.max(...chartData.map(d => d.studentCount), 1);

  // Mouse tilt handlers for the 3D chart
  const handleMouseMove = (e) => {
    if (!chartCardRef.current) return;
    const rect = chartCardRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    
    // Rotate max 15 degrees
    const rotateX = -(y / rect.height) * 15;
    const rotateY = (x / rect.width) * 15;
    setTilt({ x: rotateX, y: rotateY });
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0 });
    setHoveredBatch(null);
  };

  // Excel exporter helper for selected batch
  const handleDownloadBatchReport = async () => {
    const batchDoc = batches.find(b => b.batchId === selectedBatchId);
    if (!batchDoc) {
      toast.error("Please select a valid batch first.")
      return;
    }

    const batchStudents = students.filter(s => s.batchId === selectedBatchId);
    if (batchStudents.length === 0) {
      toast.success(`No students are currently enrolled in batch "${batchDoc.batchName}".`)
      return;
    }

    try {
      const formattedData = batchStudents.map(student => ({
        "Student ID": student.studentId || '',
        "Full Name": student.fullName || '',
        "Email": student.email || '',
        "Status": student.status || 'Active',
        "Initial Password": student.initial_password || ''
      }));

      const ws = XLSX.utils.json_to_sheet(formattedData);
      
      // Beautiful column formatting widths
      ws['!cols'] = [
        { wch: 15 }, // Student ID
        { wch: 30 }, // Full Name
        { wch: 35 }, // Email
        { wch: 12 }, // Status
        { wch: 20 }  // Initial Password
      ];

      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Students Directory");

      const fileName = `Students_${batchDoc.batchName.replace(/\s+/g, '_')}.xlsx`;
      XLSX.writeFile(wb, fileName);

      // Log activity in Firestore
      await logActivity("Downloaded Batch Report", `Students directory downloaded for Batch ${batchDoc.batchName} (${batchStudents.length} Students)`);

      triggerAlert(`Successfully downloaded ${fileName}!`);
    } catch (err) {
      console.error("Error creating student Excel sheet: ", err);
      toast.error("Failed to generate Excel report. Please try again.")
    }
  };

  // Excel exporter helper for administrative Activity logs
  const handleDownloadActivityLogs = async () => {
    if (activities.length === 0) {
      toast.success("No activity logs available for export.")
      return;
    }

    try {
      const formattedLogs = activities.map(log => {
        let formattedDate = 'Just now';
        if (log.timestamp) {
          if (typeof log.timestamp.toDate === 'function') {
            formattedDate = log.timestamp.toDate().toLocaleString();
          } else if (log.timestamp instanceof Date) {
            formattedDate = log.timestamp.toLocaleString();
          } else if (log.timestamp.seconds) {
            formattedDate = new Date(log.timestamp.seconds * 1000).toLocaleString();
          }
        }
        return {
          "Action Done": log.action || '',
          "Details": log.details || '',
          "Date & Time": formattedDate
        };
      });

      const ws = XLSX.utils.json_to_sheet(formattedLogs);
      
      // Beautiful column formatting widths
      ws['!cols'] = [
        { wch: 25 }, // Action
        { wch: 60 }, // Details
        { wch: 25 }  // Timestamp
      ];

      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Admin Activity Logs");

      const fileName = "Admin_Activity_Logs.xlsx";
      XLSX.writeFile(wb, fileName);

      // Log activity in Firestore
      await logActivity("Downloaded Audit Logs", `Admin activity log exported (${activities.length} records)`);

      triggerAlert("Successfully downloaded Admin Activity Logs!");
    } catch (err) {
      console.error("Error exporting Activity Logs: ", err);
      toast.error("Failed to export activity logs.")
    }
  };

  const triggerAlert = (msg) => {
    setAlertMessage(msg);
    setTimeout(() => {
      setAlertMessage(null);
    }, 4000);
  };

  const selectedBatchObj = batches.find(b => b.batchId === selectedBatchId);
  const selectedBatchStudents = students.filter(s => s.batchId === selectedBatchId);

  return (
    <div className="reports-container">
      {/* Alert toast notification */}
      <AnimatePresence>
        {alertMessage && (
          <motion.div 
            initial={{ opacity: 0, y: -50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.9 }}
            className="toast-alert"
          >
            <CheckCircle2 size={20} color="#22c55e" />
            <span>{alertMessage}</span>
          </motion.div>
        )}
      </AnimatePresence>

      <header className="page-header">
        <div className="header-left">
          <div className="title-row">
            <BarChart3 className="header-icon" />
            <h1 className="page-title">Reports & Analytics Hub</h1>
          </div>
          <p className="page-subtitle">
            Generate production-ready Excel exports and interact with live 3D batch volume projections.
          </p>
        </div>
      </header>

      {loading ? (
        <div className="loading-state">
          <RefreshCw className="spinner" size={40} />
          <p>Connecting to database, generating structures...</p>
        </div>
      ) : (
        <div className="reports-grid">
          
          {/* Section 1: 3D Holographic Chart */}
          <section className="chart-section">
            <div 
              className="glass-panel interactive-chart-card"
              ref={chartCardRef}
              onMouseMove={handleMouseMove}
              onMouseLeave={handleMouseLeave}
              style={{
                transform: `perspective(1000px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
                transition: 'transform 0.1s ease-out'
              }}
            >
              <div className="card-header-3d">
                <div>
                  <h2 className="card-title-neon">Batch Volume Projection</h2>
                  <p className="card-desc">Interactive 3D structural cylinder projection showing real-time enrollment size. Hover to tilt the perspective plane.</p>
                </div>
                <div className="neon-status-pill">
                  <span className="pulse-dot"></span>
                  <span>LIVE FIRESTORE STREAM</span>
                </div>
              </div>

              <div className="chart-stage">
                {/* 3D Tilted Grid Floor */}
                <div className="chart-grid-floor">
                  {chartData.map((batchData, idx) => {
                    const heightPct = Math.max((batchData.studentCount / maxStudentCount) * 80, 5);
                    const isHovered = hoveredBatch && hoveredBatch.batchId === batchData.batchId;

                    return (
                      <div 
                        key={batchData.batchId} 
                        className="bar-container-3d"
                        onMouseEnter={() => setHoveredBatch(batchData)}
                      >
                        {/* Shadow base of bar */}
                        <div className="bar-base-shadow" style={{ boxShadow: `0 0 25px 10px ${batchData.theme.glow}` }} />

                        {/* Solid 3D Bar */}
                        <div 
                          className={`bar-3d ${isHovered ? 'hovered' : ''}`}
                          style={{
                            height: `${heightPct}%`,
                            '--bar-top-color': batchData.theme.top,
                            '--bar-glow': batchData.theme.glow
                          }}
                        >
                          <div className="bar-face face-front" style={{ background: batchData.theme.front }} />
                          <div className="bar-face face-left" style={{ background: batchData.theme.left }} />
                          <div className="bar-face face-right" style={{ background: batchData.theme.right }} />
                          <div className="bar-face face-top" style={{ background: batchData.theme.top }} />
                        </div>

                        {/* Label beneath the bar lying flat on the platform */}
                        <span className="bar-label-flat">
                          {batchData.batchName}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Holographic Tooltip Details Card */}
              <div className="holographic-legend">
                <div className="legend-title">Batch Analytics Dashboard</div>
                {hoveredBatch ? (
                  <motion.div 
                    initial={{ opacity: 0, x: 10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="legend-details"
                  >
                    <div className="detail-stat">
                      <span className="stat-num" style={{ color: hoveredBatch.theme.textColor }}>{hoveredBatch.studentCount}</span>
                      <span className="stat-lbl">Enrolled Students</span>
                    </div>
                    <div className="detail-meta">
                      <p><strong>Batch:</strong> {hoveredBatch.batchName}</p>
                      <p><strong>Batch ID:</strong> {hoveredBatch.batchId}</p>
                      <p><strong>Programme ID:</strong> {hoveredBatch.programId || 'N/A'}</p>
                    </div>
                  </motion.div>
                ) : (
                  <div className="legend-placeholder">
                    Hover over any 3D pillar to project detailed metrics and batch coordinates.
                  </div>
                )}

                <div className="color-legend-row">
                  {chartData.map((b) => (
                    <div key={b.batchId} className="legend-pill" onMouseEnter={() => setHoveredBatch(b)}>
                      <span className="legend-color-dot" style={{ backgroundColor: b.theme.top, boxShadow: `0 0 8px ${b.theme.glow}` }} />
                      <span>{b.batchName} ({b.studentCount})</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* Section 2: Cards for downloads */}
          <div className="downloads-section">
            
            {/* Card A: Student list exporter */}
            <div className="glass-panel download-card">
              <div className="card-top-accent-blue" />
              <div className="card-header">
                <div className="card-title-group">
                  <div className="icon-wrapper bg-blue">
                    <Users size={22} className="card-icon" />
                  </div>
                  <div>
                    <h3>Batch Directory Exporter</h3>
                    <p>Generate secure local Excel lists for batch student bodies</p>
                  </div>
                </div>
              </div>

              <div className="card-body">
                <div className="form-group">
                  <label htmlFor="batchSelect">Select Targeting Batch</label>
                  <div className="custom-select-wrapper">
                    <select 
                      id="batchSelect" 
                      value={selectedBatchId} 
                      onChange={(e) => setSelectedBatchId(e.target.value)}
                      className="styled-select"
                    >
                      {batches.map(b => (
                        <option key={b.batchId} value={b.batchId}>
                          {b.batchName} ({students.filter(s => s.batchId === b.batchId).length} Students)
                        </option>
                      ))}
                    </select>
                    <ChevronDown size={18} className="select-arrow" />
                  </div>
                </div>

                {selectedBatchObj && (
                  <div className="batch-quick-preview">
                    <div className="preview-header">
                      <span>Data Stream Preview ({selectedBatchStudents.length} Records)</span>
                    </div>
                    {selectedBatchStudents.length > 0 ? (
                      <div className="preview-list">
                        {selectedBatchStudents.slice(0, 3).map((st) => (
                          <div key={st.studentId} className="preview-item">
                            <span className="preview-id">{st.studentId}</span>
                            <span className="preview-name">{st.fullName}</span>
                            <span className="preview-email">{st.email}</span>
                          </div>
                        ))}
                        {selectedBatchStudents.length > 3 && (
                          <div className="preview-more">
                            + {selectedBatchStudents.length - 3} more records included in spreadsheet
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="preview-empty">
                        <AlertCircle size={16} />
                        <span>No active student profiles currently assigned to this batch.</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="card-footer">
                <button 
                  onClick={handleDownloadBatchReport} 
                  disabled={selectedBatchStudents.length === 0}
                  className="download-btn btn-blue"
                >
                  <FileSpreadsheet size={18} />
                  <span>Download Student Report (.xlsx)</span>
                  <ArrowUpRight size={16} className="btn-arrow" />
                </button>
              </div>
            </div>

            {/* Card B: Activity logs exporter */}
            <div className="glass-panel download-card">
              <div className="card-top-accent-purple" />
              <div className="card-header">
                <div className="card-title-group">
                  <div className="icon-wrapper bg-purple">
                    <ShieldCheck size={22} className="card-icon" />
                  </div>
                  <div>
                    <h3>Security & Audit Log Center</h3>
                    <p>Export all registered administrator actions and updates</p>
                  </div>
                </div>
              </div>

              <div className="card-body">
                <div className="audit-logs-preview">
                  <div className="preview-header">
                    <span>Active Audit Logs ({activities.length} Recorded Actions)</span>
                  </div>
                  <div className="logs-scroller">
                    {activities.length > 0 ? (
                      activities.slice(0, 5).map((log) => {
                        let displayDate = 'Just now';
                        if (log.timestamp) {
                          if (typeof log.timestamp.toDate === 'function') {
                            displayDate = log.timestamp.toDate().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                          } else if (log.timestamp.seconds) {
                            displayDate = new Date(log.timestamp.seconds * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                          }
                        }
                        return (
                          <div key={log.id} className="log-item">
                            <div className="log-dot" />
                            <div className="log-info">
                              <span className="log-action">{log.action}</span>
                              <span className="log-details">{log.details}</span>
                            </div>
                            <span className="log-time">{displayDate}</span>
                          </div>
                        );
                      })
                    ) : (
                      <div className="preview-empty">
                        <AlertCircle size={16} />
                        <span>No system activity logs populated yet.</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="card-footer">
                <button 
                  onClick={handleDownloadActivityLogs} 
                  disabled={activities.length === 0}
                  className="download-btn btn-purple"
                >
                  <Download size={18} />
                  <span>Download Activity Log (.xlsx)</span>
                  <ArrowUpRight size={16} className="btn-arrow" />
                </button>
              </div>
            </div>

          </div>
        </div>
      )}

      {/* Styled JSX for the custom premium visual layout */}
      <style dangerouslySetInnerHTML={{ __html: `
        .reports-container {
          position: relative;
          z-index: 1;
        }

        /* Toast notification styling */
        .toast-alert {
          position: fixed;
          top: 30px;
          right: 30px;
          z-index: 9999;
          background: rgba(255, 255, 255, 0.9);
          border-left: 5px solid #22c55e;
          padding: 16px 24px;
          border-radius: 12px;
          box-shadow: 0 20px 40px rgba(13, 47, 90, 0.15);
          display: flex;
          align-items: center;
          gap: 12px;
          font-weight: 600;
          color: #0D2F5A;
          backdrop-filter: blur(10px);
          border: 1px solid rgba(224, 228, 236, 0.5);
        }

        .page-header {
          margin-bottom: 32px;
        }

        .title-row {
          display: flex;
          align-items: center;
          gap: 16px;
          margin-bottom: 8px;
        }

        .header-icon {
          color: var(--accent);
          width: 32px;
          height: 32px;
        }

        .page-title {
          font-size: 2.2rem;
          font-weight: 800;
          color: var(--primary);
          letter-spacing: -0.5px;
        }

        .page-subtitle {
          font-size: 1rem;
          color: var(--text-muted);
          max-width: 700px;
          font-weight: 500;
        }

        .loading-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          height: 350px;
          gap: 16px;
          color: var(--text-muted);
        }

        .spinner {
          animation: spin 1s linear infinite;
          color: var(--accent);
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        .reports-grid {
          display: grid;
          grid-template-columns: 1.2fr 1fr;
          gap: 32px;
          align-items: start;
        }

        @media (max-width: 1200px) {
          .reports-grid {
            grid-template-columns: 1fr;
          }
        }

        /* 3D Holographic Chart Section */
        .chart-section {
          perspective: 1000px;
        }

        .interactive-chart-card {
          padding: 30px;
          transform-style: preserve-3d;
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.8) 100%);
          border: 1px solid rgba(255, 255, 255, 0.7);
          box-shadow: 0 30px 60px rgba(13, 47, 90, 0.08);
          position: relative;
        }

        .card-header-3d {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 40px;
          gap: 16px;
        }

        .card-title-neon {
          font-size: 1.4rem;
          font-weight: 800;
          color: var(--primary);
          margin-bottom: 6px;
          letter-spacing: -0.2px;
        }

        .card-desc {
          font-size: 0.88rem;
          color: var(--text-muted);
          max-width: 480px;
          line-height: 1.4;
          font-weight: 500;
        }

        .neon-status-pill {
          display: flex;
          align-items: center;
          gap: 8px;
          background: rgba(5, 123, 254, 0.06);
          border: 1px solid rgba(5, 123, 254, 0.15);
          padding: 6px 12px;
          border-radius: 20px;
          font-size: 0.75rem;
          font-weight: 700;
          color: var(--accent);
          letter-spacing: 0.5px;
        }

        .pulse-dot {
          width: 8px;
          height: 8px;
          background-color: var(--accent);
          border-radius: 50%;
          box-shadow: 0 0 10px var(--accent);
          animation: pulse 1.5s ease-in-out infinite;
        }

        @keyframes pulse {
          0% { transform: scale(0.9); opacity: 0.6; }
          50% { transform: scale(1.2); opacity: 1; box-shadow: 0 0 14px var(--accent); }
          100% { transform: scale(0.9); opacity: 0.6; }
        }

        /* 3D Chart Stage */
        .chart-stage {
          height: 300px;
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
          margin-bottom: 24px;
        }

        .chart-grid-floor {
          width: 90%;
          height: 240px;
          position: relative;
          transform: rotateX(60deg) rotateZ(-22deg);
          transform-style: preserve-3d;
          background: 
            radial-gradient(ellipse at center, rgba(5, 123, 254, 0.02) 0%, rgba(13, 47, 90, 0.06) 100%),
            linear-gradient(rgba(224, 228, 236, 0.25) 1px, transparent 1px),
            linear-gradient(90deg, rgba(224, 228, 236, 0.25) 1px, transparent 1px);
          background-size: 100% 100%, 20px 20px, 20px 20px;
          border: 1.5px dashed rgba(5, 123, 254, 0.2);
          border-radius: 12px;
          display: flex;
          justify-content: space-around;
          align-items: flex-end;
          padding: 20px;
        }

        /* 3D Bar Elements */
        .bar-container-3d {
          position: relative;
          width: 32px;
          height: 100%;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          align-items: center;
          transform-style: preserve-3d;
          cursor: pointer;
        }

        .bar-base-shadow {
          position: absolute;
          width: 32px;
          height: 32px;
          bottom: -16px;
          border-radius: 50%;
          transform: rotateX(90deg);
          pointer-events: none;
          opacity: 0.6;
          transition: all 0.3s ease;
        }

        .bar-3d {
          position: relative;
          width: 32px;
          transform-style: preserve-3d;
          transform: rotateX(-90deg);
          transform-origin: bottom center;
          transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), filter 0.3s;
        }

        .bar-3d.hovered {
          transform: rotateX(-90deg) scaleY(1.08) scaleX(1.15) scaleZ(1.15);
          filter: brightness(1.15);
        }

        .bar-face {
          position: absolute;
          bottom: 0;
          width: 32px;
          height: 100%;
          border-radius: 1px;
        }

        .face-front {
          transform: rotateY(0deg) translateZ(16px);
          border-left: 1px solid rgba(255, 255, 255, 0.25);
          border-right: 1px solid rgba(255, 255, 255, 0.25);
        }

        .face-left {
          transform: rotateY(-90deg) translateZ(16px);
          filter: brightness(0.9);
        }

        .face-right {
          transform: rotateY(90deg) translateZ(16px);
          border-left: 1px solid rgba(255, 255, 255, 0.2);
          filter: brightness(1.1);
        }

        .face-top {
          width: 32px;
          height: 32px;
          bottom: auto;
          top: -16px;
          transform: rotateX(90deg);
          border-radius: 0px;
          box-shadow: 0 0 12px var(--bar-glow);
        }

        /* Label laying flat on tilted floor */
        .bar-label-flat {
          position: absolute;
          bottom: -40px;
          font-size: 0.72rem;
          font-weight: 700;
          color: var(--text-muted);
          transform: rotateX(0deg) rotateZ(0deg);
          white-space: nowrap;
          letter-spacing: 0.2px;
          text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
        }

        .bar-container-3d:hover .bar-label-flat {
          color: var(--primary);
          font-weight: 800;
        }

        /* Holographic Legend Panel */
        .holographic-legend {
          background: rgba(255, 255, 255, 0.6);
          border: 1px solid rgba(224, 228, 236, 0.5);
          border-radius: var(--radius-md);
          padding: 20px;
          margin-top: 16px;
        }

        .legend-title {
          font-size: 0.85rem;
          text-transform: uppercase;
          letter-spacing: 1px;
          font-weight: 700;
          color: var(--text-muted);
          margin-bottom: 12px;
        }

        .legend-details {
          display: flex;
          gap: 24px;
          align-items: center;
          background: rgba(255, 255, 255, 0.8);
          padding: 12px 18px;
          border-radius: 8px;
          margin-bottom: 16px;
          border: 1px solid rgba(224, 228, 236, 0.3);
        }

        .detail-stat {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-width: 60px;
        }

        .stat-num {
          font-size: 2.2rem;
          font-weight: 800;
          line-height: 1;
        }

        .stat-lbl {
          font-size: 0.72rem;
          color: var(--text-muted);
          font-weight: 600;
          white-space: nowrap;
          margin-top: 4px;
        }

        .detail-meta p {
          font-size: 0.85rem;
          margin-bottom: 4px;
          color: var(--text-main);
        }
        
        .detail-meta p strong {
          color: var(--text-muted);
        }

        .legend-placeholder {
          font-size: 0.88rem;
          color: var(--text-muted);
          font-style: italic;
          padding: 12px 0;
          margin-bottom: 16px;
        }

        .color-legend-row {
          display: flex;
          flex-wrap: wrap;
          gap: 12px;
          border-top: 1px solid rgba(224, 228, 236, 0.5);
          padding-top: 16px;
        }

        .legend-pill {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.8rem;
          font-weight: 600;
          color: var(--text-main);
          background: rgba(255, 255, 255, 0.7);
          padding: 6px 12px;
          border-radius: 20px;
          border: 1px solid rgba(224, 228, 236, 0.3);
          cursor: pointer;
          transition: all 0.2s;
        }

        .legend-pill:hover {
          background: white;
          transform: translateY(-1px);
          box-shadow: 0 4px 8px rgba(13, 47, 90, 0.04);
        }

        .legend-color-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
        }

        /* Downloads Section Grid */
        .downloads-section {
          display: flex;
          flex-direction: column;
          gap: 32px;
        }

        .download-card {
          position: relative;
          overflow: hidden;
          background: white;
          border: 1px solid rgba(224, 228, 236, 0.8);
          box-shadow: var(--shadow-md);
        }

        .card-top-accent-blue {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 4px;
          background: var(--premium-gradient);
        }

        .card-top-accent-purple {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 4px;
          background: linear-gradient(135deg, #7C3AED 0%, #EC4899 100%);
        }

        .card-header {
          padding: 24px 28px;
          border-bottom: 1px solid rgba(224, 228, 236, 0.5);
        }

        .card-title-group {
          display: flex;
          gap: 16px;
          align-items: center;
        }

        .icon-wrapper {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .icon-wrapper.bg-blue { background: rgba(5, 123, 254, 0.08); color: var(--accent); }
        .icon-wrapper.bg-purple { background: rgba(124, 58, 237, 0.08); color: #7C3AED; }

        .card-header h3 {
          font-size: 1.15rem;
          font-weight: 750;
          color: var(--primary);
          margin-bottom: 2px;
        }

        .card-header p {
          font-size: 0.82rem;
          color: var(--text-muted);
          font-weight: 500;
        }

        .card-body {
          padding: 28px;
        }

        /* Form styling */
        .form-group {
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-bottom: 20px;
        }

        .form-group label {
          font-size: 0.85rem;
          font-weight: 700;
          color: var(--text-muted);
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .custom-select-wrapper {
          position: relative;
          width: 100%;
        }

        .styled-select {
          width: 100%;
          background: #F8FAFC;
          border: 1px solid rgba(224, 228, 236, 0.8);
          border-radius: 10px;
          padding: 12px 16px;
          font-size: 0.95rem;
          font-weight: 600;
          color: var(--text-main);
          appearance: none;
          cursor: pointer;
          outline: none;
          transition: all 0.2s;
        }

        .styled-select:focus {
          border-color: var(--accent);
          background: white;
          box-shadow: 0 0 0 3px rgba(5, 123, 254, 0.1);
        }

        .select-arrow {
          position: absolute;
          right: 16px;
          top: 50%;
          transform: translateY(-50%);
          pointer-events: none;
          color: var(--text-muted);
        }

        /* Quick previews */
        .batch-quick-preview {
          background: #F8FAFC;
          border: 1px solid rgba(224, 228, 236, 0.5);
          border-radius: 10px;
          padding: 16px;
        }

        .preview-header {
          font-size: 0.75rem;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          font-weight: 750;
          color: var(--text-muted);
          margin-bottom: 12px;
          display: block;
        }

        .preview-list {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .preview-item {
          display: flex;
          gap: 12px;
          font-size: 0.82rem;
          background: white;
          padding: 8px 12px;
          border-radius: 6px;
          border: 1px solid rgba(224, 228, 236, 0.3);
          align-items: center;
        }

        .preview-id {
          font-family: monospace;
          font-weight: 700;
          color: var(--accent);
          min-width: 65px;
        }

        .preview-name {
          font-weight: 600;
          color: var(--text-main);
          flex: 1;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .preview-email {
          color: var(--text-muted);
          font-size: 0.78rem;
        }

        .preview-more {
          font-size: 0.78rem;
          color: var(--text-muted);
          font-style: italic;
          text-align: center;
          margin-top: 4px;
        }

        .preview-empty {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          padding: 12px;
          color: var(--text-muted);
          font-size: 0.82rem;
          font-weight: 500;
        }

        /* Audit logs preview */
        .audit-logs-preview {
          background: #F8FAFC;
          border: 1px solid rgba(224, 228, 236, 0.5);
          border-radius: 10px;
          padding: 16px;
        }

        .logs-scroller {
          display: flex;
          flex-direction: column;
          gap: 10px;
          max-height: 220px;
          overflow-y: auto;
          padding-right: 4px;
        }

        .logs-scroller::-webkit-scrollbar {
          width: 5px;
        }
        .logs-scroller::-webkit-scrollbar-thumb {
          background: rgba(13, 47, 90, 0.1);
          border-radius: 4px;
        }

        .log-item {
          display: flex;
          align-items: center;
          gap: 12px;
          font-size: 0.82rem;
          background: white;
          padding: 10px 14px;
          border-radius: 8px;
          border: 1px solid rgba(224, 228, 236, 0.3);
        }

        .log-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #7C3AED;
          flex-shrink: 0;
          box-shadow: 0 0 6px rgba(124, 58, 237, 0.6);
        }

        .log-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .log-action {
          font-weight: 700;
          color: var(--primary);
        }

        .log-details {
          color: var(--text-muted);
          font-size: 0.78rem;
        }

        .log-time {
          font-size: 0.75rem;
          color: var(--text-muted);
          font-weight: 600;
        }

        /* Button Footer */
        .card-footer {
          padding: 0 28px 28px 28px;
        }

        .download-btn {
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          padding: 14px;
          border-radius: 10px;
          font-weight: 700;
          font-size: 0.92rem;
          color: white;
          box-shadow: 0 10px 20px -5px rgba(13, 47, 90, 0.15);
          position: relative;
          overflow: hidden;
        }

        .download-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
          box-shadow: none;
        }

        .download-btn .btn-arrow {
          position: absolute;
          right: 20px;
          transition: transform 0.2s ease;
        }

        .download-btn:not(:disabled):hover .btn-arrow {
          transform: translate(2px, -2px);
        }

        .btn-blue {
          background: var(--btn-gradient);
        }
        .btn-blue:not(:disabled):hover {
          background: linear-gradient(135deg, #0054bc 0%, #0076de 100%);
          box-shadow: 0 12px 24px -5px rgba(5, 123, 254, 0.35);
        }

        .btn-purple {
          background: linear-gradient(135deg, #6D28D9 0%, #DB2777 100%);
        }
        .btn-purple:not(:disabled):hover {
          background: linear-gradient(135deg, #5B21B6 0%, #C2185B 100%);
          box-shadow: 0 12px 24px -5px rgba(124, 58, 237, 0.35);
        }
      `}} />
    </div>
  );
};

export default ReportPage;

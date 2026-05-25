import React from 'react';
import { motion } from 'framer-motion';
import { Search, Filter, MoreVertical, Eye } from 'lucide-react';

const StudentManager = () => {
  const students = [
    { id: 'IT210045', name: 'Kasun Perera', cgpa: '3.85', status: 'Active', semester: 'Sem 6' },
    { id: 'IT210122', name: 'Hansani Silva', cgpa: '3.42', status: 'Warning', semester: 'Sem 4' },
    { id: 'IT220566', name: 'Nimal Jayasuriya', cgpa: '2.95', status: 'Active', semester: 'Sem 2' },
    { id: 'IT210988', name: 'Priya Kumari', cgpa: '3.68', status: 'Active', semester: 'Sem 6' },
    { id: 'IT230112', name: 'Arjun Das', cgpa: '3.10', status: 'Inactive', semester: 'Sem 1' },
  ];

  return (
    <div className="student-manager">
      <div className="page-header-row">
        <h1 className="page-title">Student Records</h1>
        <button className="btn-primary">
          Register New Student
        </button>
      </div>

      <div className="glass-panel table-container">
        <div className="table-controls">
          <div className="search-box">
            <Search size={18} />
            <input type="text" placeholder="Search by ID or Name..." />
          </div>
          <div className="filter-group">
            <button className="filter-btn">
              <Filter size={16} /> Filter
            </button>
          </div>
        </div>

        <table className="modern-table">
          <thead>
            <tr>
              <th>Student ID</th>
              <th>Full Name</th>
              <th>Current CGPA</th>
              <th>Semester</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {students.map((student, i) => (
              <motion.tr 
                key={student.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
              >
                <td className="id-cell">{student.id}</td>
                <td className="name-cell">{student.name}</td>
                <td>
                  <span className="cgpa-badge">{student.cgpa}</span>
                </td>
                <td>{student.semester}</td>
                <td>
                  <span className={`status-pill ${student.status.toLowerCase()}`}>
                    {student.status}
                  </span>
                </td>
                <td>
                  <div className="action-btns">
                    <button className="icon-action-btn"><Eye size={16} /></button>
                    <button className="icon-action-btn"><MoreVertical size={16} /></button>
                  </div>
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>

        <div className="table-footer">
          <p>Showing 5 of 1,284 students</p>
          <div className="pagination">
            <button disabled>Prev</button>
            <button className="active">1</button>
            <button>2</button>
            <button>3</button>
            <button>Next</button>
          </div>
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .page-header-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 24px;
        }

        .table-container {
          padding: 0;
          overflow: hidden;
        }

        .table-controls {
          padding: 20px 24px;
          display: flex;
          justify-content: space-between;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }

        .search-box {
          display: flex;
          align-items: center;
          gap: 12px;
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid rgba(255, 255, 255, 0.05);
          padding: 8px 16px;
          border-radius: 10px;
          width: 320px;
        }

        .search-box input {
          background: transparent;
          border: none;
          padding: 0;
          font-size: 0.9rem;
        }

        .filter-btn {
          background: rgba(255, 255, 255, 0.05);
          color: white;
          border: 1px solid rgba(255, 255, 255, 0.1);
          padding: 8px 16px;
          border-radius: 10px;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.9rem;
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
          font-weight: 500;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }

        .modern-table td {
          padding: 16px 24px;
          font-size: 0.95rem;
          border-bottom: 1px solid rgba(255, 255, 255, 0.02);
        }

        .id-cell { font-family: monospace; font-weight: 600; color: var(--primary); }
        .name-cell { font-weight: 500; }

        .cgpa-badge {
          background: rgba(255, 255, 255, 0.05);
          padding: 4px 10px;
          border-radius: 6px;
          font-weight: 600;
        }

        .status-pill {
          padding: 4px 12px;
          border-radius: 20px;
          font-size: 0.8rem;
          font-weight: 600;
        }

        .status-pill.active { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
        .status-pill.warning { background: rgba(234, 179, 8, 0.1); color: #eab308; }
        .status-pill.inactive { background: rgba(148, 163, 184, 0.1); color: #94a3b8; }

        .action-btns {
          display: flex;
          gap: 8px;
        }

        .icon-action-btn {
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
          padding: 4px;
          border-radius: 6px;
          transition: all 0.2s;
        }

        .icon-action-btn:hover {
          background: rgba(255, 255, 255, 0.05);
          color: white;
        }

        .table-footer {
          padding: 16px 24px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 0.85rem;
          color: var(--text-muted);
        }

        .pagination {
          display: flex;
          gap: 8px;
        }

        .pagination button {
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid rgba(255, 255, 255, 0.05);
          color: white;
          padding: 4px 12px;
          border-radius: 6px;
          cursor: pointer;
        }

        .pagination button.active {
          background: var(--primary);
          border-color: var(--primary);
        }

        .pagination button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      `}} />
    </div>
  );
};

export default StudentManager;

import React from 'react';
import { Search, Bell, UserCircle, Settings } from 'lucide-react';

const Header = ({ title, subtitle }) => {
  return (
    <div className="header-container">
      <div className="header-text">
        <h1>{title || 'Dashboard'}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>

      <div className="header-actions">
        <div className="search-box">
          <Search size={18} color="var(--text-muted)" />
          <input type="text" placeholder="Search..." />
        </div>
        
        <div className="action-icons">
          <div className="icon-btn">
            <Bell size={20} />
            <span className="notification-dot"></span>
          </div>
          <div className="icon-btn">
            <Settings size={20} />
          </div>
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .header-container {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;
          margin-bottom: 32px;
        }

        .header-text h1 {
          font-size: 1.5rem;
          font-weight: 800;
          color: var(--text-main);
          margin: 0;
        }

        .header-text p {
          font-size: 0.9rem;
          color: var(--text-muted);
          margin: 4px 0 0 0;
        }

        .header-actions {
          display: flex;
          align-items: center;
          gap: 24px;
        }

        .search-box {
          display: flex;
          align-items: center;
          gap: 12px;
          background: white;
          padding: 10px 16px;
          border-radius: 12px;
          box-shadow: var(--shadow-sm);
          border: 1px solid var(--border-color);
          width: 300px;
        }

        .search-box input {
          border: none;
          background: transparent;
          outline: none;
          font-size: 0.9rem;
          width: 100%;
          color: var(--text-main);
        }

        .action-icons {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .icon-btn {
          width: 40px;
          height: 40px;
          background: white;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: var(--text-muted);
          cursor: pointer;
          transition: all 0.2s;
          position: relative;
          box-shadow: var(--shadow-sm);
          border: 1px solid var(--border-color);
        }

        .icon-btn:hover {
          color: var(--primary);
          background: var(--primary-light);
        }

        .notification-dot {
          position: absolute;
          top: 10px;
          right: 10px;
          width: 8px;
          height: 8px;
          background: #ff4d4f;
          border-radius: 50%;
          border: 2px solid white;
        }
      `}} />
    </div>
  );
};

export default Header;

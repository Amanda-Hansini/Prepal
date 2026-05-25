import React, { useState, useRef } from 'react';
import { Home, BarChart3, User, ChevronDown } from 'lucide-react';

const Sidebar = ({ activePage, setPage, currentAdmin }) => {
  const menuItems = [
    { id: 'dashboard', label: 'Home', icon: Home },
    { id: 'report', label: 'Report', icon: BarChart3 },
    { id: 'profile', label: 'Profile', icon: User },
  ];

  // 3D Tilt state
  const [rotateX, setRotateX] = useState(0);
  const [rotateY, setRotateY] = useState(0);
  const sidebarRef = useRef(null);

  const handleMouseMove = (e) => {
    if (!sidebarRef.current || window.innerWidth <= 1024) return;
    
    const rect = sidebarRef.current.getBoundingClientRect();
    const width = rect.width;
    const height = rect.height;
    
    // Cursor position relative to element center
    const mouseX = e.clientX - rect.left - width / 2;
    const mouseY = e.clientY - rect.top - height / 2;

    // Small controlled tilt (max 7 degrees)
    const maxTilt = 7;
    const rX = -(mouseY / (height / 2)) * maxTilt;
    const rY = (mouseX / (width / 2)) * maxTilt;

    setRotateX(rX);
    setRotateY(rY);
  };

  const handleMouseLeave = () => {
    // Reset back smoothly
    setRotateX(0);
    setRotateY(0);
  };

  const isCenter = rotateX === 0 && rotateY === 0;

  return (
    <aside 
      ref={sidebarRef}
      className="sidebar"
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{
        transform: rotateX === 0 && rotateY === 0
          ? 'perspective(1000px) rotateX(0deg) rotateY(0deg)'
          : `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
        transition: isCenter 
          ? 'transform 0.6s cubic-bezier(0.25, 1, 0.5, 1), box-shadow 0.3s ease' 
          : 'transform 0.08s ease-out, box-shadow 0.3s ease'
      }}
    >
      <div className="sidebar-brand">
        <div className="brand-logo-container">
          <img src="/logo.png" alt="PrePal Logo" className="sidebar-logo" />
        </div>
        <div className="brand-text">
          <h1>PrePal</h1>
          <span>Admin Panel</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = activePage === item.id;

          return (
            <div
              key={item.id}
              className={`nav-item ${isActive ? 'active' : ''}`}
              onClick={() => setPage(item.id)}
            >
              <div className="nav-icon-wrapper">
                <Icon size={20} />
              </div>
              <span>{item.label}</span>
            </div>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile-card" onClick={() => setPage('profile')} style={{ cursor: 'pointer' }}>
          <div className="user-avatar-circle" style={{ overflow: 'hidden', padding: 0 }}>
            {currentAdmin?.profile_image_base64 ? (
              <img 
                src={`data:image/jpeg;base64,${currentAdmin.profile_image_base64}`} 
                alt="Avatar" 
                style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
              />
            ) : (
              <User size={24} color="white" fill="white" />
            )}
          </div>
          <div className="user-details">
            <p className="user-name">{currentAdmin?.full_name || 'Admin User'}</p>
            <p className="user-role">{currentAdmin?.status || 'Active'} Admin</p>
          </div>
          <ChevronDown size={18} className="user-dropdown-icon" />
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .sidebar {
          position: fixed;
          left: 20px;
          top: 20px;
          bottom: 20px;
          width: 240px;
          background: #E8EEF5; /* Cool light slate-blue background */
          border-radius: 24px;
          display: flex;
          flex-direction: column;
          padding: 36px 20px;
          z-index: 100;

          /* 3D Setup & Specular Edge Borders */
          transform-style: preserve-3d;
          border-top: 1.5px solid rgba(255, 255, 255, 0.95);
          border-left: 1.5px solid rgba(255, 255, 255, 0.95);
          border-right: 2px solid rgba(13, 47, 90, 0.12);
          border-bottom: 4px solid rgba(13, 47, 90, 0.22);
          box-shadow: 
            0 12px 32px rgba(13, 47, 90, 0.08), 
            0 4px 10px rgba(13, 47, 90, 0.04),   
            inset 0 2px 2px rgba(255, 255, 255, 0.9), 
            inset 0 -2px 3px rgba(13, 47, 90, 0.06);
        }

        /* 3D Extrusion Back-Plate (creates physical depth behind the main panel) */
        .sidebar::before {
          content: '';
          position: absolute;
          inset: -1px;
          background: #D5DFEC; /* Deeper cool slate depth tone */
          border-radius: 24px;
          transform: translateY(5px) translateZ(-12px);
          z-index: -1;
          box-shadow: 
            0 16px 36px rgba(13, 47, 90, 0.12),
            0 4px 12px rgba(13, 47, 90, 0.05);
          border-bottom: 2px solid rgba(13, 47, 90, 0.15);
        }

        /* Specular Diagonal Reflection overlay */
        .sidebar::after {
          content: '';
          position: absolute;
          top: 0; left: 0; right: 0;
          height: 45%;
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0) 100%);
          border-radius: 24px 24px 0 0;
          pointer-events: none;
          z-index: 2;
        }

        .sidebar-brand {
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          margin-bottom: 48px;
          transform: translateZ(20px); /* Float brand components on high tier */
          transform-style: preserve-3d;
        }

        .brand-logo-container {
          background: white;
          padding: 8px;
          border-radius: 20px;
          box-shadow: 0 4px 12px rgba(13, 47, 90, 0.04);
          transform: translateZ(10px);
          margin-bottom: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .sidebar-logo {
          width: 80px;
          height: auto;
        }

        .brand-text h1 {
          font-size: 1.85rem;
          font-weight: 800;
          color: var(--primary);
          margin: 0;
          letter-spacing: -0.5px;
          transform: translateZ(12px);
        }

        .brand-text span {
          font-size: 0.9rem;
          color: var(--text-muted);
          font-weight: 500;
          transform: translateZ(8px);
        }

        .sidebar-nav {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 14px;
          transform: translateZ(14px); /* Float menus on mid tier */
          transform-style: preserve-3d;
        }

        .nav-item {
          display: flex;
          align-items: center;
          gap: 14px;
          padding: 12px 18px;
          border-radius: 12px;
          color: #50647D;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
          border: 1px solid transparent;
          transform-style: preserve-3d;
        }

        .nav-item:hover {
          background: rgba(13, 47, 90, 0.05);
          color: var(--primary);
          transform: translateZ(8px) translateX(2px);
          box-shadow: 
            0 4px 8px rgba(13, 47, 90, 0.05),
            inset 0 1px 1px rgba(255, 255, 255, 0.7);
        }

        .nav-item.active {
          background: var(--accent);
          color: white;
          border: 1.5px solid rgba(255, 255, 255, 0.25);
          box-shadow: 
            0 8px 20px rgba(5, 123, 254, 0.28), 
            inset 0 2px 2px rgba(255, 255, 255, 0.45), 
            inset 0 -2px 3px rgba(0, 0, 0, 0.25),
            0 0 0 3px rgba(5, 123, 254, 0.08);
          transform: scale(1.03) translateZ(12px);
        }

        .nav-icon-wrapper {
          display: flex;
          align-items: center;
          justify-content: center;
          transform: translateZ(4px);
        }

        .nav-item span {
          transform: translateZ(4px);
        }

        .sidebar-footer {
          margin-top: auto;
          padding-top: 20px;
          transform: translateZ(18px); /* Footer card floats highest */
          transform-style: preserve-3d;
        }

        .user-profile-card {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 14px;
          background: #FFFFFF;
          border-radius: 20px;
          box-shadow: 
            0 6px 14px rgba(13, 47, 90, 0.04), 
            inset 0 1px 1px rgba(255, 255, 255, 0.95);
          border-top: 1px solid rgba(255, 255, 255, 0.95);
          border-left: 1px solid rgba(255, 255, 255, 0.95);
          border-right: 1.5px solid rgba(13, 47, 90, 0.08);
          border-bottom: 3.5px solid rgba(13, 47, 90, 0.14); /* Beveled 3D foot */
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
          transform-style: preserve-3d;
        }

        .user-profile-card:hover {
          background: #FFFFFF;
          border-color: var(--accent);
          transform: translateZ(12px) translateY(-3px) scale(1.02);
          box-shadow: 
            0 12px 24px rgba(13, 47, 90, 0.08),
            inset 0 1px 1px rgba(255, 255, 255, 0.95);
        }

        .user-avatar-circle {
          width: 40px;
          height: 40px;
          background: #007bff;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          transform: translateZ(6px);
        }

        .user-details {
          flex: 1;
          overflow: hidden;
          transform: translateZ(6px);
        }

        .user-name {
          font-size: 0.9rem;
          font-weight: 700;
          color: var(--text-main);
          margin: 0;
          white-space: nowrap;
          text-overflow: ellipsis;
        }

        .user-role {
          font-size: 0.78rem;
          color: var(--text-muted);
          margin: 1px 0 0 0;
        }

        .user-dropdown-icon {
          color: var(--text-muted);
          transform: translateZ(6px);
        }

        @media (max-width: 1024px) {
          .sidebar {
            left: 0;
            top: 0;
            bottom: 0;
            width: 280px;
            border-radius: 0;
            border: none;
            border-right: 1px solid rgba(200, 214, 230, 0.8);
            box-shadow: 4px 0 20px rgba(13, 47, 90, 0.05);
            background: #E8EEF5;
            transform: translateX(-100%) !important;
            transition: transform 0.3s ease !important;
            padding: 40px 24px;
          }
          .sidebar::before, .sidebar::after {
            display: none; /* Disable 3D depth borders in mobile flyout drawer */
          }
          .sidebar.open {
            transform: translateX(0) !important;
          }
        }
      `}} />
    </aside>
  );
};

export default Sidebar;

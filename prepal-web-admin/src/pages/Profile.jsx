import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  User, 
  Mail, 
  Shield, 
  Lock, 
  LogOut, 
  Camera, 
  Save, 
  Eye, 
  EyeOff, 
  CheckCircle, 
  AlertCircle, 
  Settings, 
  Bell, 
  KeyRound, 
  Info,
  Loader2
} from 'lucide-react';
import { db } from '../firebase';
import { doc, updateDoc } from 'firebase/firestore';
import { hashPassword } from '../utils/security';
import { logActivity } from '../utils/activityLogger';

const Profile = ({ currentAdmin, setPage, onLogout }) => {
  // Input fields
  const [fullName, setFullName] = useState(currentAdmin?.full_name || '');
  const [email, setEmail] = useState(currentAdmin?.email || '');
  
  // Change Password state
  const [showPasswordSection, setShowPasswordSection] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  
  // Settings switches (synced with Firestore and localStorage to replicate Android SharedPreferences)
  const [settingsEmailNotif, setSettingsEmailNotif] = useState(currentAdmin?.emailAlertsEnabled !== false); // default true
  const [settingsSystemAlerts, setSettingsSystemAlerts] = useState(currentAdmin?.systemAlertsEnabled !== false); // default true

  // UI state
  const [isLoading, setIsLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [imageError, setImageError] = useState('');
  const fileInputRef = useRef(null);

  // Sync state if currentAdmin changes from snapshot
  useEffect(() => {
    if (currentAdmin) {
      setFullName(currentAdmin.full_name || '');
      setEmail(currentAdmin.email || '');
      setSettingsEmailNotif(currentAdmin.emailAlertsEnabled !== false);
      setSettingsSystemAlerts(currentAdmin.systemAlertsEnabled !== false);
    }
  }, [currentAdmin]);

  // Flash messages helper
  const showNotification = (type, text) => {
    if (type === 'success') {
      setSuccessMsg(text);
      setErrorMsg('');
      setTimeout(() => setSuccessMsg(''), 4000);
    } else {
      setErrorMsg(text);
      setSuccessMsg('');
      setTimeout(() => setErrorMsg(''), 4000);
    }
  };

  // 1. Handle profile text updates
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (!fullName.trim() || !email.trim()) {
      showNotification('error', 'Fields cannot be empty.');
      return;
    }

    setIsLoading(true);
    try {
      const adminDocRef = doc(db, 'Admins', currentAdmin.adminId);
      await updateDoc(adminDocRef, {
        full_name: fullName.trim(),
        email: email.trim(),
      });
      await logActivity('Profile Updated', `Admin updated profile info (Name: ${fullName.trim()}, Email: ${email.trim()}).`);
      showNotification('success', 'Profile updated successfully!');
    } catch (err) {
      console.error(err);
      showNotification('error', 'Failed to update profile.');
    } finally {
      setIsLoading(false);
    }
  };

  // 2. Handle Profile Image Upload & Compression (max 400px, 70% quality JPEG)
  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleImageChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate size (before compression check for extreme files)
    if (file.size > 10 * 1024 * 1024) {
      setImageError('Image file must be less than 10MB');
      return;
    }

    setImageError('');
    setIsLoading(true);

    try {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = async () => {
          // Initialize canvas
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');

          // Compute new dimensions keeping aspect ratio (max dimension 400px matching mobile app)
          const maxDimension = 400;
          let width = img.width;
          let height = img.height;

          if (width > maxDimension || height > maxDimension) {
            const scale = Math.min(maxDimension / width, maxDimension / height);
            width = Math.round(width * scale);
            height = Math.round(height * scale);
          }

          canvas.width = width;
          canvas.height = height;

          // Clear and draw image
          ctx.clearRect(0, 0, width, height);
          ctx.drawImage(img, 0, 0, width, height);

          // Get optimized base64 string (JPEG format at 70% quality matching Android JPEG compress quality)
          const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.7);
          
          // Android app uploads string without data URL metadata prefix (just pure base64)
          // Look at Sidebar.jsx: data:image/jpeg;base64,${currentAdmin.profile_image_base64}
          const base64String = compressedDataUrl.replace(/^data:image\/[a-z]+;base64,/, '');

          // Update Firestore
          const adminDocRef = doc(db, 'Admins', currentAdmin.adminId);
          await updateDoc(adminDocRef, {
            profile_image_base64: base64String
          });

          await logActivity('Avatar Changed', 'Admin updated their profile picture.');
          showNotification('success', 'Profile image updated successfully!');
        };
        img.onerror = () => {
          setImageError('Invalid image file format.');
        };
        img.src = event.target.result;
      };
      reader.readAsDataURL(file);
    } catch (err) {
      console.error('Error processing image:', err);
      showNotification('error', 'Error processing selected image.');
    } finally {
      setIsLoading(false);
    }
  };

  // 3. Handle Local Settings Changes (mirroring Android SharedPreferences toggles and syncing with Firestore)

  const handleToggleEmailNotif = async (checked) => {
    setSettingsEmailNotif(checked);
    localStorage.setItem('admin_settings_email', checked.toString());
    try {
      const adminDocRef = doc(db, 'Admins', currentAdmin.adminId);
      await updateDoc(adminDocRef, {
        emailAlertsEnabled: checked
      });
      await logActivity('Settings Changed', `Email Notifications setting changed to: ${checked ? 'ON' : 'OFF'}`);
    } catch (err) {
      console.error("Error updating email alerts in Firestore: ", err);
    }
  };

  const handleToggleSystemAlerts = async (checked) => {
    setSettingsSystemAlerts(checked);
    localStorage.setItem('admin_settings_alerts', checked.toString());
    try {
      const adminDocRef = doc(db, 'Admins', currentAdmin.adminId);
      await updateDoc(adminDocRef, {
        systemAlertsEnabled: checked
      });
      await logActivity('Settings Changed', `System Alerts setting changed to: ${checked ? 'ON' : 'OFF'}`);
    } catch (err) {
      console.error("Error updating system alerts in Firestore: ", err);
    }
  };

  // 4. Handle change password
  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    if (!newPassword || !confirmPassword) {
      showNotification('error', 'Password fields cannot be empty.');
      return;
    }

    if (newPassword.length < 6) {
      showNotification('error', 'Password must be at least 6 characters.');
      return;
    }

    if (newPassword !== confirmPassword) {
      showNotification('error', 'Passwords do not match.');
      return;
    }

    setIsLoading(true);
    try {
      const hashedPwd = await hashPassword(newPassword);
      const adminDocRef = doc(db, 'Admins', currentAdmin.adminId);
      
      await updateDoc(adminDocRef, {
        hashed_password: hashedPwd
      });

      await logActivity('Password Updated', 'Admin changed their account password securely.');
      
      // Clean up fields
      setNewPassword('');
      setConfirmPassword('');
      setShowPasswordSection(false);
      showNotification('success', 'Password updated successfully!');
    } catch (err) {
      console.error(err);
      showNotification('error', 'Failed to update password.');
    } finally {
      setIsLoading(false);
    }
  };

  // 5. Handle Logout
  const handleLogoutClick = async () => {
    try {
      await logActivity('Admin Logout', `${currentAdmin?.full_name || 'Admin'} logged out.`);
    } catch (err) {
      console.error(err);
    }
    onLogout();
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -15 }}
      className="profile-workspace"
    >
      {/* Title Header */}
      <div className="profile-header">
        <div>
          <h1 className="workspace-title">Administrator Profile</h1>
          <p className="workspace-subtitle">Manage your personal information, security preferences, and settings.</p>
        </div>
        
        {/* Dynamic Alerts Banner */}
        <AnimatePresence>
          {successMsg && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.9, x: 20 }}
              animate={{ opacity: 1, scale: 1, x: 0 }}
              exit={{ opacity: 0, scale: 0.9, x: 20 }}
              className="toast success-toast"
            >
              <CheckCircle size={18} />
              <span>{successMsg}</span>
            </motion.div>
          )}
          {errorMsg && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.9, x: 20 }}
              animate={{ opacity: 1, scale: 1, x: 0 }}
              exit={{ opacity: 0, scale: 0.9, x: 20 }}
              className="toast error-toast"
            >
              <AlertCircle size={18} />
              <span>{errorMsg}</span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Main Grid View */}
      <div className="profile-grid">
        
        {/* LEFT COLUMN: Hero details & Quick Settings */}
        <div className="profile-sidebar-col">
          <div className="glass-panel card-avatar-hero">
            
            {/* Base64 Avatar Container */}
            <div className="avatar-pick-wrapper">
              <div 
                className="avatar-viewport-large" 
                onClick={handleAvatarClick}
                style={{ cursor: 'pointer' }}
                title="Click to change profile picture"
              >
                {currentAdmin?.profile_image_base64 ? (
                  <img 
                    src={`data:image/jpeg;base64,${currentAdmin.profile_image_base64}`} 
                    alt="Admin Profile" 
                    className="img-avatar-full" 
                  />
                ) : (
                  <div className="avatar-placeholder">
                    <User size={64} color="var(--primary)" />
                  </div>
                )}
                
                {/* Floating camera upload trigger */}
                <div className="camera-hover-overlay">
                  <Camera size={20} />
                  <span>Update</span>
                </div>
              </div>

              {/* Hidden file input */}
              <input 
                type="file" 
                ref={fileInputRef} 
                onChange={handleImageChange} 
                accept="image/*" 
                style={{ display: 'none' }} 
              />
            </div>

            {imageError && (
              <p className="avatar-error-msg"><AlertCircle size={12} /> {imageError}</p>
            )}

            {/* Admin details Hero block */}
            <div className="admin-hero-info">
              <h3>{fullName || 'Admin User'}</h3>
              <p className="admin-status-badge">
                <span className="pulse-indicator"></span>
                {currentAdmin?.status || 'Active'} Administrator
              </p>
              <span className="admin-id-badge">ID: {currentAdmin?.adminId || 'N/A'}</span>
            </div>

            <div className="hero-divider"></div>

            {/* Quick Actions (Logout / Settings shortcut) */}
            <div className="hero-actions-list">
              <button className="btn-secondary logout-trigger-btn" onClick={handleLogoutClick}>
                <LogOut size={16} />
                <span>Log Out Session</span>
              </button>
            </div>
          </div>

          {/* Settings Section (matching Mobile Settings switches) */}
          <div className="glass-panel card-settings-hub">
            <div className="card-header-icon-row">
              <Settings size={18} className="icon-blue" />
              <h4>Preferences & Security</h4>
            </div>

            <div className="settings-toggles-container">
              {/* Email Notifications */}
              <div className="setting-toggle-item">
                <div className="toggle-label-section">
                  <span className="toggle-main-label">Email Notifications</span>
                  <span className="toggle-sub-label">Receive updates on curriculum edits.</span>
                </div>
                <label className="switch-container">
                  <input 
                    type="checkbox" 
                    checked={settingsEmailNotif} 
                    onChange={(e) => handleToggleEmailNotif(e.target.checked)} 
                  />
                  <span className="switch-slider"></span>
                </label>
              </div>

              {/* System Alerts */}
              <div className="setting-toggle-item">
                <div className="toggle-label-section">
                  <span className="toggle-main-label">Critical System Alerts</span>
                  <span className="toggle-sub-label">Warning alerts for offline status.</span>
                </div>
                <label className="switch-container">
                  <input 
                    type="checkbox" 
                    checked={settingsSystemAlerts} 
                    onChange={(e) => handleToggleSystemAlerts(e.target.checked)} 
                  />
                  <span className="switch-slider"></span>
                </label>
              </div>
            </div>
            
            <div className="help-box-banner">
              <Info size={16} />
              <div className="help-box-content">
                <h5>Support & FAQ</h5>
                <p>Support is offline. Contact <a href="mailto:support@saegis.ac.lk">support@saegis.ac.lk</a></p>
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Profile Editor and Change Password */}
        <div className="profile-details-col">
          
          {/* Main Info Form */}
          <div className="glass-panel profile-editor-card">
            <div className="card-header-icon-row border-header">
              <User size={20} className="icon-blue" />
              <h3>Personal Identification</h3>
            </div>

            <form onSubmit={handleSaveProfile} className="profile-details-form">
              <div className="form-fields-grid">
                
                {/* Full name input */}
                <div className="input-group-row">
                  <label className="field-label-text">Full Name</label>
                  <div className="premium-input-container">
                    <User className="input-inner-icon" size={18} />
                    <input 
                      type="text" 
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="e.g. Kasun Perera"
                      required
                      disabled={isLoading}
                      className="premium-input-box"
                    />
                  </div>
                </div>

                {/* Email input */}
                <div className="input-group-row">
                  <label className="field-label-text">Email Address</label>
                  <div className="premium-input-container">
                    <Mail className="input-inner-icon" size={18} />
                    <input 
                      type="email" 
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="e.g. kasun@saegis.ac.lk"
                      required
                      disabled={isLoading}
                      className="premium-input-box"
                    />
                  </div>
                </div>

                {/* Read only ID */}
                <div className="input-group-row">
                  <label className="field-label-text">System ID (Read Only)</label>
                  <div className="premium-input-container disabled-input">
                    <Shield className="input-inner-icon" size={18} />
                    <input 
                      type="text" 
                      value={currentAdmin?.adminId || ''} 
                      disabled 
                      className="premium-input-box"
                    />
                  </div>
                </div>

                {/* Read only status */}
                <div className="input-group-row">
                  <label className="field-label-text">Administrative Privilege</label>
                  <div className="premium-input-container disabled-input">
                    <Bell className="input-inner-icon" size={18} />
                    <input 
                      type="text" 
                      value={(currentAdmin?.status || 'Active') + ' Administrator'} 
                      disabled 
                      className="premium-input-box"
                    />
                  </div>
                </div>

              </div>

              {/* Form submit */}
              <div className="form-action-footer">
                <button type="submit" className="btn-primary-gradient save-profile-btn" disabled={isLoading}>
                  {isLoading ? (
                    <Loader2 size={16} className="spinning-loader" />
                  ) : (
                    <Save size={16} />
                  )}
                  <span>Save Profile Updates</span>
                </button>
              </div>
            </form>
          </div>

          {/* Change Password Panel */}
          <div className="glass-panel security-password-card">
            <div 
              className="card-header-icon-row border-header clickable-header"
              onClick={() => setShowPasswordSection(!showPasswordSection)}
            >
              <KeyRound size={20} className="icon-blue" />
              <div className="title-desc-header">
                <h3>Change Account Password</h3>
                <p>Modify your credentials to protect access.</p>
              </div>
              <button 
                type="button" 
                className={`collapse-toggle-arrow ${showPasswordSection ? 'rotated' : ''}`}
              >
                <Lock size={16} />
              </button>
            </div>

            <AnimatePresence>
              {showPasswordSection && (
                <motion.div 
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.25 }}
                  className="password-section-body"
                >
                  <form onSubmit={handleUpdatePassword} className="password-details-form">
                    <div className="form-fields-grid">
                      
                      {/* New password input */}
                      <div className="input-group-row">
                        <label className="field-label-text">New Security Password</label>
                        <div className="premium-input-container">
                          <Lock className="input-inner-icon" size={18} />
                          <input 
                            type={showNewPassword ? 'text' : 'password'}
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="At least 6 characters"
                            required
                            disabled={isLoading}
                            className="premium-input-box"
                          />
                          <button 
                            type="button" 
                            className="input-eye-toggle"
                            onClick={() => setShowNewPassword(!showNewPassword)}
                          >
                            {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                          </button>
                        </div>
                      </div>

                      {/* Confirm password input */}
                      <div className="input-group-row">
                        <label className="field-label-text">Confirm Security Password</label>
                        <div className="premium-input-container">
                          <Lock className="input-inner-icon" size={18} />
                          <input 
                            type={showConfirmPassword ? 'text' : 'password'}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="Must match exactly"
                            required
                            disabled={isLoading}
                            className="premium-input-box"
                          />
                          <button 
                            type="button" 
                            className="input-eye-toggle"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          >
                            {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                          </button>
                        </div>
                      </div>

                    </div>

                    <div className="form-action-footer no-border">
                      <button type="submit" className="btn-primary-gradient change-pwd-btn" disabled={isLoading}>
                        {isLoading ? (
                          <Loader2 size={16} className="spinning-loader" />
                        ) : (
                          <CheckCircle size={16} />
                        )}
                        <span>Confirm Password Update</span>
                      </button>
                    </div>
                  </form>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

        </div>

      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .profile-workspace {
          display: flex;
          flex-direction: column;
          gap: 32px;
          position: relative;
          z-index: 10;
        }

        .profile-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 20px;
          flex-wrap: wrap;
        }

        .workspace-title {
          font-size: 2.2rem;
          font-weight: 800;
          color: var(--primary);
          margin-bottom: 6px;
          letter-spacing: -0.5px;
        }

        .workspace-subtitle {
          color: var(--text-muted);
          font-size: 0.95rem;
          font-weight: 500;
        }

        .toast {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px 20px;
          border-radius: 14px;
          font-weight: 600;
          font-size: 0.9rem;
          box-shadow: 0 10px 25px rgba(13, 47, 90, 0.08);
          border: 1px solid transparent;
        }

        .success-toast {
          background: rgba(34, 197, 94, 0.08);
          color: #22c55e;
          border-color: rgba(34, 197, 94, 0.15);
        }

        .error-toast {
          background: rgba(239, 68, 68, 0.08);
          color: #ef4444;
          border-color: rgba(239, 68, 68, 0.15);
        }

        .profile-grid {
          display: grid;
          grid-template-columns: 340px 1fr;
          gap: 32px;
          align-items: start;
        }

        @media (max-width: 1024px) {
          .profile-grid {
            grid-template-columns: 1fr;
          }
        }

        .profile-sidebar-col, .profile-details-col {
          display: flex;
          flex-direction: column;
          gap: 32px;
        }

        .card-avatar-hero {
          padding: 40px 24px;
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
        }

        .avatar-pick-wrapper {
          position: relative;
          margin-bottom: 24px;
        }

        .avatar-viewport-large {
          width: 160px;
          height: 160px;
          border-radius: 50%;
          background: #e9ecef;
          position: relative;
          overflow: hidden;
          border: 4px solid white;
          box-shadow: 0 12px 30px rgba(13, 47, 90, 0.12);
          transition: all 0.3s ease;
        }

        .avatar-viewport-large:hover {
          border-color: var(--accent);
          transform: scale(1.02);
        }

        .img-avatar-full {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .avatar-placeholder {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #E6EEFF;
        }

        .camera-hover-overlay {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(13, 47, 90, 0.75);
          color: white;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 6px;
          opacity: 0;
          transition: opacity 0.25s ease;
        }

        .avatar-viewport-large:hover .camera-hover-overlay {
          opacity: 1;
        }

        .camera-hover-overlay span {
          font-size: 0.8rem;
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 1px;
        }

        .avatar-error-msg {
          color: #ef4444;
          font-size: 0.8rem;
          font-weight: 600;
          margin-top: 8px;
          display: flex;
          align-items: center;
          gap: 4px;
        }

        .admin-hero-info h3 {
          font-size: 1.35rem;
          font-weight: 800;
          color: var(--primary);
          margin-bottom: 8px;
        }

        .admin-status-badge {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 6px 14px;
          border-radius: 20px;
          background: rgba(34, 197, 94, 0.08);
          color: #22c55e;
          font-size: 0.82rem;
          font-weight: 700;
          margin-bottom: 12px;
        }

        .pulse-indicator {
          width: 8px;
          height: 8px;
          background: #22c55e;
          border-radius: 50%;
          box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
          animation: pulse 1.6s infinite;
        }

        @keyframes pulse {
          0% {
            transform: scale(0.95);
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
          }
          70% {
            transform: scale(1);
            box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
          }
          100% {
            transform: scale(0.95);
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
          }
        }

        .admin-id-badge {
          display: block;
          font-family: monospace;
          color: var(--text-muted);
          font-size: 0.85rem;
          font-weight: 600;
        }

        .hero-divider {
          width: 100%;
          height: 1px;
          background: rgba(224, 228, 236, 0.6);
          margin: 24px 0;
        }

        .hero-actions-list {
          width: 100%;
        }

        .logout-trigger-btn {
          width: 100%;
          padding: 14px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          background: #fff5f5;
          color: #e53e3e;
          border: 1px solid #fed7d7;
          font-weight: 700;
          font-size: 0.95rem;
          transition: all 0.2s ease;
        }

        .logout-trigger-btn:hover {
          background: #e53e3e;
          color: white;
          border-color: #e53e3e;
          box-shadow: 0 6px 15px rgba(229, 62, 62, 0.2);
        }

        .card-settings-hub {
          padding: 28px;
        }

        .card-header-icon-row {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 24px;
        }

        .card-header-icon-row h4 {
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--primary);
          margin: 0;
        }

        .icon-blue {
          color: var(--accent);
        }

        .settings-toggles-container {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .setting-toggle-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 16px;
        }

        .toggle-label-section {
          display: flex;
          flex-direction: column;
          flex: 1;
        }

        .toggle-main-label {
          font-size: 0.92rem;
          font-weight: 700;
          color: var(--text-main);
          margin-bottom: 2px;
        }

        .toggle-sub-label {
          font-size: 0.78rem;
          color: var(--text-muted);
          line-height: 1.3;
        }

        /* Pure CSS switches */
        .switch-container {
          position: relative;
          display: inline-block;
          width: 44px;
          height: 24px;
          flex-shrink: 0;
        }

        .switch-container input {
          opacity: 0;
          width: 0;
          height: 0;
        }

        .switch-slider {
          position: absolute;
          cursor: pointer;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: #cbd5e1;
          transition: .3s;
          border-radius: 24px;
        }

        .switch-slider:before {
          position: absolute;
          content: "";
          height: 18px;
          width: 18px;
          left: 3px;
          bottom: 3px;
          background-color: white;
          transition: .3s;
          border-radius: 50%;
        }

        .switch-container input:checked + .switch-slider {
          background-color: var(--accent);
        }

        .switch-container input:checked + .switch-slider:before {
          transform: translateX(20px);
        }

        .help-box-banner {
          margin-top: 24px;
          padding: 16px;
          background: rgba(5, 123, 254, 0.04);
          border: 1px dashed rgba(5, 123, 254, 0.2);
          border-radius: 12px;
          display: flex;
          gap: 12px;
          align-items: flex-start;
          color: var(--accent);
        }

        .help-box-content h5 {
          font-size: 0.88rem;
          font-weight: 700;
          margin-bottom: 2px;
          color: var(--primary);
        }

        .help-box-content p {
          font-size: 0.78rem;
          color: var(--text-muted);
          margin: 0;
        }

        .help-box-content a {
          color: var(--accent);
          font-weight: 600;
          text-decoration: underline;
        }

        .profile-editor-card {
          padding: 36px;
        }

        .border-header {
          border-bottom: 1px solid rgba(224, 228, 236, 0.6);
          padding-bottom: 18px;
          margin-bottom: 30px;
        }

        .border-header h3 {
          font-size: 1.3rem;
          font-weight: 800;
          color: var(--primary);
          margin: 0;
        }

        .form-fields-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 24px;
        }

        @media (max-width: 640px) {
          .form-fields-grid {
            grid-template-columns: 1fr;
          }
        }

        .input-group-row {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .field-label-text {
          font-size: 0.85rem;
          font-weight: 700;
          color: var(--text-muted);
        }

        .premium-input-container {
          position: relative;
          display: flex;
          align-items: center;
        }

        .input-inner-icon {
          position: absolute;
          left: 16px;
          color: var(--text-muted);
          pointer-events: none;
        }

        .premium-input-box {
          width: 100%;
          padding: 14px 16px 14px 48px;
          background: rgba(224, 228, 236, 0.2);
          border: 1px solid rgba(224, 228, 236, 0.8);
          border-radius: 12px;
          color: var(--text-main);
          font-size: 0.95rem;
          font-weight: 600;
          transition: all 0.2s ease;
        }

        .premium-input-box::placeholder {
          color: #a0aec0;
        }

        .premium-input-box:focus {
          outline: none;
          background: white;
          border-color: var(--accent);
          box-shadow: 0 0 0 4px rgba(5, 123, 254, 0.1);
        }

        .disabled-input .premium-input-box {
          background: rgba(224, 228, 236, 0.3);
          border-color: rgba(224, 228, 236, 0.3);
          color: var(--text-muted);
          cursor: not-allowed;
        }

        .form-action-footer {
          margin-top: 36px;
          padding-top: 24px;
          border-top: 1px solid rgba(224, 228, 236, 0.6);
          display: flex;
          justify-content: flex-end;
        }

        .form-action-footer.no-border {
          border-top: none;
          margin-top: 20px;
          padding-top: 0;
        }

        .btn-primary-gradient {
          padding: 14px 28px;
          border-radius: 12px;
          background: var(--btn-gradient);
          color: white;
          font-weight: 700;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          gap: 10px;
          box-shadow: 0 8px 20px rgba(5, 123, 254, 0.3);
          transition: all 0.2s ease;
        }

        .btn-primary-gradient:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 10px 25px rgba(5, 123, 254, 0.4);
        }

        .btn-primary-gradient:active:not(:disabled) {
          transform: translateY(0);
        }

        .btn-primary-gradient:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        .spinning-loader {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        .security-password-card {
          padding: 0;
          overflow: hidden;
        }

        .clickable-header {
          cursor: pointer;
          user-select: none;
          padding: 28px 36px;
          margin-bottom: 0;
          border-bottom: none;
          display: flex;
          justify-content: space-between;
          align-items: center;
          transition: background-color 0.2s;
        }

        .clickable-header:hover {
          background-color: rgba(224, 228, 236, 0.15);
        }

        .title-desc-header {
          flex: 1;
          margin-left: 12px;
        }

        .title-desc-header h3 {
          font-size: 1.15rem;
          font-weight: 800;
          color: var(--primary);
          margin-bottom: 4px;
        }

        .title-desc-header p {
          color: var(--text-muted);
          font-size: 0.8rem;
          margin: 0;
          font-weight: 500;
        }

        .collapse-toggle-arrow {
          background: rgba(224, 228, 236, 0.4);
          color: var(--primary);
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.25s ease;
        }

        .collapse-toggle-arrow.rotated {
          transform: rotate(180deg);
          background: var(--accent);
          color: white;
        }

        .password-section-body {
          padding: 12px 36px 36px 36px;
          border-top: 1px solid rgba(224, 228, 236, 0.6);
        }

        .input-eye-toggle {
          position: absolute;
          right: 16px;
          color: var(--text-muted);
          background: transparent;
          border: none;
          padding: 0;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .input-eye-toggle:hover {
          color: var(--text-main);
        }
      `}} />
    </motion.div>
  );
};

export default Profile;

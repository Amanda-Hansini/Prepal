import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { LogIn, Mail, Lock, Eye, EyeOff, AlertCircle, Loader2, User, Shield } from 'lucide-react';
import { db } from '../firebase';
import { doc, getDoc, setDoc, collection, query, where, getDocs } from 'firebase/firestore';
import { hashPassword } from '../utils/security';

const Login = ({ onLogin }) => {
  // Mode selection
  const [isSignUp, setIsSignUp] = useState(false);

  // Login states
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Sign Up states
  const [signUpAdminId, setSignUpAdminId] = useState('');
  const [signUpFullName, setSignUpFullName] = useState('');
  const [signUpEmail, setSignUpEmail] = useState('');
  const [signUpPassword, setSignUpPassword] = useState('');
  const [signUpConfirmPassword, setSignUpConfirmPassword] = useState('');
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Common UI states
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const enteredHashedPassword = await hashPassword(password);
      
      // 1. Try to find by Document ID (Admin ID)
      const adminDocRef = doc(db, "Admins", userId.trim());
      const adminDoc = await getDoc(adminDocRef);

      if (adminDoc.exists()) {
        const storedHashedPassword = adminDoc.data().hashed_password;
        if (enteredHashedPassword === storedHashedPassword) {
          onLogin({ adminId: adminDoc.id, ...adminDoc.data() });
          return;
        } else {
          setError("Invalid password. Please try again.");
          setIsLoading(false);
          return;
        }
      }

      // 2. Try to find by Email
      const adminQuery = query(collection(db, "Admins"), where("email", "==", userId.trim().toLowerCase()));
      const querySnapshot = await getDocs(adminQuery);

      if (!querySnapshot.empty) {
        const adminDoc = querySnapshot.docs[0];
        const adminData = adminDoc.data();
        const storedHashedPassword = adminData.hashed_password;
        
        if (enteredHashedPassword === storedHashedPassword) {
          onLogin({ adminId: adminDoc.id, ...adminData });
          return;
        } else {
          setError("Invalid password. Please try again.");
        }
      } else {
        // Enforce "Admin Only" rule
        setError("Access Restricted: This portal is only for Administrators.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("System Error: Failed to connect to database.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignUp = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    const cleanAdminId = signUpAdminId.trim();
    const cleanFullName = signUpFullName.trim();
    const cleanEmail = signUpEmail.trim().toLowerCase();

    // Validations
    if (!cleanAdminId || !cleanFullName || !cleanEmail || !signUpPassword) {
      setError("Please fill in all fields.");
      setIsLoading(false);
      return;
    }

    if (signUpPassword !== signUpConfirmPassword) {
      setError("Passwords do not match.");
      setIsLoading(false);
      return;
    }

    if (signUpPassword.length < 6) {
      setError("Password must be at least 6 characters long.");
      setIsLoading(false);
      return;
    }

    try {
      // 1. Check if Admin ID is already taken (Checks Firestore document ID)
      const adminDocRef = doc(db, "Admins", cleanAdminId);
      const adminDoc = await getDoc(adminDocRef);

      if (adminDoc.exists()) {
        setError("Admin ID already exists. Please choose a different ID.");
        setIsLoading(false);
        return;
      }

      // 2. Check if Email is already taken (Checks email field query)
      const emailQuery = query(collection(db, "Admins"), where("email", "==", cleanEmail));
      const querySnapshot = await getDocs(emailQuery);

      if (!querySnapshot.empty) {
        setError("Email is already registered. Please login or choose another email.");
        setIsLoading(false);
        return;
      }

      // 3. Hash Password (SHA-256 for cross-platform Android parity)
      const hashedPasswordValue = await hashPassword(signUpPassword);

      // 4. Create Admin record
      const newAdminData = {
        full_name: cleanFullName,
        email: cleanEmail,
        hashed_password: hashedPasswordValue,
        status: "Active",
        profile_image_base64: ""
      };

      await setDoc(adminDocRef, newAdminData);

      // Auto login newly registered admin
      onLogin({ adminId: cleanAdminId, ...newAdminData });
    } catch (err) {
      console.error("Sign Up error:", err);
      setError("System Error: Failed to register Administrator account.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="decor-top-right"></div>
      <div className="decor-bottom-left"></div>
      <div className="decor-dots-top"></div>
      <div className="decor-dots-bottom"></div>

      <div className="login-scroll-container">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="login-card-modern"
        >
          <div className="branding">
            <div className="logo-container">
               <img src="/logo.png" alt="Prepal Logo" className="app-logo" />
            </div>
            <h1 className="brand-name">PrePal</h1>
            <p className="brand-subtitle">Personal Exam Results Predictor</p>
            <div className="accent-line"></div>
          </div>

          {/* Dynamic Switcher */}
          <div className="auth-toggle">
            <button 
              type="button" 
              className={`toggle-tab ${!isSignUp ? 'active' : ''}`}
              onClick={() => { setIsSignUp(false); setError(''); }}
              disabled={isLoading}
            >
              Login
            </button>
            <button 
              type="button" 
              className={`toggle-tab ${isSignUp ? 'active' : ''}`}
              onClick={() => { setIsSignUp(true); setError(''); }}
              disabled={isLoading}
            >
              Sign Up
            </button>
          </div>

          <div className="welcome-section">
            <h2 className="welcome-title">Admin Portal</h2>
            <p className="welcome-subtitle">
              {isSignUp ? "Register a secure administrator profile" : "Secure access for staff only"}
            </p>
          </div>

          {error && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="error-message"
            >
              <AlertCircle size={18} />
              <span>{error}</span>
            </motion.div>
          )}

          {!isSignUp ? (
            // LOGIN FORM
            <form onSubmit={handleLogin} className="login-form">
              <div className="field-group">
                <div className="field-container">
                  <Mail className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Admin ID or Email</label>
                    <input 
                      type="text" 
                      placeholder="Enter your ID or email"
                      value={userId}
                      onChange={(e) => setUserId(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                </div>
              </div>

              <div className="field-group">
                <div className="field-container">
                  <Lock className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Password</label>
                    <input 
                      type={showPassword ? "text" : "password"} 
                      placeholder="••••••••" 
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                  <button 
                    type="button" 
                    className="toggle-password"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                  </button>
                </div>
              </div>

              <button type="submit" className="login-button-gradient" disabled={isLoading}>
                {isLoading ? <Loader2 className="spinner" size={24} /> : "Login to Dashboard"}
              </button>
            </form>
          ) : (
            // SIGN UP FORM
            <form onSubmit={handleSignUp} className="login-form">
              <div className="field-group">
                <div className="field-container">
                  <Shield className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Admin ID (Username)</label>
                    <input 
                      type="text" 
                      placeholder="e.g. ADM-1002"
                      value={signUpAdminId}
                      onChange={(e) => setSignUpAdminId(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                </div>
              </div>

              <div className="field-group">
                <div className="field-container">
                  <User className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Full Name</label>
                    <input 
                      type="text" 
                      placeholder="e.g. John Doe"
                      value={signUpFullName}
                      onChange={(e) => setSignUpFullName(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                </div>
              </div>

              <div className="field-group">
                <div className="field-container">
                  <Mail className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Email Address</label>
                    <input 
                      type="email" 
                      placeholder="e.g. johndoe@saegis.ac.lk"
                      value={signUpEmail}
                      onChange={(e) => setSignUpEmail(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                </div>
              </div>

              <div className="field-group">
                <div className="field-container">
                  <Lock className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Password</label>
                    <input 
                      type={showPassword ? "text" : "password"} 
                      placeholder="At least 6 characters" 
                      value={signUpPassword}
                      onChange={(e) => setSignUpPassword(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                  <button 
                    type="button" 
                    className="toggle-password"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                  </button>
                </div>
              </div>

              <div className="field-group">
                <div className="field-container">
                  <Lock className="field-icon" size={20} />
                  <div className="input-box">
                    <label>Confirm Password</label>
                    <input 
                      type={showConfirmPassword ? "text" : "password"} 
                      placeholder="Confirm your password" 
                      value={signUpConfirmPassword}
                      onChange={(e) => setSignUpConfirmPassword(e.target.value)}
                      required 
                      disabled={isLoading}
                    />
                  </div>
                  <button 
                    type="button" 
                    className="toggle-password"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  >
                    {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                  </button>
                </div>
              </div>

              <button type="submit" className="login-button-gradient" disabled={isLoading}>
                {isLoading ? <Loader2 className="spinner" size={24} /> : "Register & Log In"}
              </button>
            </form>
          )}

          <div className="login-footer-note">
             Students should use the Prepal Mobile App to access their results.
          </div>
        </motion.div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .login-page {
          min-height: 100vh;
          background-color: var(--app-bg);
          position: relative;
          overflow: hidden;
          display: flex;
          justify-content: center;
          align-items: center;
          font-family: 'Inter', sans-serif;
        }

        .decor-top-right {
          position: absolute;
          top: -100px;
          right: -100px;
          width: 350px;
          height: 350px;
          background: linear-gradient(135deg, var(--accent) 0%, #01C9FF 100%);
          border-radius: 0 0 0 350px;
          opacity: 0.15;
          z-index: 1;
        }

        .decor-bottom-left {
          position: absolute;
          bottom: -100px;
          left: -100px;
          width: 300px;
          height: 300px;
          background: linear-gradient(315deg, var(--accent) 0%, #01C9FF 100%);
          border-radius: 0 300px 0 0;
          opacity: 0.1;
          z-index: 1;
        }

        .decor-dots-top {
          position: absolute;
          top: 100px;
          left: 60px;
          width: 120px;
          height: 120px;
          background-image: radial-gradient(var(--accent) 2px, transparent 2px);
          background-size: 20px 20px;
          opacity: 0.2;
          z-index: 1;
        }

        .decor-dots-bottom {
          position: absolute;
          bottom: 100px;
          right: 60px;
          width: 120px;
          height: 120px;
          background-image: radial-gradient(var(--accent) 2px, transparent 2px);
          background-size: 20px 20px;
          opacity: 0.2;
          z-index: 1;
        }

        .login-scroll-container {
          width: 100%;
          max-width: 480px;
          padding: 20px;
          z-index: 10;
        }

        .login-card-modern {
          background: white;
          border-radius: 32px;
          padding: 56px 48px;
          box-shadow: 0 20px 60px rgba(13, 47, 90, 0.08);
          text-align: center;
        }

        .branding {
          margin-bottom: 24px;
        }

        .app-logo {
          width: 110px;
          height: auto;
          margin-bottom: 16px;
        }

        .brand-name {
          font-size: 2.5rem;
          font-weight: 800;
          color: var(--primary);
          margin-bottom: 4px;
        }

        .brand-subtitle {
          font-size: 0.85rem;
          color: var(--text-secondary);
          margin-bottom: 16px;
        }

        .accent-line {
          width: 60px;
          height: 4px;
          background: var(--accent);
          margin: 0 auto;
          border-radius: 2px;
        }

        /* sliding switcher toggles */
        .auth-toggle {
          display: flex;
          background: #F1F5F9;
          border-radius: 14px;
          padding: 4px;
          margin-bottom: 28px;
          border: 1px solid rgba(226, 232, 240, 0.8);
          position: relative;
          z-index: 5;
        }

        .toggle-tab {
          flex: 1;
          padding: 12px;
          font-size: 0.95rem;
          font-weight: 700;
          border-radius: 10px;
          background: transparent;
          color: var(--text-secondary);
          border: none;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .toggle-tab.active {
          background: white;
          color: var(--primary);
          box-shadow: 0 4px 10px rgba(13, 47, 90, 0.05);
        }

        .welcome-section {
          margin-bottom: 24px;
        }

        .welcome-title {
          font-size: 1.75rem;
          font-weight: 700;
          color: var(--text-primary);
          margin-bottom: 4px;
        }

        .welcome-subtitle {
          color: var(--text-secondary);
          font-size: 0.95rem;
        }

        .error-message {
          background: rgba(239, 68, 68, 0.1);
          color: #ef4444;
          padding: 12px 16px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 24px;
          font-size: 0.9rem;
          font-weight: 600;
          text-align: left;
        }

        .field-container {
          display: flex;
          align-items: center;
          background: white;
          border: 1.5px solid #E2E8F0;
          border-radius: 18px;
          padding: 14px 20px;
          box-shadow: 0 4px 12px rgba(13, 47, 90, 0.03);
          transition: all 0.2s;
          margin-bottom: 16px;
        }

        .field-container:focus-within {
          border-color: var(--accent);
          box-shadow: 0 0 0 4px rgba(5, 123, 254, 0.1);
        }

        .field-icon {
          color: var(--text-secondary);
          margin-right: 16px;
        }

        .input-box {
          flex: 1;
          text-align: left;
        }

        .input-box label {
          display: block;
          font-size: 0.75rem;
          color: var(--text-secondary);
          margin-bottom: 2px;
          font-weight: 500;
        }

        .input-box input {
          width: 100%;
          border: none;
          background: transparent;
          font-size: 1rem;
          color: var(--text-primary);
          padding: 0;
          font-weight: 600;
        }

        .input-box input:focus {
          outline: none;
        }

        .toggle-password {
          background: transparent;
          border: none;
          color: var(--text-secondary);
          cursor: pointer;
          display: flex;
          align-items: center;
        }

        .login-button-gradient {
          width: 100%;
          height: 60px;
          background: var(--btn-gradient);
          border: none;
          border-radius: 18px;
          color: white;
          font-size: 1.15rem;
          font-weight: 700;
          cursor: pointer;
          box-shadow: 0 10px 25px rgba(1, 97, 215, 0.3);
          transition: all 0.2s;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .login-button-gradient:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 15px 30px rgba(1, 97, 215, 0.4);
        }

        .login-button-gradient:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        .spinner {
          animation: rotate 2s linear infinite;
        }

        @keyframes rotate {
          100% { transform: rotate(360deg); }
        }

        .login-footer-note {
          margin-top: 32px;
          font-size: 0.85rem;
          color: var(--text-secondary);
          line-height: 1.5;
        }
      `}} />
    </div>
  );
};

export default Login;

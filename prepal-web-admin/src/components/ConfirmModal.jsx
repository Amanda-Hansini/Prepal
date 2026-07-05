import React from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle, X } from 'lucide-react';

const ConfirmModal = ({ isOpen, title, message, onConfirm, onCancel, confirmText = "Confirm", cancelText = "Cancel" }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" style={{ zIndex: 9999 }}>
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="glass-panel modal-content"
        style={{ maxWidth: '400px', width: '90%', padding: '32px', textAlign: 'center', position: 'relative', margin: 'auto' }}
      >
        <button 
          onClick={onCancel} 
          style={{ position: 'absolute', top: '16px', right: '16px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}
        >
          <X size={20} />
        </button>
        
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px' }}>
          <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'rgba(239, 68, 68, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <AlertTriangle size={32} color="#ef4444" />
          </div>
        </div>

        <h2 style={{ fontSize: '1.25rem', fontWeight: '800', color: 'var(--text-main)', marginBottom: '12px' }}>{title}</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginBottom: '32px', lineHeight: '1.5' }}>
          {message}
        </p>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button className="btn-secondary" style={{ flex: 1, padding: '12px' }} onClick={onCancel}>
            {cancelText}
          </button>
          <button className="btn-primary" style={{ flex: 1, padding: '12px', backgroundColor: '#ef4444', borderColor: '#ef4444' }} onClick={onConfirm}>
            {confirmText}
          </button>
        </div>
      </motion.div>
    </div>
  );
};

export default ConfirmModal;

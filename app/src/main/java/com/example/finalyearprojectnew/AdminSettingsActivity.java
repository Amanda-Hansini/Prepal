package com.example.finalyearprojectnew;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;

public class AdminSettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout llChangePassword, llHelpFAQ;
    private androidx.appcompat.widget.SwitchCompat switch2FA, switchEmailNotif, switchSystemAlerts;
    private FirebaseFirestore db;
    private String adminId;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        db = FirebaseFirestore.getInstance();
        adminId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("admin_id", "");
        prefs = getSharedPreferences("AdminSettings", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        llChangePassword = findViewById(R.id.llChangePassword);
        llHelpFAQ = findViewById(R.id.llHelpFAQ);
        switch2FA = findViewById(R.id.switch2FA);
        switchEmailNotif = findViewById(R.id.switchEmailNotif);
        switchSystemAlerts = findViewById(R.id.switchSystemAlerts);
    }

    private void loadSettings() {
        switch2FA.setChecked(prefs.getBoolean("2fa_enabled", false));
        switchEmailNotif.setChecked(prefs.getBoolean("email_notif", true));
        switchSystemAlerts.setChecked(prefs.getBoolean("system_alerts", true));

        if (adminId != null && !adminId.isEmpty()) {
            db.collection("Admins").document(adminId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean f2fa = doc.getBoolean("twoFactorEnabled");
                        Boolean femail = doc.getBoolean("emailAlertsEnabled");
                        Boolean fsystem = doc.getBoolean("systemAlertsEnabled");

                        if (f2fa != null) {
                            switch2FA.setChecked(f2fa);
                            prefs.edit().putBoolean("2fa_enabled", f2fa).apply();
                        }
                        if (femail != null) {
                            switchEmailNotif.setChecked(femail);
                            prefs.edit().putBoolean("email_notif", femail).apply();
                        }
                        if (fsystem != null) {
                            switchSystemAlerts.setChecked(fsystem);
                            prefs.edit().putBoolean("system_alerts", fsystem).apply();
                            if (fsystem) {
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(token -> {
                                        db.collection("Admins").document(adminId).update("fcmToken", token);
                                    });
                            }
                        }
                    }
                });
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        switch2FA.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("2fa_enabled", isChecked).apply();
            if (adminId != null && !adminId.isEmpty()) {
                db.collection("Admins").document(adminId).update("twoFactorEnabled", isChecked);
                SystemAlertHelper.queueSystemAlert("SECURITY", "2FA Settings Changed", "Admin " + adminId + " set 2FA to " + isChecked);
            }
        });

        switchEmailNotif.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("email_notif", isChecked).apply();
            if (adminId != null && !adminId.isEmpty()) {
                db.collection("Admins").document(adminId).update("emailAlertsEnabled", isChecked);
            }
        });

        switchSystemAlerts.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("system_alerts", isChecked).apply();
            if (adminId != null && !adminId.isEmpty()) {
                db.collection("Admins").document(adminId).update("systemAlertsEnabled", isChecked);
                if (isChecked) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            db.collection("Admins").document(adminId).update("fcmToken", token);
                        });
                } else {
                    db.collection("Admins").document(adminId).update("fcmToken", "");
                }
            }
        });

        llChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        llHelpFAQ.setOnClickListener(v -> {
            Toast.makeText(this, "Help Center is currently offline. Please contact support@saegis.ac.lk", Toast.LENGTH_LONG).show();
        });
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        AppCompatButton btnUpdate = dialogView.findViewById(R.id.btnUpdatePassword);

        btnUpdate.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confPass = etConfirmPassword.getText().toString().trim();

            if (newPass.isEmpty() || newPass.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }

            if (!newPass.equals(confPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }

            btnUpdate.setEnabled(false);
            String hashedPwd = SecurityUtils.hashPassword(newPass);
            db.collection("Admins").document(adminId)
                .update("hashed_password", hashedPwd)
                .addOnSuccessListener(aVoid -> {
                    SystemAlertHelper.queueSystemAlert("SECURITY", "Admin Password Changed", "Admin " + adminId + " updated their password.");
                    Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    btnUpdate.setEnabled(true);
                    Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        dialog.show();
    }
}

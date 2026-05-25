package com.example.finalyearprojectnew;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private AppCompatButton btnLogin;
    private TextView tvForgotPassword;
    private boolean isPasswordVisible = false;
    private int loginAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
                ivTogglePassword.setColorFilter(getResources().getColor(R.color.textColorSecondary, getTheme()));
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_secure);
                ivTogglePassword.setColorFilter(getResources().getColor(R.color.colorAccent, getTheme()));
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.length());
        });

        // Login Button Click
        btnLogin.setOnClickListener(v -> {
            if (loginAttempts >= 5) {
                showAccountLockedDialog("Account Locked", "Your account has been locked. To continue using PrePal, please contact the support team.");
                return;
            }

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Please enter your ID");
                etEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Please enter your password");
                etPassword.requestFocus();
                return;
            }

            Toast.makeText(this, "Verifying Details...", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Smart Unified Authentication: Check Admin first
            db.collection("Admins").document(email).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedHashedPassword = documentSnapshot.getString("hashed_password");
                            String enteredHashedPassword = SecurityUtils.hashPassword(password);

                            if (enteredHashedPassword.equals(storedHashedPassword)) {
                                loginAttempts = 0; // Reset on success
                                String adminId = documentSnapshot.getString("admin_id");
                                String adminEmail = documentSnapshot.getString("email");
                                Boolean twoFactorEnabled = documentSnapshot.getBoolean("twoFactorEnabled");

                                if (Boolean.TRUE.equals(twoFactorEnabled)) {
                                    send2FAOtp(adminId, adminEmail, documentSnapshot);
                                } else {
                                    completeAdminLogin(adminId, documentSnapshot);
                                }
                            } else {
                                btnLogin.setEnabled(true);
                                loginAttempts++;
                                if (loginAttempts >= 5) {
                                    showAccountLockedDialog("Account Locked", "Your account has been locked. To continue using PrePal, please contact the support team.");
                                } else {
                                    showLoginFailedDialog(5 - loginAttempts);
                                }
                                etPassword.requestFocus();
                            }
                        } else {
                            // Check by email if ID not found
                            db.collection("Admins").whereEqualTo("email", email).get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            DocumentSnapshot adminDoc = queryDocumentSnapshots.getDocuments().get(0);
                                            String storedHashedPassword = adminDoc.getString("hashed_password");
                                            String enteredHashedPassword = SecurityUtils.hashPassword(password);

                                            if (enteredHashedPassword.equals(storedHashedPassword)) {
                                                loginAttempts = 0;
                                                String adminId = adminDoc.getString("admin_id");
                                                String adminEmail = adminDoc.getString("email");
                                                Boolean twoFactorEnabled = adminDoc.getBoolean("twoFactorEnabled");

                                                if (Boolean.TRUE.equals(twoFactorEnabled)) {
                                                    send2FAOtp(adminId, adminEmail, adminDoc);
                                                } else {
                                                    completeAdminLogin(adminId, adminDoc);
                                                }
                                            } else {
                                                btnLogin.setEnabled(true);
                                                loginAttempts++;
                                                if (loginAttempts >= 5) {
                                                    showAccountLockedDialog("Account Locked", "Your account has been locked. To continue using PrePal, please contact the support team.");
                                                } else {
                                                    showLoginFailedDialog(5 - loginAttempts);
                                                }
                                            }
                                        } else {
                                            // Fallback to Student Login
                                            checkStudentLogin(email, password, db);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        btnLogin.setEnabled(true);
                                        Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        tvForgotPassword.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_forgot_password, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            AppCompatButton btnOk = dialogView.findViewById(R.id.btnOk);
            btnOk.setOnClickListener(v1 -> dialog.dismiss());

            dialog.show();
        });
    }

    private void checkStudentLogin(String studentId, String password, FirebaseFirestore db) {
        // Use the flat AllStudents collection for direct lookup
        db.collection("AllStudents").document(studentId).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        handleStudentDocument(studentDoc, password, db);
                    } else {
                        btnLogin.setEnabled(true);
                        loginAttempts++;
                        if (loginAttempts >= 5) {
                            showAccountLockedDialog("Account Locked", "Your account has been locked. To continue using PrePal, please contact the support team.");
                        } else {
                            showLoginFailedDialog(5 - loginAttempts);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    showErrorDialog("Database Error", e.getMessage());
                });
    }

    private void handleStudentDocument(DocumentSnapshot studentDoc, String password, FirebaseFirestore db) {
        String storedHashedPassword = studentDoc.getString("hashed_password");
        String status = studentDoc.getString("status");
        Boolean isFirstLogin = studentDoc.getBoolean("isFirstLogin");

        if ("inactive".equalsIgnoreCase(status)) {
            btnLogin.setEnabled(true);
            showAccountLockedDialog("Account Inactive", "Your account is currently disabled. Please contact your administrator.");
            return;
        }

        String enteredHashedPassword = SecurityUtils.hashPassword(password);
        if (enteredHashedPassword.equals(storedHashedPassword)) {
            loginAttempts = 0;
            getSharedPreferences("UserSession", MODE_PRIVATE)
                    .edit()
                    .putString("student_id", studentDoc.getString("studentId"))
                    .putString("student_name", studentDoc.getString("name"))
                    .putString("degree", studentDoc.getString("degree"))
                    .putString("user_type", "student")
                    .apply();

            if (Boolean.TRUE.equals(isFirstLogin)) {
                Intent intent = new Intent(MainActivity.this, ChangePasswordActivity.class);
                intent.putExtra("STUDENT_ID", studentDoc.getString("studentId"));
                intent.putExtra("DOCUMENT_PATH", studentDoc.getReference().getPath());
                startActivity(intent);
            } else {
                Boolean resultsEntered = studentDoc.getBoolean("resultsEntered");
                if (Boolean.FALSE.equals(resultsEntered) || resultsEntered == null) {
                    Intent intent = new Intent(MainActivity.this, ManualResultEntryActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, StudentHomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } else {
            btnLogin.setEnabled(true);
            loginAttempts++;
            if (loginAttempts >= 5) {
                showAccountLockedDialog("Account Locked", "Your account has been locked. To continue using PrePal, please contact the support team.");
            } else {
                showLoginFailedDialog(5 - loginAttempts);
            }
        }
    }

    private void showErrorDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_modern_error, null);
        builder.setView(dialogView);
        
        TextView tvTitle = dialogView.findViewById(R.id.tvErrorTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvErrorMessage);
        
        tvTitle.setText(title);
        tvMessage.setText(message);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        AppCompatButton btnErrorOk = dialogView.findViewById(R.id.btnErrorOk);
        btnErrorOk.setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void showLoginFailedDialog(int attemptsLeft) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_login_failed, null);
        builder.setView(dialogView);

        TextView tvAttemptsCount = dialogView.findViewById(R.id.tvAttemptsCount);
        String text = "You have <font color='#E53E3E'>" + attemptsLeft + "</font> attempts remaining";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvAttemptsCount.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvAttemptsCount.setText(android.text.Html.fromHtml(text));
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnTryAgain = dialogView.findViewById(R.id.btnTryAgain);
        btnTryAgain.setOnClickListener(v1 -> dialog.dismiss());

        TextView tvForgot = dialogView.findViewById(R.id.tvDialogForgot);
        tvForgot.setOnClickListener(v1 -> {
            dialog.dismiss();
            tvForgotPassword.performClick();
        });

        dialog.show();
    }

    private void showAccountLockedDialog(String title, String message) {
        SystemAlertHelper.queueSystemAlert("SECURITY", "Account Security Alert", "Title: " + title + "\nMessage: " + message);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_login_failed, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvErrorTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvErrorMessage);
        View llAttemptsBox = dialogView.findViewById(R.id.llAttemptsBox);
        AppCompatButton btnTryAgain = dialogView.findViewById(R.id.btnTryAgain);
        TextView tvForgot = dialogView.findViewById(R.id.tvDialogForgot);

        tvTitle.setText(title);
        tvMessage.setText(message);
        llAttemptsBox.setVisibility(View.GONE);
        tvForgot.setVisibility(View.GONE);
        btnTryAgain.setText("Understood");

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnTryAgain.setOnClickListener(v1 -> dialog.dismiss());

        dialog.show();
    }

    private void completeAdminLogin(String adminId, DocumentSnapshot documentSnapshot) {
        loginAttempts = 0; // Reset on success
        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putString("admin_id", adminId)
                .putString("user_type", "admin")
                .apply();

        Toast.makeText(MainActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, AdminHomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void send2FAOtp(String adminId, String adminEmail, DocumentSnapshot adminDoc) {
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes expiry

        java.util.Map<String, Object> verificationData = new java.util.HashMap<>();
        verificationData.put("code", otp);
        verificationData.put("expiresAt", expiryTime);

        FirebaseFirestore.getInstance().collection("Admins").document(adminDoc.getId())
                .update("twoFactorVerification", verificationData)
                .addOnSuccessListener(aVoid -> {
                    // Queue the 2FA Email Document to trigger delivery
                    java.util.Map<String, Object> emailDoc = new java.util.HashMap<>();
                    emailDoc.put("to", adminEmail != null ? adminEmail : adminDoc.getId());
                    java.util.Map<String, Object> message = new java.util.HashMap<>();
                    message.put("subject", "PrePal Two-Factor Authentication OTP");
                    message.put("text", "Your PrePal login verification code is: " + otp + ". This code will expire in 5 minutes.");
                    emailDoc.put("message", message);

                    FirebaseFirestore.getInstance().collection("2FA_Emails").add(emailDoc);

                    // Alert test mode with a Toast
                    Toast.makeText(MainActivity.this, "[TEST MODE] OTP Sent to Email: " + otp, Toast.LENGTH_LONG).show();

                    btnLogin.setEnabled(true);
                    show2FaVerificationDialog(adminId, adminDoc, otp);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this, "2FA Initialization Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void show2FaVerificationDialog(String adminId, DocumentSnapshot adminDoc, String correctOtp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Two-Factor Verification");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(getResources().getColor(R.color.textColorPrimary, getTheme()));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("A 6-digit verification code has been sent to your email. Enter the code below to complete your login.");
        tvSub.setTextSize(14);
        tvSub.setPadding(0, 16, 0, 16);
        tvSub.setTextColor(getResources().getColor(R.color.textColorSecondary, getTheme()));
        layout.addView(tvSub);

        EditText etOtp = new EditText(this);
        etOtp.setHint("Enter 6-Digit Code");
        etOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        etOtp.setTextSize(16);
        etOtp.setGravity(android.view.Gravity.CENTER);
        etOtp.setBackgroundResource(R.drawable.bg_rounded_input);
        etOtp.setPadding(20, 24, 20, 24);
        layout.addView(etOtp);

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        card.setRadius(32);
        card.setCardBackgroundColor(getResources().getColor(R.color.inputBackground, getTheme()));
        card.setCardElevation(12);
        card.addView(layout);

        builder.setView(card);
        AlertDialog finalDialog = builder.create();
        if (finalDialog.getWindow() != null) {
            finalDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnVerify = new AppCompatButton(this);
        btnVerify.setText("Verify & Log In");
        btnVerify.setTextColor(android.graphics.Color.WHITE);
        btnVerify.setTextSize(15);
        btnVerify.setTypeface(null, android.graphics.Typeface.BOLD);
        btnVerify.setBackgroundResource(R.drawable.bg_gradient_button);
        btnVerify.setAllCaps(false);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 120
        );
        params.setMargins(0, 32, 0, 0);
        btnVerify.setLayoutParams(params);
        layout.addView(btnVerify);

        btnVerify.setOnClickListener(v -> {
            String enteredCode = etOtp.getText().toString().trim();
            if (enteredCode.length() != 6) {
                etOtp.setError("Code must be 6 digits");
                return;
            }

            FirebaseFirestore.getInstance().collection("Admins").document(adminDoc.getId()).get()
                    .addOnSuccessListener(latestSnap -> {
                        if (latestSnap.exists()) {
                            java.util.Map<String, Object> verification = (java.util.Map<String, Object>) latestSnap.get("twoFactorVerification");
                            if (verification != null) {
                                String code = (String) verification.get("code");
                                Long expiry = (Long) verification.get("expiresAt");

                                if (code != null && code.equals(enteredCode)) {
                                    if (expiry != null && System.currentTimeMillis() <= expiry) {
                                        finalDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "OTP Verified!", Toast.LENGTH_SHORT).show();
                                        completeAdminLogin(adminId, adminDoc);
                                    } else {
                                        etOtp.setError("Verification code has expired");
                                    }
                                } else {
                                    etOtp.setError("Invalid verification code");
                                }
                            } else {
                                etOtp.setError("No verification request found");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        finalDialog.show();
    }
}
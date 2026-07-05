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
                                 if (adminId == null || adminId.isEmpty()) {
                                     adminId = documentSnapshot.getId();
                                 }
                                 completeAdminLogin(adminId, documentSnapshot);
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
                                                if (adminId == null || adminId.isEmpty()) {
                                                    adminId = adminDoc.getId();
                                                }
                                                completeAdminLogin(adminId, adminDoc);
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

    private void checkStudentLogin(final String studentId, final String password, final FirebaseFirestore db) {
        final String upperId = studentId.toUpperCase();
        final String lowerId = studentId.toLowerCase();

        // Try uppercase first (standardized for new records)
        db.collection("AllStudents").document(upperId).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        handleStudentDocument(studentDoc, password, db);
                    } else {
                        // Fallback 1: Try lowercase (common historical entries)
                        db.collection("AllStudents").document(lowerId).get()
                                .addOnSuccessListener(studentDocLower -> {
                                    if (studentDocLower.exists()) {
                                        handleStudentDocument(studentDocLower, password, db);
                                    } else {
                                        // Fallback 2: Try exact casing entered
                                        db.collection("AllStudents").document(studentId).get()
                                                .addOnSuccessListener(studentDocExact -> {
                                                    if (studentDocExact.exists()) {
                                                        handleStudentDocument(studentDocExact, password, db);
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
                                })
                                .addOnFailureListener(e -> {
                                    btnLogin.setEnabled(true);
                                    showErrorDialog("Database Error", e.getMessage());
                                });
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
        final Boolean isFirstLogin = studentDoc.getBoolean("isFirstLogin");

        if ("inactive".equalsIgnoreCase(status)) {
            btnLogin.setEnabled(true);
            showAccountLockedDialog("Account Inactive", "Your account is currently disabled. Please contact your administrator.");
            return;
        }

        String enteredHashedPassword = SecurityUtils.hashPassword(password);
        if (enteredHashedPassword.equals(storedHashedPassword)) {
            // Resolve program ID contextually
            String programId = studentDoc.getString("programId");
            if (programId == null || programId.isEmpty()) {
                programId = studentDoc.getString("degree");
            }
            if (programId == null || programId.isEmpty()) {
                String batchId = studentDoc.getString("batchId");
                if (batchId != null && !batchId.isEmpty()) {
                    int index = batchId.indexOf('(');
                    if (index > 0) {
                        programId = batchId.substring(0, index).trim();
                    } else {
                        index = batchId.indexOf(' ');
                        if (index > 0) {
                            programId = batchId.substring(0, index).trim();
                        } else {
                            programId = batchId.trim();
                        }
                    }
                }
            }

            if (programId != null && !programId.isEmpty()) {
                final String finalProgramId = programId;
                db.collection("Degrees").document(programId).get()
                        .addOnSuccessListener(degreeDoc -> {
                            if (degreeDoc.exists()) {
                                String degreeStatus = degreeDoc.getString("status");
                                if ("inactive".equalsIgnoreCase(degreeStatus)) {
                                    btnLogin.setEnabled(true);
                                    showAccountLockedDialog("Program Inactive", "Your degree program (" + finalProgramId + ") is currently deactivated. Please contact your administrator.");
                                } else {
                                    proceedWithStudentLogin(studentDoc, isFirstLogin);
                                }
                            } else {
                                proceedWithStudentLogin(studentDoc, isFirstLogin);
                            }
                        })
                        .addOnFailureListener(e -> {
                            proceedWithStudentLogin(studentDoc, isFirstLogin);
                        });
            } else {
                proceedWithStudentLogin(studentDoc, isFirstLogin);
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

    private void proceedWithStudentLogin(DocumentSnapshot studentDoc, Boolean isFirstLogin) {
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


}
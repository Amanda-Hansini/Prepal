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
    private TextView tvSignUp, tvForgotPassword;
    private boolean isPasswordVisible = false;
    private int authFailedAttempts = 0;
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
        tvSignUp = findViewById(R.id.tvSignUp);
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

            // First, let's check if this input is an Admin ID
            db.collection("Admins").document(email).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedHashedPassword = documentSnapshot.getString("hashed_password");
                            String enteredHashedPassword = SecurityUtils.hashPassword(password);

                            if (enteredHashedPassword.equals(storedHashedPassword)) {
                                loginAttempts = 0; // Reset on success
                                String adminId = documentSnapshot.getString("admin_id");
                                getSharedPreferences("UserSession", MODE_PRIVATE)
                                        .edit()
                                        .putString("admin_id", adminId)
                                        .putString("user_type", "admin")
                                        .apply();

                                Toast.makeText(MainActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, AdminHomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                btnLogin.setEnabled(true);
                                loginAttempts++;
                                if (loginAttempts >= 5) {
                                    showErrorDialog("Access Restricted", "Too many failed attempts. Please try again later.");
                                } else {
                                    showErrorDialog("Invalid Password", "The password you entered is incorrect. Attempts left: " + (5 - loginAttempts));
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
                                                getSharedPreferences("UserSession", MODE_PRIVATE)
                                                        .edit()
                                                        .putString("admin_id", adminId)
                                                        .putString("user_type", "admin")
                                                        .apply();

                                                Toast.makeText(MainActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(MainActivity.this, AdminHomeActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                btnLogin.setEnabled(true);
                                                loginAttempts++;
                                                showErrorDialog("Invalid Password", "The password you entered is incorrect. Attempts left: " + (5 - loginAttempts));
                                            }
                                        } else {
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

        tvSignUp.setOnClickListener(v -> {
            // Label is now "Admin Registration" in XML to clarify for students
            if (authFailedAttempts >= 3) {
                Toast.makeText(MainActivity.this, "Admin sign-up is locked due to too many failed attempts.", Toast.LENGTH_LONG).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_auth_key, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            EditText etAuthKey = dialogView.findViewById(R.id.etAuthKey);
            AppCompatButton btnVerifyKey = dialogView.findViewById(R.id.btnVerifyKey);

            btnVerifyKey.setOnClickListener(verifyView -> {
                String enteredKey = etAuthKey.getText().toString().trim();
                if (enteredKey.equals("Prepal_Admin_Saegis")) {
                    authFailedAttempts = 0;
                    dialog.dismiss();
                    startActivity(new Intent(MainActivity.this, admin_sign_up.class));
                } else {
                    authFailedAttempts++;
                    if (authFailedAttempts >= 3) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Access Locked. Too many failed attempts.", Toast.LENGTH_LONG).show();
                    } else {
                        etAuthKey.setError("Invalid Key. Attempts left: " + (3 - authFailedAttempts));
                        etAuthKey.requestFocus();
                    }
                }
            });

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
                        showErrorDialog("ID Not Found", "The Student ID you entered is not registered. Please ensure the admin has synced the batch.");
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
            showErrorDialog("Account Inactive", "Your account is currently disabled. Please contact your administrator.");
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
            showErrorDialog("Invalid Password", "The password you entered is incorrect. Attempts left: " + (5 - loginAttempts));
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
}
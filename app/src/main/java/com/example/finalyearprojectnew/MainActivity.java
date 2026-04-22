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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Remove top padding logic to let the background extend or handle normally
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
                // Hide Password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
                ivTogglePassword.setColorFilter(getResources().getColor(R.color.textColorSecondary, getTheme()));
            } else {
                // Show Password
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_secure); // Optionally use an open eye icon if
                                                                                 // available
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
                etEmail.setError("Please enter your email");
                etEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Please enter your password");
                etPassword.requestFocus();
                return;
            }

            // Implement actual authentication here
            Toast.makeText(this, "Verifying Details...", Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // First, let's check if this email exists in our "Admins" table
            db.collection("Admins").whereEqualTo("email", email).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Admin found! Now let's verify the password
                            DocumentSnapshot adminDoc = queryDocumentSnapshots.getDocuments().get(0);
                            String storedHashedPassword = adminDoc.getString("hashed_password");

                            // Hash the password the user just typed in
                            String enteredHashedPassword = SecurityUtils.hashPassword(password);

                            if (enteredHashedPassword.equals(storedHashedPassword)) {
                                // Passwords match! Login successful
                                Toast.makeText(MainActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, AdminHomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Passwords do not match
                                btnLogin.setEnabled(true);
                                etPassword.setError("Invalid password");
                                etPassword.requestFocus();
                            }
                        } else {
                            // Admin NOT found, let's check Students table using studentId
                            db.collection("Students").whereEqualTo("studentId", email).get()
                                .addOnSuccessListener(studentSnapshots -> {
                                    if (!studentSnapshots.isEmpty()) {
                                        // Student found
                                        DocumentSnapshot studentDoc = studentSnapshots.getDocuments().get(0);
                                        String storedHashedPassword = studentDoc.getString("hashed_password");
                                        String status = studentDoc.getString("status");
                                        Boolean isFirstLogin = studentDoc.getBoolean("isFirstLogin");
                                        
                                        if ("inactive".equalsIgnoreCase(status)) {
                                            btnLogin.setEnabled(true);
                                            Toast.makeText(MainActivity.this, "Your account is inactive. Please contact Admin.", Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        String enteredHashedPassword = SecurityUtils.hashPassword(password);
                                        if (enteredHashedPassword.equals(storedHashedPassword)) {
                                            Toast.makeText(MainActivity.this, "Student Login Successful!", Toast.LENGTH_SHORT).show();
                                            
                                            // Check First Login Strategy
                                            if (Boolean.TRUE.equals(isFirstLogin)) {
                                                Intent intent = new Intent(MainActivity.this, ChangePasswordActivity.class);
                                                intent.putExtra("STUDENT_ID", studentDoc.getString("studentId"));
                                                startActivity(intent);
                                            } else {
                                                Intent intent = new Intent(MainActivity.this, StudentHomeActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        } else {
                                            btnLogin.setEnabled(true);
                                            etPassword.setError("Invalid password");
                                            etPassword.requestFocus();
                                        }

                                    } else {
                                        // Neither Admin nor Student found
                                        btnLogin.setEnabled(true);
                                        
                                        // Display the modern "Please register first" popup
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_modern_error, null);
                                        builder.setView(dialogView);
                                        
                                        AlertDialog dialog = builder.create();
                                        if (dialog.getWindow() != null) {
                                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                        }
                                        
                                        AppCompatButton btnErrorOk = dialogView.findViewById(R.id.btnErrorOk);
                                        btnErrorOk.setOnClickListener(v1 -> dialog.dismiss());
                                        
                                        dialog.show();
                                    }
                                })
                                .addOnFailureListener(studentErr -> {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "Student DB Error: " + studentErr.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        tvSignUp.setOnClickListener(v -> {
            if (authFailedAttempts >= 3) {
                Toast.makeText(MainActivity.this, "Admin sign-up is locked due to too many failed attempts.", Toast.LENGTH_LONG).show();
                return;
            }

            // Protected Admin Sign Up with an Organization Key
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
                // The secret organization key defined in code
                if (enteredKey.equals("Prepal_Admin_Saegis")) {
                    authFailedAttempts = 0; // Reset attempts on success
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
}
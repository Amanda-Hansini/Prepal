package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class admin_sign_up extends AppCompatActivity {

    private EditText etAdminId, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private AppCompatButton btnSignUp;
    private TextView tvLogin;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
    }

    private void initViews() {
        etAdminId = findViewById(R.id.etAdminId);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
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
                ivTogglePassword.setImageResource(android.R.drawable.ic_secure);
                ivTogglePassword.setColorFilter(getResources().getColor(R.color.colorAccent, getTheme()));
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.length());
        });

        // Toggle confirm password visibility
        ivToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                // Hide Password
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_view);
                ivToggleConfirmPassword.setColorFilter(getResources().getColor(R.color.textColorSecondary, getTheme()));
            } else {
                // Show Password
                etConfirmPassword
                        .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleConfirmPassword.setImageResource(android.R.drawable.ic_secure);
                ivToggleConfirmPassword.setColorFilter(getResources().getColor(R.color.colorAccent, getTheme()));
            }
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            etConfirmPassword.setSelection(etConfirmPassword.length());
        });

        // Sign Up Button Click
        btnSignUp.setOnClickListener(v -> {
            String adminId = etAdminId.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(adminId)) {
                etAdminId.setError("Please enter your Admin ID");
                etAdminId.requestFocus();
                return;
            }

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

            if (TextUtils.isEmpty(confirmPassword)) {
                etConfirmPassword.setError("Please confirm your password");
                etConfirmPassword.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return;
            }

            Toast.makeText(this, "Validating & Saving...", Toast.LENGTH_SHORT).show();
            btnSignUp.setEnabled(false); // Prevent multiple clicks

            // 1. Hash the password securely
            String hashedPassword = SecurityUtils.hashPassword(password);

            // 2. Prepare data for Firestore Database
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("admin_id", adminId);
            adminData.put("email", email);
            adminData.put("hashed_password", hashedPassword);
            // Storing the account status
            adminData.put("status", "Active");

            // 3. Save to Firestore (Creating a new document inside "Admins" collection)
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            // We use the adminId as the Document ID so it's easy to look up later
            db.collection("Admins").document(adminId)
                    .set(adminData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Admin Registered Successfully!", Toast.LENGTH_LONG).show();
                        // Navigate back to Login
                        Intent intent = new Intent(admin_sign_up.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSignUp.setEnabled(true);
                        Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        tvLogin.setOnClickListener(v -> {
            // Navigate back to Login
            Intent intent = new Intent(admin_sign_up.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

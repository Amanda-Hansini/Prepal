package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class sign_up extends AppCompatActivity {

    private EditText etStudentId, etFullName, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private AppCompatButton btnSignUp;
    private TextView tvLogin;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
    }

    private void initViews() {
        etStudentId = findViewById(R.id.etStudentId);
        etFullName = findViewById(R.id.etFullName);
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
            String studentId = etStudentId.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(studentId)) {
                etStudentId.setError("Please enter your Student ID");
                etStudentId.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Please enter your Full Name");
                etFullName.requestFocus();
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

            // Simulate Sign Up Process
            Toast.makeText(this, "Signing up...", Toast.LENGTH_SHORT).show();
            // TODO: Implement actual registration here
        });

        tvLogin.setOnClickListener(v -> {
            // Navigate back to Login
            Intent intent = new Intent(sign_up.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

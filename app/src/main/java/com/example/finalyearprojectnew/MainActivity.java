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

            // Simulate Login Process
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
            
            Intent intent;
            if (email.toLowerCase().startsWith("adm") || email.toLowerCase().contains("admin")) {
                intent = new Intent(MainActivity.this, AdminHomeActivity.class);
            } else {
                intent = new Intent(MainActivity.this, StudentHomeActivity.class);
            }
            startActivity(intent);
            finish();
            // TODO: Implement actual authentication here
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        tvSignUp.setOnClickListener(v -> {
            showSignUpDialog();
        });
    }

    private void showSignUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signup_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnStudentSignUp = dialogView.findViewById(R.id.btnStudentSignUp);
        AppCompatButton btnAdminSignUp = dialogView.findViewById(R.id.btnAdminSignUp);
        TextView tvCancel = dialogView.findViewById(R.id.tvCancel);

        btnStudentSignUp.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, sign_up.class));
        });

        btnAdminSignUp.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, admin_sign_up.class));
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
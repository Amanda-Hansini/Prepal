package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etNewPassword, etConfirmPassword;
    private AppCompatButton btnChangePassword;

    private String studentId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        db = FirebaseFirestore.getInstance();
        studentId = getIntent().getStringExtra("STUDENT_ID");

        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Error: Student ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish()); // Ideally should log out or prevent going back

        btnChangePassword.setOnClickListener(v -> {
            String newPwd = etNewPassword.getText().toString().trim();
            String confPwd = etConfirmPassword.getText().toString().trim();

            if (newPwd.isEmpty() || confPwd.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPwd.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPwd.equals(confPwd)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePasswordInFirestore(newPwd);
        });
    }

    private void updatePasswordInFirestore(String newPwd) {
        btnChangePassword.setEnabled(false);
        String hashedNewPwd = SecurityUtils.hashPassword(newPwd);

        db.collection("Students").document(studentId)
                .update(
                        "hashed_password", hashedNewPwd,
                        "isFirstLogin", false
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ChangePasswordActivity.this, StudentHomeActivity.class);
                    // Clear task so user can't press back to login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

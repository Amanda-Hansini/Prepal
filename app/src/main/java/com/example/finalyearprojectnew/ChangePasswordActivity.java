package com.example.finalyearprojectnew;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView ivBack, ivToggleNewPassword, ivToggleConfirmPassword;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private EditText etNewPassword, etConfirmPassword;
    private AppCompatButton btnChangePassword;

    // Password strength & suggestion UI
    private TextView tvStrengthLabel, btnSuggestPassword, tvSuggestionTip;
    private View viewStrengthBar1, viewStrengthBar2, viewStrengthBar3, viewStrengthBar4;
    private TextView tvCheckLength, tvCheckCase, tvCheckNumber, tvCheckSymbol;

    private String studentId, documentPath;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        db = FirebaseFirestore.getInstance();
        studentId = getIntent().getStringExtra("STUDENT_ID");
        documentPath = getIntent().getStringExtra("DOCUMENT_PATH");

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
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        tvStrengthLabel = findViewById(R.id.tvStrengthLabel);
        btnSuggestPassword = findViewById(R.id.btnSuggestPassword);
        tvSuggestionTip = findViewById(R.id.tvSuggestionTip);
        viewStrengthBar1 = findViewById(R.id.viewStrengthBar1);
        viewStrengthBar2 = findViewById(R.id.viewStrengthBar2);
        viewStrengthBar3 = findViewById(R.id.viewStrengthBar3);
        viewStrengthBar4 = findViewById(R.id.viewStrengthBar4);
        tvCheckLength = findViewById(R.id.tvCheckLength);
        tvCheckCase = findViewById(R.id.tvCheckCase);
        tvCheckNumber = findViewById(R.id.tvCheckNumber);
        tvCheckSymbol = findViewById(R.id.tvCheckSymbol);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish()); // Ideally should log out or prevent going back

        ivToggleNewPassword.setOnClickListener(v -> {
            isNewPasswordVisible = !isNewPasswordVisible;
            if (isNewPasswordVisible) {
                etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleNewPassword.setImageResource(R.drawable.ic_eye_filled);
            } else {
                etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleNewPassword.setImageResource(R.drawable.ic_eye_off_filled);
            }
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_filled);
            } else {
                etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off_filled);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnSuggestPassword.setOnClickListener(v -> {
            String suggested = generateStrongPassword();
            etNewPassword.setText(suggested);
            etConfirmPassword.setText(suggested);
            etNewPassword.setSelection(suggested.length());
            Toast.makeText(this, "Strong password suggested: " + suggested, Toast.LENGTH_SHORT).show();
        });

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                evaluatePasswordStrength(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

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

            int strengthScore = calculateScore(newPwd);
            if (strengthScore < 2) {
                Toast.makeText(this, "Password is too weak. Please add letters, numbers, or symbols.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPwd.equals(confPwd)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePasswordInFirestore(newPwd);
        });
    }

    private int calculateScore(String pwd) {
        if (pwd == null || pwd.isEmpty()) return 0;
        int score = 0;
        if (pwd.length() >= 8) score++;
        if (pwd.matches(".*[A-Z].*") && pwd.matches(".*[a-z].*")) score++;
        if (pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`].*")) score++;
        return score;
    }

    private void evaluatePasswordStrength(String pwd) {
        boolean hasLength = pwd.length() >= 8;
        boolean hasCase = pwd.matches(".*[A-Z].*") && pwd.matches(".*[a-z].*");
        boolean hasNumber = pwd.matches(".*[0-9].*");
        boolean hasSymbol = pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`].*");

        int greenColor = Color.parseColor("#48BB78");
        int greyColor = Color.parseColor("#718096");

        tvCheckLength.setText(hasLength ? "✓ At least 8 characters long" : "• At least 8 characters long");
        tvCheckLength.setTextColor(hasLength ? greenColor : greyColor);

        tvCheckCase.setText(hasCase ? "✓ Uppercase and lowercase letters (A-z)" : "• Uppercase and lowercase letters (A-z)");
        tvCheckCase.setTextColor(hasCase ? greenColor : greyColor);

        tvCheckNumber.setText(hasNumber ? "✓ Contains at least one number (0-9)" : "• Contains at least one number (0-9)");
        tvCheckNumber.setTextColor(hasNumber ? greenColor : greyColor);

        tvCheckSymbol.setText(hasSymbol ? "✓ Contains a special symbol (!@#$%^&*)" : "• Contains a special symbol (!@#$%^&*)");
        tvCheckSymbol.setTextColor(hasSymbol ? greenColor : greyColor);

        int score = calculateScore(pwd);

        int colorGrey = Color.parseColor("#E0E0E0");
        int colorRed = Color.parseColor("#E53E3E");
        int colorOrange = Color.parseColor("#ED8936");
        int colorBlue = Color.parseColor("#4299E1");
        int colorGreen = Color.parseColor("#48BB78");

        if (pwd.isEmpty()) {
            viewStrengthBar1.setBackgroundColor(colorGrey);
            viewStrengthBar2.setBackgroundColor(colorGrey);
            viewStrengthBar3.setBackgroundColor(colorGrey);
            viewStrengthBar4.setBackgroundColor(colorGrey);
            tvStrengthLabel.setText("Strength: Enter a password");
            tvStrengthLabel.setTextColor(greyColor);
            tvSuggestionTip.setText("💡 Tip: Combine 8+ characters, numbers, and symbols for a strong password.");
        } else if (score == 1) {
            viewStrengthBar1.setBackgroundColor(colorRed);
            viewStrengthBar2.setBackgroundColor(colorGrey);
            viewStrengthBar3.setBackgroundColor(colorGrey);
            viewStrengthBar4.setBackgroundColor(colorGrey);
            tvStrengthLabel.setText("Strength: Weak");
            tvStrengthLabel.setTextColor(colorRed);
            tvSuggestionTip.setText("💡 Weak: Try adding numbers, uppercase letters, and symbols.");
        } else if (score == 2) {
            viewStrengthBar1.setBackgroundColor(colorOrange);
            viewStrengthBar2.setBackgroundColor(colorOrange);
            viewStrengthBar3.setBackgroundColor(colorGrey);
            viewStrengthBar4.setBackgroundColor(colorGrey);
            tvStrengthLabel.setText("Strength: Moderate");
            tvStrengthLabel.setTextColor(colorOrange);
            tvSuggestionTip.setText("💡 Moderate: Add special characters (!@#$) or more letters to strengthen.");
        } else if (score == 3) {
            viewStrengthBar1.setBackgroundColor(colorBlue);
            viewStrengthBar2.setBackgroundColor(colorBlue);
            viewStrengthBar3.setBackgroundColor(colorBlue);
            viewStrengthBar4.setBackgroundColor(colorGrey);
            tvStrengthLabel.setText("Strength: Good");
            tvStrengthLabel.setTextColor(colorBlue);
            tvSuggestionTip.setText("💡 Good: ALMOST STRONG! Add one more security element (symbol or number).");
        } else {
            viewStrengthBar1.setBackgroundColor(colorGreen);
            viewStrengthBar2.setBackgroundColor(colorGreen);
            viewStrengthBar3.setBackgroundColor(colorGreen);
            viewStrengthBar4.setBackgroundColor(colorGreen);
            tvStrengthLabel.setText("Strength: Strong ✓");
            tvStrengthLabel.setTextColor(colorGreen);
            tvSuggestionTip.setText("🔒 Excellent! Your password is strong and highly secure.");
        }
    }

    private String generateStrongPassword() {
        String[] prefixes = {"Saegis", "Prepal", "Student", "Degree", "Scholar", "Campus", "Future", "Academic"};
        String[] symbols = {"#", "@", "$", "!", "*", "&", "?"};
        Random random = new Random();
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String symbol1 = symbols[random.nextInt(symbols.length)];
        String symbol2 = symbols[random.nextInt(symbols.length)];
        int number = 100 + random.nextInt(900);
        return prefix + symbol1 + number + symbol2;
    }

    private void updatePasswordInFirestore(String newPwd) {
        btnChangePassword.setEnabled(false);
        String hashedNewPwd = SecurityUtils.hashPassword(newPwd);

        // Also update the flat AllStudents collection for future logins
        db.collection("AllStudents").document(studentId)
                .update(
                        "hashed_password", hashedNewPwd,
                        "isFirstLogin", false
                );

        if (documentPath != null && !documentPath.isEmpty()) {
            db.document(documentPath)
                    .update(
                            "hashed_password", hashedNewPwd,
                            "isFirstLogin", false
                    )
                    .addOnSuccessListener(aVoid -> handleSuccess())
                    .addOnFailureListener(e -> handleFailure(e));
        } else {
            // Fallback to legacy path if documentPath missing
            db.collection("Students").document(studentId)
                    .update(
                            "hashed_password", hashedNewPwd,
                            "isFirstLogin", false
                    )
                    .addOnSuccessListener(aVoid -> handleSuccess())
                    .addOnFailureListener(e -> handleFailure(e));
        }
    }

    private void handleSuccess() {
        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ChangePasswordActivity.this, StudentHomeActivity.class);
        // Clear task so user can't press back to login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleFailure(Exception e) {
        btnChangePassword.setEnabled(true);
        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}

package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private AppCompatButton btnSettings, btnSaveProfile;
    private TextView tvLogout;
    private EditText etFullName, etEmail, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        initViews();
        setupBottomNavigation();
        setupListeners();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
        btnSettings = findViewById(R.id.btnSettings);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        tvLogout = findViewById(R.id.tvLogout);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
    }

    private void setupBottomNavigation() {
        // Set profile selected
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AdminProfileActivity.this, AdminHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_report) {
                Intent intent = new Intent(AdminProfileActivity.this, AdminReportActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AdminProfileActivity.this, AdminSettingsActivity.class);
            startActivity(intent);
        });

        btnSaveProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();
        });

        tvLogout.setOnClickListener(v -> {
            // Logic to sign out
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminReportActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private AppCompatButton btnExportStudents, btnDownloadLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report);

        initViews();
        setupBottomNavigation();
        setupListeners();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
        btnExportStudents = findViewById(R.id.btnExportStudents);
        btnDownloadLogs = findViewById(R.id.btnDownloadLogs);
    }

    private void setupBottomNavigation() {
        // Set report selected
        bottomNavigationView.setSelectedItemId(R.id.nav_report);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AdminReportActivity.this, AdminHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_report) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(AdminReportActivity.this, AdminProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        btnExportStudents.setOnClickListener(v -> {
            Toast.makeText(this, "Exporting CSV...", Toast.LENGTH_SHORT).show();
            // TODO: Download logic
        });

        btnDownloadLogs.setOnClickListener(v -> {
            Toast.makeText(this, "Downloading System Logs...", Toast.LENGTH_SHORT).show();
            // TODO: Download logic
        });
    }
}

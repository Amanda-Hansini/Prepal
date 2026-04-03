package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminHomeActivity extends AppCompatActivity {

    private CardView cardDegree, cardBatch, cardSemester, cardModule;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardDegree = findViewById(R.id.cardDegree);
        cardBatch = findViewById(R.id.cardBatch);
        cardSemester = findViewById(R.id.cardSemester);
        cardModule = findViewById(R.id.cardModule);
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
    }

    private void setupListeners() {
        cardDegree.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ManageDegreesActivity.class);
            startActivity(intent);
        });

        cardBatch.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ManageBatchesActivity.class);
            startActivity(intent);
        });

        cardSemester.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ManageSemestersActivity.class);
            startActivity(intent);
        });

        cardModule.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ManageModulesActivity.class);
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_report) {
                Intent intent = new Intent(AdminHomeActivity.this, AdminReportActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(AdminHomeActivity.this, AdminProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}

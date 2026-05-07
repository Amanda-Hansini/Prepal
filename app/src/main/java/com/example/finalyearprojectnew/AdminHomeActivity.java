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
    private android.widget.TextView tvAdminId, tvWelcomeAdmin, tvCurrentDate;
    private android.widget.ImageView ivAdminProfile;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private String adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        adminId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("admin_id", "");

        initViews();
        setupListeners();
        loadAdminInfo();
        setCurrentDate();
    }

    private void initViews() {
        cardDegree = findViewById(R.id.cardDegree);
        cardBatch = findViewById(R.id.cardBatch);
        cardSemester = findViewById(R.id.cardSemester);
        cardModule = findViewById(R.id.cardModule);
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
        
        tvAdminId = findViewById(R.id.tvAdminId);
        tvWelcomeAdmin = findViewById(R.id.tvWelcomeAdmin);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        ivAdminProfile = findViewById(R.id.ivAdminProfile);
    }

    private void loadAdminInfo() {
        if (adminId.isEmpty()) return;
        
        tvAdminId.setText("Admin ID: " + adminId);
        
        db.collection("Admins").document(adminId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("full_name");
                        String profileImageUrl = documentSnapshot.getString("profile_image_url");
                        String profileImageBase64 = documentSnapshot.getString("profile_image_base64");

                        if (name != null && !name.isEmpty()) {
                            tvWelcomeAdmin.setText("WELCOME, " + name.toUpperCase());
                        }

                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            byte[] decodedString = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivAdminProfile.setImageBitmap(decodedByte);
                        } else if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile_filled)
                                    .into(ivAdminProfile);
                        }
                    }
                });
    }

    private void setCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new java.util.Date()));
    }

    private void setupListeners() {
        ivAdminProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminProfileActivity.class);
            startActivity(intent);
        });

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

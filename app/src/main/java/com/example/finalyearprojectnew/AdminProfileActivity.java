package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private AppCompatButton btnSettings, btnSaveProfile;
    private TextView tvLogout, tvAdminId, tvProfileName, tvProfileStatus;
    private EditText etFullName, etEmail;
    private ImageView ivAvatar;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private String adminId;
    private android.net.Uri imageUri;
    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivAvatar.setImageURI(imageUri);
                    uploadImageToFirebase();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        adminId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("admin_id", "");

        initViews();
        setupBottomNavigation();
        setupListeners();
        loadAdminData();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
        btnSettings = findViewById(R.id.btnSettings);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        tvLogout = findViewById(R.id.tvLogout);

        tvAdminId = findViewById(R.id.tvAdminId);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        ivAvatar = findViewById(R.id.ivAvatar);
    }

    private void loadAdminData() {
        if (adminId.isEmpty()) return;

        db.collection("Admins").document(adminId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("full_name");
                        if (name == null || name.isEmpty()) name = "Admin User";
                        String email = documentSnapshot.getString("email");
                        String status = documentSnapshot.getString("status");
                        String profileImageUrl = documentSnapshot.getString("profile_image_url");
                        String profileImageBase64 = documentSnapshot.getString("profile_image_base64");

                        tvAdminId.setText(adminId);
                        tvProfileName.setText(name);
                        tvProfileStatus.setText(status != null ? status : "Active");
                        etFullName.setText(name);
                        etEmail.setText(email);

                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            byte[] decodedString = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivAvatar.setImageBitmap(decodedByte);
                        } else if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile_filled)
                                    .into(ivAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (adminId.isEmpty()) {
            Toast.makeText(this, "Cannot upload: Admin ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Optimizing & Saving...", Toast.LENGTH_SHORT).show();
        
        try {
            // 1. Convert URI to Bitmap
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            
            // 2. Resize Bitmap (Important to stay under Firestore 1MB limit)
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min(400f / width, 400f / height);
            if (scale < 1) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postScale(scale, scale);
                bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            }

            // 3. Convert Bitmap to Base64 String
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] b = baos.toByteArray();
            String encodedImage = android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);

            // 4. Save Base64 string to Firestore
            db.collection("Admins").document(adminId).update("profile_image_base64", encodedImage)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Image Updated Successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UPLOAD_ERROR", "Firestore Save Failed", e);
                    Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        } catch (java.io.IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AdminProfileActivity.this, AdminSettingsActivity.class);
            startActivity(intent);
        });

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etFullName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("full_name", newName);
            updates.put("email", newEmail);

            db.collection("Admins").document(adminId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                        tvProfileName.setText(newName);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
        });

        tvLogout.setOnClickListener(v -> {
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

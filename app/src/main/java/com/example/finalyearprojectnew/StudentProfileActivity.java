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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private AppCompatButton btnSaveProfile;
    private TextView tvLogout, tvStudentId, tvProfileName, tvProfileStatus;
    private EditText etFullName, etEmail;
    private ImageView ivAvatar;
    private FirebaseFirestore db;
    private String studentId;
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
        setContentView(R.layout.activity_student_profile);

        db = FirebaseFirestore.getInstance();
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "");

        initViews();
        setupBottomNavigation();
        setupListeners();
        loadStudentData();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        tvLogout = findViewById(R.id.tvLogout);

        tvStudentId = findViewById(R.id.tvStudentId);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        ivAvatar = findViewById(R.id.ivAvatar);
    }

    private void loadStudentData() {
        if (studentId.isEmpty()) return;

        db.collection("AllStudents").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("full_name");
                        if (name == null || name.isEmpty()) name = "Student User";
                        String email = documentSnapshot.getString("email");
                        String profileImageBase64 = documentSnapshot.getString("profile_image_base64");

                        tvStudentId.setText(studentId);
                        tvProfileName.setText(name);
                        tvProfileStatus.setText("Active Student");
                        etFullName.setText(name);
                        etEmail.setText(email);

                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            byte[] decodedString = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivAvatar.setImageBitmap(decodedByte);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToFirebase() {
        if (imageUri == null || studentId.isEmpty()) return;

        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min(400f / width, 400f / height);
            if (scale < 1) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postScale(scale, scale);
                bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] b = baos.toByteArray();
            String encodedImage = android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);

            db.collection("AllStudents").document(studentId).update("profile_image_base64", encodedImage)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile Image Updated!", Toast.LENGTH_SHORT).show());

        } catch (java.io.IOException e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, StudentHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, StudentCalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_chats) {
                startActivity(new Intent(this, ChatListActivity.class));
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

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etFullName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("full_name", newName);
            updates.put("email", newEmail);

            db.collection("AllStudents").document(studentId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        tvProfileName.setText(newName);
                    });
        });

        tvLogout.setOnClickListener(v -> {
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

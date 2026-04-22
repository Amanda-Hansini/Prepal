package com.example.finalyearprojectnew;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageStudentsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvScreenSubtitle, tvScreenTitle;
    private AppCompatButton btnUploadCsv, btnAddSingle;
    private RecyclerView rvStudents;
    private ProgressBar progressBar;

    private String batchId;
    private String programId;
    private String batchName;

    private FirebaseFirestore db;
    private StudentAdapter adapter;
    private List<Student> studentList;

    private static final int CSV_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        db = FirebaseFirestore.getInstance();
        batchId = getIntent().getStringExtra("BATCH_ID");
        programId = getIntent().getStringExtra("PROGRAM_ID");
        batchName = getIntent().getStringExtra("BATCH_NAME");

        initViews();
        setupRecyclerView();
        fetchStudents();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvScreenSubtitle = findViewById(R.id.tvScreenSubtitle);
        btnUploadCsv = findViewById(R.id.btnUploadCsv);
        btnAddSingle = findViewById(R.id.btnAddSingle);
        rvStudents = findViewById(R.id.rvStudents);
        progressBar = findViewById(R.id.progressBar);

        tvScreenSubtitle.setText(programId + " - " + batchName + " (" + batchId + ")");
    }

    private void setupRecyclerView() {
        studentList = new ArrayList<>();
        adapter = new StudentAdapter(studentList, new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onDeleteClick(Student student) {
                deleteStudent(student);
            }

            @Override
            public void onStatusToggle(Student student) {
                String newStatus = student.getStatus().equalsIgnoreCase("active") ? "inactive" : "active";
                updateStudentStatus(student.getStudentId(), newStatus);
            }
        });

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);
    }

    private void fetchStudents() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Students").whereEqualTo("batchId", batchId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Student s = doc.toObject(Student.class);
                        if (s != null) {
                            studentList.add(s);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteStudent(Student student) {
        db.collection("Students").document(student.getStudentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show();
                    fetchStudents();
                });
    }

    private void updateStudentStatus(String studentId, String newStatus) {
        db.collection("Students").document(studentId).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    fetchStudents();
                });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnAddSingle.setOnClickListener(v -> showAddSingleStudentDialog());

        btnUploadCsv.setOnClickListener(v -> openCsvPicker());
    }

    private void showAddSingleStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_manage_batches, null); // We should use a smaller dialog layout, let's create it inline to save time or use Alert Dialog simple fields
        
        // Since we didn't create a specific dialog xml, let's create one programmatically or build a simple structure
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etId = new EditText(this);
        etId.setHint("Student ID (e.g. STU001)");
        layout.addView(etId);

        final EditText etName = new EditText(this);
        etName.setHint("Full Name");
        layout.addView(etName);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        layout.addView(etEmail);

        final EditText etPassword = new EditText(this);
        etPassword.setHint("Initial Password");
        layout.addView(etPassword);

        builder.setView(layout);
        builder.setTitle("Add Single Student");
        builder.setPositiveButton("Add", (dialog, which) -> {
            String sid = etId.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();

            if (!sid.isEmpty() && !name.isEmpty() && !email.isEmpty() && !pwd.isEmpty()) {
                saveStudentToFirebase(sid, name, email, pwd, "active");
            } else {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, CSV_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CSV_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                parseAndUploadCsv(uri);
            }
        }
    }

    private void parseAndUploadCsv(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                
                // Read header row
                reader.readLine(); 
                
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length >= 7) {
                        String sId = tokens[2].trim();
                        String name = tokens[3].trim();
                        String email = tokens[4].trim();
                        String status = tokens[5].trim().toLowerCase();
                        String rawPassword = tokens[6].trim();

                        saveStudentToFirebaseSync(sId, name, email, rawPassword, status);
                        count++;
                    }
                }
                
                int finalCount = count;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Uploaded " + finalCount + " students", Toast.LENGTH_SHORT).show();
                    fetchStudents();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error parsing CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveStudentToFirebase(String studentId, String fullName, String email, String rawPassword, String status) {
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("studentId", studentId);
        studentMap.put("fullName", fullName);
        studentMap.put("email", email);
        studentMap.put("status", status);
        studentMap.put("batchId", batchId);
        studentMap.put("programId", programId);
        studentMap.put("hashed_password", hashedPassword);
        studentMap.put("isFirstLogin", true);

        db.collection("Students").document(studentId).set(studentMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student Added", Toast.LENGTH_SHORT).show();
                    fetchStudents();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed Add: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveStudentToFirebaseSync(String studentId, String fullName, String email, String rawPassword, String status) {
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("studentId", studentId);
        studentMap.put("fullName", fullName);
        studentMap.put("email", email);
        studentMap.put("status", status);
        studentMap.put("batchId", batchId);
        studentMap.put("programId", programId);
        studentMap.put("hashed_password", hashedPassword);
        studentMap.put("isFirstLogin", true);

        // Blocking intentionally since we are running in a background thread for CSV parsing
        db.collection("Students").document(studentId).set(studentMap);
    }
}

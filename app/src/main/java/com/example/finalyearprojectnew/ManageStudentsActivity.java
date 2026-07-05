package com.example.finalyearprojectnew;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private Spinner spinnerDegreeFilter, spinnerBatchFilter;

    private String batchId;
    private String programId;
    private String batchName;

    private FirebaseFirestore db;
    private StudentAdapter adapter;
    private List<Student> studentList;

    private List<Student> allStudentsMasterList = new ArrayList<>();
    private List<String> degreesList = new ArrayList<>();
    private List<String> batchesList = new ArrayList<>();
    private List<String> allBatchesList = new ArrayList<>();
    
    // For mapping batches to their parent degree IDs and document paths
    private Map<String, String> batchToDegreeMap = new HashMap<>(); 
    private Map<String, String> batchIdToDocPathMap = new HashMap<>(); // batchId -> programId(batchName)

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
        loadFilterMetadata();
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
        spinnerDegreeFilter = findViewById(R.id.spinnerDegreeFilter);
        spinnerBatchFilter = findViewById(R.id.spinnerBatchFilter);

        if (batchId != null) {
            tvScreenSubtitle.setText(programId + " - " + batchName + " (" + batchId + ")");
        } else {
            tvScreenSubtitle.setText("Global Student Directory");
        }
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

            @Override
            public void onEditClick(Student student) {
                showEditStudentDialog(student);
            }
        });

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);
    }

    private void loadFilterMetadata() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Degrees").get().addOnSuccessListener(degreeSnaps -> {
            degreesList.clear();
            degreesList.add("All Degrees");
            for (DocumentSnapshot doc : degreeSnaps) {
                degreesList.add(doc.getId());
            }
            ArrayAdapter<String> degAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreesList);
            degAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerDegreeFilter.setAdapter(degAdapter);

            // Fetch all batches
            db.collection("Batches").get().addOnSuccessListener(batchSnaps -> {
                batchesList.clear();
                batchesList.add("All Batches");
                allBatchesList.clear();
                batchToDegreeMap.clear();
                batchIdToDocPathMap.clear();
                
                for (DocumentSnapshot doc : batchSnaps) {
                    String bId = doc.getString("batchId");
                    String bName = doc.getString("batchName");
                    String pId = doc.getString("programId");
                    if (bId != null) {
                        allBatchesList.add(bId);
                        if (pId != null) {
                            batchToDegreeMap.put(bId, pId);
                        }
                        batchIdToDocPathMap.put(bId, doc.getId()); // docId is programId(batchName)
                    }
                }

                // If opened from a specific Degree context, pre-filter batches immediately
                if (programId != null) {
                    for (String bId : allBatchesList) {
                        String pId = batchToDegreeMap.get(bId);
                        if (pId != null && pId.equalsIgnoreCase(programId)) {
                            batchesList.add(bId);
                        }
                    }
                } else {
                    batchesList.addAll(allBatchesList);
                }

                ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchesList);
                batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spinnerBatchFilter.setAdapter(batchAdapter);

                // Setup default spinner selections from contextual intent if present
                if (programId != null && batchId != null) {
                    setSpinnerSelection(spinnerDegreeFilter, programId);
                    setSpinnerSelection(spinnerBatchFilter, batchId);
                }

                // Fetch global students
                fetchGlobalStudents();
            });
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void fetchGlobalStudents() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("AllStudents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudentsMasterList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Student s = doc.toObject(Student.class);
                        if (s != null) {
                            allStudentsMasterList.add(s);
                        }
                    }
                    applyFilters();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load global roster", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        if (allStudentsMasterList == null) return;

        Object degObj = spinnerDegreeFilter.getSelectedItem();
        Object batchObj = spinnerBatchFilter.getSelectedItem();

        String selectedDeg = degObj != null ? degObj.toString() : "All Degrees";
        String selectedBatch = batchObj != null ? batchObj.toString() : "All Batches";

        studentList.clear();
        for (Student s : allStudentsMasterList) {
            String sBatch = s.getBatchId();
            String sDegree = sBatch != null ? batchToDegreeMap.get(sBatch) : null;

            boolean degMatch = selectedDeg.equals("All Degrees") || (sDegree != null && sDegree.equalsIgnoreCase(selectedDeg));
            boolean batchMatch = selectedBatch.equals("All Batches") || (sBatch != null && sBatch.equalsIgnoreCase(selectedBatch));

            if (degMatch && batchMatch) {
                studentList.add(s);
            }
        }
        adapter.notifyDataSetChanged();

        // Update action parameters to contextually match the currently selected filter
        if (!studentList.isEmpty()) {
            Student representative = studentList.get(0);
            batchId = representative.getBatchId();
            programId = representative.getBatchId() != null ? batchToDegreeMap.get(representative.getBatchId()) : null;
            batchName = batchId; // fallback
        } else {
            // Keep filter values
            batchId = selectedBatch.equals("All Batches") ? null : selectedBatch;
            programId = selectedDeg.equals("All Degrees") ? null : selectedDeg;
            batchName = batchId;
        }
    }

    private void deleteStudent(Student student) {
        String docPath = batchIdToDocPathMap.get(student.getBatchId());
        if (docPath == null) {
            docPath = programId + "(" + batchName + ")"; // Fallback
        }
        final String finalDocPath = docPath;
        db.collection("Students").document(finalDocPath).collection("Student IDs").document(student.getStudentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("AllStudents").document(student.getStudentId()).delete();
                    ActivityLogger.logAction(this, "Deleted Student", "ID: " + student.getStudentId());
                    Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show();
                    fetchGlobalStudents();
                });
    }

    private void updateStudentStatus(String studentId, String newStatus) {
        // Resolve student batch from master list
        String sBatchId = null;
        for (Student s : allStudentsMasterList) {
            if (s.getStudentId().equals(studentId)) {
                sBatchId = s.getBatchId();
                break;
            }
        }
        
        String docPath = batchIdToDocPathMap.get(sBatchId);
        if (docPath == null) {
            docPath = programId + "(" + batchName + ")"; // Fallback
        }
        
        final String finalDocPath = docPath;
        db.collection("Students").document(finalDocPath).collection("Student IDs").document(studentId).update("status", newStatus);
        db.collection("AllStudents").document(studentId).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    ActivityLogger.logAction(this, "Changed Student Status", "ID: " + studentId + " -> " + newStatus);
                    fetchGlobalStudents();
                });
    }

    private void updateBatchFilterDropdown(String selectedDegree) {
        String currentSelectedBatch = spinnerBatchFilter.getSelectedItem() != null ? 
                spinnerBatchFilter.getSelectedItem().toString() : "All Batches";

        batchesList.clear();
        batchesList.add("All Batches");

        for (String bId : allBatchesList) {
            String pId = batchToDegreeMap.get(bId);
            if (selectedDegree.equals("All Degrees") || (pId != null && pId.equalsIgnoreCase(selectedDegree))) {
                batchesList.add(bId);
            }
        }

        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchesList);
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatchFilter.setAdapter(batchAdapter);

        // Try to restore the previously selected batch if it is still in the list
        if (batchesList.contains(currentSelectedBatch)) {
            setSpinnerSelection(spinnerBatchFilter, currentSelectedBatch);
        } else {
            spinnerBatchFilter.setSelection(0); // Select "All Batches"
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnAddSingle.setOnClickListener(v -> showAddSingleStudentDialog());

        btnUploadCsv.setOnClickListener(v -> openCsvPicker());

        findViewById(R.id.btnSyncLogin).setOnClickListener(v -> syncStudentsWithLoginSystem());

        spinnerDegreeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDegree = spinnerDegreeFilter.getSelectedItem().toString();
                updateBatchFilterDropdown(selectedDegree);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBatchFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void syncStudentsWithLoginSystem() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("AllStudents").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Toast.makeText(this, "Sync complete (flat login table)", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddSingleStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final Spinner spinnerBatchSelect = new Spinner(this);
        List<String> selectableBatches = new ArrayList<>(batchesList);
        selectableBatches.remove("All Batches");
        ArrayAdapter<String> batchSelAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, selectableBatches);
        batchSelAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatchSelect.setAdapter(batchSelAdapter);
        
        if (batchId != null && selectableBatches.contains(batchId)) {
            spinnerBatchSelect.setSelection(selectableBatches.indexOf(batchId));
        }
        layout.addView(spinnerBatchSelect);

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

        final EditText etStatus = new EditText(this);
        etStatus.setHint("Status (active/inactive)");
        layout.addView(etStatus);

        builder.setView(layout);
        builder.setTitle("Add Single Student");
        builder.setPositiveButton("Add", (dialog, which) -> {
            String targetBatch = spinnerBatchSelect.getSelectedItem() != null ? spinnerBatchSelect.getSelectedItem().toString() : batchId;
            String sid = etId.getText().toString().trim().toUpperCase();
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();
            String status = etStatus.getText().toString().trim().toLowerCase();
            
            if (status.isEmpty()) {
                status = "active";
            }

            if (targetBatch == null || targetBatch.isEmpty()) {
                Toast.makeText(this, "A valid Batch must be selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!sid.isEmpty() && !name.isEmpty() && !email.isEmpty() && !pwd.isEmpty()) {
                saveStudentToFirebase(sid, name, email, pwd, status, targetBatch);
            } else {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditStudentDialog(Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etId = new EditText(this);
        etId.setText(student.getStudentId());
        etId.setEnabled(false); // ID should not be editable
        layout.addView(etId);

        final EditText etName = new EditText(this);
        etName.setText(student.getFullName());
        etName.setHint("Full Name");
        layout.addView(etName);

        final EditText etEmail = new EditText(this);
        etEmail.setText(student.getEmail());
        etEmail.setHint("Email");
        layout.addView(etEmail);

        final EditText etStatus = new EditText(this);
        etStatus.setText(student.getStatus());
        etStatus.setHint("Status (active/inactive)");
        layout.addView(etStatus);

        builder.setView(layout);
        builder.setTitle("Edit Student");
        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String status = etStatus.getText().toString().trim().toLowerCase();
            
            if (status.isEmpty()) {
                status = "active";
            }

            if (!name.isEmpty() && !email.isEmpty()) {
                updateStudentDetailsInFirebase(student.getStudentId(), name, email, status);
            } else {
                Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateStudentDetailsInFirebase(String studentId, String fullName, String email, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("status", status);

        // Resolve student batch from master list
        String sBatchId = null;
        for (Student s : allStudentsMasterList) {
            if (s.getStudentId().equals(studentId)) {
                sBatchId = s.getBatchId();
                break;
            }
        }
        
        String docPath = batchIdToDocPathMap.get(sBatchId);
        if (docPath == null) {
            docPath = programId + "(" + batchName + ")"; // Fallback
        }

        final String finalDocPath = docPath;
        db.collection("Students").document(finalDocPath).collection("Student IDs").document(studentId).update(updates);
        db.collection("AllStudents").document(studentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    ActivityLogger.logAction(this, "Updated Student Details", "ID: " + studentId);
                    Toast.makeText(this, "Student Updated", Toast.LENGTH_SHORT).show();
                    fetchGlobalStudents();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openCsvPicker() {
        if (batchId == null || batchId.equals("All Batches")) {
            Toast.makeText(this, "Please select a specific Batch filter first to upload students under!", Toast.LENGTH_LONG).show();
            return;
        }
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
                    if (line.trim().isEmpty()) continue;
                    
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].replaceAll("^\"|\"$", "").trim();
                    }
                    
                    if (tokens.length >= 5) {
                        String sId = tokens[0].trim().toUpperCase();
                        String name = tokens[1].trim();
                        String email = tokens[2].trim();
                        String rawPassword = tokens[3].trim();
                        String status = tokens[4].trim().toLowerCase();

                        saveStudentToFirebaseSync(sId, name, email, rawPassword, status, batchId);
                        count++;
                    }
                }
                
                int finalCount = count;
                runOnUiThread(() -> {
                    ActivityLogger.logAction(this, "Uploaded Students CSV", "Batch: " + batchId + ", Count: " + finalCount);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Uploaded " + finalCount + " students under batch: " + batchId, Toast.LENGTH_SHORT).show();
                    fetchGlobalStudents();
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

    private void saveStudentToFirebase(String studentId, String fullName, String email, String rawPassword, String status, String targetBatch) {
        final String finalStudentId = studentId.toUpperCase();
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        // Resolve programId from targetBatch mapping
        String targetProgramId = batchToDegreeMap.get(targetBatch);
        if (targetProgramId == null) {
            targetProgramId = programId != null ? programId : "Unknown";
        }

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("studentId", finalStudentId);
        studentMap.put("fullName", fullName);
        studentMap.put("email", email);
        studentMap.put("status", status);
        studentMap.put("batchId", targetBatch);
        studentMap.put("programId", targetProgramId);
        studentMap.put("hashed_password", hashedPassword);
        studentMap.put("isFirstLogin", true);

        String docPath = batchIdToDocPathMap.get(targetBatch);
        if (docPath == null) {
            docPath = targetProgramId + "(" + targetBatch + ")"; // fallback
        }
        
        // Save to nested collection (for Admin)
        db.collection("Students").document(docPath).collection("Student IDs").document(finalStudentId).set(studentMap);
        
        // Save to flat collection (for Login)
        db.collection("AllStudents").document(finalStudentId).set(studentMap)
                .addOnSuccessListener(aVoid -> {
                    ActivityLogger.logAction(this, "Added Single Student", "ID: " + finalStudentId + ", Batch: " + targetBatch);
                    Toast.makeText(this, "Student Added", Toast.LENGTH_SHORT).show();
                    fetchGlobalStudents();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed Add: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveStudentToFirebaseSync(String studentId, String fullName, String email, String rawPassword, String status, String targetBatch) {
        final String finalStudentId = studentId.toUpperCase();
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        // Resolve programId from targetBatch mapping
        String targetProgramId = batchToDegreeMap.get(targetBatch);
        if (targetProgramId == null) {
            targetProgramId = programId != null ? programId : "Unknown";
        }

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("studentId", finalStudentId);
        studentMap.put("fullName", fullName);
        studentMap.put("email", email);
        studentMap.put("status", status);
        studentMap.put("batchId", targetBatch);
        studentMap.put("programId", targetProgramId);
        studentMap.put("hashed_password", hashedPassword);
        studentMap.put("isFirstLogin", true);

        String docPath = batchIdToDocPathMap.get(targetBatch);
        if (docPath == null) {
            docPath = targetProgramId + "(" + targetBatch + ")";
        }
        db.collection("Students").document(docPath).collection("Student IDs").document(finalStudentId).set(studentMap);
        db.collection("AllStudents").document(finalStudentId).set(studentMap);
    }
}

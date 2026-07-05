package com.example.finalyearprojectnew;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageDegreesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerDegrees;
    private LinearLayout llDegreeDetails;
    private TextView tvSelectedDegreeId, tvSelectedDegreeName, tvSelectedDuration;
    private AppCompatButton btnToggleAddProgram, btnSaveProgram, btnEditProgram, btnDeleteProgram, btnLaunchWizard;
    private CardView cardAddProgramForm;
    private TextView tvCancelAddProgram, tvFormTitle;
    private EditText etProgramId, etProgramName, etDuration;

    // Mock data for degrees
    private List<Degree> degreeList;
    private Degree currentlySelectedDegree = null;
    private boolean isUpdateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_degrees);

        initViews();
        loadDegrees();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerDegrees = findViewById(R.id.spinnerDegrees);
        llDegreeDetails = findViewById(R.id.llDegreeDetails);
        tvSelectedDegreeId = findViewById(R.id.tvSelectedDegreeId);
        tvSelectedDegreeName = findViewById(R.id.tvSelectedDegreeName);
        tvSelectedDuration = findViewById(R.id.tvSelectedDuration);

        btnToggleAddProgram = findViewById(R.id.btnToggleAddProgram);
        btnLaunchWizard = findViewById(R.id.btnLaunchWizard);
        btnSaveProgram = findViewById(R.id.btnSaveProgram);
        btnEditProgram = findViewById(R.id.btnEditProgram);
        btnDeleteProgram = findViewById(R.id.btnDeleteProgram);
        cardAddProgramForm = findViewById(R.id.cardAddProgramForm);
        tvCancelAddProgram = findViewById(R.id.tvCancelAddProgram);
        tvFormTitle = findViewById(R.id.tvFormTitle);

        etProgramId = findViewById(R.id.etProgramId);
        etProgramName = findViewById(R.id.etProgramName);
        etDuration = findViewById(R.id.etDuration);
    }

    private void loadDegrees() {
        degreeList = new ArrayList<>();
        
        List<String> degreeNames = new ArrayList<>();
        degreeNames.add("Select a Degree...");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDegrees.setAdapter(adapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Degrees").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    degreeList.clear();
                    degreeNames.clear();
                    degreeNames.add("Select a Degree...");
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getString("id");
                        String name = doc.getString("name");
                        String duration = doc.getString("duration");
                        if (id != null && name != null) {
                            degreeList.add(new Degree(id, name, duration != null ? duration : "N/A"));
                            degreeNames.add(name);
                        }
                    }
                    
                    ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(ManageDegreesActivity.this, 
                            R.layout.spinner_item, degreeNames);
                    updatedAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinnerDegrees.setAdapter(updatedAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load degrees", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerDegrees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentlySelectedDegree = degreeList.get(position - 1);
                    llDegreeDetails.setVisibility(View.VISIBLE);
                    tvSelectedDegreeId.setText("ID: " + currentlySelectedDegree.getId());
                    tvSelectedDegreeName.setText("Name: " + currentlySelectedDegree.getName());
                    tvSelectedDuration.setText("Duration: " + currentlySelectedDegree.getDuration());
                } else {
                    currentlySelectedDegree = null;
                    llDegreeDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                llDegreeDetails.setVisibility(View.GONE);
            }
        });

        btnLaunchWizard.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ManageDegreesActivity.this, ProgrammeSetupWizardActivity.class);
            startActivity(intent);
        });

        btnToggleAddProgram.setOnClickListener(v -> {
            isUpdateMode = false;
            tvFormTitle.setText("New Program Details");
            etProgramId.setText("");
            etProgramId.setEnabled(true);
            etProgramName.setText("");
            etDuration.setText("");
            btnSaveProgram.setText("Save Program");

            cardAddProgramForm.setVisibility(View.VISIBLE);
            btnToggleAddProgram.setVisibility(View.GONE);
            btnLaunchWizard.setVisibility(View.GONE);
        });

        btnEditProgram.setOnClickListener(v -> {
            if (currentlySelectedDegree != null) {
                isUpdateMode = true;
                tvFormTitle.setText("Update Program Details");
                etProgramId.setText(currentlySelectedDegree.getId());
                etProgramId.setEnabled(false);
                etProgramName.setText(currentlySelectedDegree.getName());
                etDuration.setText(currentlySelectedDegree.getDuration());
                btnSaveProgram.setText("Update Program");
                
                cardAddProgramForm.setVisibility(View.VISIBLE);
                btnToggleAddProgram.setVisibility(View.GONE);
                btnLaunchWizard.setVisibility(View.GONE);
            }
        });

        tvCancelAddProgram.setOnClickListener(v -> closeAddProgramForm());

        btnDeleteProgram.setOnClickListener(v -> {
            if (currentlySelectedDegree != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Program & Cascade")
                        .setMessage("Are you sure you want to delete this program? WARNING: This will permanently delete all associated batches, semesters, modules, and enrolled students.")
                        .setPositiveButton("Yes, Delete All", (dialog, which) -> {
                            performCascadedDelete(currentlySelectedDegree.getId());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnSaveProgram.setOnClickListener(v -> {
            String id = etProgramId.getText().toString().trim();
            String name = etProgramName.getText().toString().trim();
            String duration = etDuration.getText().toString().trim();

            if (id.isEmpty() || name.isEmpty() || duration.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Saving Program...", Toast.LENGTH_SHORT).show();
            btnSaveProgram.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> degreeData = new HashMap<>();
            degreeData.put("id", id);
            degreeData.put("name", name);
            degreeData.put("duration", duration);

            db.collection("Degrees").document(id)
                    .set(degreeData)
                    .addOnSuccessListener(aVoid -> {
                        ActivityLogger.logAction(this, isUpdateMode ? "Updated Degree" : "Added Degree", "ID: " + id + ", Name: " + name);
                        btnSaveProgram.setEnabled(true);
                        Toast.makeText(ManageDegreesActivity.this, isUpdateMode ? "Program updated successfully" : "Program saved successfully", Toast.LENGTH_SHORT).show();
                        
                        loadDegrees();
                        closeAddProgramForm();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveProgram.setEnabled(true);
                        Toast.makeText(ManageDegreesActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void closeAddProgramForm() {
        cardAddProgramForm.setVisibility(View.GONE);
        btnToggleAddProgram.setVisibility(View.VISIBLE);
        btnLaunchWizard.setVisibility(View.VISIBLE);
        etProgramId.setText("");
        etProgramId.setEnabled(true);
        etProgramName.setText("");
        etDuration.setText("");
        isUpdateMode = false;
        tvFormTitle.setText("New Program Details");
        btnSaveProgram.setText("Save Program");
    }

    private void performCascadedDelete(String degreeId) {
        android.widget.Toast.makeText(this, "Deleting degree and cascading related data...", android.widget.Toast.LENGTH_LONG).show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // We will collect all async queries first
        List<com.google.android.gms.tasks.Task<?>> queryTasks = new ArrayList<>();

        // 1. Fetch batches for this degree
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchBatches = 
                db.collection("Batches").whereEqualTo("programId", degreeId).get();
        queryTasks.add(fetchBatches);

        // 2. Fetch semesters under Degrees/{degreeId}/Semesters
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchSemestersDirect = 
                db.collection("Degrees").document(degreeId).collection("Semesters").get();
        queryTasks.add(fetchSemestersDirect);

        // 3. Fetch modules under Degrees/{degreeId}/Modules
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchModulesDirect = 
                db.collection("Degrees").document(degreeId).collection("Modules").get();
        queryTasks.add(fetchModulesDirect);

        // 4. Fetch all semesters from collectionGroups
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchSemestersCG = 
                db.collectionGroup("Semesters").get();
        queryTasks.add(fetchSemestersCG);

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchSemestersLegacy = 
                db.collectionGroup("Semester IDs").get();
        queryTasks.add(fetchSemestersLegacy);

        // 5. Fetch all modules from collectionGroups
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchModulesCG = 
                db.collectionGroup("Modules").get();
        queryTasks.add(fetchModulesCG);

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchModulesLegacy = 
                db.collectionGroup("Module IDs").get();
        queryTasks.add(fetchModulesLegacy);

        // 6. Fetch AllStudents
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> fetchAllStudents = 
                db.collection("AllStudents").get();
        queryTasks.add(fetchAllStudents);

        // Wait for all initial queries to complete
        com.google.android.gms.tasks.Tasks.whenAllComplete(queryTasks)
                .addOnSuccessListener(aVoid -> {
                    List<String> batchIds = new ArrayList<>();
                    List<String> batchDocPaths = new ArrayList<>();
                    
                    if (fetchBatches.isSuccessful() && fetchBatches.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchBatches.getResult()) {
                            String bId = doc.getString("batchId");
                            if (bId != null) batchIds.add(bId);
                            batchDocPaths.add(doc.getId());
                        }
                    }

                    List<com.google.android.gms.tasks.Task<Void>> deletionTasks = new ArrayList<>();
                    List<com.google.android.gms.tasks.Task<?>> studentFetchTasks = new ArrayList<>();

                    // A. Delete Batches
                    for (String docPath : batchDocPaths) {
                        deletionTasks.add(db.collection("Batches").document(docPath).delete());
                        deletionTasks.add(db.collection("Degrees").document(degreeId).collection("Batches").document(docPath).delete());
                    }

                    // B. Delete Semesters (direct)
                    if (fetchSemestersDirect.isSuccessful() && fetchSemestersDirect.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchSemestersDirect.getResult()) {
                            deletionTasks.add(doc.getReference().delete());
                        }
                    }

                    // C. Delete Semesters from collectionGroups (filter by degreeId or batchId)
                    if (fetchSemestersCG.isSuccessful() && fetchSemestersCG.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchSemestersCG.getResult()) {
                            String dId = doc.getString("degreeId");
                            String bId = doc.getString("batchId");
                            String[] pathParts = doc.getReference().getPath().split("/");
                            String parentDegId = pathParts.length > 1 ? pathParts[1] : "";
                            
                            if (degreeId.equalsIgnoreCase(dId) || degreeId.equalsIgnoreCase(parentDegId) || (bId != null && batchIds.contains(bId))) {
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    }

                    if (fetchSemestersLegacy.isSuccessful() && fetchSemestersLegacy.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchSemestersLegacy.getResult()) {
                            String dId = doc.getString("degreeId");
                            String bId = doc.getString("batchId");
                            String[] pathParts = doc.getReference().getPath().split("/");
                            String parentDegId = pathParts.length > 1 ? pathParts[1] : "";

                            if (degreeId.equalsIgnoreCase(dId) || degreeId.equalsIgnoreCase(parentDegId) || (bId != null && batchIds.contains(bId))) {
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    }

                    // D. Delete Modules (direct)
                    if (fetchModulesDirect.isSuccessful() && fetchModulesDirect.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchModulesDirect.getResult()) {
                            deletionTasks.add(doc.getReference().delete());
                        }
                    }

                    // E. Delete Modules from collectionGroups (filter by degreeId or batchId)
                    if (fetchModulesCG.isSuccessful() && fetchModulesCG.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchModulesCG.getResult()) {
                            String dId = doc.getString("degreeId");
                            String bId = doc.getString("batchId");
                            String[] pathParts = doc.getReference().getPath().split("/");
                            String parentDegId = pathParts.length > 1 ? pathParts[1] : "";

                            if (degreeId.equalsIgnoreCase(dId) || degreeId.equalsIgnoreCase(parentDegId) || (bId != null && batchIds.contains(bId))) {
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    }

                    if (fetchModulesLegacy.isSuccessful() && fetchModulesLegacy.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchModulesLegacy.getResult()) {
                            String dId = doc.getString("degreeId");
                            String bId = doc.getString("batchId");
                            String[] pathParts = doc.getReference().getPath().split("/");
                            String parentDegId = pathParts.length > 1 ? pathParts[1] : "";

                            if (degreeId.equalsIgnoreCase(dId) || degreeId.equalsIgnoreCase(parentDegId) || (bId != null && batchIds.contains(bId))) {
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    }

                    // F. Clean up Students in AllStudents
                    if (fetchAllStudents.isSuccessful() && fetchAllStudents.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : fetchAllStudents.getResult()) {
                            String bId = doc.getString("batchId");
                            if (bId != null && batchIds.contains(bId)) {
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    }

                    // G. Fetch students nested inside Students/{batchDocPath}/Student IDs
                    for (String docPath : batchDocPaths) {
                        studentFetchTasks.add(db.collection("Students").document(docPath).collection("Student IDs").get());
                    }

                    if (!studentFetchTasks.isEmpty()) {
                        com.google.android.gms.tasks.Tasks.whenAllComplete(studentFetchTasks)
                                .addOnSuccessListener(fetchResults -> {
                                    for (com.google.android.gms.tasks.Task<?> t : studentFetchTasks) {
                                        if (t.isSuccessful() && t.getResult() instanceof com.google.firebase.firestore.QuerySnapshot) {
                                            com.google.firebase.firestore.QuerySnapshot stuSnap = 
                                                    (com.google.firebase.firestore.QuerySnapshot) t.getResult();
                                            for (com.google.firebase.firestore.DocumentSnapshot stuDoc : stuSnap) {
                                                deletionTasks.add(stuDoc.getReference().delete());
                                            }
                                        }
                                    }

                                    // Delete parent Students documents
                                    for (String docPath : batchDocPaths) {
                                        deletionTasks.add(db.collection("Students").document(docPath).delete());
                                    }

                                    // Add the main Degree document deletion
                                    deletionTasks.add(db.collection("Degrees").document(degreeId).delete());
                                    deletionTasks.add(db.collection("Semesters").document(degreeId).delete());

                                    // Execute all deletions in parallel
                                    com.google.android.gms.tasks.Tasks.whenAllComplete(deletionTasks)
                                            .addOnCompleteListener(allDone -> {
                                                ActivityLogger.logAction(this, "Cascaded Deleted Degree", "Degree: " + degreeId);
                                                Toast.makeText(this, "Degree and all associated data deleted successfully!", Toast.LENGTH_LONG).show();
                                                loadDegrees();
                                                closeAddProgramForm();
                                            });
                                });
                    } else {
                        // No nested student collections to fetch, just delete degree and finalize
                        deletionTasks.add(db.collection("Degrees").document(degreeId).delete());
                        deletionTasks.add(db.collection("Semesters").document(degreeId).delete());

                        com.google.android.gms.tasks.Tasks.whenAllComplete(deletionTasks)
                                .addOnCompleteListener(allDone -> {
                                    ActivityLogger.logAction(this, "Cascaded Deleted Degree", "Degree: " + degreeId);
                                    Toast.makeText(this, "Degree deleted successfully!", Toast.LENGTH_SHORT).show();
                                    loadDegrees();
                                    closeAddProgramForm();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed during cascading delete queries: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Simple inner class to hold degree data
    private static class Degree {
        private String id;
        private String name;
        private String duration;

        public Degree(String id, String name, String duration) {
            this.id = id;
            this.name = name;
            this.duration = duration;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDuration() {
            return duration;
        }
    }
}

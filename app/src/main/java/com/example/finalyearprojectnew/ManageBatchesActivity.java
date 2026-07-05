package com.example.finalyearprojectnew;

import android.content.Intent;
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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageBatchesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerBatches;
    private Spinner spinnerDegreeFilter;
    private LinearLayout llBatchDetails;
    private TextView tvSelectedProgramId, tvSelectedBatchId, tvSelectedBatchName, tvSelectedIntakeYear;
    private AppCompatButton btnToggleAddBatch, btnSaveBatch, btnManageStudents, btnEditBatch, btnDeleteBatch;
    private CardView cardAddBatchForm;
    private TextView tvCancelAddBatch, tvFormTitle;

    // Batch Fields
    private EditText etProgramId, etBatchId, etBatchName, etIntakeYear;

    private FirebaseFirestore db;
    private List<Batch> batchList;
    private List<String> degreesList = new ArrayList<>();
    private List<Batch> allBatchesList = new ArrayList<>();
    private Batch currentlySelectedBatch = null;
    private boolean isUpdateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_batches);

        db = FirebaseFirestore.getInstance();
        initViews();
        fetchBatchesFromFirestore();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerBatches = findViewById(R.id.spinnerBatches);
        spinnerDegreeFilter = findViewById(R.id.spinnerDegreeFilter);
        llBatchDetails = findViewById(R.id.llBatchDetails);

        tvSelectedProgramId = findViewById(R.id.tvSelectedProgramId);
        tvSelectedBatchId = findViewById(R.id.tvSelectedBatchId);
        tvSelectedBatchName = findViewById(R.id.tvSelectedBatchName);
        tvSelectedIntakeYear = findViewById(R.id.tvSelectedIntakeYear);

        btnToggleAddBatch = findViewById(R.id.btnToggleAddBatch);
        btnSaveBatch = findViewById(R.id.btnSaveBatch);
        btnManageStudents = findViewById(R.id.btnManageStudents);
        btnEditBatch = findViewById(R.id.btnEditBatch);
        btnDeleteBatch = findViewById(R.id.btnDeleteBatch);
        cardAddBatchForm = findViewById(R.id.cardAddBatchForm);
        tvCancelAddBatch = findViewById(R.id.tvCancelAddBatch);
        tvFormTitle = findViewById(R.id.tvFormTitle);

        etProgramId = findViewById(R.id.etProgramId);
        etBatchId = findViewById(R.id.etBatchId);
        etBatchName = findViewById(R.id.etBatchName);
        etIntakeYear = findViewById(R.id.etIntakeYear);
    }

    private void fetchBatchesFromFirestore() {
        batchList = new ArrayList<>();
        allBatchesList = new ArrayList<>();
        
        // Fetch all degrees first
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
            db.collection("Batches").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    allBatchesList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String programId = document.getString("programId");
                        String batchId = document.getString("batchId");
                        String batchName = document.getString("batchName");
                        String intakeYear = document.getString("intakeYear");
                        
                        Batch b = new Batch(programId, batchId, batchName, intakeYear, document.getId());
                        allBatchesList.add(b);
                    }
                    
                    String selectedDegree = spinnerDegreeFilter.getSelectedItem() != null ? 
                            spinnerDegreeFilter.getSelectedItem().toString() : "All Degrees";
                    updateBatchSpinner(selectedDegree);
                } else {
                    Toast.makeText(ManageBatchesActivity.this, "Failed to load batches", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(ManageBatchesActivity.this, "Failed to load degrees", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateBatchSpinner(String selectedDegree) {
        String prevSelectedBatchId = currentlySelectedBatch != null ? currentlySelectedBatch.getBatchId() : null;

        batchList.clear();
        List<String> batchNames = new ArrayList<>();
        batchNames.add("Select a Batch...");

        for (Batch b : allBatchesList) {
            if (selectedDegree.equals("All Degrees") || (b.getProgramId() != null && b.getProgramId().equalsIgnoreCase(selectedDegree))) {
                batchList.add(b);
                batchNames.add(b.getBatchName() + " (" + b.getProgramId() + ")");
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageBatchesActivity.this, R.layout.spinner_item, batchNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatches.setAdapter(adapter);

        // Restore selected batch if it still exists in the filtered list
        if (prevSelectedBatchId != null) {
            for (int i = 0; i < batchList.size(); i++) {
                if (batchList.get(i).getBatchId().equalsIgnoreCase(prevSelectedBatchId)) {
                    spinnerBatches.setSelection(i + 1);
                    return;
                }
            }
        }
        
        // If not restored, reset selection to 0
        spinnerBatches.setSelection(0);
        currentlySelectedBatch = null;
        llBatchDetails.setVisibility(View.GONE);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerDegreeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDegree = spinnerDegreeFilter.getSelectedItem().toString();
                updateBatchSpinner(selectedDegree);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBatches.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentlySelectedBatch = batchList.get(position - 1);
                    llBatchDetails.setVisibility(View.VISIBLE);
                    tvSelectedProgramId.setText("Program ID: " + currentlySelectedBatch.getProgramId());
                    tvSelectedBatchId.setText("Batch ID: " + currentlySelectedBatch.getBatchId());
                    tvSelectedBatchName.setText("Name: " + currentlySelectedBatch.getBatchName());
                    tvSelectedIntakeYear.setText("Intake Year: " + currentlySelectedBatch.getIntakeYear());
                } else {
                    currentlySelectedBatch = null;
                    llBatchDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedBatch = null;
                llBatchDetails.setVisibility(View.GONE);
            }
        });

        btnManageStudents.setOnClickListener(v -> {
            if (currentlySelectedBatch != null) {
                Intent intent = new Intent(ManageBatchesActivity.this, ManageStudentsActivity.class);
                intent.putExtra("BATCH_ID", currentlySelectedBatch.getBatchId());
                intent.putExtra("PROGRAM_ID", currentlySelectedBatch.getProgramId());
                intent.putExtra("BATCH_NAME", currentlySelectedBatch.getBatchName());
                startActivity(intent);
            }
        });

        btnToggleAddBatch.setOnClickListener(v -> {
            isUpdateMode = false;
            tvFormTitle.setText("New Batch Details");
            etBatchId.setEnabled(true);
            
            // Pre-populate Programme ID if a specific degree filter is selected
            String selectedDegree = spinnerDegreeFilter.getSelectedItem() != null ? 
                    spinnerDegreeFilter.getSelectedItem().toString() : "All Degrees";
            if (!selectedDegree.equals("All Degrees")) {
                etProgramId.setText(selectedDegree);
            } else {
                etProgramId.setText("");
            }
            
            cardAddBatchForm.setVisibility(View.VISIBLE);
            btnToggleAddBatch.setVisibility(View.GONE);
        });

        btnEditBatch.setOnClickListener(v -> {
            if (currentlySelectedBatch != null) {
                isUpdateMode = true;
                tvFormTitle.setText("Update Batch Details");
                
                etProgramId.setText(currentlySelectedBatch.getProgramId());
                etBatchId.setText(currentlySelectedBatch.getBatchId());
                etBatchId.setEnabled(false); // disable ID edit
                etBatchName.setText(currentlySelectedBatch.getBatchName());
                etIntakeYear.setText(currentlySelectedBatch.getIntakeYear());
                
                btnSaveBatch.setText("Update Batch");
                
                cardAddBatchForm.setVisibility(View.VISIBLE);
                btnToggleAddBatch.setVisibility(View.GONE);
            }
        });

        tvCancelAddBatch.setOnClickListener(v -> closeAddBatchForm());

        btnDeleteBatch.setOnClickListener(v -> {
            if (currentlySelectedBatch != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Batch & Cascade")
                        .setMessage("Are you sure you want to delete this batch? WARNING: This will permanently delete all associated semesters, modules, and enrolled students for this batch.")
                        .setPositiveButton("Yes, Delete All", (dialog, which) -> {
                            performCascadedBatchDelete(currentlySelectedBatch);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnSaveBatch.setOnClickListener(v -> {
            String programId = etProgramId.getText().toString().trim();
            String batchId = etBatchId.getText().toString().trim();
            String batchName = etBatchName.getText().toString().trim();
            String intakeYear = etIntakeYear.getText().toString().trim();

            if (programId.isEmpty() || batchId.isEmpty() || batchName.isEmpty() || intakeYear.isEmpty()) {
                Toast.makeText(this, "Please fill all batch fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveBatch.setEnabled(false);
            btnSaveBatch.setText("Saving...");

            Map<String, Object> batchData = new HashMap<>();
            batchData.put("programId", programId);
            batchData.put("batchId", batchId);
            batchData.put("batchName", batchName);
            batchData.put("intakeYear", intakeYear);

            String batchDocPath = programId + "(" + batchName + ")";
            db.collection("Batches").document(batchDocPath).set(batchData)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("Degrees").document(programId).collection("Batches").document(batchDocPath).set(batchData)
                                .addOnSuccessListener(aVoid2 -> {
                                    ActivityLogger.logAction(this, isUpdateMode ? "Updated Batch" : "Added Batch", "ID: " + batchId + ", Name: " + batchName);
                                    Toast.makeText(ManageBatchesActivity.this, isUpdateMode ? "Batch updated successfully" : "Batch saved successfully", Toast.LENGTH_SHORT).show();
                                    closeAddBatchForm();
                                    fetchBatchesFromFirestore(); // Refresh list
                                    btnSaveBatch.setEnabled(true);
                                    btnSaveBatch.setText("Save Batch");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ManageBatchesActivity.this, "Failed to save to Degree Batches: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSaveBatch.setEnabled(true);
                                    btnSaveBatch.setText("Save Batch");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ManageBatchesActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSaveBatch.setEnabled(true);
                        btnSaveBatch.setText("Save Batch");
                    });
        });
    }

    private void closeAddBatchForm() {
        cardAddBatchForm.setVisibility(View.GONE);
        btnToggleAddBatch.setVisibility(View.VISIBLE);

        etProgramId.setText("");
        etBatchId.setText("");
        etBatchId.setEnabled(true);
        etBatchName.setText("");
        etIntakeYear.setText("");
        isUpdateMode = false;
        tvFormTitle.setText("New Batch Details");
        btnSaveBatch.setText("Save Batch");
    }

    private void performCascadedBatchDelete(Batch batch) {
        Toast.makeText(this, "Deleting batch and cascading related data...", Toast.LENGTH_LONG).show();
        String programId = batch.getProgramId();
        String batchId = batch.getBatchId();
        String batchName = batch.getBatchName();
        String batchDocPath = batch.getDocId(); // The actual document ID in Firestore

        // If for some reason docId is missing or equals batchId, fallback to consistent naming
        if (batchDocPath == null || batchDocPath.isEmpty() || batchDocPath.equals(batchId)) {
            batchDocPath = programId + "(" + batchName + ")";
        }

        final String finalBatchDocPath = batchDocPath;

        List<com.google.android.gms.tasks.Task<Void>> deletionTasks = new ArrayList<>();

        // 1. Delete batch documents
        deletionTasks.add(db.collection("Batches").document(finalBatchDocPath).delete());
        deletionTasks.add(db.collection("Degrees").document(programId).collection("Batches").document(finalBatchDocPath).delete());

        // 2. Fetch direct semesters under Degrees/{programId}/Semesters and check batchId or docId starts with batchDocPath
        db.collection("Degrees").document(programId).collection("Semesters").get()
                .addOnSuccessListener(semWebSnaps -> {
                    for (com.google.firebase.firestore.DocumentSnapshot semDoc : semWebSnaps) {
                        String bId = semDoc.getString("batchId");
                        if (batchId.equals(bId) || semDoc.getId().startsWith(finalBatchDocPath + "_")) {
                            deletionTasks.add(semDoc.getReference().delete());
                        }
                    }

                    // 3. Fetch direct modules under Degrees/{programId}/Modules and check batchId or docId starts with batchDocPath
                    db.collection("Degrees").document(programId).collection("Modules").get()
                            .addOnSuccessListener(modWebSnaps -> {
                                for (com.google.firebase.firestore.DocumentSnapshot modDoc : modWebSnaps) {
                                    String bId = modDoc.getString("batchId");
                                    if (batchId.equals(bId) || modDoc.getId().startsWith(finalBatchDocPath + "_")) {
                                        deletionTasks.add(modDoc.getReference().delete());
                                    }
                                }

                                // 4. Fetch collectionGroup "Semesters" and "Semester IDs"
                                db.collectionGroup("Semesters").get()
                                        .addOnSuccessListener(semCGSnaps -> {
                                            for (com.google.firebase.firestore.DocumentSnapshot semCGDoc : semCGSnaps) {
                                                String bId = semCGDoc.getString("batchId");
                                                if (batchId.equals(bId)) {
                                                    deletionTasks.add(semCGDoc.getReference().delete());
                                                }
                                            }

                                            db.collectionGroup("Semester IDs").get()
                                                    .addOnSuccessListener(semLegacySnaps -> {
                                                        for (com.google.firebase.firestore.DocumentSnapshot semLegDoc : semLegacySnaps) {
                                                            String bId = semLegDoc.getString("batchId");
                                                            if (batchId.equals(bId)) {
                                                                deletionTasks.add(semLegDoc.getReference().delete());
                                                            }
                                                        }

                                                        // 5. Fetch collectionGroup "Modules" and "Module IDs"
                                                        db.collectionGroup("Modules").get()
                                                                .addOnSuccessListener(modCGSnaps -> {
                                                                    for (com.google.firebase.firestore.DocumentSnapshot modCGDoc : modCGSnaps) {
                                                                        String bId = modCGDoc.getString("batchId");
                                                                        if (batchId.equals(bId)) {
                                                                            deletionTasks.add(modCGDoc.getReference().delete());
                                                                        }
                                                                    }

                                                                    db.collectionGroup("Module IDs").get()
                                                                            .addOnSuccessListener(modLegacySnaps -> {
                                                                                for (com.google.firebase.firestore.DocumentSnapshot modLegDoc : modLegacySnaps) {
                                                                                    String bId = modLegDoc.getString("batchId");
                                                                                    if (batchId.equals(bId)) {
                                                                                        deletionTasks.add(modLegDoc.getReference().delete());
                                                                                    }
                                                                                }

                                                                                // 6. Clean up students associated with this batch
                                                                                db.collection("Students").document(finalBatchDocPath).collection("Student IDs").get()
                                                                                        .addOnSuccessListener(stuSnaps -> {
                                                                                            for (com.google.firebase.firestore.DocumentSnapshot stuDoc : stuSnaps) {
                                                                                                String studentId = stuDoc.getId();
                                                                                                deletionTasks.add(db.collection("AllStudents").document(studentId).delete());
                                                                                                deletionTasks.add(stuDoc.getReference().delete());
                                                                                            }

                                                                                            // Delete parent Student document
                                                                                            deletionTasks.add(db.collection("Students").document(finalBatchDocPath).delete());

                                                                                            // Run all deletions
                                                                                            com.google.android.gms.tasks.Tasks.whenAllComplete(deletionTasks)
                                                                                                    .addOnCompleteListener(allDone -> {
                                                                                                        ActivityLogger.logAction(this, "Cascaded Deleted Batch", "Batch: " + batchId + " (" + batchName + ")");
                                                                                                        Toast.makeText(this, "Batch and all associated data deleted successfully!", Toast.LENGTH_LONG).show();
                                                                                                        fetchBatchesFromFirestore();
                                                                                                        closeAddBatchForm();
                                                                                                    });
                                                                                        });
                                                                            });
                                                                });
                                                    });
                                        });
                            });
                });
    }

    // Simple inner class to hold batch data
    public static class Batch {
        private String programId;
        private String batchId;
        private String batchName;
        private String intakeYear;
        private String docId;

        public Batch(String programId, String batchId, String batchName, String intakeYear, String docId) {
            this.programId = programId;
            this.batchId = batchId;
            this.batchName = batchName;
            this.intakeYear = intakeYear;
            this.docId = docId;
        }

        public String getProgramId() { return programId; }
        public String getBatchId() { return batchId; }
        public String getBatchName() { return batchName; }
        public String getIntakeYear() { return intakeYear; }
        public String getDocId() { return docId; }
    }
}

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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageBatchesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerBatches;
    private LinearLayout llBatchDetails;
    private TextView tvSelectedProgramId, tvSelectedBatchId, tvSelectedBatchName, tvSelectedIntakeYear;
    private AppCompatButton btnToggleAddBatch, btnSaveBatch, btnManageStudents, btnEditBatch, btnDeleteBatch;
    private CardView cardAddBatchForm;
    private TextView tvCancelAddBatch, tvFormTitle;

    // Batch Fields
    private EditText etProgramId, etBatchId, etBatchName, etIntakeYear;

    private FirebaseFirestore db;
    private List<Batch> batchList;
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
        db.collection("Batches").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                batchList.clear();
                List<String> batchNames = new ArrayList<>();
                batchNames.add("Select a Batch...");

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String programId = document.getString("programId");
                    String batchId = document.getString("batchId");
                    String batchName = document.getString("batchName");
                    String intakeYear = document.getString("intakeYear");
                    
                    Batch b = new Batch(programId, batchId, batchName, intakeYear);
                    batchList.add(b);
                    batchNames.add(b.getBatchName() + " (" + b.getProgramId() + ")");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageBatchesActivity.this, R.layout.spinner_item, batchNames);
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spinnerBatches.setAdapter(adapter);
            } else {
                Toast.makeText(ManageBatchesActivity.this, "Failed to load batches", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

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
                        .setTitle("Delete Batch")
                        .setMessage("Are you sure you want to delete this batch?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("Batches").document(currentlySelectedBatch.getBatchId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        ActivityLogger.logAction(this, "Deleted Batch", "ID: " + currentlySelectedBatch.getBatchId());
                                        Toast.makeText(this, "Batch deleted", Toast.LENGTH_SHORT).show();
                                        fetchBatchesFromFirestore();
                                        closeAddBatchForm();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
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

            db.collection("Batches").document(batchId).set(batchData)
                    .addOnSuccessListener(aVoid -> {
                        ActivityLogger.logAction(this, isUpdateMode ? "Updated Batch" : "Added Batch", "ID: " + batchId + ", Name: " + batchName);
                        Toast.makeText(ManageBatchesActivity.this, isUpdateMode ? "Batch updated successfully" : "Batch saved successfully", Toast.LENGTH_SHORT).show();
                        closeAddBatchForm();
                        fetchBatchesFromFirestore(); // Refresh list
                        btnSaveBatch.setEnabled(true);
                        btnSaveBatch.setText("Save Batch");
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

    // Simple inner class to hold batch data
    public static class Batch {
        private String programId;
        private String batchId;
        private String batchName;
        private String intakeYear;

        public Batch(String programId, String batchId, String batchName, String intakeYear) {
            this.programId = programId;
            this.batchId = batchId;
            this.batchName = batchName;
            this.intakeYear = intakeYear;
        }

        public String getProgramId() { return programId; }
        public String getBatchId() { return batchId; }
        public String getBatchName() { return batchName; }
        public String getIntakeYear() { return intakeYear; }
    }
}

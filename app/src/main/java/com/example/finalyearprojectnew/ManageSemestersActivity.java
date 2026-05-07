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

public class ManageSemestersActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerSemesters, spinnerDegreeFilter, spinnerBatchFilter, spinnerDegreeForm, spinnerBatchForm;
    private LinearLayout llSemesterDetails;
    private TextView tvSelectedDegreeId, tvSelectedBatchId, tvSelectedSemesterId, tvSelectedAcademicYear, tvSelectedSemesterNo;
    private AppCompatButton btnToggleAddSemester, btnSaveSemester, btnEditSemester, btnDeleteSemester;
    private CardView cardAddSemesterForm;
    private TextView tvCancelAddSemester, tvFormTitle;

    // Semester Fields
    private EditText etSemesterId, etAcademicYear, etSemesterNo;

    // Mock data for semesters
    private List<Semester> semesterList;
    private Semester currentlySelectedSemester = null;
    private boolean isUpdateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_semesters);

        initViews();
        loadInitialData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerSemesters = findViewById(R.id.spinnerSemesters);
        spinnerDegreeFilter = findViewById(R.id.spinnerDegreeFilter);
        spinnerBatchFilter = findViewById(R.id.spinnerBatchFilter);
        spinnerDegreeForm = findViewById(R.id.spinnerDegreeForm);
        spinnerBatchForm = findViewById(R.id.spinnerBatchForm);
        llSemesterDetails = findViewById(R.id.llSemesterDetails);

        tvSelectedDegreeId = findViewById(R.id.tvSelectedDegreeId); // Need to add this to layout or just use Batch
        tvSelectedBatchId = findViewById(R.id.tvSelectedBatchId);
        tvSelectedSemesterId = findViewById(R.id.tvSelectedSemesterId);
        tvSelectedAcademicYear = findViewById(R.id.tvSelectedAcademicYear);
        tvSelectedSemesterNo = findViewById(R.id.tvSelectedSemesterNo);

        btnToggleAddSemester = findViewById(R.id.btnToggleAddSemester);
        btnSaveSemester = findViewById(R.id.btnSaveSemester);
        btnEditSemester = findViewById(R.id.btnEditSemester);
        btnDeleteSemester = findViewById(R.id.btnDeleteSemester);
        cardAddSemesterForm = findViewById(R.id.cardAddSemesterForm);
        tvCancelAddSemester = findViewById(R.id.tvCancelAddSemester);
        tvFormTitle = findViewById(R.id.tvFormTitle);

        etSemesterId = findViewById(R.id.etSemesterId);
        etAcademicYear = findViewById(R.id.etAcademicYear);
        etSemesterNo = findViewById(R.id.etSemesterNo);
    }

    private List<String> degreeList = new ArrayList<>();
    private List<String> batchList = new ArrayList<>();

    private void loadInitialData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Load Degrees
        db.collection("Degrees").get().addOnSuccessListener(snapshots -> {
            degreeList.clear();
            degreeList.add("Select Degree");
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                degreeList.add(doc.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeList);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerDegreeFilter.setAdapter(adapter);
            spinnerDegreeForm.setAdapter(adapter);
        });

        // Load Batches
        db.collection("Batches").get().addOnSuccessListener(snapshots -> {
            batchList.clear();
            batchList.add("Select Batch");
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                batchList.add(doc.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchList);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerBatchFilter.setAdapter(adapter);
            spinnerBatchForm.setAdapter(adapter);
        });
    }

    private void loadSemesters() {
        String degree = spinnerDegreeFilter.getSelectedItem() != null ? spinnerDegreeFilter.getSelectedItem().toString() : "";
        String batch = spinnerBatchFilter.getSelectedItem() != null ? spinnerBatchFilter.getSelectedItem().toString() : "";

        if (degree.equals("Select Degree") || batch.equals("Select Batch")) {
            semesterList = new ArrayList<>();
            updateSemesterSpinner(new ArrayList<>());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Semesters").document(degree)
                .collection("Batches").document(batch)
                .collection("Semester IDs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    semesterList = new ArrayList<>();
                    List<String> semesterNames = new ArrayList<>();
                    semesterNames.add("Select a Semester...");
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String sId = doc.getString("semesterId");
                        String academicYear = doc.getString("academicYear");
                        String semesterNo = doc.getString("semesterNo");
                        if (sId != null && academicYear != null && semesterNo != null) {
                            semesterList.add(new Semester(degree, batch, sId, academicYear, semesterNo));
                            semesterNames.add(sId + " - " + semesterNo);
                        }
                    }
                    updateSemesterSpinner(semesterNames);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load semesters", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSemesterSpinner(List<String> names) {
        if (names.isEmpty()) {
            names.add("No semesters found");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, names);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSemesters.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSemesters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerDegreeFilter.setOnItemSelectedListener(filterListener);
        spinnerBatchFilter.setOnItemSelectedListener(filterListener);

        spinnerSemesters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && semesterList != null && position <= semesterList.size()) {
                    currentlySelectedSemester = semesterList.get(position - 1);
                    llSemesterDetails.setVisibility(View.VISIBLE);
                    if (tvSelectedDegreeId != null) tvSelectedDegreeId.setText("Degree: " + currentlySelectedSemester.getDegreeId());
                    tvSelectedBatchId.setText("Batch: " + currentlySelectedSemester.getBatchId());
                    tvSelectedSemesterId.setText("Semester ID: " + currentlySelectedSemester.getSemesterId());
                    tvSelectedAcademicYear.setText("Academic Year: " + currentlySelectedSemester.getAcademicYear());
                    tvSelectedSemesterNo.setText("Semester No: " + currentlySelectedSemester.getSemesterNo());
                } else {
                    currentlySelectedSemester = null;
                    llSemesterDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedSemester = null;
                llSemesterDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddSemester.setOnClickListener(v -> {
            isUpdateMode = false;
            tvFormTitle.setText("New Semester Details");
            spinnerDegreeForm.setEnabled(true);
            spinnerBatchForm.setEnabled(true);
            etSemesterId.setEnabled(true);
            
            cardAddSemesterForm.setVisibility(View.VISIBLE);
            btnToggleAddSemester.setVisibility(View.GONE);
        });

        btnEditSemester.setOnClickListener(v -> {
            if (currentlySelectedSemester != null) {
                isUpdateMode = true;
                tvFormTitle.setText("Update Semester Details");
                
                setSpinnerValue(spinnerDegreeForm, currentlySelectedSemester.getDegreeId());
                spinnerDegreeForm.setEnabled(false);
                setSpinnerValue(spinnerBatchForm, currentlySelectedSemester.getBatchId());
                spinnerBatchForm.setEnabled(false);
                
                etSemesterId.setText(currentlySelectedSemester.getSemesterId());
                etSemesterId.setEnabled(false);
                etAcademicYear.setText(currentlySelectedSemester.getAcademicYear());
                etSemesterNo.setText(currentlySelectedSemester.getSemesterNo());
                
                btnSaveSemester.setText("Update Semester");
                
                cardAddSemesterForm.setVisibility(View.VISIBLE);
                btnToggleAddSemester.setVisibility(View.GONE);
            }
        });
        tvCancelAddSemester.setOnClickListener(v -> closeAddSemesterForm());

        btnDeleteSemester.setOnClickListener(v -> {
            if (currentlySelectedSemester != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Semester")
                        .setMessage("Are you sure you want to delete this semester?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("Semesters").document(currentlySelectedSemester.getDegreeId())
                                    .collection("Batches").document(currentlySelectedSemester.getBatchId())
                                    .collection("Semester IDs").document(currentlySelectedSemester.getSemesterId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        ActivityLogger.logAction(this, "Deleted Semester", "Batch: " + currentlySelectedSemester.getBatchId() + ", ID: " + currentlySelectedSemester.getSemesterId());
                                        Toast.makeText(this, "Semester deleted", Toast.LENGTH_SHORT).show();
                                        loadSemesters();
                                        closeAddSemesterForm();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        btnSaveSemester.setOnClickListener(v -> {
            String degreeId = spinnerDegreeForm.getSelectedItem() != null ? spinnerDegreeForm.getSelectedItem().toString() : "";
            String batchId = spinnerBatchForm.getSelectedItem() != null ? spinnerBatchForm.getSelectedItem().toString() : "";
            String semesterId = etSemesterId.getText().toString().trim();
            String academicYear = etAcademicYear.getText().toString().trim();
            String semesterNo = etSemesterNo.getText().toString().trim();

            if (degreeId.equals("Select Degree") || batchId.equals("Select Batch") || semesterId.isEmpty() || academicYear.isEmpty() || semesterNo.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Saving Semester...", Toast.LENGTH_SHORT).show();
            btnSaveSemester.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> semesterData = new HashMap<>();
            semesterData.put("degreeId", degreeId);
            semesterData.put("batchId", batchId);
            semesterData.put("semesterId", semesterId);
            semesterData.put("academicYear", academicYear);
            semesterData.put("semesterNo", semesterNo);

            db.collection("Semesters").document(degreeId)
                    .collection("Batches").document(batchId)
                    .collection("Semester IDs").document(semesterId)
                    .set(semesterData)
                    .addOnSuccessListener(aVoid -> {
                        ActivityLogger.logAction(this, isUpdateMode ? "Updated Semester" : "Added Semester", "Batch: " + batchId + ", ID: " + semesterId);
                        btnSaveSemester.setEnabled(true);
                        Toast.makeText(ManageSemestersActivity.this, isUpdateMode ? "Semester updated successfully" : "Semester saved successfully", Toast.LENGTH_SHORT).show();
                        
                        loadSemesters();
                        closeAddSemesterForm();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveSemester.setEnabled(true);
                        Toast.makeText(ManageSemestersActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void closeAddSemesterForm() {
        cardAddSemesterForm.setVisibility(View.GONE);
        btnToggleAddSemester.setVisibility(View.VISIBLE);

        spinnerDegreeForm.setSelection(0);
        spinnerDegreeForm.setEnabled(true);
        spinnerBatchForm.setSelection(0);
        spinnerBatchForm.setEnabled(true);
        etSemesterId.setText("");
        etSemesterId.setEnabled(true);
        etAcademicYear.setText("");
        etSemesterNo.setText("");
        isUpdateMode = false;
        tvFormTitle.setText("New Semester Details");
        btnSaveSemester.setText("Save Semester");
    }

    // Simple inner class to hold semester data
    private static class Semester {
        private String degreeId;
        private String batchId;
        private String semesterId;
        private String academicYear;
        private String semesterNo;

        public Semester(String degreeId, String batchId, String semesterId, String academicYear, String semesterNo) {
            this.degreeId = degreeId;
            this.batchId = batchId;
            this.semesterId = semesterId;
            this.academicYear = academicYear;
            this.semesterNo = semesterNo;
        }

        public String getDegreeId() {
            return degreeId;
        }

        public String getBatchId() {
            return batchId;
        }

        public String getSemesterId() {
            return semesterId;
        }

        public String getAcademicYear() {
            return academicYear;
        }

        public String getSemesterNo() {
            return semesterNo;
        }
    }
}

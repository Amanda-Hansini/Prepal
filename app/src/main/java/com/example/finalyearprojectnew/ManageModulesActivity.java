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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class ManageModulesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerModules, spinnerDegreeFilter, spinnerBatchFilter, spinnerDegreeForm, spinnerBatchForm, spinnerSemesterForm;
    private LinearLayout llModuleDetails;
    private TextView tvSelectedBatchId, tvSelectedDegreeId, tvSelectedSemesterId, tvSelectedModuleId, tvSelectedModuleName, tvSelectedCredits, tvSelectModuleLabel;
    private AppCompatButton btnToggleAddModule, btnUploadCsvModules, btnSaveModule, btnEditModule, btnDeleteModule;
    private CardView cardAddModuleForm;
    private TextView tvCancelAddModule, tvFormTitle;

    // Module Fields
    private EditText etModuleId, etModuleName, etCredits;

    private List<Module> moduleList; // currently filtered list
    private List<Module> allModulesList; // master list
    private Module currentlySelectedModule = null;
    private boolean isUpdateMode = false;
    private static final int CSV_REQUEST_CODE = 1002;

    private List<String> degreeList = new ArrayList<>();
    private List<String> batchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_modules);

        initViews();
        loadInitialData();
        loadModules();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerModules = findViewById(R.id.spinnerModules);
        spinnerDegreeFilter = findViewById(R.id.spinnerDegreeFilter);
        spinnerBatchFilter = findViewById(R.id.spinnerBatchFilter);
        
        spinnerDegreeForm = findViewById(R.id.spinnerDegreeForm);
        spinnerBatchForm = findViewById(R.id.spinnerBatchForm);
        spinnerSemesterForm = findViewById(R.id.spinnerSemesterForm);
        
        llModuleDetails = findViewById(R.id.llModuleDetails);

        tvSelectedBatchId = findViewById(R.id.tvSelectedBatchId);
        tvSelectedDegreeId = findViewById(R.id.tvSelectedDegreeId);
        tvSelectedSemesterId = findViewById(R.id.tvSelectedSemesterId);
        tvSelectedModuleId = findViewById(R.id.tvSelectedModuleId);
        tvSelectedModuleName = findViewById(R.id.tvSelectedModuleName);
        tvSelectedCredits = findViewById(R.id.tvSelectedCredits);

        btnToggleAddModule = findViewById(R.id.btnToggleAddModule);
        btnUploadCsvModules = findViewById(R.id.btnUploadCsvModules);
        btnSaveModule = findViewById(R.id.btnSaveModule);
        btnEditModule = findViewById(R.id.btnEditModule);
        btnDeleteModule = findViewById(R.id.btnDeleteModule);
        cardAddModuleForm = findViewById(R.id.cardAddModuleForm);
        tvCancelAddModule = findViewById(R.id.tvCancelAddModule);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvSelectModuleLabel = findViewById(R.id.tvSelectModuleLabel);

        etModuleId = findViewById(R.id.etModuleId);
        etModuleName = findViewById(R.id.etModuleName);
        etCredits = findViewById(R.id.etCredits);
    }

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

    private void loadSemestersForForm() {
        String degree = spinnerDegreeForm.getSelectedItem() != null ? spinnerDegreeForm.getSelectedItem().toString() : "";
        String batch = spinnerBatchForm.getSelectedItem() != null ? spinnerBatchForm.getSelectedItem().toString() : "";

        if (degree.equals("Select Degree") || batch.equals("Select Batch")) {
            updateSemesterFormSpinner(new ArrayList<>());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Semesters").document(degree)
                .collection("Batches").document(batch)
                .collection("Semester IDs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> semesters = new ArrayList<>();
                    semesters.add("Select Semester");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        semesters.add(doc.getId());
                    }
                    updateSemesterFormSpinner(semesters);
                });
    }

    private void updateSemesterFormSpinner(List<String> semesters) {
        if (semesters.isEmpty()) {
            semesters.add("No semesters found");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, semesters);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSemesterForm.setAdapter(adapter);
    }

    private void loadModules() {
        allModulesList = new ArrayList<>();
        moduleList = new ArrayList<>();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collectionGroup("Module IDs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allModulesList.clear();
                    java.util.Set<String> degrees = new java.util.TreeSet<>();
                    java.util.Set<String> batches = new java.util.TreeSet<>();
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bId = doc.getString("batchId");
                        String dId = doc.getString("degreeId");
                        String sId = doc.getString("semesterId");
                        String mId = doc.getString("moduleId");
                        String mName = doc.getString("moduleName");
                        String creds = doc.getString("credits");
                        
                        if (mId != null && mName != null) {
                            Module m = new Module(
                                bId != null ? bId : "N/A",
                                dId != null ? dId : "N/A",
                                sId != null ? sId : "N/A",
                                mId, mName,
                                creds != null ? creds : "0"
                            );
                            allModulesList.add(m);
                            if (dId != null && !dId.trim().isEmpty()) degrees.add(dId.trim());
                            if (bId != null && !bId.trim().isEmpty()) batches.add(bId.trim());
                        }
                    }
                    
                    setupFilterSpinners(degrees, batches);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load modules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupFilterSpinners(java.util.Set<String> degrees, java.util.Set<String> batches) {
        List<String> degreeFilterList = new ArrayList<>();
        degreeFilterList.add("All Degrees");
        degreeFilterList.addAll(degrees);
        
        ArrayAdapter<String> degAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeFilterList);
        degAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDegreeFilter.setAdapter(degAdapter);
        
        List<String> batchFilterList = new ArrayList<>();
        batchFilterList.add("All Batches");
        batchFilterList.addAll(batches);
        
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchFilterList);
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatchFilter.setAdapter(batchAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        
        spinnerDegreeFilter.setOnItemSelectedListener(filterListener);
        spinnerBatchFilter.setOnItemSelectedListener(filterListener);
    }

    private void applyFilters() {
        if (allModulesList == null) return;

        Object degObj = spinnerDegreeFilter.getSelectedItem();
        Object batchObj = spinnerBatchFilter.getSelectedItem();
        
        String selectedDeg = degObj != null ? degObj.toString() : "All Degrees";
        String selectedBatch = batchObj != null ? batchObj.toString() : "All Batches";
        
        moduleList.clear();
        List<String> moduleNames = new ArrayList<>();
        moduleNames.add("Select a Module...");
        
        for (Module m : allModulesList) {
            boolean degMatch = selectedDeg.equals("All Degrees") || (m.getDegreeId() != null && m.getDegreeId().equalsIgnoreCase(selectedDeg));
            boolean batchMatch = selectedBatch.equals("All Batches") || (m.getBatchId() != null && m.getBatchId().equalsIgnoreCase(selectedBatch));
            
            if (degMatch && batchMatch) {
                moduleList.add(m);
                moduleNames.add(m.getModuleId() + " - " + m.getModuleName());
            }
        }
        
        ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, moduleNames);
        updatedAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerModules.setAdapter(updatedAdapter);
        
        // Update title to show how many modules found
        if (tvSelectModuleLabel != null) {
            tvSelectModuleLabel.setText("Select Module (" + (moduleNames.size() - 1) + " found)");
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        AdapterView.OnItemSelectedListener formFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSemestersForForm();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerDegreeForm.setOnItemSelectedListener(formFilterListener);
        spinnerBatchForm.setOnItemSelectedListener(formFilterListener);

        spinnerModules.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentlySelectedModule = moduleList.get(position - 1);
                    llModuleDetails.setVisibility(View.VISIBLE);
                    tvSelectedBatchId.setText("Batch ID: " + currentlySelectedModule.getBatchId());
                    tvSelectedDegreeId.setText("Degree ID: " + currentlySelectedModule.getDegreeId());
                    tvSelectedSemesterId.setText("Semester ID: " + currentlySelectedModule.getSemesterId());
                    tvSelectedModuleId.setText("Module ID: " + currentlySelectedModule.getModuleId());
                    tvSelectedModuleName.setText("Module Name: " + currentlySelectedModule.getModuleName());
                    tvSelectedCredits.setText("Credits: " + currentlySelectedModule.getCredits());
                } else {
                    currentlySelectedModule = null;
                    llModuleDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentlySelectedModule = null;
                llModuleDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddModule.setOnClickListener(v -> {
            isUpdateMode = false;
            tvFormTitle.setText("New Module Details");
            spinnerDegreeForm.setEnabled(true);
            spinnerBatchForm.setEnabled(true);
            spinnerSemesterForm.setEnabled(true);
            etModuleId.setEnabled(true);
            
            cardAddModuleForm.setVisibility(View.VISIBLE);
            btnToggleAddModule.setVisibility(View.GONE);
        });

        btnEditModule.setOnClickListener(v -> {
            if (currentlySelectedModule != null) {
                isUpdateMode = true;
                tvFormTitle.setText("Update Module Details");
                
                setSpinnerValue(spinnerDegreeForm, currentlySelectedModule.getDegreeId());
                spinnerDegreeForm.setEnabled(false); // disable ID edit
                setSpinnerValue(spinnerBatchForm, currentlySelectedModule.getBatchId());
                spinnerBatchForm.setEnabled(false); // disable ID edit
                
                // Need to load semesters for this degree/batch before setting the value
                loadSemestersForFormWithCallback(currentlySelectedModule.getDegreeId(), currentlySelectedModule.getBatchId(), () -> {
                    setSpinnerValue(spinnerSemesterForm, currentlySelectedModule.getSemesterId());
                    spinnerSemesterForm.setEnabled(false); // disable ID edit
                });

                etModuleId.setText(currentlySelectedModule.getModuleId());
                etModuleId.setEnabled(false); // disable ID edit
                
                etModuleName.setText(currentlySelectedModule.getModuleName());
                etCredits.setText(currentlySelectedModule.getCredits());
                
                btnSaveModule.setText("Update Module");
                
                cardAddModuleForm.setVisibility(View.VISIBLE);
                btnToggleAddModule.setVisibility(View.GONE);
            }
        });

        tvCancelAddModule.setOnClickListener(v -> closeAddModuleForm());

        btnDeleteModule.setOnClickListener(v -> {
            if (currentlySelectedModule != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Delete Module")
                        .setMessage("Are you sure you want to delete this module?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String parentDocId = currentlySelectedModule.getBatchId() + "_" + currentlySelectedModule.getDegreeId() + "_" + currentlySelectedModule.getSemesterId();
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("Modules").document(parentDocId)
                                    .collection("Module IDs").document(currentlySelectedModule.getModuleId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        ActivityLogger.logAction(this, "Deleted Module", "ID: " + currentlySelectedModule.getModuleId() + ", Batch: " + currentlySelectedModule.getBatchId());
                                        Toast.makeText(this, "Module deleted", Toast.LENGTH_SHORT).show();
                                        loadModules();
                                        closeAddModuleForm();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        btnUploadCsvModules.setOnClickListener(v -> openCsvPicker());

        btnSaveModule.setOnClickListener(v -> {
            String degreeId = spinnerDegreeForm.getSelectedItem() != null ? spinnerDegreeForm.getSelectedItem().toString() : "";
            String batchId = spinnerBatchForm.getSelectedItem() != null ? spinnerBatchForm.getSelectedItem().toString() : "";
            String semesterId = spinnerSemesterForm.getSelectedItem() != null ? spinnerSemesterForm.getSelectedItem().toString() : "";
            String moduleId = etModuleId.getText().toString().trim();
            String moduleName = etModuleName.getText().toString().trim();
            String credits = etCredits.getText().toString().trim();

            if (degreeId.equals("Select Degree") || batchId.equals("Select Batch") || semesterId.equals("Select Semester") || moduleId.isEmpty() || moduleName.isEmpty() || credits.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Saving Module...", Toast.LENGTH_SHORT).show();
            btnSaveModule.setEnabled(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> moduleData = new HashMap<>();
            moduleData.put("batchId", batchId);
            moduleData.put("degreeId", degreeId);
            moduleData.put("semesterId", semesterId);
            moduleData.put("moduleId", moduleId);
            moduleData.put("moduleName", moduleName);
            moduleData.put("credits", credits);

            String parentDocId = batchId + "_" + degreeId + "_" + semesterId;

            db.collection("Modules").document(parentDocId)
                    .collection("Module IDs").document(moduleId)
                    .set(moduleData)
                    .addOnSuccessListener(aVoid -> {
                        ActivityLogger.logAction(this, isUpdateMode ? "Updated Module" : "Added Module", "ID: " + moduleId + ", Name: " + moduleName);
                        btnSaveModule.setEnabled(true);
                        Toast.makeText(ManageModulesActivity.this, isUpdateMode ? "Module updated successfully" : "Module saved successfully", Toast.LENGTH_SHORT).show();
                        
                        loadModules();
                        closeAddModuleForm();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveModule.setEnabled(true);
                        Toast.makeText(ManageModulesActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadSemestersForFormWithCallback(String degree, String batch, Runnable callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Semesters").document(degree)
                .collection("Batches").document(batch)
                .collection("Semester IDs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> semesters = new ArrayList<>();
                    semesters.add("Select Semester");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        semesters.add(doc.getId());
                    }
                    updateSemesterFormSpinner(semesters);
                    callback.run();
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
        Toast.makeText(this, "Uploading CSV...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                
                int count = 0;
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                
                String currentSemesterId = "Unknown Semester";
                
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // handle commas in quotes
                    for (int i=0; i<tokens.length; i++) {
                        tokens[i] = tokens[i].replaceAll("^\"|\"$", "").trim();
                    }
                    
                    // Check for semester header
                    boolean isHeader = false;
                    for (String token : tokens) {
                        if (token.toLowerCase().contains("year") && token.toLowerCase().contains("semester")) {
                            currentSemesterId = token;
                            isHeader = true;
                            break;
                        }
                    }
                    if (isHeader) continue;
                    
                    if (tokens.length >= 3) {
                        String batchId = tokens[0].trim();
                        String moduleId = tokens[1].trim(); // Course Code
                        String moduleName = tokens[2].trim(); // Course Title
                        
                        if (batchId.isEmpty() || moduleId.isEmpty() || moduleName.isEmpty() || moduleId.equalsIgnoreCase("Course Code")) {
                            continue;
                        }
                        
                        if (moduleId.toLowerCase().contains("total") || moduleName.toLowerCase().contains("total credits")) {
                            continue;
                        }
                        
                        String credits = "0";
                        if (tokens.length >= 5 && !tokens[4].trim().isEmpty()) {
                            credits = tokens[4].trim();
                        }
                        
                        String degreeId = moduleId.split("\\s+")[0];
                        if (degreeId.isEmpty()) degreeId = "Unknown";

                        Map<String, Object> moduleData = new HashMap<>();
                        moduleData.put("batchId", batchId);
                        moduleData.put("degreeId", degreeId);
                        moduleData.put("semesterId", currentSemesterId);
                        moduleData.put("moduleId", moduleId);
                        moduleData.put("moduleName", moduleName);
                        moduleData.put("credits", credits);

                        db.collection("Modules").document(batchId + "_" + degreeId + "_" + currentSemesterId)
                                .collection("Module IDs").document(moduleId).set(moduleData);
                        count++;
                    }
                }
                
                int finalCount = count;
                runOnUiThread(() -> {
                    ActivityLogger.logAction(this, "Uploaded Modules CSV", "Count: " + finalCount);
                    Toast.makeText(this, "Uploaded " + finalCount + " modules", Toast.LENGTH_SHORT).show();
                    loadModules(); // refresh list
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error parsing CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void closeAddModuleForm() {
        cardAddModuleForm.setVisibility(View.GONE);
        btnToggleAddModule.setVisibility(View.VISIBLE);

        spinnerDegreeForm.setSelection(0);
        spinnerDegreeForm.setEnabled(true);
        spinnerBatchForm.setSelection(0);
        spinnerBatchForm.setEnabled(true);
        spinnerSemesterForm.setSelection(0);
        spinnerSemesterForm.setEnabled(true);
        
        etModuleId.setText("");
        etModuleId.setEnabled(true);
        etModuleName.setText("");
        etCredits.setText("");
        
        isUpdateMode = false;
        tvFormTitle.setText("New Module Details");
        btnSaveModule.setText("Save Module");
    }

    // Simple inner class to hold module data
    private static class Module {
        private String batchId;
        private String degreeId;
        private String semesterId;
        private String moduleId;
        private String moduleName;
        private String credits;

        public Module(String batchId, String degreeId, String semesterId, String moduleId, String moduleName, String credits) {
            this.batchId = batchId;
            this.degreeId = degreeId;
            this.semesterId = semesterId;
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.credits = credits;
        }

        public String getBatchId() {
            return batchId;
        }

        public String getDegreeId() {
            return degreeId;
        }

        public String getSemesterId() {
            return semesterId;
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getCredits() {
            return credits;
        }
    }
}

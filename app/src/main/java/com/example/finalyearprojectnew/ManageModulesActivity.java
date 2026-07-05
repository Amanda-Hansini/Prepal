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
    private Spinner spinnerModules, spinnerDegreeFilter, spinnerBatchFilter, spinnerSemesterFilter, spinnerDegreeForm, spinnerBatchForm, spinnerSemesterForm;
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
    private List<String> allBatchesList = new ArrayList<>();
    private Map<String, String> batchToDegreeMap = new HashMap<>();
    private Map<String, String> batchDocPathToBatchIdMap = new HashMap<>();
    private List<String> allModuleBatchesList = new ArrayList<>();
    private List<String> currentBatchFilterList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_modules);

        initViews();
        loadInitialData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerModules = findViewById(R.id.spinnerModules);
        spinnerDegreeFilter = findViewById(R.id.spinnerDegreeFilter);
        spinnerBatchFilter = findViewById(R.id.spinnerBatchFilter);
        spinnerSemesterFilter = findViewById(R.id.spinnerSemesterFilter);
        
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
        
        // Step 1: Load Degrees
        db.collection("Degrees").get().addOnSuccessListener(snapshots -> {
            degreeList.clear();
            degreeList.add("Select Degree");
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                degreeList.add(doc.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeList);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerDegreeForm.setAdapter(adapter);

            // Step 2: Load Batches inside Degrees success listener
            db.collection("Batches").get().addOnSuccessListener(batchSnaps -> {
                batchList.clear();
                batchList.add("Select Batch");
                allBatchesList.clear();
                batchToDegreeMap.clear();
                batchDocPathToBatchIdMap.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : batchSnaps) {
                    String bId = doc.getId();
                    String realBatchId = doc.getString("batchId");
                    String pId = doc.getString("programId");
                    batchList.add(bId);
                    allBatchesList.add(bId);
                    if (pId != null) {
                        batchToDegreeMap.put(bId, pId);
                    }
                    if (realBatchId != null) {
                        batchDocPathToBatchIdMap.put(bId, realBatchId);
                    }
                }
                ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchList);
                batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spinnerBatchForm.setAdapter(batchAdapter);

                // Step 3: Now load modules and setup listeners sequentially!
                loadModules();
                setupListeners();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load batches metadata", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load degrees metadata", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSemestersForForm() {
        String degree = spinnerDegreeForm.getSelectedItem() != null ? spinnerDegreeForm.getSelectedItem().toString() : "";
        String batch = spinnerBatchForm.getSelectedItem() != null ? spinnerBatchForm.getSelectedItem().toString() : "";

        if (degree.equals("Select Degree") || batch.equals("Select Batch")) {
            updateSemesterFormSpinner(new ArrayList<>());
            return;
        }

        String realBatchId = batchDocPathToBatchIdMap.get(batch);
        if (realBatchId == null) {
            realBatchId = batch;
        }

        final String finalRealBatchId = realBatchId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Degrees").document(degree)
                .collection("Semesters").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> semesters = new ArrayList<>();
                    semesters.add("Select Semester");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bId = doc.getString("batchId");
                        String sId = doc.getString("semesterId");
                        if (bId != null && sId != null) {
                            if (bId.equalsIgnoreCase(finalRealBatchId) || bId.equalsIgnoreCase(batch)) {
                                semesters.add(sId);
                            }
                        }
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
        
        // Fetch from Location A (Web style): subcollections named "Modules"
        db.collectionGroup("Modules").get()
                .addOnSuccessListener(webSnaps -> {
                    List<Module> tempCombined = new ArrayList<>();
                    
                    // Process Location A modules
                    for (com.google.firebase.firestore.DocumentSnapshot doc : webSnaps) {
                        String bId = doc.getString("batchId");
                        String dId = doc.getString("degreeId");
                        String sId = doc.getString("semesterId");
                        String mId = doc.getString("moduleId");
                        if (mId == null) {
                            mId = doc.getString("moduleCode");
                        }
                        String mName = doc.getString("moduleName");
                        Object credsObj = doc.get("credits");
                        String creds = credsObj != null ? String.valueOf(credsObj) : "0";
                        
                        if (mId != null && mName != null) {
                            Module m = new Module(
                                bId != null ? bId : "N/A",
                                dId != null ? dId : "N/A",
                                sId != null ? sId : "N/A",
                                mId, mName, creds
                            );
                            tempCombined.add(m);
                        }
                    }
                    
                    // Now fetch from Location B (Legacy/Mobile style): subcollections named "Module IDs"
                    db.collectionGroup("Module IDs").get()
                            .addOnSuccessListener(mobSnaps -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : mobSnaps) {
                                    String bId = doc.getString("batchId");
                                    String dId = doc.getString("degreeId");
                                    String sId = doc.getString("semesterId");
                                    String mId = doc.getString("moduleId");
                                    if (mId == null) {
                                        mId = doc.getString("moduleCode");
                                    }
                                    String mName = doc.getString("moduleName");
                                    Object credsObj = doc.get("credits");
                                    String creds = credsObj != null ? String.valueOf(credsObj) : "0";
                                    
                                    if (mId != null && mName != null) {
                                        Module m = new Module(
                                            bId != null ? bId : "N/A",
                                            dId != null ? dId : "N/A",
                                            sId != null ? sId : "N/A",
                                            mId, mName, creds
                                        );
                                        
                                        // Avoid duplicate entries
                                        boolean duplicate = false;
                                        for (Module existing : tempCombined) {
                                            if (existing.getModuleId().equalsIgnoreCase(m.getModuleId()) &&
                                                existing.getDegreeId().equalsIgnoreCase(m.getDegreeId()) &&
                                                existing.getBatchId().equalsIgnoreCase(m.getBatchId()) &&
                                                existing.getSemesterId().equalsIgnoreCase(m.getSemesterId())) {
                                                duplicate = true;
                                                break;
                                            }
                                        }
                                        if (!duplicate) {
                                            tempCombined.add(m);
                                        }
                                    }
                                }
                                
                                allModulesList.clear();
                                allModulesList.addAll(tempCombined);
                                
                                java.util.Set<String> degrees = new java.util.TreeSet<>();
                                java.util.Set<String> batches = new java.util.TreeSet<>();
                                for (Module m : allModulesList) {
                                    if (m.getDegreeId() != null && !m.getDegreeId().trim().isEmpty()) {
                                        degrees.add(m.getDegreeId().trim());
                                    }
                                    if (m.getBatchId() != null && !m.getBatchId().trim().isEmpty()) {
                                        batches.add(m.getBatchId().trim());
                                    }
                                }
                                setupFilterSpinners(degrees, batches);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load legacy modules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load modules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBatchFilterDropdown(String selectedDegree) {
        String currentSelectedBatch = spinnerBatchFilter.getSelectedItem() != null ? 
                spinnerBatchFilter.getSelectedItem().toString() : "All Batches";

        currentBatchFilterList.clear();
        currentBatchFilterList.add("All Batches");

        for (String bId : allModuleBatchesList) {
            // Find corresponding degree ID for bId (which can be a display batch ID or a short batch ID)
            String resolvedDegreeId = null;
            
            // 1. Try directly (if it is a display batch ID)
            resolvedDegreeId = batchToDegreeMap.get(bId);
            
            // 2. If null, try to resolve from short ID to display path
            if (resolvedDegreeId == null) {
                String displayPath = null;
                for (Map.Entry<String, String> entry : batchDocPathToBatchIdMap.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(bId)) {
                        displayPath = entry.getKey();
                        break;
                    }
                }
                if (displayPath != null) {
                    resolvedDegreeId = batchToDegreeMap.get(displayPath);
                }
            }

            // 3. Fallback: if still null, check if bId contains the selectedDegree name (e.g. "BIT" in "BIT(Batch 05)")
            if (resolvedDegreeId == null && selectedDegree != null && !selectedDegree.equals("All Degrees")) {
                if (bId.toLowerCase().contains(selectedDegree.toLowerCase())) {
                    resolvedDegreeId = selectedDegree;
                }
            }

            if (selectedDegree.equals("All Degrees") || (resolvedDegreeId != null && resolvedDegreeId.equalsIgnoreCase(selectedDegree))) {
                currentBatchFilterList.add(bId);
            }
        }

        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, currentBatchFilterList);
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatchFilter.setAdapter(batchAdapter);

        if (currentBatchFilterList.contains(currentSelectedBatch)) {
            setSpinnerValue(spinnerBatchFilter, currentSelectedBatch);
        } else {
            spinnerBatchFilter.setSelection(0);
        }
    }

    private void setupFilterSpinners(java.util.Set<String> degrees, java.util.Set<String> batches) {
        List<String> degreeFilterList = new ArrayList<>();
        degreeFilterList.add("All Degrees");
        degreeFilterList.addAll(degrees);
        
        ArrayAdapter<String> degAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeFilterList);
        degAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDegreeFilter.setAdapter(degAdapter);
        
        allModuleBatchesList.clear();
        allModuleBatchesList.addAll(batches);

        spinnerDegreeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDegree = spinnerDegreeFilter.getSelectedItem().toString();
                updateBatchFilterDropdown(selectedDegree);
                loadSemestersForFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initialize with default update
        updateBatchFilterDropdown("All Degrees");

        spinnerBatchFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSemestersForFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> sems = new ArrayList<>();
        sems.add("All Semesters");
        updateSemesterFilterDropdown(sems);

        spinnerSemesterFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSemestersForFilter() {
        String degree = spinnerDegreeFilter.getSelectedItem() != null ? spinnerDegreeFilter.getSelectedItem().toString() : "";
        String batch = spinnerBatchFilter.getSelectedItem() != null ? spinnerBatchFilter.getSelectedItem().toString() : "";

        if (degree.equals("All Degrees") || degree.equals("Select Degree") || batch.equals("All Batches") || batch.equals("Select Batch")) {
            List<String> sems = new ArrayList<>();
            sems.add("All Semesters");
            updateSemesterFilterDropdown(sems);
            applyFilters();
            return;
        }

        String realBatchId = batchDocPathToBatchIdMap.get(batch);
        if (realBatchId == null) {
            realBatchId = batch;
        }

        final String finalRealBatchId = realBatchId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Degrees").document(degree)
                .collection("Semesters").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> semesters = new ArrayList<>();
                    semesters.add("All Semesters");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bId = doc.getString("batchId");
                        String sId = doc.getString("semesterId");
                        if (bId != null && sId != null) {
                            if (bId.equalsIgnoreCase(finalRealBatchId) || bId.equalsIgnoreCase(batch)) {
                                semesters.add(sId);
                            }
                        }
                    }
                    updateSemesterFilterDropdown(semesters);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    List<String> sems = new ArrayList<>();
                    sems.add("All Semesters");
                    updateSemesterFilterDropdown(sems);
                    applyFilters();
                });
    }

    private void updateSemesterFilterDropdown(List<String> semesters) {
        if (semesters.isEmpty()) {
            semesters.add("All Semesters");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, semesters);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSemesterFilter.setAdapter(adapter);
    }

    private void applyFilters() {
        if (allModulesList == null) return;

        Object degObj = spinnerDegreeFilter.getSelectedItem();
        Object batchObj = spinnerBatchFilter.getSelectedItem();
        Object semObj = spinnerSemesterFilter.getSelectedItem();
        
        String selectedDeg = degObj != null ? degObj.toString() : "All Degrees";
        String selectedBatch = batchObj != null ? batchObj.toString() : "All Batches";
        String selectedSem = semObj != null ? semObj.toString() : "All Semesters";
        
        moduleList.clear();
        List<String> moduleNames = new ArrayList<>();
        moduleNames.add("Select a Module...");
        
        for (Module m : allModulesList) {
            boolean degMatch = selectedDeg.equals("All Degrees") || (m.getDegreeId() != null && m.getDegreeId().equalsIgnoreCase(selectedDeg));
            
            String realBatchId = batchDocPathToBatchIdMap.get(selectedBatch);
            if (realBatchId == null) {
                realBatchId = selectedBatch;
            }
            boolean batchMatch = selectedBatch.equals("All Batches") || 
                                 (m.getBatchId() != null && (m.getBatchId().equalsIgnoreCase(realBatchId) || m.getBatchId().equalsIgnoreCase(selectedBatch)));
            
            boolean semMatch = selectedSem.equals("All Semesters") || (m.getSemesterId() != null && m.getSemesterId().equalsIgnoreCase(selectedSem));
            
            if (degMatch && batchMatch && semMatch) {
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
            
            // Pre-populate degree and batch from active filters if selected
            String selectedDegree = spinnerDegreeFilter.getSelectedItem() != null ? spinnerDegreeFilter.getSelectedItem().toString() : "All Degrees";
            String selectedBatch = spinnerBatchFilter.getSelectedItem() != null ? spinnerBatchFilter.getSelectedItem().toString() : "All Batches";
            
            if (!selectedDegree.equals("All Degrees")) {
                setSpinnerValue(spinnerDegreeForm, selectedDegree);
            } else {
                spinnerDegreeForm.setSelection(0);
            }
            if (!selectedBatch.equals("All Batches")) {
                setSpinnerValue(spinnerBatchForm, selectedBatch);
            } else {
                spinnerBatchForm.setSelection(0);
            }

            cardAddModuleForm.setVisibility(View.VISIBLE);
            btnToggleAddModule.setVisibility(View.GONE);
        });

        btnEditModule.setOnClickListener(v -> {
            if (currentlySelectedModule != null) {
                isUpdateMode = true;
                tvFormTitle.setText("Update Module Details");
                
                setSpinnerValue(spinnerDegreeForm, currentlySelectedModule.getDegreeId());
                spinnerDegreeForm.setEnabled(false); // disable ID edit
                
                String docPath = null;
                for (Map.Entry<String, String> entry : batchDocPathToBatchIdMap.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(currentlySelectedModule.getBatchId())) {
                        docPath = entry.getKey();
                        break;
                    }
                }
                if (docPath == null) {
                    docPath = currentlySelectedModule.getBatchId();
                }
                setSpinnerValue(spinnerBatchForm, docPath);
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
                            String degreeId = currentlySelectedModule.getDegreeId();
                            String realBatchId = currentlySelectedModule.getBatchId();
                            String semesterId = currentlySelectedModule.getSemesterId();
                            String moduleId = currentlySelectedModule.getModuleId();

                            // Delete from Location A (Web style)
                            String finalBatchDocPath = null;
                            for (Map.Entry<String, String> entry : batchDocPathToBatchIdMap.entrySet()) {
                                if (entry.getValue().equalsIgnoreCase(realBatchId)) {
                                    finalBatchDocPath = entry.getKey();
                                    break;
                                }
                            }
                            if (finalBatchDocPath == null) {
                                finalBatchDocPath = degreeId + "(" + realBatchId + ")";
                            }
                            String webModDocId = finalBatchDocPath + "_" + semesterId + "_" + moduleId;
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("Degrees").document(degreeId)
                                    .collection("Modules").document(webModDocId).delete();

                            // Delete from Location B (Mobile style)
                            String parentDocId = realBatchId + "_" + degreeId + "_" + semesterId;
                            db.collection("Modules").document(parentDocId)
                                    .collection("Module IDs").document(moduleId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        ActivityLogger.logAction(this, "Deleted Module", "ID: " + moduleId + ", Batch: " + realBatchId);
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

            double creditVal;
            try {
                creditVal = Double.parseDouble(credits);
            } catch (NumberFormatException e) {
                etCredits.setError("Credits must be a valid number!");
                etCredits.requestFocus();
                return;
            }
            if (creditVal < 1.0) {
                etCredits.setError("Credits must be at least 1!");
                etCredits.requestFocus();
                return;
            }

            Toast.makeText(this, "Saving Module...", Toast.LENGTH_SHORT).show();
            btnSaveModule.setEnabled(false);

            String realBatchId = batchDocPathToBatchIdMap.get(batchId);
            if (realBatchId == null) {
                realBatchId = batchId;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> moduleData = new HashMap<>();
            moduleData.put("batchId", realBatchId);
            moduleData.put("degreeId", degreeId);
            moduleData.put("semesterId", semesterId);
            moduleData.put("moduleId", moduleId);
            moduleData.put("moduleName", moduleName);
            moduleData.put("credits", credits);

            // Location A: Dual write to Web-style path
            String finalBatchDocPath = null;
            for (Map.Entry<String, String> entry : batchDocPathToBatchIdMap.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(realBatchId)) {
                    finalBatchDocPath = entry.getKey();
                    break;
                }
            }
            if (finalBatchDocPath == null) {
                finalBatchDocPath = degreeId + "(" + realBatchId + ")";
            }

            String webModDocId = finalBatchDocPath + "_" + semesterId + "_" + moduleId;
            Map<String, Object> webModMap = new HashMap<>();
            webModMap.put("degreeId", degreeId);
            webModMap.put("batchId", realBatchId);
            webModMap.put("semesterId", semesterId);
            webModMap.put("moduleId", moduleId);
            webModMap.put("moduleCode", moduleId);
            webModMap.put("moduleName", moduleName);
            webModMap.put("credits", credits);

            db.collection("Degrees").document(degreeId).collection("Modules").document(webModDocId).set(webModMap);

            // Location B: Write to Mobile-style path
            String parentDocId = realBatchId + "_" + degreeId + "_" + semesterId;

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
        String realBatchId = batchDocPathToBatchIdMap.get(batch);
        if (realBatchId == null) {
            realBatchId = batch;
        }
        final String finalRealBatchId = realBatchId;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Degrees").document(degree)
                .collection("Semesters").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> semesters = new ArrayList<>();
                    semesters.add("Select Semester");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bId = doc.getString("batchId");
                        String sId = doc.getString("semesterId");
                        if (bId != null && sId != null) {
                            if (bId.equalsIgnoreCase(finalRealBatchId) || bId.equalsIgnoreCase(batch)) {
                                semesters.add(sId);
                            }
                        }
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
                        
                        String credits = "3";
                        if (tokens.length >= 5 && !tokens[4].trim().isEmpty()) {
                            try {
                                double c = Double.parseDouble(tokens[4].trim());
                                if (c >= 1.0) {
                                    credits = tokens[4].trim();
                                }
                            } catch (NumberFormatException ignored) {}
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

package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManualResultEntryActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerDegree, spinnerBatch, spinnerSemester;
    private LinearLayout llModuleContainer;
    private AppCompatButton btnAddModuleRow, btnProceed;

    private FirebaseFirestore db;
    private List<DegreeInfo> degreeList = new ArrayList<>();
    private List<BatchInfo> batchList = new ArrayList<>();
    private List<SemesterInfo> semesterList = new ArrayList<>();
    private List<ModuleData> availableModules = new ArrayList<>();

    private String[] grades = {"Select Grade", "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F", "AB", "MC", "NE", "WH", "INC"};
    private Map<String, Double> gradePoints = new HashMap<>();

    private boolean isEditMode = false;
    private String semesterDocId;
    private String studentId;
    private List<com.google.firebase.firestore.DocumentSnapshot> historySemesters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_result_entry);

        db = FirebaseFirestore.getInstance();
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "STU-0000");
        
        isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        semesterDocId = getIntent().getStringExtra("semesterDocId");

        initGradePoints();
        initViews();
        setupListeners();
        
        if (isEditMode) {
            loadExistingResultsForEdit();
        } else {
            loadInitialData();
            loadStudentHistory();
        }
    }

    private void loadStudentHistory() {
        db.collection("AllStudents").document(studentId)
                .collection("SemesterResults")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historySemesters = queryDocumentSnapshots.getDocuments();
                });
    }

    private void loadExistingResultsForEdit() {
        db.collection("AllStudents").document(studentId)
                .collection("SemesterResults").document(semesterDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<Map<String, Object>> modules = (List<Map<String, Object>>) doc.get("modules");
                        if (modules != null) {
                            llModuleContainer.removeAllViews();
                            for (Map<String, Object> m : modules) {
                                String id = (String) m.get("module_id");
                                String name = (String) m.get("module_name");
                                String grade = (String) m.get("grade");
                                double creds = 0;
                                Object cObj = m.get("credits");
                                if (cObj instanceof Double) creds = (Double) cObj;
                                else if (cObj instanceof Long) creds = ((Long) cObj).doubleValue();

                                ModuleData module = new ModuleData(id, name, String.valueOf(creds));
                                addModuleRowWithGrade(module, grade);
                            }
                        }
                        
                        // Disable spinners in edit mode to prevent changing semester association
                        spinnerDegree.setEnabled(false);
                        spinnerBatch.setEnabled(false);
                        spinnerSemester.setEnabled(false);
                        
                        // Set a title or label for edit mode
                        btnProceed.setText("Update and Forecast");
                    }
                });
    }

    private void addModuleRowWithGrade(ModuleData module, String savedGrade) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_manual_module_row, llModuleContainer, false);
        TextView tvModuleNameRow = rowView.findViewById(R.id.tvModuleNameRow);
        Spinner spinnerGradeRow = rowView.findViewById(R.id.spinnerGradeRow);

        tvModuleNameRow.setText(module.id + " - " + module.name);

        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, grades);
        gradeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerGradeRow.setAdapter(gradeAdapter);

        if (savedGrade != null) {
            for (int i = 0; i < grades.length; i++) {
                if (grades[i].equals(savedGrade)) {
                    spinnerGradeRow.setSelection(i);
                    break;
                }
            }
        }

        rowView.setTag(module);
        llModuleContainer.addView(rowView);
    }

    private void initGradePoints() {
        gradePoints.put("A+", 4.00);
        gradePoints.put("A", 4.00);
        gradePoints.put("A-", 3.70);
        gradePoints.put("B+", 3.30);
        gradePoints.put("B", 3.00);
        gradePoints.put("B-", 2.70);
        gradePoints.put("C+", 2.30);
        gradePoints.put("C", 2.00);
        gradePoints.put("C-", 1.70);
        gradePoints.put("D+", 1.30);
        gradePoints.put("D", 1.00);
        gradePoints.put("F", 0.00);
        gradePoints.put("AB", 0.00);
        gradePoints.put("MC", 0.00);
        gradePoints.put("NE", 0.00);
        gradePoints.put("WH", 0.00);
        gradePoints.put("INC", 0.00);
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerDegree = findViewById(R.id.spinnerDegree);
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        llModuleContainer = findViewById(R.id.llModuleContainer);
        btnAddModuleRow = findViewById(R.id.btnAddModuleRow);
        btnProceed = findViewById(R.id.btnProceed);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnProceed.setOnClickListener(v -> calculateAndProceed());

        AdapterView.OnItemSelectedListener selectionListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSemestersForSelection();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerDegree.setOnItemSelectedListener(selectionListener);
        spinnerBatch.setOnItemSelectedListener(selectionListener);

        spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadFilteredModules();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadInitialData() {
        db.collection("AllStudents").document(studentId).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        String studentBatchId = studentDoc.getString("batchId");
                        if (studentBatchId != null && !studentBatchId.isEmpty()) {
                            // Query Batches collection to find the batch where batchId matches
                            db.collection("Batches")
                                    .whereEqualTo("batchId", studentBatchId)
                                    .get()
                                    .addOnSuccessListener(batchSnapshots -> {
                                        if (!batchSnapshots.isEmpty()) {
                                            com.google.firebase.firestore.DocumentSnapshot batchDoc = batchSnapshots.getDocuments().get(0);
                                            String batchDocId = batchDoc.getId(); // programId(batchName)
                                            String batchName = batchDoc.getString("batchName");
                                            String degreeId = batchDoc.getString("programId");

                                            if (degreeId != null) {
                                                // Fetch Degree full name
                                                db.collection("Degrees").document(degreeId).get()
                                                        .addOnSuccessListener(degreeDoc -> {
                                                            String degreeName = degreeDoc.exists() ? degreeDoc.getString("name") : degreeId;
                                                            
                                                            // Populate and pre-select single Degree
                                                            degreeList.clear();
                                                            degreeList.add(new DegreeInfo(degreeId, degreeName));
                                                            List<String> degDisplay = new ArrayList<>();
                                                            degDisplay.add("Select Degree");
                                                            degDisplay.add(degreeId + " - " + degreeName);
                                                            ArrayAdapter<String> degAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, degDisplay);
                                                            degAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                                                            spinnerDegree.setAdapter(degAdapter);
                                                            spinnerDegree.setSelection(1); // Auto-select the resolved degree
                                                            spinnerDegree.setEnabled(false); // Lock it!

                                                            // Populate and pre-select single Batch
                                                            batchList.clear();
                                                            batchList.add(new BatchInfo(batchDocId, batchName, studentBatchId));
                                                            List<String> batchDisplay = new ArrayList<>();
                                                            batchDisplay.add("Select Batch");
                                                            batchDisplay.add(batchDocId + " - " + batchName);
                                                            ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batchDisplay);
                                                            batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                                                            spinnerBatch.setAdapter(batchAdapter);
                                                            spinnerBatch.setSelection(1); // Auto-select the resolved batch
                                                            spinnerBatch.setEnabled(false); // Lock it!
                                                        })
                                                        .addOnFailureListener(e -> loadAllDegreesAndBatches());
                                                return;
                                            }
                                        }
                                        loadAllDegreesAndBatches();
                                    })
                                    .addOnFailureListener(e -> loadAllDegreesAndBatches());
                            return;
                        }
                    }
                    loadAllDegreesAndBatches();
                })
                .addOnFailureListener(e -> loadAllDegreesAndBatches());
    }

    private void loadAllDegreesAndBatches() {
        // Load Degrees
        db.collection("Degrees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            degreeList.clear();
            List<String> displayNames = new ArrayList<>();
            displayNames.add("Select Degree");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String id = doc.getId();
                String name = doc.getString("name");
                if (name != null) {
                    degreeList.add(new DegreeInfo(id, name));
                    displayNames.add(id + " - " + name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, displayNames);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerDegree.setAdapter(adapter);
            spinnerDegree.setEnabled(true);
        });

        // Load Batches
        db.collection("Batches").get().addOnSuccessListener(queryDocumentSnapshots -> {
            batchList.clear();
            List<String> displayNames = new ArrayList<>();
            displayNames.add("Select Batch");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String id = doc.getId();
                String name = doc.getString("batchName");
                String realBatchId = doc.getString("batchId");
                if (name != null) {
                    batchList.add(new BatchInfo(id, name, realBatchId != null ? realBatchId : name));
                    displayNames.add(id + " - " + name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, displayNames);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerBatch.setAdapter(adapter);
            spinnerBatch.setEnabled(true);
        });
    }

    private void loadSemestersForSelection() {
        int degPos = spinnerDegree.getSelectedItemPosition();
        int batchPos = spinnerBatch.getSelectedItemPosition();

        if (degPos <= 0 || batchPos <= 0) {
            updateSemesterSpinner(new ArrayList<>());
            return;
        }

        String degId = degreeList.get(degPos - 1).id;
        String batchDocId = batchList.get(batchPos - 1).id;

        db.collection("Batches").document(batchDocId).get()
                .addOnSuccessListener(batchDoc -> {
                    String realBatchId = batchDoc.getString("batchId");
                    if (realBatchId == null) realBatchId = batchDocId;

                    final String finalRealBatchId = realBatchId;
                    
                    db.collection("Degrees").document(degId).collection("Semesters").get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                semesterList.clear();
                                List<String> displayNames = new ArrayList<>();
                                displayNames.add("Select Semester");
                                
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    String bId = doc.getString("batchId");
                                    String sId = doc.getString("semesterId");
                                    String semName = doc.getString("name");
                                    
                                    if (semName == null) {
                                       String ay = doc.getString("academicYear");
                                       String sn = doc.getString("semesterNo");
                                       semName = (ay != null ? ay : "") + " " + (sn != null ? sn : "");
                                    }

                                    if (sId != null && bId != null) {
                                        if (bId.equalsIgnoreCase(finalRealBatchId) || bId.equalsIgnoreCase(batchDocId)) {
                                            String fullId = batchDocId + "_" + degId + "_" + sId;
                                            semesterList.add(new SemesterInfo(fullId, semName.trim().isEmpty() ? sId : semName, sId));
                                            displayNames.add(semName.trim().isEmpty() ? sId : semName);
                                        }
                                    }
                                }
                                updateSemesterSpinner(displayNames);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load semesters: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load batch info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSemesterSpinner(List<String> displayNames) {
        if (displayNames.isEmpty()) {
            displayNames.add("No semesters found");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, displayNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSemester.setAdapter(adapter);
    }

    private void loadFilteredModules() {
        int degPos = spinnerDegree.getSelectedItemPosition();
        int batchPos = spinnerBatch.getSelectedItemPosition();
        int semPos = spinnerSemester.getSelectedItemPosition();

        if (degPos <= 0 || batchPos <= 0 || semPos <= 0) {
            llModuleContainer.removeAllViews();
            return;
        }

        String degId = degreeList.get(degPos - 1).id;
        String batchFull = batchList.get(batchPos - 1).realBatchId; 
        String semFullId = semesterList.get(semPos - 1).id;
        String sId = semesterList.get(semPos - 1).semesterId;
        
        Toast.makeText(this, "Querying Modules...", Toast.LENGTH_SHORT).show();
        
        availableModules.clear();
        llModuleContainer.removeAllViews(); // Clear previous semester modules

        java.util.Set<String> addedModuleIds = new java.util.HashSet<>();
        
        db.collection("Degrees").document(degId).collection("Modules")
                .whereEqualTo("batchId", batchFull)
                .whereEqualTo("semesterId", sId)
                .get()
                .addOnCompleteListener(taskA -> {
                    if (taskA.isSuccessful() && taskA.getResult() != null) {
                        for (QueryDocumentSnapshot doc : taskA.getResult()) {
                            addModuleFromDoc(doc, addedModuleIds);
                        }
                    }
                    
                    db.collection("Modules").document(semFullId)
                            .collection("Module IDs").get()
                            .addOnCompleteListener(taskB -> {
                                if (taskB.isSuccessful() && taskB.getResult() != null) {
                                    for (QueryDocumentSnapshot doc : taskB.getResult()) {
                                        addModuleFromDoc(doc, addedModuleIds);
                                    }
                                }
                                
                                if (availableModules.isEmpty()) {
                                    Toast.makeText(ManualResultEntryActivity.this, "No modules found for this semester", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void addModuleFromDoc(QueryDocumentSnapshot doc, java.util.Set<String> addedModuleIds) {
        String mId = doc.getString("moduleId");
        if (mId == null) {
            mId = doc.getString("moduleCode");
        }
        if (mId == null || addedModuleIds.contains(mId)) {
            return;
        }

        String mName = doc.getString("moduleName");
        Object credsObj = doc.get("credits");
        String credits = credsObj != null ? String.valueOf(credsObj) : "0";

        ModuleData module = new ModuleData(mId, mName, credits);
        availableModules.add(module);
        addedModuleIds.add(mId);
        addModuleRow(module);
    }

    private void addModuleRow(ModuleData module) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_manual_module_row, llModuleContainer, false);
        TextView tvModuleNameRow = rowView.findViewById(R.id.tvModuleNameRow);
        Spinner spinnerGradeRow = rowView.findViewById(R.id.spinnerGradeRow);

        tvModuleNameRow.setText(module.id + " - " + module.name);

        // Setup Grade Spinner
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, grades);
        gradeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerGradeRow.setAdapter(gradeAdapter);

        // Store module data and current semester in the row's tag for calculation later
        rowView.setTag(module);
        
        llModuleContainer.addView(rowView);
    }

    private void calculateAndProceed() {
        String currentSem = "";
        if (isEditMode) {
            currentSem = semesterDocId;
        } else {
            if (spinnerSemester.getSelectedItem() == null) return;
            currentSem = spinnerSemester.getSelectedItem().toString();
        }

        int rowCount = llModuleContainer.getChildCount();
        if (rowCount == 0) {
            Toast.makeText(this, "No modules added", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalQualityPoints = 0;
        double totalCreditHours = 0;
        double currentSemPoints = 0;
        double currentSemCredits = 0;
        
        boolean cumHasSpecial = false;
        boolean semHasSpecial = false;

        List<Map<String, Object>> selectedResults = new ArrayList<>();

        // 1. Add history to CGPA (Excluding the one being edited if in edit mode)
        for (com.google.firebase.firestore.DocumentSnapshot doc : historySemesters) {
            if (isEditMode && doc.getId().equals(semesterDocId)) continue;
            
            List<Map<String, Object>> modules = (List<Map<String, Object>>) doc.get("modules");
            if (modules != null) {
                for (Map<String, Object> m : modules) {
                    String g = (String) m.get("grade");
                    if (g != null && (g.equals("MC") || g.equals("AB") || g.equals("NE") || g.equals("WH") || g.equals("INC"))) {
                        cumHasSpecial = true;
                        continue;
                    }

                    double c = 0;
                    Object co = m.get("credits");
                    if (co instanceof Double) c = (Double) co;
                    else if (co instanceof Long) c = ((Long) co).doubleValue();

                    double p = 0;
                    Object po = m.get("grade_point");
                    if (po instanceof Double) p = (Double) po;
                    else if (po instanceof Long) p = ((Long) po).doubleValue();

                    totalQualityPoints += (p * c);
                    totalCreditHours += c;
                }
            }
        }

        // 2. Process Current UI Inputs
        for (int i = 0; i < rowCount; i++) {
            View rowView = llModuleContainer.getChildAt(i);
            Spinner spinnerGradeRow = rowView.findViewById(R.id.spinnerGradeRow);
            ModuleData module = (ModuleData) rowView.getTag();

            int gradePos = spinnerGradeRow.getSelectedItemPosition();
            if (gradePos == 0) {
                Toast.makeText(this, "Please select grades for all modules", Toast.LENGTH_SHORT).show();
                return;
            }

            String grade = grades[gradePos];
            double credits = Double.parseDouble(module.credits);
            Double pVal = gradePoints.get(grade);
            double points = pVal != null ? pVal : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("module_id", module.id);
            result.put("module_name", module.name);
            result.put("grade", grade);
            result.put("grade_point", points);
            result.put("credits", credits);
            result.put("semester", currentSem);
            selectedResults.add(result);

            // Deduct credits for special grades (Don't count in divisor) and flag
            if (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC")) {
                semHasSpecial = true;
                cumHasSpecial = true;
                continue;
            }

            currentSemPoints += (points * credits);
            currentSemCredits += credits;
            
            totalQualityPoints += (points * credits);
            totalCreditHours += credits;
        }

        double currentGpa = semHasSpecial ? 0.0 : (currentSemCredits > 0 ? (currentSemPoints / currentSemCredits) : 0);
        double cumulativeGpa = cumHasSpecial ? 0.0 : (totalCreditHours > 0 ? (totalQualityPoints / totalCreditHours) : 0);
        
        showSummaryDialog(currentGpa, cumulativeGpa, selectedResults, currentSem, semHasSpecial, cumHasSpecial);
    }

    private void showSummaryDialog(double currentGpa, double cumulativeGpa, List<Map<String, Object>> results, String semesterName, boolean semHasSpecial, boolean cumHasSpecial) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Result Summary");
        
        String semGpaStr = semHasSpecial ? "N/A" : String.format(java.util.Locale.US, "%.2f", currentGpa);
        String cumGpaStr = cumHasSpecial ? "N/A" : String.format(java.util.Locale.US, "%.2f", cumulativeGpa);
        
        String message = String.format("Semester: %s\n\nCurrent Semester GPA: %s\nCumulative GPA: %s\n\nProceed to Quiz for Future GPA Prediction?", 
                semesterName, semGpaStr, cumGpaStr);
        builder.setMessage(message);
        builder.setPositiveButton("Proceed", (dialog, which) -> {
            // Proceed to Quiz Activity for lifestyle data
            android.util.Log.d("MANUAL_DEBUG", "Sending results size: " + results.size());
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("cumulativeGpa", cumulativeGpa);
            intent.putExtra("semesterGpa", currentGpa);
            intent.putExtra("results", (java.io.Serializable) results);
            intent.putExtra("semesterName", semesterName);
            startActivity(intent);
        });
        builder.setNegativeButton("Edit", null);
        builder.show();
    }

    private static class DegreeInfo {
        String id, name;
        DegreeInfo(String id, String name) { this.id = id; this.name = name; }
    }

    private static class BatchInfo {
        String id, name, realBatchId;
        BatchInfo(String id, String name, String realBatchId) { 
            this.id = id; 
            this.name = name; 
            this.realBatchId = realBatchId;
        }
    }

    private static class SemesterInfo {
        String id, name, semesterId;
        SemesterInfo(String id, String name, String semesterId) { 
            this.id = id; 
            this.name = name; 
            this.semesterId = semesterId;
        }
    }

    private static class ModuleData {
        String id, name, credits;
        ModuleData(String id, String name, String credits) {
            this.id = id;
            this.name = name;
            this.credits = credits;
        }
    }
}

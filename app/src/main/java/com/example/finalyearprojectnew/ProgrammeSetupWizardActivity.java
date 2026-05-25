package com.example.finalyearprojectnew;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgrammeSetupWizardActivity extends AppCompatActivity {

    // Common Views
    private ImageView ivBack;
    private TextView tvWizardTitle;
    private TextView tvStepTitle;
    private TextView tvStep1, tvStep2, tvStep3, tvStep4, tvStep5;
    private View lineStep1to2, lineStep2to3, lineStep3to4, lineStep4to5;
    
    // Panel Views
    private LinearLayout panelStep1, panelStep2, panelStep3, panelStep4, panelStep5;
    
    // Footer Actions
    private androidx.appcompat.widget.AppCompatButton btnPrevious, btnNext;

    // Step 1 Views
    private EditText etProgId, etProgName;
    private Spinner spinnerProgDuration;

    // Step 2 Views
    private EditText etBatchName, etBatchId;

    // Step 3 Views
    private TextView tvSemesterExplanation;
    private LinearLayout llSemesterPreviewList;

    // Step 4 Views
    private EditText etStudentId, etStudentName, etStudentEmail, etStudentPassword;
    private Button btnAddStudentLocal;
    private androidx.appcompat.widget.AppCompatButton btnUploadCsvStudents;
    private TextView tvEnrolledLabel;
    private LinearLayout llStudentListWrapper;

    // Step 5 Views
    private Spinner spinnerModuleSemester;
    private EditText etModuleCode, etModuleName, etModuleCredits;
    private Button btnAddModuleLocal;
    private androidx.appcompat.widget.AppCompatButton btnUploadCsvModules;
    private TextView tvModulesLabel;
    private LinearLayout llModuleListWrapper;

    private static final int CSV_REQ_STUDENTS = 2001;
    private static final int CSV_REQ_MODULES = 2002;

    // Local state data
    private int currentStep = 1;
    private String selectedDuration = "3 Years"; // Default
    
    // Helper lists for dynamic semester generation
    private static class TempSemester {
        String id;
        String academicYear;
        String semesterNo;

        TempSemester(String id, String academicYear, String semesterNo) {
            this.id = id;
            this.academicYear = academicYear;
            this.semesterNo = semesterNo;
        }
    }
    
    private List<TempSemester> generatedSemestersList = new ArrayList<>();

    // Local model list for students
    private static class TempStudent {
        String studentId;
        String fullName;
        String email;
        String password;
        String status;

        TempStudent(String studentId, String fullName, String email, String password, String status) {
            this.studentId = studentId;
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.status = status;
        }
    }
    private List<TempStudent> localStudentsList = new ArrayList<>();

    // Local model list for modules
    private static class TempModule {
        String semesterId;
        String moduleCode;
        String moduleName;
        String credits;

        TempModule(String semesterId, String moduleCode, String moduleName, String credits) {
            this.semesterId = semesterId;
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
            this.credits = credits;
        }
    }
    private List<TempModule> localModulesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programme_setup_wizard);

        initViews();
        setupSpinnerData();
        setupListeners();
        updateStepUI();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvWizardTitle = findViewById(R.id.tvWizardTitle);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        
        tvStep1 = findViewById(R.id.tvStep1);
        tvStep2 = findViewById(R.id.tvStep2);
        tvStep3 = findViewById(R.id.tvStep3);
        tvStep4 = findViewById(R.id.tvStep4);
        tvStep5 = findViewById(R.id.tvStep5);
        
        lineStep1to2 = findViewById(R.id.lineStep1to2);
        lineStep2to3 = findViewById(R.id.lineStep2to3);
        lineStep3to4 = findViewById(R.id.lineStep3to4);
        lineStep4to5 = findViewById(R.id.lineStep4to5);
        
        panelStep1 = findViewById(R.id.panelStep1);
        panelStep2 = findViewById(R.id.panelStep2);
        panelStep3 = findViewById(R.id.panelStep3);
        panelStep4 = findViewById(R.id.panelStep4);
        panelStep5 = findViewById(R.id.panelStep5);
        
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        // Step 1
        etProgId = findViewById(R.id.etProgId);
        etProgName = findViewById(R.id.etProgName);
        spinnerProgDuration = findViewById(R.id.spinnerProgDuration);

        // Step 2
        etBatchName = findViewById(R.id.etBatchName);
        etBatchId = findViewById(R.id.etBatchId);

        // Step 3
        tvSemesterExplanation = findViewById(R.id.tvSemesterExplanation);
        llSemesterPreviewList = findViewById(R.id.llSemesterPreviewList);

        // Step 4
        etStudentId = findViewById(R.id.etStudentId);
        etStudentName = findViewById(R.id.etStudentName);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnAddStudentLocal = findViewById(R.id.btnAddStudentLocal);
        btnUploadCsvStudents = findViewById(R.id.btnUploadCsvStudents);
        tvEnrolledLabel = findViewById(R.id.tvEnrolledLabel);
        llStudentListWrapper = findViewById(R.id.llStudentListWrapper);

        // Step 5
        spinnerModuleSemester = findViewById(R.id.spinnerModuleSemester);
        etModuleCode = findViewById(R.id.etModuleCode);
        etModuleName = findViewById(R.id.etModuleName);
        etModuleCredits = findViewById(R.id.etModuleCredits);
        btnAddModuleLocal = findViewById(R.id.btnAddModuleLocal);
        btnUploadCsvModules = findViewById(R.id.btnUploadCsvModules);
        tvModulesLabel = findViewById(R.id.tvModulesLabel);
        llModuleListWrapper = findViewById(R.id.llModuleListWrapper);
    }

    private void setupSpinnerData() {
        // Duration options
        List<String> durations = new ArrayList<>();
        durations.add("3 Years");
        durations.add("4 Years");
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, durations);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProgDuration.setAdapter(durationAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> handleBackNavigation());

        btnPrevious.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateStepUI();
            }
        });

        btnNext.setOnClickListener(v -> handleNextNavigation());

        // Step 4: Add student locally
        btnAddStudentLocal.setOnClickListener(v -> addStudentLocally());
        btnUploadCsvStudents.setOnClickListener(v -> openCsvPicker(CSV_REQ_STUDENTS));

        // Step 5: Add module locally
        btnAddModuleLocal.setOnClickListener(v -> addModuleLocally());
        btnUploadCsvModules.setOnClickListener(v -> openCsvPicker(CSV_REQ_MODULES));
    }

    private void handleBackNavigation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit Wizard?")
                .setMessage("Are you sure you want to exit the setup wizard? All progress will be lost.")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleNextNavigation() {
        if (currentStep == 1) {
            // Validate Step 1
            String progId = etProgId.getText().toString().trim();
            String progName = etProgName.getText().toString().trim();
            if (TextUtils.isEmpty(progId) || TextUtils.isEmpty(progName)) {
                Toast.makeText(this, "Programme ID and Name are required", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedDuration = spinnerProgDuration.getSelectedItem().toString();
            
            // Check program ID uniqueness in Firestore asynchronously before proceeding
            btnNext.setEnabled(false);
            btnNext.setText("Checking ID...");
            FirebaseFirestore.getInstance().collection("Degrees").document(progId).get()
                    .addOnCompleteListener(task -> {
                        btnNext.setEnabled(true);
                        btnNext.setText("Next Step");
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            etProgId.setError("Programme ID is already in use!");
                            Toast.makeText(this, "Programme ID is already taken!", Toast.LENGTH_LONG).show();
                        } else {
                            // Unique ID, proceed
                            currentStep++;
                            updateStepUI();
                        }
                    });
            
        } else if (currentStep == 2) {
            // Validate Step 2
            String batchId = etBatchId.getText().toString().trim();
            String batchName = etBatchName.getText().toString().trim();
            if (TextUtils.isEmpty(batchId) || TextUtils.isEmpty(batchName)) {
                Toast.makeText(this, "Batch ID and Name are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Generate semesters in memory based on Step 1 duration
            generateSemesters();
            currentStep++;
            updateStepUI();
            
        } else if (currentStep == 3) {
            // Step 3 (Semester configuration) is auto-generated and read-only.
            // Populates the dropdown spinner for Step 5 module mapping
            setupModuleSemesterSpinner();
            currentStep++;
            updateStepUI();
            
        } else if (currentStep == 4) {
            // Step 4 (Students) - optional to enroll immediately, but validates local list duplicates if they exist
            currentStep++;
            updateStepUI();
            
        } else if (currentStep == 5) {
            // Step 5 - Complete Setup (Final Execution)
            completeProgrammeSetup();
        }
    }

    private void updateStepUI() {
        // Toggle visibility of panels
        panelStep1.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        panelStep2.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        panelStep3.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
        panelStep4.setVisibility(currentStep == 4 ? View.VISIBLE : View.GONE);
        panelStep5.setVisibility(currentStep == 5 ? View.VISIBLE : View.GONE);

        // Update stepper visuals
        updateStepperIndicators();

        // Update buttons
        btnPrevious.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
        if (currentStep < 5) {
            btnNext.setText("Next Step");
            btnNext.setBackgroundResource(R.drawable.bg_gradient_button);
        } else {
            btnNext.setText("Complete Setup");
            btnNext.setBackgroundResource(R.drawable.bg_gradient_button); // Can set specific green theme or similar
        }
    }

    private void updateStepperIndicators() {
        // Clear all states
        tvStep1.setBackgroundResource(R.drawable.bg_circle_gray);
        tvStep1.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvStep2.setBackgroundResource(R.drawable.bg_circle_gray);
        tvStep2.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvStep3.setBackgroundResource(R.drawable.bg_circle_gray);
        tvStep3.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvStep4.setBackgroundResource(R.drawable.bg_circle_gray);
        tvStep4.setTextColor(getResources().getColor(R.color.textColorSecondary));
        tvStep5.setBackgroundResource(R.drawable.bg_circle_gray);
        tvStep5.setTextColor(getResources().getColor(R.color.textColorSecondary));

        lineStep1to2.setBackgroundColor(getResources().getColor(R.color.dividerColor));
        lineStep2to3.setBackgroundColor(getResources().getColor(R.color.dividerColor));
        lineStep3to4.setBackgroundColor(getResources().getColor(R.color.dividerColor));
        lineStep4to5.setBackgroundColor(getResources().getColor(R.color.dividerColor));

        // Highlight active and completed
        if (currentStep >= 1) {
            tvStep1.setBackgroundResource(R.drawable.bg_circle_blue);
            tvStep1.setTextColor(getResources().getColor(android.R.color.white));
            tvStepTitle.setText("Step 1: Programme Details");
        }
        if (currentStep >= 2) {
            lineStep1to2.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            tvStep2.setBackgroundResource(currentStep == 2 ? R.drawable.bg_circle_blue : R.drawable.bg_circle_blue); // Can use checkmark drawable or simple blue
            tvStep2.setTextColor(getResources().getColor(android.R.color.white));
            if (currentStep == 2) tvStepTitle.setText("Step 2: Add Initial Batch");
        }
        if (currentStep >= 3) {
            lineStep2to3.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            tvStep3.setBackgroundResource(R.drawable.bg_circle_blue);
            tvStep3.setTextColor(getResources().getColor(android.R.color.white));
            if (currentStep == 3) tvStepTitle.setText("Step 3: Semester Configuration");
        }
        if (currentStep >= 4) {
            lineStep3to4.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            tvStep4.setBackgroundResource(R.drawable.bg_circle_blue);
            tvStep4.setTextColor(getResources().getColor(android.R.color.white));
            if (currentStep == 4) tvStepTitle.setText("Step 4: Enroll Students");
        }
        if (currentStep >= 5) {
            lineStep4to5.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            tvStep5.setBackgroundResource(R.drawable.bg_circle_blue);
            tvStep5.setTextColor(getResources().getColor(android.R.color.white));
            if (currentStep == 5) tvStepTitle.setText("Step 5: Module Configuration");
        }
    }

    private void generateSemesters() {
        generatedSemestersList.clear();
        llSemesterPreviewList.removeAllViews();
        
        int years = 3;
        if (selectedDuration.contains("4")) {
            years = 4;
        }

        tvSemesterExplanation.setText("Based on your selected duration of " + selectedDuration + ", " + (years * 2) + " semesters will be automatically generated and mapped to this batch.");

        String[] romanYears = {"Year I", "Year II", "Year III", "Year IV"};
        String[] romanSems = {"Semester I", "Semester II"};

        int semCounter = 1;
        for (int y = 0; y < years; y++) {
            for (int s = 0; s < 2; s++) {
                String semId = String.format("SEM%02d", semCounter);
                TempSemester sem = new TempSemester(semId, romanYears[y], romanSems[s]);
                generatedSemestersList.add(sem);

                // Add simple preview dynamic view
                TextView tvSem = new TextView(this);
                tvSem.setText(semId + " - " + romanYears[y] + " " + romanSems[s]);
                tvSem.setPadding(32, 20, 32, 20);
                tvSem.setTextSize(14);
                tvSem.setTextColor(getResources().getColor(R.color.textColorPrimary));
                tvSem.setTypeface(null, android.graphics.Typeface.BOLD);
                llSemesterPreviewList.addView(tvSem);

                semCounter++;
            }
        }
    }

    private void addStudentLocally() {
        String id = etStudentId.getText().toString().trim();
        String name = etStudentName.getText().toString().trim();
        String email = etStudentEmail.getText().toString().trim();
        String password = etStudentPassword.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Student ID, Name, and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etStudentEmail.setError("Invalid Email format");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            password = "123456"; // Default
        }

        // Validate local duplicates
        for (TempStudent existing : localStudentsList) {
            if (existing.studentId.equalsIgnoreCase(id)) {
                etStudentId.setError("Duplicate Student ID entered locally!");
                Toast.makeText(this, "Duplicate Student ID in local list!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        TempStudent student = new TempStudent(id, name, email, password, "active");
        localStudentsList.add(student);

        // Clear inputs
        etStudentId.setText("");
        etStudentName.setText("");
        etStudentEmail.setText("");
        etStudentPassword.setText("");

        refreshStudentsListUI();
    }

    private void refreshStudentsListUI() {
        llStudentListWrapper.removeAllViews();
        tvEnrolledLabel.setText("Enrolled List (" + localStudentsList.size() + ")");

        for (int i = 0; i < localStudentsList.size(); i++) {
            TempStudent s = localStudentsList.get(i);
            final int index = i;

            // Dynamic layout inflation for premium feel
            View view = LayoutInflater.from(this).inflate(R.layout.item_student_list, llStudentListWrapper, false);
            
            TextView tvId = view.findViewById(R.id.tvStudentId);
            TextView tvName = view.findViewById(R.id.tvStudentName);
            TextView tvEmail = view.findViewById(R.id.tvStudentEmail);
            ImageView ivDelete = view.findViewById(R.id.ivDeleteStudent);

            tvId.setText("ID: " + s.studentId);
            tvName.setText(s.fullName);
            tvEmail.setText(s.email);

            ivDelete.setOnClickListener(v -> {
                localStudentsList.remove(index);
                refreshStudentsListUI();
            });

            llStudentListWrapper.addView(view);
        }
    }

    private void setupModuleSemesterSpinner() {
        List<String> semNames = new ArrayList<>();
        for (TempSemester s : generatedSemestersList) {
            semNames.add(s.id);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModuleSemester.setAdapter(adapter);
    }

    private void addModuleLocally() {
        if (spinnerModuleSemester.getSelectedItem() == null) {
            Toast.makeText(this, "No semesters generated!", Toast.LENGTH_SHORT).show();
            return;
        }

        String semId = spinnerModuleSemester.getSelectedItem().toString();
        String code = etModuleCode.getText().toString().trim();
        String name = etModuleName.getText().toString().trim();
        String credits = etModuleCredits.getText().toString().trim();

        if (TextUtils.isEmpty(code) || TextUtils.isEmpty(name) || TextUtils.isEmpty(credits)) {
            Toast.makeText(this, "Module Code, Name, and Credits are required", Toast.LENGTH_SHORT).show();
            return;
        }

        TempModule module = new TempModule(semId, code, name, credits);
        localModulesList.add(module);

        // Clear inputs
        etModuleCode.setText("");
        etModuleName.setText("");
        etModuleCredits.setText("");

        refreshModulesListUI();
    }

    private void refreshModulesListUI() {
        llModuleListWrapper.removeAllViews();
        tvModulesLabel.setText("Configured Modules (" + localModulesList.size() + ")");

        for (int i = 0; i < localModulesList.size(); i++) {
            TempModule m = localModulesList.get(i);
            final int index = i;

            View view = LayoutInflater.from(this).inflate(R.layout.item_wizard_module_row, llModuleListWrapper, false);
            
            TextView tvSem = view.findViewById(R.id.tvRowSemester);
            TextView tvCode = view.findViewById(R.id.tvRowCode);
            TextView tvName = view.findViewById(R.id.tvRowName);
            TextView tvCredits = view.findViewById(R.id.tvRowCredits);
            ImageView ivDelete = view.findViewById(R.id.ivDeleteModule);

            tvSem.setText(m.semesterId);
            tvCode.setText(m.moduleCode);
            tvName.setText(m.moduleName);
            tvCredits.setText(m.credits + " Cr");

            ivDelete.setOnClickListener(v -> {
                localModulesList.remove(index);
                refreshModulesListUI();
            });

            llModuleListWrapper.addView(view);
        }
    }

    private void completeProgrammeSetup() {
        // Confirm before processing final writes
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Complete Setup?")
                .setMessage("This will atomically configure the new programme, initial batch, semesters, enrolled students, and modules. Proceed?")
                .setPositiveButton("Complete", (dialog, which) -> saveToFirestoreAtomically())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToFirestoreAtomically() {
        btnNext.setEnabled(false);
        btnNext.setText("Saving Setup...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batchCommit = db.batch();

        // 1. Gather all dynamic keys
        String progId = etProgId.getText().toString().trim();
        String progName = etProgName.getText().toString().trim();
        String batchId = etBatchId.getText().toString().trim();
        String batchName = etBatchName.getText().toString().trim();
        String batchDocPath = progId + "(" + batchName + ")";

        // Check system student ID duplicates inside Firestore before committing
        if (!localStudentsList.isEmpty()) {
            List<Task<DocumentSnapshot>> studentCheckTasks = new ArrayList<>();
            for (TempStudent s : localStudentsList) {
                studentCheckTasks.add(db.collection("AllStudents").document(s.studentId).get());
            }

            Tasks.whenAllComplete(studentCheckTasks).addOnCompleteListener(task -> {
                List<String> duplicateIds = new ArrayList<>();
                for (Task<?> t : studentCheckTasks) {
                    if (t.isSuccessful()) {
                        DocumentSnapshot snap = (DocumentSnapshot) t.getResult();
                        if (snap != null && snap.exists()) {
                            duplicateIds.add(snap.getId());
                        }
                    }
                }

                if (!duplicateIds.isEmpty()) {
                    btnNext.setEnabled(true);
                    btnNext.setText("Complete Setup");
                    new androidx.appcompat.app.AlertDialog.Builder(ProgrammeSetupWizardActivity.this)
                            .setTitle("Duplicate IDs Detected")
                            .setMessage("Setup Blocked! The following Student IDs are already in use system-wide:\n\n" + 
                                    TextUtils.join(", ", duplicateIds) + 
                                    "\n\nPlease go back to Step 4 and fix these IDs.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                currentStep = 4;
                                updateStepUI();
                            })
                            .show();
                } else {
                    executeBatchCommit(batchCommit, progId, progName, batchId, batchName, batchDocPath);
                }
            });
        } else {
            // No students, commit directly
            executeBatchCommit(batchCommit, progId, progName, batchId, batchName, batchDocPath);
        }
    }

    private void executeBatchCommit(WriteBatch batchCommit, String progId, String progName, String batchId, String batchName, String batchDocPath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            // 1. Write Programme (Degree)
            Map<String, Object> degreeMap = new HashMap<>();
            degreeMap.put("id", progId);
            degreeMap.put("name", progName);
            degreeMap.put("duration", selectedDuration);
            degreeMap.put("status", "Active");

            batchCommit.set(db.collection("Degrees").document(progId), degreeMap);

            // 2. Write Batch (Dual Writes)
            Map<String, Object> batchMap = new HashMap<>();
            batchMap.put("programId", progId);
            batchMap.put("batchId", batchId);
            batchMap.put("batchName", batchName);
            batchMap.put("intakeYear", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

            batchCommit.set(db.collection("Batches").document(batchDocPath), batchMap);
            batchCommit.set(db.collection("Degrees").document(progId).collection("Batches").document(batchDocPath), batchMap);

            // 3. Write Semesters (Dual Writes)
            for (TempSemester sem : generatedSemestersList) {
                // Location A (Web style)
                String webSemDocId = batchDocPath + "_" + sem.id;
                Map<String, Object> webSemMap = new HashMap<>();
                webSemMap.put("degreeId", progId);
                webSemMap.put("batchId", batchId);
                webSemMap.put("semesterId", sem.id);
                webSemMap.put("academicYear", sem.academicYear);
                webSemMap.put("semesterNo", sem.semesterNo);
                webSemMap.put("name", sem.academicYear + " " + sem.semesterNo);

                batchCommit.set(db.collection("Degrees").document(progId).collection("Semesters").document(webSemDocId), webSemMap);

                // Location B (Mobile style)
                Map<String, Object> mobSemMap = new HashMap<>();
                mobSemMap.put("degreeId", progId);
                mobSemMap.put("batchId", batchId);
                mobSemMap.put("semesterId", sem.id);
                mobSemMap.put("academicYear", sem.academicYear);
                mobSemMap.put("semesterNo", sem.semesterNo);

                batchCommit.set(db.collection("Semesters").document(progId)
                        .collection("Batches").document(batchId)
                        .collection("Semester IDs").document(sem.id), mobSemMap);
            }

            // 4. Write Enrolled Students (Nested + Login flat collection)
            for (TempStudent s : localStudentsList) {
                // Hash the password securely via SHA-256 (matching Web and Mobile login engines)
                String hashedPassword = SecurityUtils.hashPassword(s.password);

                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("studentId", s.studentId);
                studentMap.put("fullName", s.fullName);
                studentMap.put("email", s.email);
                studentMap.put("status", s.status);
                studentMap.put("batchId", batchId);
                studentMap.put("hashed_password", hashedPassword);
                studentMap.put("initial_password", s.password);
                studentMap.put("isFirstLogin", true);

                // Nested Batch
                batchCommit.set(db.collection("Students").document(batchDocPath).collection("Student IDs").document(s.studentId), studentMap);
                // Flat Login Lookup
                batchCommit.set(db.collection("AllStudents").document(s.studentId), studentMap);
            }

            // 5. Write Modules (Dual Writes)
            for (TempModule mod : localModulesList) {
                // Location A (Web style)
                String webModDocId = batchDocPath + "_" + mod.semesterId + "_" + mod.moduleCode;
                Map<String, Object> webModMap = new HashMap<>();
                webModMap.put("degreeId", progId);
                webModMap.put("batchId", batchId);
                webModMap.put("semesterId", mod.semesterId);
                webModMap.put("moduleCode", mod.moduleCode);
                webModMap.put("moduleName", mod.moduleName);
                webModMap.put("credits", mod.credits);

                batchCommit.set(db.collection("Degrees").document(progId).collection("Modules").document(webModDocId), webModMap);

                // Location B (Mobile style)
                String mobParentDocId = batchId + "_" + progId + "_" + mod.semesterId;
                Map<String, Object> mobModMap = new HashMap<>();
                mobModMap.put("degreeId", progId);
                mobModMap.put("batchId", batchId);
                mobModMap.put("semesterId", mod.semesterId);
                mobModMap.put("moduleId", mod.moduleCode);
                mobModMap.put("moduleName", mod.moduleName);
                mobModMap.put("credits", mod.credits);

                batchCommit.set(db.collection("Modules").document(mobParentDocId).collection("Module IDs").document(mod.moduleCode), mobModMap);
            }

            // Commit batch atomically
            batchCommit.commit()
                    .addOnSuccessListener(aVoid -> {
                        ActivityLogger.logAction(this, "Created Programme Setup Wizard", progId + ": " + progName);
                        Toast.makeText(this, "Programme setup wizard completed successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnNext.setEnabled(true);
                        btnNext.setText("Complete Setup");
                        Toast.makeText(this, "Failed to complete setup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            btnNext.setEnabled(true);
            btnNext.setText("Complete Setup");
            Toast.makeText(this, "Error building setup payload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openCsvPicker(int requestCode) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == CSV_REQ_STUDENTS) {
                    parseAndAddStudentsCsv(uri);
                } else if (requestCode == CSV_REQ_MODULES) {
                    parseAndAddModulesCsv(uri);
                }
            }
        }
    }

    private void parseAndAddStudentsCsv(android.net.Uri uri) {
        Toast.makeText(this, "Importing students from CSV...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                String line;
                
                // Read header row
                reader.readLine(); 
                
                int count = 0;
                int duplicates = 0;
                List<TempStudent> parsedStudents = new java.util.ArrayList<>();
                
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].replaceAll("^\"|\"$", "").trim();
                    }
                    
                    if (tokens.length >= 3) {
                        String sId = tokens[0].trim();
                        String name = tokens[1].trim();
                        String email = tokens[2].trim();
                        if (sId.isEmpty() || name.isEmpty() || email.isEmpty() || sId.equalsIgnoreCase("Student ID")) {
                            continue;
                        }
                        
                        String pwd = "123456"; // Default
                        if (tokens.length >= 4 && !tokens[3].trim().isEmpty()) {
                            pwd = tokens[3].trim();
                        }
                        
                        String status = "active"; // Default
                        if (tokens.length >= 5 && !tokens[4].trim().isEmpty()) {
                            status = tokens[4].trim().toLowerCase();
                        }
                        
                        // Check local duplicate
                        boolean isDuplicate = false;
                        for (TempStudent ts : localStudentsList) {
                            if (ts.studentId.equalsIgnoreCase(sId)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        for (TempStudent ts : parsedStudents) {
                            if (ts.studentId.equalsIgnoreCase(sId)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        
                        if (!isDuplicate) {
                            parsedStudents.add(new TempStudent(sId, name, email, pwd, status));
                            count++;
                        } else {
                            duplicates++;
                        }
                    }
                }
                
                final int finalCount = count;
                final int finalDuplicates = duplicates;
                runOnUiThread(() -> {
                    localStudentsList.addAll(parsedStudents);
                    refreshStudentsListUI();
                    if (finalDuplicates > 0) {
                        Toast.makeText(this, "Imported " + finalCount + " students (" + finalDuplicates + " duplicates skipped)", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Successfully imported " + finalCount + " students", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to parse students: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void parseAndAddModulesCsv(android.net.Uri uri) {
        if (spinnerModuleSemester.getSelectedItem() == null) {
            Toast.makeText(this, "Error: No semesters configured/generated yet!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String selectedSem = spinnerModuleSemester.getSelectedItem().toString();
        Toast.makeText(this, "Importing modules from CSV...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                String line;
                
                // Read header row
                reader.readLine();
                
                int count = 0;
                int duplicates = 0;
                List<TempModule> parsedModules = new java.util.ArrayList<>();
                
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].replaceAll("^\"|\"$", "").trim();
                    }
                    
                    if (tokens.length >= 2) {
                        String code = tokens[0].trim();
                        String name = tokens[1].trim();
                        if (code.isEmpty() || name.isEmpty() || code.equalsIgnoreCase("Module Code") || code.equalsIgnoreCase("Course Code")) {
                            continue;
                        }
                        
                        String credits = "3"; // Default credits
                        if (tokens.length >= 3 && !tokens[2].trim().isEmpty()) {
                            credits = tokens[2].trim();
                        }
                        
                        String semester = selectedSem;
                        if (tokens.length >= 4 && !tokens[3].trim().isEmpty()) {
                            String maybeSem = tokens[3].trim();
                            // Validate that maybeSem is a valid semester in our generated list
                            boolean validSem = false;
                            for (TempSemester ts : generatedSemestersList) {
                                if (ts.id.equalsIgnoreCase(maybeSem)) {
                                    semester = ts.id;
                                    validSem = true;
                                    break;
                                }
                            }
                            if (!validSem) {
                                // Default to selected semester if CSV semester is invalid/not generated
                                semester = selectedSem;
                            }
                        }
                        
                        // Check local duplicate in both localModulesList and newly parsed ones
                        boolean isDuplicate = false;
                        for (TempModule tm : localModulesList) {
                            if (tm.moduleCode.equalsIgnoreCase(code) && tm.semesterId.equalsIgnoreCase(semester)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        for (TempModule tm : parsedModules) {
                            if (tm.moduleCode.equalsIgnoreCase(code) && tm.semesterId.equalsIgnoreCase(semester)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        
                        if (!isDuplicate) {
                            parsedModules.add(new TempModule(semester, code, name, credits));
                            count++;
                        } else {
                            duplicates++;
                        }
                    }
                }
                
                final int finalCount = count;
                final int finalDuplicates = duplicates;
                runOnUiThread(() -> {
                    localModulesList.addAll(parsedModules);
                    refreshModulesListUI();
                    if (finalDuplicates > 0) {
                        Toast.makeText(this, "Imported " + finalCount + " modules (" + finalDuplicates + " duplicates skipped)", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Successfully imported " + finalCount + " modules", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to parse modules: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

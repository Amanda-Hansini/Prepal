package com.example.finalyearprojectnew;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.finalyearprojectnew.models.PredictionRequest;
import com.example.finalyearprojectnew.models.PredictionResponse;
import com.example.finalyearprojectnew.network.RetrofitClient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstSemesterQuizActivity extends AppCompatActivity {

    private TextView tvQuestionCount, tvQuestionText, tvQuestionContext, tvRatingValue, tvLabelStart, tvLabelEnd;
    private ProgressBar quizProgress;
    private EditText etQuizInput;
    private LinearLayout llRatingContainer, llAttendance, llPss, llCaMarks;
    private ScrollView svAttendance, svPss, svCaMarks;
    private List<EditText> midMarkInputs = new ArrayList<>();
    private List<EditText> assgMarkInputs = new ArrayList<>();
    private SeekBar seekBarRating;
    private AppCompatButton btnPrev, btnNext;

    private int currentQuestionIndex = 0;
    private String studentId, semesterName, semesterDocId, programId;
    private int targetStudyHours = 0;
    private List<Map<String, Object>> studentResults = new ArrayList<>();

    private List<String> questions = new ArrayList<>();
    private List<String> contexts = new ArrayList<>();
    // questionTypes: 0=Dynamic Attendance, 1=Numeric, 2=Rating (not used anymore), 3=Dynamic PSS, 4=O/L Grades
    private List<Integer> questionTypes = new ArrayList<>();
    private List<Double> answers = new ArrayList<>();
    
    private List<String> moduleNamesForAttendance = new ArrayList<>();
    private List<String> moduleDisplayNamesForAttendance = new ArrayList<>();
    private Map<String, Double> moduleAttendances = new HashMap<>();
    private List<Spinner> attendanceSpinners = new ArrayList<>();
    
    private String[] pssQuestions = {
        "1. In the last month, how often have you been upset because of something that happened unexpectedly?",
        "2. In the last month, how often have you felt that you were unable to control the important things in your life?",
        "3. In the last month, how often have you felt nervous and \"stressed\"?",
        "4. In the last month, how often have you felt confident about your ability to handle your personal problems?",
        "5. In the last month, how often have you felt that things were going your way?",
        "6. In the last month, how often have you found that you could not cope with all the things that you had to do?",
        "7. In the last month, how often have you been able to control irritations in your life?",
        "8. In the last month, how often have you felt that you were on top of things?",
        "9. In the last month, how often have you been angered because of things that were outside of your control?",
        "10. In the last month, how often have you felt difficulties were piling up so high that you could not overcome them?"
    };
    private List<Spinner> pssSpinners = new ArrayList<>();
    private int pssTotalScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_semester_quiz);

        semesterDocId = getIntent().getStringExtra("semesterDocId");
        semesterName = getIntent().getStringExtra("semesterName");
        programId = getIntent().getStringExtra("programId");
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "Unknown");

        initViews();
        setupListeners();
        fetchModulesFromDatabase();
    }

    private void fetchModulesFromDatabase() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Loading Semester Modules...");
        dialog.setCancelable(false);
        dialog.show();

        String batchId = getIntent().getStringExtra("batchId");
        if (batchId == null) batchId = "";
        
        if (programId == null || programId.trim().isEmpty()) {
            programId = "BIT";
        }
        if (semesterName == null || semesterName.trim().isEmpty()) {
            semesterName = "SEM01";
        }

        final String finalBatchId = batchId;

        FirebaseFirestore.getInstance().collection("Degrees").document(programId)
                .collection("Modules")
                .whereEqualTo("semesterId", semesterName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dialog.dismiss();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Map<String, Object>> allModules = new ArrayList<>();
                        List<Map<String, Object>> matchingModules = new ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                allModules.add(data);
                                String semBatchId = (String) data.get("batchId");
                                String semBatchName = (String) data.get("batchName");
                                if (!finalBatchId.isEmpty()) {
                                    if (finalBatchId.equalsIgnoreCase(semBatchId) || finalBatchId.equalsIgnoreCase(semBatchName)) {
                                        matchingModules.add(data);
                                    }
                                }
                            }
                        }

                        List<Map<String, Object>> modulesToUse = !matchingModules.isEmpty() ? matchingModules : allModules;

                        studentResults = modulesToUse;
                        buildQuestionsList();
                        populateDynamicCaMarksList();
                        populateDynamicAttendanceList();
                        updateQuestion();
                    } else {
                        Toast.makeText(this, "No modules found for " + semesterName, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Failed to load modules: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void buildQuestionsList() {
        int totalCredits = 0;
        
        // Populate module names for attendance
        if (studentResults != null) {
            for (Map<String, Object> module : studentResults) {
                String moduleName = "";
                Object mNameObj = module.get("module_name");
                if (mNameObj == null) mNameObj = module.get("moduleName");
                if (mNameObj != null) moduleName = mNameObj.toString();

                String moduleId = "";
                Object mIdObj = module.get("module_id");
                if (mIdObj == null) mIdObj = module.get("moduleId");
                if (mIdObj != null) moduleId = mIdObj.toString();

                moduleNamesForAttendance.add(moduleName);
                
                if (!moduleId.isEmpty() && !moduleName.isEmpty()) {
                    moduleDisplayNamesForAttendance.add(moduleId + " - " + moduleName);
                } else {
                    moduleDisplayNamesForAttendance.add(moduleName);
                }
                
                Object creditsObj = module.get("credits");
                if (creditsObj != null) {
                    try {
                        totalCredits += (int) Double.parseDouble(creditsObj.toString());
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // Step 1: Continuous Assessment Marks (Dynamic List: Midterm + Assignment)
        questions.add("Enter your Midterm & Assignment marks for each module:");
        contexts.add("Continuous Assessment (CA = Midterm /20 + Assignment /20 = /40) makes up 40% of your final grade. A CA total below 8/40 bars you from the final exam.");
        questionTypes.add(5);
        answers.add(0.0); // Placeholder

        // Step 2: Attendance (Dynamic List)
        questions.add("Select your target attendance for each module:");
        contexts.add("Saegis Campus By-Laws require a minimum of 80% attendance to be eligible for end-semester examinations.");
        questionTypes.add(0);
        answers.add(0.0); // Placeholder
        
        int totalNotionalHours = totalCredits * 50;
        int weeklyStudyTarget = totalNotionalHours / 15; // standard 15 week semester

        // Store target for later so we can save it to history
        this.targetStudyHours = weeklyStudyTarget;

        // Step 3: Study Hours
        questions.add("How many hours per week do you realistically commit to focused self-study?");
        contexts.add("Based on SLQF, your " + totalCredits + " registered credits require " + totalNotionalHours + " notional hours. This equals roughly " + weeklyStudyTarget + " hours of self-study per week.");
        questionTypes.add(1);
        answers.add(0.0);

        // Step 4: Sleep
        questions.add("On average, how many hours of consistent, uninterrupted sleep do you get per night?");
        contexts.add("Research indicates that memory consolidation degrades significantly if sleep schedules are restricted or highly erratic.");
        questionTypes.add(1);
        answers.add(0.0);
        
        // Step 5: PSS-10 (Dynamic List)
        questions.add("Perceived Stress Scale (PSS-10)");
        contexts.add("Please answer the following questions based on your feelings in the last month.");
        questionTypes.add(3);
        answers.add(0.0);
        
        quizProgress.setMax(questions.size());
    }

    private void initViews() {
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvQuestionContext = findViewById(R.id.tvQuestionContext);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        tvLabelStart = findViewById(R.id.tvLabelStart);
        tvLabelEnd = findViewById(R.id.tvLabelEnd);
        quizProgress = findViewById(R.id.quizProgress);
        etQuizInput = findViewById(R.id.etQuizInput);
        llRatingContainer = findViewById(R.id.llRatingContainer);
        seekBarRating = findViewById(R.id.seekBarRating);
        svAttendance = findViewById(R.id.svAttendance);
        llAttendance = findViewById(R.id.llAttendance);
        svPss = findViewById(R.id.svPss);
        llPss = findViewById(R.id.llPss);
        svCaMarks = findViewById(R.id.svCaMarks);
        llCaMarks = findViewById(R.id.llCaMarks);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        populateDynamicPssList();
    }

    private void populateDynamicCaMarksList() {
        llCaMarks.removeAllViews();
        midMarkInputs.clear();
        assgMarkInputs.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String displayName : moduleDisplayNamesForAttendance) {
            View itemView = inflater.inflate(R.layout.item_module_ca_marks, llCaMarks, false);
            TextView tvModuleNameCa = itemView.findViewById(R.id.tvModuleNameCa);
            EditText etMidMark = itemView.findViewById(R.id.etMidMark);
            EditText etAssignmentMark = itemView.findViewById(R.id.etAssignmentMark);

            tvModuleNameCa.setText(displayName);

            midMarkInputs.add(etMidMark);
            assgMarkInputs.add(etAssignmentMark);
            llCaMarks.addView(itemView);
        }
    }

    private void populateDynamicAttendanceList() {
        llAttendance.removeAllViews();
        attendanceSpinners.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        String[] attendanceOptions = {"90% - 100%", "80% - 89%", "70% - 79%", "Below 70%"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, attendanceOptions);

        for (String displayName : moduleDisplayNamesForAttendance) {
            View itemView = inflater.inflate(R.layout.item_module_attendance, llAttendance, false);
            TextView tvModuleName = itemView.findViewById(R.id.tvModuleName);
            Spinner spinnerAttendance = itemView.findViewById(R.id.spinnerAttendance);
            
            tvModuleName.setText(displayName);
            spinnerAttendance.setAdapter(adapter);
            
            attendanceSpinners.add(spinnerAttendance);
            llAttendance.addView(itemView);
        }
    }

    private void populateDynamicPssList() {
        llPss.removeAllViews();
        pssSpinners.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        String[] pssOptions = {"Never (0)", "Almost Never (1)", "Sometimes (2)", "Fairly Often (3)", "Very Often (4)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pssOptions);

        for (String pssQuestion : pssQuestions) {
            View itemView = inflater.inflate(R.layout.item_pss_question, llPss, false);
            TextView tvPssQuestion = itemView.findViewById(R.id.tvPssQuestion);
            Spinner spinnerPss = itemView.findViewById(R.id.spinnerPss);
            
            tvPssQuestion.setText(pssQuestion);
            spinnerPss.setAdapter(adapter);
            
            pssSpinners.add(spinnerPss);
            llPss.addView(itemView);
        }
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (saveAnswer()) {
                if (currentQuestionIndex < questions.size() - 1) {
                    currentQuestionIndex++;
                    updateQuestion();
                } else {
                    submitResults();
                }
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                updateQuestion();
            }
        });
    }

    private void updateQuestion() {
        tvQuestionCount.setText("Step " + (currentQuestionIndex + 1) + " of " + questions.size());
        tvQuestionText.setText(questions.get(currentQuestionIndex));
        
        String contextStr = contexts.get(currentQuestionIndex);
        if (contextStr != null && !contextStr.isEmpty()) {
            tvQuestionContext.setVisibility(View.VISIBLE);
            tvQuestionContext.setText(contextStr);
        } else {
            tvQuestionContext.setVisibility(View.GONE);
        }
        
        quizProgress.setProgress(currentQuestionIndex + 1);

        int type = questionTypes.get(currentQuestionIndex);
        if (type == 5) {
            // Dynamic List (Continuous Assessment: Midterm + Assignment)
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.GONE);
            svAttendance.setVisibility(View.GONE);
            svPss.setVisibility(View.GONE);
            svCaMarks.setVisibility(View.VISIBLE);
        } else if (type == 0) {
            // Dynamic List (Attendance)
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.GONE);
            svPss.setVisibility(View.GONE);
            svCaMarks.setVisibility(View.GONE);
            svAttendance.setVisibility(View.VISIBLE);
        } else if (type == 3) {
            // Dynamic List (PSS-10)
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.GONE);
            svAttendance.setVisibility(View.GONE);
            svCaMarks.setVisibility(View.GONE);
            svPss.setVisibility(View.VISIBLE);
        } else {
            // Numeric Input (Study Hours, Sleep Hours)
            etQuizInput.setVisibility(View.VISIBLE);
            llRatingContainer.setVisibility(View.GONE);
            svAttendance.setVisibility(View.GONE);
            svPss.setVisibility(View.GONE);
            svCaMarks.setVisibility(View.GONE);
            double ans = answers.get(currentQuestionIndex);
            etQuizInput.setText(ans > 0 ? String.valueOf(ans) : "");
            etQuizInput.requestFocus();
        }

        btnPrev.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentQuestionIndex == questions.size() - 1 ? "Get Prediction" : "Continue");
    }

    private double mapDropdownToPercentage(int position) {
        switch (position) {
            case 0: return 95.0;
            case 1: return 85.0;
            case 2: return 75.0;
            case 3: return 65.0;
            default: return 95.0;
        }
    }

    private boolean saveAnswer() {
        try {
            int type = questionTypes.get(currentQuestionIndex);
            if (type == 5) {
                // Save from CA Marks inputs into studentResults
                for (int i = 0; i < studentResults.size(); i++) {
                    String midStr = i < midMarkInputs.size() ? midMarkInputs.get(i).getText().toString().trim() : "0";
                    String assgStr = i < assgMarkInputs.size() ? assgMarkInputs.get(i).getText().toString().trim() : "0";
                    double midMark = midStr.isEmpty() ? 0.0 : Double.parseDouble(midStr);
                    double assgMark = assgStr.isEmpty() ? 0.0 : Double.parseDouble(assgStr);

                    if (midMark < 0 || midMark > 20) {
                        Toast.makeText(this, "Midterm mark for module " + (i+1) + " must be between 0 and 20", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (assgMark < 0 || assgMark > 20) {
                        Toast.makeText(this, "Assignment mark for module " + (i+1) + " must be between 0 and 20", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    Map<String, Object> mod = studentResults.get(i);
                    mod.put("mid_mark", midMark);
                    mod.put("midMark", midMark);
                    mod.put("assignment_mark", assgMark);
                    mod.put("assignmentMark", assgMark);
                }
            } else if (type == 0) {
                // Save from Attendance Spinners into map
                for (int i = 0; i < moduleNamesForAttendance.size(); i++) {
                    double pct = mapDropdownToPercentage(attendanceSpinners.get(i).getSelectedItemPosition());
                    moduleAttendances.put(moduleNamesForAttendance.get(i), pct);
                }
            } else if (type == 3) {
                // Save from PSS Spinners is done directly during submitResults
            } else if (type == 4) {
                // Save from O/L Grades is done directly during submitResults
            } else {
                String input = etQuizInput.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                double val = Double.parseDouble(input);
                answers.set(currentQuestionIndex, val);
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void submitResults() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating AI Academic Forecast...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Calculate Average Attendance from Map
        double totalAttendance = 0.0;
        for (Double val : moduleAttendances.values()) {
            totalAttendance += val;
        }
        double avgAttendance = moduleAttendances.size() > 0 ? totalAttendance / moduleAttendances.size() : 0.0;
        
        // Step 3 and 4 are Study and Sleep Hours (index 2 and 3)
        double studyHours = answers.get(2);
        double sleepHours = answers.get(3);
        
        // Calculate PSS-10 Score
        pssTotalScore = 0;
        for (int i = 0; i < pssSpinners.size(); i++) {
            int score = pssSpinners.get(i).getSelectedItemPosition(); // 0-4 mapping is direct!
            
            // Reverse score for questions 4, 5, 7, 8 (array index 3, 4, 6, 7)
            if (i == 3 || i == 4 || i == 6 || i == 7) {
                score = 4 - score;
            }
            pssTotalScore += score;
        }

        // Map PSS-10 (0-40) to Model's expected Stress Level (1-5)
        double mappedStressLevel = 1.0;
        if (pssTotalScore <= 8) mappedStressLevel = 1.0;
        else if (pssTotalScore <= 16) mappedStressLevel = 2.0;
        else if (pssTotalScore <= 24) mappedStressLevel = 3.0;
        else if (pssTotalScore <= 32) mappedStressLevel = 4.0;
        else mappedStressLevel = 5.0;

        PredictionRequest request = new PredictionRequest();
        request.studentId = studentId;
        request.attendance = avgAttendance; 
        request.moduleAttendances = moduleAttendances;
        request.studyHours = studyHours;
        request.sleepHours = sleepHours;
        double passedCgpa = getIntent().getDoubleExtra("cumulativeGpa", 0.0);
        int passedStudentType = getIntent().getIntExtra("studentType", 2);

        request.gpa = passedCgpa;
        request.cgpa = passedCgpa;
        request.results = studentResults;
        request.studentType = passedStudentType;
        request.olMaths = "";
        request.olEnglish = "";

        RetrofitClient.getApiService().predictGpa(request).enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    saveToHistoryAndNavigate(response.body(), request);
                } else {
                    Toast.makeText(FirstSemesterQuizActivity.this, "AI Analysis Failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(FirstSemesterQuizActivity.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveToHistoryAndNavigate(PredictionResponse response, PredictionRequest requestData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Save Prediction Snapshot (Structured under Student ID)
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("studentId", studentId);
        historyData.put("timestamp", com.google.firebase.Timestamp.now());
        historyData.put("currentSemGpa", response.semesterGpa);
        historyData.put("predictedGpa", response.predictedFutureGpa);
        historyData.put("motivationTip", response.motivationTip);
        historyData.put("eligible", response.eligible);
        historyData.put("semesterName", semesterName);
        
        // Save the inputs that caused this prediction
        historyData.put("attendance", requestData.attendance);
        historyData.put("moduleAttendances", requestData.moduleAttendances);
        historyData.put("studyHours", requestData.studyHours);
        historyData.put("targetStudyHours", this.targetStudyHours); // Save SLQF target!
        historyData.put("sleepHours", requestData.sleepHours);
        historyData.put("stressLevel", requestData.stressLevel); // 1-5 scale
        historyData.put("pssScore", pssTotalScore); // Raw 0-40 scale
        historyData.put("acknowledgementsRequired", response.acknowledgementsRequired);

        db.collection("AllStudents").document(studentId)
                .collection("PredictionHistory").add(historyData)
                .addOnSuccessListener(documentReference -> {
                    db.collection("AllStudents").document(studentId).update("resultsEntered", true);
                    
                    AcknowledgementDialogHelper.showWarningDialog(FirstSemesterQuizActivity.this, response.acknowledgementsRequired, () -> {
                        Intent intent = new Intent(FirstSemesterQuizActivity.this, StudentHomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to persist data", Toast.LENGTH_SHORT).show();
                    AcknowledgementDialogHelper.showWarningDialog(FirstSemesterQuizActivity.this, response.acknowledgementsRequired, () -> {
                        startActivity(new Intent(FirstSemesterQuizActivity.this, StudentHomeActivity.class));
                        finish();
                    });
                });
    }
}

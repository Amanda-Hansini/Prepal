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

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionCount, tvQuestionText, tvQuestionContext, tvRatingValue, tvLabelStart, tvLabelEnd;
    private ProgressBar quizProgress;
    private EditText etQuizInput;
    private LinearLayout llRatingContainer, llAttendance, llPss;
    private ScrollView svAttendance, svPss;
    private SeekBar seekBarRating;
    private AppCompatButton btnPrev, btnNext;

    private int currentQuestionIndex = 0;
    private double currentGpa, cumulativeGpa;
    private List<Map<String, Object>> studentResults;
    private String studentId, semesterName, programId, batchId;
    private int targetStudyHours = 0;

    private List<String> questions = new ArrayList<>();
    private List<String> contexts = new ArrayList<>();
    // questionTypes: 0=Dynamic Attendance, 1=Numeric, 2=Rating (not used anymore), 3=Dynamic PSS
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
        setContentView(R.layout.activity_quiz);

        cumulativeGpa = getIntent().getDoubleExtra("cumulativeGpa", 0.0);
        currentGpa = getIntent().getDoubleExtra("semesterGpa", 0.0);
        studentResults = (List<Map<String, Object>>) getIntent().getSerializableExtra("results");
        semesterName = getIntent().getStringExtra("semesterName");
        programId = getIntent().getStringExtra("programId");
        batchId = getIntent().getStringExtra("batchId");
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "Unknown");

        initViews();
        setupListeners();

        if (studentResults != null && !studentResults.isEmpty()) {
            buildQuestionsList();
            populateDynamicAttendanceList();
            quizProgress.setMax(questions.size());
            updateQuestion();
        } else {
            fetchModulesFromDatabase();
        }
    }

    private void fetchModulesFromDatabase() {
        android.app.ProgressDialog dialog = new android.app.ProgressDialog(this);
        dialog.setMessage("Loading Semester Modules...");
        dialog.setCancelable(false);
        dialog.show();

        if (batchId == null) batchId = "";
        
        if (programId == null || programId.trim().isEmpty()) {
            programId = "BIT";
        }
        if (semesterName == null || semesterName.trim().isEmpty()) {
            semesterName = "SEM01";
        }

        final String finalBatchId = batchId;

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Degrees").document(programId)
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
                        populateDynamicAttendanceList();
                        quizProgress.setMax(questions.size());
                        updateQuestion();
                    } else {
                        android.widget.Toast.makeText(this, "No modules found for " + semesterName, android.widget.Toast.LENGTH_LONG).show();
                        buildQuestionsList();
                        populateDynamicAttendanceList();
                        quizProgress.setMax(questions.size());
                        updateQuestion();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    android.widget.Toast.makeText(this, "Failed to load modules: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    buildQuestionsList();
                    populateDynamicAttendanceList();
                    quizProgress.setMax(questions.size());
                    updateQuestion();
                });
    }

    private void buildQuestionsList() {
        questions.clear();
        contexts.clear();
        questionTypes.clear();
        answers.clear();
        moduleNamesForAttendance.clear();
        moduleDisplayNamesForAttendance.clear();
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
        
        // Step 1: Attendance (Dynamic List)
        questions.add("Select your target attendance for each module:");
        contexts.add("Saegis Campus By-Laws require a minimum of 80% attendance to be eligible for end-semester examinations.");
        questionTypes.add(0);
        answers.add(0.0); // Placeholder
        
        int totalNotionalHours = totalCredits * 50;
        int studyHoursPerWeek = (int) Math.round((totalNotionalHours * 0.30) / 15.0);
        if (studyHoursPerWeek <= 0) studyHoursPerWeek = 10;
        targetStudyHours = studyHoursPerWeek;

        // Step 2: Numeric (Study Hours)
        questions.add("How many hours per week do you realistically commit to focused self-study?");
        contexts.add("Based on your course load (" + totalCredits + " credits), we recommend aiming for at least " + targetStudyHours + " hours per week.");
        questionTypes.add(1);
        answers.add(0.0);
        
        // Step 3: Numeric (Sleep Hours)
        questions.add("On average, how many hours of consistent, uninterrupted sleep do you get per night?");
        contexts.add("Adequate sleep (7-9 hours) is vital for cognitive retention, academic performance, and overall well-being.");
        questionTypes.add(1);
        answers.add(0.0);
        
        // Step 4: PSS-10
        questions.add("Perceived Stress Scale (PSS-10)");
        contexts.add("Please answer the following 10 questions to evaluate your stress level over the last month.");
        questionTypes.add(3);
        answers.add(0.0);
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
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        
        quizProgress.setMax(4);
        populateDynamicPssList();
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
        if (type == 0) {
            // Dynamic List (Attendance)
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.GONE);
            svPss.setVisibility(View.GONE);
            svAttendance.setVisibility(View.VISIBLE);
        } else if (type == 3) {
            // Dynamic List (PSS-10)
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.GONE);
            svAttendance.setVisibility(View.GONE);
            svPss.setVisibility(View.VISIBLE);
        } else {
            // Numeric Input (Study Hours, Sleep Hours)
            etQuizInput.setVisibility(View.VISIBLE);
            llRatingContainer.setVisibility(View.GONE);
            svAttendance.setVisibility(View.GONE);
            svPss.setVisibility(View.GONE);
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
            if (type == 0) {
                // Save from Attendance Spinners into map
                for (int i = 0; i < moduleNamesForAttendance.size(); i++) {
                    double pct = mapDropdownToPercentage(attendanceSpinners.get(i).getSelectedItemPosition());
                    moduleAttendances.put(moduleNamesForAttendance.get(i), pct);
                }
            } else if (type == 3) {
                // Save from PSS Spinners is done directly during submitResults
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
        
        // Step 2 and 3 are Study and Sleep Hours
        double studyHours = answers.get(1);
        double sleepHours = answers.get(2);
        
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
        request.stressLevel = mappedStressLevel;
        request.gpa = currentGpa; 
        request.cgpa = cumulativeGpa;
        request.results = studentResults;

        if (cumulativeGpa > 0 && studentResults != null && !studentResults.isEmpty()) {
            request.studentType = 3; // Model C: Comprehensive Master
        } else if (cumulativeGpa > 0) {
            request.studentType = 1; // Model A: Pre-Semester Baseline
        } else {
            request.studentType = 2; // Model B: Mid-Semester Fresher
        }

        RetrofitClient.getApiService().predictGpa(request).enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    saveToHistoryAndNavigate(response.body(), request);
                } else {
                    Toast.makeText(QuizActivity.this, "AI Analysis Failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(QuizActivity.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
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
                    
                    AcknowledgementDialogHelper.showWarningDialog(QuizActivity.this, response.acknowledgementsRequired, () -> {
                        Intent intent = new Intent(QuizActivity.this, StudentHomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to persist data", Toast.LENGTH_SHORT).show();
                    AcknowledgementDialogHelper.showWarningDialog(QuizActivity.this, response.acknowledgementsRequired, () -> {
                        startActivity(new Intent(QuizActivity.this, StudentHomeActivity.class));
                        finish();
                    });
                });
    }
}

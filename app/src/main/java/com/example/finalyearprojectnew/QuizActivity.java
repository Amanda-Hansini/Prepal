package com.example.finalyearprojectnew;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.finalyearprojectnew.models.PredictionRequest;
import com.example.finalyearprojectnew.models.PredictionResponse;
import com.example.finalyearprojectnew.network.RetrofitClient;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private LinearLayout llRatingContainer;
    private SeekBar seekBarRating;
    private AppCompatButton btnPrev, btnNext;

    private int currentQuestionIndex = 0;
    private double currentGpa, cumulativeGpa;
    private List<Map<String, Object>> studentResults;
    private String studentId, semesterName;

    private String[] questions = {
            "What is your target overall average attendance percentage across all modules? (0-100)",
            "How many hours per week do you realistically commit to focused self-study?",
            "On average, how many hours of consistent, uninterrupted sleep do you get per night?",
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

    private String[] contexts = new String[13];
    private double[] answers = new double[13];
    private boolean[] isRatingQuestion = {
            false, false, false,
            true, true, true, true, true, true, true, true, true, true
    };
    
    private int pssTotalScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        cumulativeGpa = getIntent().getDoubleExtra("cumulativeGpa", 0.0);
        currentGpa = getIntent().getDoubleExtra("semesterGpa", 0.0);
        studentResults = (List<Map<String, Object>>) getIntent().getSerializableExtra("results");
        semesterName = getIntent().getStringExtra("semesterName");
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "Unknown");

        calculateContexts();
        initViews();
        setupListeners();
        updateQuestion();
    }

    private void calculateContexts() {
        int totalCredits = 0;
        if (studentResults != null) {
            for (Map<String, Object> module : studentResults) {
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
        
        int totalNotionalHours = totalCredits * 50;
        int weeklyStudyTarget = totalNotionalHours / 15; // standard 15 week semester

        contexts[0] = "Saegis Campus By-Laws require a minimum of 80% attendance to be eligible for end-semester examinations.";
        contexts[1] = "Based on SLQF, your " + totalCredits + " registered credits require " + totalNotionalHours + " notional hours. This equals roughly " + weeklyStudyTarget + " hours of self-study per week.";
        contexts[2] = "Research indicates that memory consolidation degrades significantly if sleep schedules are restricted or highly erratic.";
        
        for(int i = 3; i < 13; i++) {
            contexts[i] = "Perceived Stress Scale (PSS-10). Answer based on your feelings in the last month.";
        }
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
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        
        quizProgress.setMax(questions.length);
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (saveAnswer()) {
                if (currentQuestionIndex < questions.length - 1) {
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

        seekBarRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRatingValue.setText(getPssLabel(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private String getPssLabel(int progress) {
        switch (progress) {
            case 0: return "Never (0)";
            case 1: return "Almost Never (1)";
            case 2: return "Sometimes (2)";
            case 3: return "Fairly Often (3)";
            case 4: return "Very Often (4)";
            default: return "";
        }
    }

    private void updateQuestion() {
        tvQuestionCount.setText("Step " + (currentQuestionIndex + 1) + " of " + questions.length);
        tvQuestionText.setText(questions[currentQuestionIndex]);
        
        if (contexts[currentQuestionIndex] != null && !contexts[currentQuestionIndex].isEmpty()) {
            tvQuestionContext.setVisibility(View.VISIBLE);
            tvQuestionContext.setText(contexts[currentQuestionIndex]);
        } else {
            tvQuestionContext.setVisibility(View.GONE);
        }
        
        quizProgress.setProgress(currentQuestionIndex + 1);

        if (isRatingQuestion[currentQuestionIndex]) {
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.VISIBLE);
            seekBarRating.setMax(4);
            seekBarRating.setProgress((int) answers[currentQuestionIndex]);
            tvRatingValue.setText(getPssLabel(seekBarRating.getProgress()));
            tvLabelStart.setText("Never");
            tvLabelEnd.setText("Very Often");
        } else {
            etQuizInput.setVisibility(View.VISIBLE);
            llRatingContainer.setVisibility(View.GONE);
            etQuizInput.setText(answers[currentQuestionIndex] > 0 ? String.valueOf(answers[currentQuestionIndex]) : "");
            etQuizInput.requestFocus();
        }

        btnPrev.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentQuestionIndex == questions.length - 1 ? "Get Prediction" : "Continue");
    }

    private boolean saveAnswer() {
        try {
            if (isRatingQuestion[currentQuestionIndex]) {
                answers[currentQuestionIndex] = seekBarRating.getProgress();
            } else {
                String input = etQuizInput.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
                    return false;
                }
                double val = Double.parseDouble(input);
                if (currentQuestionIndex == 0 && (val < 0 || val > 100)) {
                    Toast.makeText(this, "Attendance must be between 0 and 100", Toast.LENGTH_SHORT).show();
                    return false;
                }
                answers[currentQuestionIndex] = val;
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

        // Calculate PSS-10 Score
        pssTotalScore = 0;
        for (int i = 3; i < 13; i++) {
            int score = (int) answers[i];
            // Reverse score for questions 4, 5, 7, 8 (Indices 6, 7, 9, 10 in array)
            if (i == 6 || i == 7 || i == 9 || i == 10) {
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
        request.attendance = answers[0];
        request.studyHours = answers[1];
        request.sleepHours = answers[2];
        request.stressLevel = mappedStressLevel;
        request.gpa = currentGpa; 
        request.results = studentResults;

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
        
        // 1. Save Semester Results to HISTORY
        Map<String, Object> semesterData = new HashMap<>();
        semesterData.put("semesterName", semesterName);
        semesterData.put("semesterGpa", response.semesterGpa);
        semesterData.put("modules", requestData.results);
        semesterData.put("timestamp", com.google.firebase.Timestamp.now());
        semesterData.put("abModules", response.abModules);
        semesterData.put("mcModules", response.mcModules);
        semesterData.put("neModules", response.neModules);

        db.collection("AllStudents").document(studentId)
                .collection("SemesterResults").document(semesterName)
                .set(semesterData);

        // 2. Save Prediction Snapshot (Structured under Student ID)
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
        historyData.put("studyHours", requestData.studyHours);
        historyData.put("sleepHours", requestData.sleepHours);
        historyData.put("stressLevel", requestData.stressLevel); // 1-5 scale
        historyData.put("pssScore", pssTotalScore); // Raw 0-40 scale

        db.collection("AllStudents").document(studentId)
                .collection("PredictionHistory").add(historyData)
                .addOnSuccessListener(documentReference -> {
                    db.collection("AllStudents").document(studentId).update("resultsEntered", true);
                    
                    Intent intent = new Intent(QuizActivity.this, StudentHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to persist data", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(QuizActivity.this, StudentHomeActivity.class));
                    finish();
                });
    }
}

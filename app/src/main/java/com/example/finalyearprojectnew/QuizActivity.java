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

    private TextView tvQuestionCount, tvQuestionText, tvRatingValue;
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
            "What is your overall attendance percentage? (0-100%)",
            "How many hours per week do you spend studying?",
            "How many hours of sleep do you get per day?",
            "On a scale of 1-5, how would you rate your stress level?"
    };

    private double[] answers = new double[4];
    private boolean[] isRatingQuestion = {false, false, false, true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        cumulativeGpa = getIntent().getDoubleExtra("cumulativeGpa", 0.0);
        currentGpa = getIntent().getDoubleExtra("semesterGpa", 0.0);
        studentResults = (List<Map<String, Object>>) getIntent().getSerializableExtra("results");
        semesterName = getIntent().getStringExtra("semesterName");
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "Unknown");

        initViews();
        setupListeners();
        updateQuestion();
    }

    private void initViews() {
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvRatingValue = findViewById(R.id.tvRatingValue);
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
                tvRatingValue.setText(String.valueOf(progress + 1));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateQuestion() {
        tvQuestionCount.setText("Step " + (currentQuestionIndex + 1) + " of " + questions.length);
        tvQuestionText.setText(questions[currentQuestionIndex]);
        quizProgress.setProgress(currentQuestionIndex + 1);

        if (isRatingQuestion[currentQuestionIndex]) {
            etQuizInput.setVisibility(View.GONE);
            llRatingContainer.setVisibility(View.VISIBLE);
            seekBarRating.setProgress((int) answers[currentQuestionIndex] > 0 ? (int) answers[currentQuestionIndex] - 1 : 2);
            tvRatingValue.setText(String.valueOf(seekBarRating.getProgress() + 1));
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
                answers[currentQuestionIndex] = seekBarRating.getProgress() + 1;
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

        PredictionRequest request = new PredictionRequest();
        request.studentId = studentId;
        request.attendance = answers[0];
        request.studyHours = answers[1];
        request.sleepHours = answers[2];
        request.stressLevel = answers[3];
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

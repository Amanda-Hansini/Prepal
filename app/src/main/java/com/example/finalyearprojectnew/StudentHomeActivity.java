package com.example.finalyearprojectnew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentHomeActivity extends AppCompatActivity {

    private LineChart lineChartGpa;
    private BarChart barChartPerformance;
    private RecyclerView rvSemesterSelector;
    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private String studentId;
    
    private List<DocumentSnapshot> allSemesters = new ArrayList<>();
    private List<DocumentSnapshot> predictionHistory = new ArrayList<>();
    private DocumentSnapshot currentPredictionDoc;
    private SemesterChipAdapter chipAdapter;
    private double calculatedStudentCgpa = 0.0;
    private boolean hasSpecialStatusGrade = false;
    
    private TextView tvStudentId, tvMotivationTip, tvWelcomeText, tvSpecialStatusNote, tvCurrentDate;
    private TextView tvSemGpa, tvCumGpa, tvPredGpa, btnViewFullReport;
    private TextView tvSemGpaSub, tvCumGpaSub, tvPredGpaSub;
    private androidx.appcompat.widget.AppCompatButton btnRedoPrediction;
    private android.widget.ImageView ivProfile;
    private android.widget.HorizontalScrollView hsvGpaCards;
    private android.widget.ImageView ivScrollIndicator;
    private androidx.cardview.widget.CardView cardPredGpa;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAiAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        db = FirebaseFirestore.getInstance();
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        studentId = prefs.getString("student_id", "STU-0000");
        String studentName = prefs.getString("student_name", "Student");

        initViews();
        tvWelcomeText.setText("Welcome, " + studentName);
        setCurrentDate();
        
        setupBottomNavigation();
        loadStudentAcademicData();
        loadStudentProfileImage();
    }

    private void setCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new java.util.Date()));
    }

    private void initViews() {
        lineChartGpa = findViewById(R.id.lineChartGpa);
        barChartPerformance = findViewById(R.id.barChartPerformance);
        rvSemesterSelector = findViewById(R.id.rvSemesterSelector);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvMotivationTip = findViewById(R.id.tvMotivationTip);
        tvWelcomeText = findViewById(R.id.tvWelcomeText);
        ivProfile = findViewById(R.id.ivProfile);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        
        tvSemGpa = findViewById(R.id.tvSemGpa);
        tvCumGpa = findViewById(R.id.tvCumGpa);
        tvPredGpa = findViewById(R.id.tvPredGpa);
        tvSemGpaSub = findViewById(R.id.tvSemGpaSub);
        tvCumGpaSub = findViewById(R.id.tvCumGpaSub);
        btnRedoPrediction = findViewById(R.id.btnRedoPrediction);
        btnViewFullReport = findViewById(R.id.btnViewFullReport);

        tvStudentId.setText(studentId);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        rvSemesterSelector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(StudentHomeActivity.this, StudentProfileActivity.class));
        });

        btnRedoPrediction.setOnClickListener(v -> {
            checkFirstSemesterAndRedirect();
        });

        btnViewFullReport.setOnClickListener(v -> {
            startActivity(new Intent(StudentHomeActivity.this, PerformanceReportActivity.class));
        });

        fabAiAssistant = findViewById(R.id.fabAiAssistant);
        if (fabAiAssistant != null) {
            fabAiAssistant.setOnClickListener(v -> {
                startActivity(new Intent(StudentHomeActivity.this, AiChatActivity.class));
            });
        }
        
        cardPredGpa = findViewById(R.id.cardPredGpa);
        cardPredGpa.setOnClickListener(v -> showPredictionInsightsDialog());
        
        android.widget.LinearLayout llTapForInsights = findViewById(R.id.llTapForInsights);
        android.animation.ObjectAnimator pulseAnim = android.animation.ObjectAnimator.ofFloat(llTapForInsights, "alpha", 1f, 0.5f, 1f);
        pulseAnim.setDuration(1500);
        pulseAnim.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        pulseAnim.start();
        
        hsvGpaCards = findViewById(R.id.hsvGpaCards);
        ivScrollIndicator = findViewById(R.id.ivScrollIndicator);

        // Bounce animation for the scroll indicator
        android.animation.ObjectAnimator bounceAnim = android.animation.ObjectAnimator.ofFloat(ivScrollIndicator, "translationX", 0f, 15f, 0f);
        bounceAnim.setDuration(1500);
        bounceAnim.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        bounceAnim.start();
        
        ivScrollIndicator.setOnClickListener(v -> {
            hsvGpaCards.smoothScrollBy(500, 0);
        });



        // Peek animation to show the scrollable area
        hsvGpaCards.postDelayed(() -> {
            hsvGpaCards.smoothScrollBy(150, 0);
            hsvGpaCards.postDelayed(() -> hsvGpaCards.smoothScrollBy(-150, 0), 400);
        }, 800);
    }

    private void loadStudentProfileImage() {
        db.collection("AllStudents").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profileImageBase64 = documentSnapshot.getString("profile_image_base64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            byte[] decodedString = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivProfile.setImageBitmap(decodedByte);
                        }
                    }
                });
    }


    private void loadStudentAcademicData() {
        db.collection("AllStudents").document(studentId)
                .collection("PredictionHistory")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(predictionSnap -> {
                    predictionHistory = predictionSnap.getDocuments();
                    
                    if (!predictionHistory.isEmpty()) {
                        currentPredictionDoc = predictionHistory.get(predictionHistory.size() - 1);
                        String tip = currentPredictionDoc.getString("motivationTip");
                        if (tip != null) tvMotivationTip.setText(tip);
                        Double predVal = currentPredictionDoc.getDouble("predictedGpa");
                        tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", predVal != null ? predVal : 0.0));
                    } else {
                        currentPredictionDoc = null;
                        tvPredGpa.setText("0.00");
                        tvMotivationTip.setText("Keep going! Complete a prediction to see your target.");
                    }

                    db.collection("AllStudents").document(studentId)
                            .collection("SemesterResults")
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener(resultSnap -> {
                                if (!resultSnap.isEmpty()) {
                                    List<DocumentSnapshot> validSemesters = new ArrayList<>();
                                    for (DocumentSnapshot doc : resultSnap.getDocuments()) {
                                        Boolean isPredOnly = doc.getBoolean("isPredictionOnly");
                                        if (isPredOnly != null && isPredOnly) continue;

                                        List<Map<String, Object>> mods = (List<Map<String, Object>>) doc.get("modules");
                                        boolean hasRealGrade = false;
                                        if (mods != null) {
                                            for (Map<String, Object> m : mods) {
                                                if (m.get("grade_point") != null || (m.get("grade") != null && !"N/A".equals(m.get("grade")))) {
                                                    hasRealGrade = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (hasRealGrade) {
                                            validSemesters.add(doc);
                                        }
                                    }

                                    allSemesters = validSemesters;
                                    
                                    if (!allSemesters.isEmpty()) {
                                        if (checkFirstSemesterIncomplete(allSemesters)) {
                                             showFirstSemesterIncompleteDialog(allSemesters.get(0).getId());
                                             return;
                                        }
                                        calculateGpasAndPopulateUI(allSemesters);
                                        setupSemesterSelectionBar();
                                    } else {
                                        calculateGpasAndPopulateUI(allSemesters);
                                        setupSemesterSelectionBar();
                                        if (currentPredictionDoc == null) {
                                            checkFirstSemesterAndRedirect();
                                        }
                                    }
                                } else {
                                    allSemesters = new ArrayList<>();
                                    calculateGpasAndPopulateUI(allSemesters);
                                    setupSemesterSelectionBar();
                                    if (currentPredictionDoc == null) {
                                        checkFirstSemesterAndRedirect();
                                    }
                                }
                            });
                });
    }

    private String resolveProgramId(DocumentSnapshot studentSnap) {
        String programId = studentSnap.getString("programId");
        if (programId == null || programId.trim().isEmpty()) {
            programId = studentSnap.getString("degree");
        }
        if (programId == null || programId.trim().isEmpty()) {
            programId = studentSnap.getString("degreeId");
        }
        if (programId == null || programId.trim().isEmpty()) {
            String batchId = studentSnap.getString("batchId");
            if (batchId != null && !batchId.trim().isEmpty()) {
                int index = batchId.indexOf('(');
                if (index > 0) {
                    programId = batchId.substring(0, index).trim();
                } else {
                    index = batchId.indexOf(' ');
                    if (index > 0) {
                        programId = batchId.substring(0, index).trim();
                    } else {
                        programId = batchId.trim();
                    }
                }
            }
        }
        return (programId != null && !programId.trim().isEmpty()) ? programId.trim() : "BIT";
    }

    private void checkFirstSemesterAndRedirect() {
        db.collection("AllStudents").document(studentId).get()
                .addOnSuccessListener(studentSnap -> {
                    if (studentSnap.exists()) {
                        String batchId = studentSnap.getString("batchId");
                        String batchName = studentSnap.getString("batchName");
                        String programId = resolveProgramId(studentSnap);

                        db.collection("Degrees").document(programId)
                                .collection("Semesters")
                                .get()
                                .addOnSuccessListener(semestersSnap -> {
                                    List<DocumentSnapshot> sems = new ArrayList<>();
                                    for (DocumentSnapshot doc : semestersSnap.getDocuments()) {
                                        String semBatchId = doc.getString("batchId");
                                        String semBatchName = doc.getString("batchName");

                                        if (batchId != null && batchId.equalsIgnoreCase(semBatchId)) {
                                            sems.add(doc);
                                        } else if (batchName != null && batchName.equalsIgnoreCase(semBatchId)) {
                                            sems.add(doc);
                                        } else if (batchId != null && batchId.equalsIgnoreCase(semBatchName)) {
                                            sems.add(doc);
                                        } else if (batchName != null && batchName.equalsIgnoreCase(semBatchName)) {
                                            sems.add(doc);
                                        }
                                    }

                                    if (sems.isEmpty()) {
                                        sems = semestersSnap.getDocuments();
                                    }

                                    // Sort semesters by semesterId (e.g. SEM01, SEM02)
                                    java.util.Collections.sort(sems, (d1, d2) -> {
                                        String id1 = d1.getString("semesterId");
                                        String id2 = d2.getString("semesterId");
                                        if (id1 == null) return 1;
                                        if (id2 == null) return -1;
                                        return id1.compareTo(id2);
                                    });

                                    String activeSemDocId = null;
                                    String activeSemName = "SEM01";
                                    DocumentSnapshot activeSemDoc = null;

                                    if (!sems.isEmpty()) {
                                        java.util.Date today = new java.util.Date();
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

                                        // First priority: check if any semester explicitly has status "Active" in Firestore
                                        for (DocumentSnapshot doc : sems) {
                                            String st = doc.getString("status");
                                            if (st != null && st.trim().equalsIgnoreCase("Active")) {
                                                activeSemDoc = doc;
                                                break;
                                            }
                                        }

                                        // Second priority: fall back to checking if today falls between startDate and endDate
                                        if (activeSemDoc == null) {
                                            for (DocumentSnapshot doc : sems) {
                                                String startStr = doc.getString("startDate");
                                                String endStr = doc.getString("endDate");
                                                try {
                                                    if (startStr != null && endStr != null && !startStr.equalsIgnoreCase("Not Set") && !endStr.equalsIgnoreCase("Not Set")) {
                                                        java.util.Date sDate = sdf.parse(startStr.trim());
                                                        java.util.Date eDate = sdf.parse(endStr.trim());
                                                        if (!today.before(sDate) && !today.after(eDate)) {
                                                            activeSemDoc = doc;
                                                            break;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        if (activeSemDoc == null) {
                                            int completedCount = (allSemesters != null) ? allSemesters.size() : 0;
                                            if (completedCount < sems.size()) {
                                                activeSemDoc = sems.get(completedCount);
                                            } else {
                                                activeSemDoc = sems.get(sems.size() - 1);
                                            }
                                        }

                                        activeSemDocId = activeSemDoc.getId();
                                        String semIdAttr = activeSemDoc.getString("semesterId");
                                        if (semIdAttr != null && !semIdAttr.trim().isEmpty()) {
                                            activeSemName = semIdAttr;
                                        } else {
                                            activeSemName = activeSemDocId;
                                        }
                                    }

                                    // Show selection modal to identify Model A, B, or C for the active semester!
                                    showDataSelectionDialog(programId, batchId, activeSemName, activeSemDocId, activeSemDoc, sems);
                                })
                                .addOnFailureListener(e -> fallbackToManualEntry());
                    } else {
                        fallbackToManualEntry();
                    }
                })
                .addOnFailureListener(e -> fallbackToManualEntry());
    }

    private void fallbackToManualEntry() {
        showDataSelectionDialog("BIT", "", "SEM01", null, null, new ArrayList<>());
    }

    private void showDataSelectionDialog(String programId, String batchId, String firstSemName, String firstSemDocId, DocumentSnapshot activeSemDoc, List<DocumentSnapshot> sems) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✨ AI GPA PREDICTOR SETUP");
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#1B5E20")); // Dark Green
        tvTitle.setGravity(android.view.Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("What academic records do you have ready today? Check all that apply:");
        tvSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvSub.setTextColor(Color.parseColor("#424242"));
        tvSub.setPadding(0, pad / 2, 0, pad / 2);
        root.addView(tvSub);

        android.widget.CheckBox cbCgpa = new android.widget.CheckBox(this);
        cbCgpa.setText("Model A (Pre-Semester Baseline):\nPredict using my CGPA only (Before midterm exams)");
        cbCgpa.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        cbCgpa.setTextColor(Color.parseColor("#212121"));
        cbCgpa.setPadding(0, pad / 4, 0, pad / 4);
        root.addView(cbCgpa);

        android.widget.CheckBox cbMid = new android.widget.CheckBox(this);
        cbMid.setText("Model C (Comprehensive Master):\nPredict using my CGPA + Midterm & Assignment marks");
        cbMid.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        cbMid.setTextColor(Color.parseColor("#212121"));
        cbMid.setPadding(0, pad / 4, 0, pad / 4);
        root.addView(cbMid);

        int semIndex = 1;
        if (sems != null && activeSemDoc != null) {
            int idx = sems.indexOf(activeSemDoc);
            if (idx >= 0) semIndex = idx + 1;
        } else if (firstSemName != null) {
            if (firstSemName.toUpperCase().contains("02") || firstSemName.toUpperCase().contains("SEM02")) semIndex = 2;
            else if (firstSemName.toUpperCase().contains("03") || firstSemName.toUpperCase().contains("SEM03")) semIndex = 3;
            else if (firstSemName.toUpperCase().contains("04") || firstSemName.toUpperCase().contains("SEM04")) semIndex = 4;
            else if (firstSemName.toUpperCase().contains("05") || firstSemName.toUpperCase().contains("SEM05")) semIndex = 5;
            else if (firstSemName.toUpperCase().contains("06") || firstSemName.toUpperCase().contains("SEM06")) semIndex = 6;
        }

        boolean isWithinReleaseWindow = true;
        if (semIndex >= 2 && activeSemDoc != null) {
            String startStr = activeSemDoc.getString("startDate");
            try {
                if (startStr != null && !startStr.equalsIgnoreCase("Not Set")) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    java.util.Date sDate = sdf.parse(startStr.trim());
                    java.util.Date today = new java.util.Date();
                    long diffWeeks = (today.getTime() - sDate.getTime()) / (1000L * 60 * 60 * 24 * 7);
                    if (diffWeeks > 10) {
                        isWithinReleaseWindow = false;
                    }
                } else {
                    isWithinReleaseWindow = false;
                }
            } catch (Exception e) {
                isWithinReleaseWindow = false;
            }
        }

        boolean hasCompletedPrevious = (allSemesters != null && !allSemesters.isEmpty()) || (calculatedStudentCgpa > 0.0);

        androidx.appcompat.widget.AppCompatButton btnEnterGrades = null;
        if (!hasCompletedPrevious && (semIndex == 1 || (semIndex == 2 && isWithinReleaseWindow))) {
            tvSub.setText("As a First Year First Semester student (or awaiting Semester 1 results release within 5–10 weeks of Semester 2), you do not have a Cumulative GPA yet. Predict your GPA using your current Midterm & Assignment marks:");
            cbCgpa.setVisibility(android.view.View.GONE);
            cbCgpa.setChecked(false);
            cbMid.setText("Model B (Mid-Semester Fresher):\nPredict using my Midterm & Assignment marks");
            cbMid.setChecked(true);
        } else if (!hasCompletedPrevious && !isWithinReleaseWindow && semIndex >= 2) {
            tvSub.setText("📢 Official results for your previous semester have been released! To use Model A or Model C and get accurate future predictions, please select your model and then enter your completed semester grades.");
        } else if (hasCompletedPrevious) {
            tvSub.setText("You have completed previous semesters (CGPA = " + String.format(java.util.Locale.US, "%.2f", calculatedStudentCgpa) + "). Select the model based on what marks you currently have for " + ((firstSemName != null) ? firstSemName : "this semester") + ":");
        }

        androidx.appcompat.widget.AppCompatButton btnContinue = new androidx.appcompat.widget.AppCompatButton(this);
        btnContinue.setText("CONTINUE TO PREDICTOR ➔");
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setBackgroundColor(Color.parseColor("#2E7D32"));
        btnContinue.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, pad / 2, 0, 0);
        btnContinue.setLayoutParams(btnParams);
        root.addView(btnContinue);

        builder.setView(root);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        if (btnEnterGrades != null) {
            btnEnterGrades.setOnClickListener(v -> {
                dialog.dismiss();
                // This fallback is kept just in case but it shouldn't be rendered anymore.
            });
        }

        final int finalSemIndex = semIndex;
        final boolean finalIsWithinReleaseWindow = isWithinReleaseWindow;
        final boolean finalHasCompletedPrevious = hasCompletedPrevious;

        btnContinue.setOnClickListener(v -> {
            boolean hasCgpa = cbCgpa.isChecked();
            boolean hasMid = cbMid.isChecked();

            if (!hasCgpa && !hasMid) {
                android.widget.Toast.makeText(this, "Please select a prediction model!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();

            if (hasCgpa && hasSpecialStatusGrade) {
                android.widget.Toast.makeText(this, "⚠️ Cannot use CGPA: You have Special Status grades (AB, NE, MC, WH, INC). PrePal will predict using your Midterm & Assignment marks.", android.widget.Toast.LENGTH_LONG).show();
                hasCgpa = false;
                hasMid = true;
            }

            if (!finalHasCompletedPrevious && (finalSemIndex == 1 || (finalSemIndex == 2 && finalIsWithinReleaseWindow))) {
                // Model B: Mid-Semester Fresher (No CGPA, module marks required)
                Intent intent = new Intent(this, FirstSemesterQuizActivity.class);
                if (firstSemDocId != null) {
                    intent.putExtra("semesterDocId", firstSemDocId);
                }
                intent.putExtra("semesterName", (firstSemName != null && !firstSemName.trim().isEmpty()) ? firstSemName : "SEM01");
                intent.putExtra("programId", (programId != null && !programId.trim().isEmpty()) ? programId : "BIT");
                if (batchId != null) {
                    intent.putExtra("batchId", batchId);
                }
                startActivity(intent);
            } else if (!finalHasCompletedPrevious && !finalIsWithinReleaseWindow && finalSemIndex >= 2) {
                // Senior student logging in for the first time
                if (hasMid) {
                    openManualResultEntryForCgpa(true, firstSemDocId, firstSemName, programId, batchId);
                } else if (hasCgpa) {
                    openManualResultEntryForCgpa(false, firstSemDocId, firstSemName, programId, batchId);
                }
            } else if (finalHasCompletedPrevious) {
                if (hasMid) {
                    // Model C: Comprehensive Master (Both CGPA and Module marks required)
                    if (calculatedStudentCgpa <= 0.0) {
                        openManualResultEntryForCgpa(true, firstSemDocId, firstSemName, programId, batchId);
                    } else {
                        launchModelWithCgpa(true, calculatedStudentCgpa, firstSemDocId, firstSemName, programId, batchId);
                    }
                } else if (hasCgpa) {
                    // Model A: Pre-Semester Baseline (CGPA only, no module midterms required)
                    if (calculatedStudentCgpa <= 0.0) {
                        openManualResultEntryForCgpa(false, firstSemDocId, firstSemName, programId, batchId);
                    } else {
                        launchModelWithCgpa(false, calculatedStudentCgpa, firstSemDocId, firstSemName, programId, batchId);
                    }
                }
            } else {
                openManualResultEntryForCgpa(false, firstSemDocId, firstSemName, programId, batchId);
            }
        });

        dialog.show();
    }

    private void openManualResultEntryForCgpa(boolean isModelC, String firstSemDocId, String firstSemName, String programId, String batchId) {
        android.widget.Toast.makeText(this, "Please enter your completed semester grades first so PrePal can calculate your official CGPA!", android.widget.Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, ManualResultEntryActivity.class);
        if (isModelC) {
            intent.putExtra("nextStepModelC", true);
            if (firstSemDocId != null) intent.putExtra("firstSemDocId", firstSemDocId);
            if (firstSemName != null) intent.putExtra("firstSemName", firstSemName);
            if (programId != null) intent.putExtra("programId", programId);
            if (batchId != null) intent.putExtra("batchId", batchId);
        }
        startActivity(intent);
    }

    private void launchModelWithCgpa(boolean isModelC, double cgpaToUse, String firstSemDocId, String firstSemName, String programId, String batchId) {
        if (isModelC) {
            // Model C: Comprehensive Master (CGPA + Module Midterm/Assignment marks)
            Intent intent = new Intent(this, FirstSemesterQuizActivity.class);
            if (firstSemDocId != null) {
                intent.putExtra("semesterDocId", firstSemDocId);
            }
            intent.putExtra("semesterName", (firstSemName != null && !firstSemName.trim().isEmpty()) ? firstSemName : "SEM01");
            intent.putExtra("programId", (programId != null && !programId.trim().isEmpty()) ? programId : "BIT");
            if (batchId != null) {
                intent.putExtra("batchId", batchId);
            }
            intent.putExtra("cumulativeGpa", cgpaToUse);
            intent.putExtra("studentType", 3);
            startActivity(intent);
        } else {
            // Model A: Pre-Semester Baseline (CGPA only, no module midterms required)
            Intent intent = new Intent(this, QuizActivity.class);
            if (firstSemDocId != null) {
                intent.putExtra("semesterDocId", firstSemDocId);
            }
            intent.putExtra("semesterName", (firstSemName != null && !firstSemName.trim().isEmpty()) ? firstSemName : "SEM01");
            intent.putExtra("programId", (programId != null && !programId.trim().isEmpty()) ? programId : "BIT");
            if (batchId != null) {
                intent.putExtra("batchId", batchId);
            }
            intent.putExtra("cumulativeGpa", cgpaToUse);
            intent.putExtra("results", new java.util.ArrayList<Map<String, Object>>());
            startActivity(intent);
        }
    }

    private boolean checkFirstSemesterIncomplete(List<DocumentSnapshot> semesters) {
        if (semesters == null || semesters.isEmpty()) return false;
        DocumentSnapshot firstSem = semesters.get(0);
        List<Map<String, Object>> modules = (List<Map<String, Object>>) firstSem.get("modules");
        if (modules == null) return false;
        for (Map<String, Object> mod : modules) {
            String grade = (String) mod.get("grade");
            if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC"))) {
                return true;
            }
        }
        return false;
    }

    private void showFirstSemesterIncompleteDialog(String firstSemesterDocId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_first_semester_incomplete, null);
        builder.setView(view);
        builder.setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
            dialog.dismiss();
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(StudentHomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        view.findViewById(R.id.btnEnterResultsNow).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(StudentHomeActivity.this, ManualResultEntryActivity.class);
            intent.putExtra("isEditMode", true);
            intent.putExtra("semesterDocId", firstSemesterDocId);
            startActivity(intent);
        });

        dialog.show();
    }

    private boolean isSemesterValid(DocumentSnapshot semDoc) {
        if (semDoc == null) return false;
        Boolean isPredOnly = semDoc.getBoolean("isPredictionOnly");
        if (isPredOnly != null && isPredOnly) return false;

        List<Map<String, Object>> modules = (List<Map<String, Object>>) semDoc.get("modules");
        if (modules == null || modules.isEmpty()) return false;
        boolean hasActualGrade = false;
        for (Map<String, Object> mod : modules) {
            String grade = (String) mod.get("grade");
            Object gp = mod.get("grade_point");
            if (gp != null || (grade != null && !"N/A".equals(grade) && !"INC".equals(grade))) {
                hasActualGrade = true;
            }
            if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC") || grade.equals("N/A"))) {
                return false;
            }
        }
        return hasActualGrade;
    }

    private void setupSemesterSelectionBar() {
        List<String> semLabels = new ArrayList<>();
        for (DocumentSnapshot doc : allSemesters) {
            String name = doc.getString("semesterName");
            if (name != null) {
                if (name.contains("Semester VIII")) semLabels.add("Sem 8");
                else if (name.contains("Semester VII")) semLabels.add("Sem 7");
                else if (name.contains("Semester VI")) semLabels.add("Sem 6");
                else if (name.contains("Semester V")) semLabels.add("Sem 5");
                else if (name.contains("Semester IV")) semLabels.add("Sem 4");
                else if (name.contains("Semester III")) semLabels.add("Sem 3");
                else if (name.contains("Semester II")) semLabels.add("Sem 2");
                else if (name.contains("Semester I")) semLabels.add("Sem 1");
                else semLabels.add(name);
            } else {
                semLabels.add("Sem " + (semLabels.size() + 1));
            }
        }
        
        chipAdapter = new SemesterChipAdapter(semLabels, position -> {
            if (allSemesters != null && position < allSemesters.size()) {
                DocumentSnapshot sem = allSemesters.get(position);
                List<Map<String, Object>> modules = (List<Map<String, Object>>) sem.get("modules");
                setupPerformanceBarChart(modules);
                
                // Find the last valid semester index
                int targetPos = position;
                while (targetPos >= 0 && !isSemesterValid(allSemesters.get(targetPos))) {
                    targetPos--;
                }
                
                if (targetPos >= 0) {
                    DocumentSnapshot validSem = allSemesters.get(targetPos);
                    
                    // Calculate GPAs locally for 'targetPos'
                    double totalPts = 0;
                    double totalCreds = 0;
                    double currentSemPts = 0;
                    double currentSemCreds = 0;

                    for (int i = 0; i <= targetPos; i++) {
                        DocumentSnapshot s = allSemesters.get(i);
                        List<Map<String, Object>> mods = (List<Map<String, Object>>) s.get("modules");
                        if (mods == null) continue;
                        
                        for (Map<String, Object> m : mods) {
                            String grade = (String) m.get("grade");
                            if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC"))) {
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
                            
                            totalPts += (p * c);
                            totalCreds += c;
                            
                            if (i == targetPos) {
                                currentSemPts += (p * c);
                                currentSemCreds += c;
                            }
                        }
                    }
                    
                    double semGpa = currentSemCreds > 0 ? (currentSemPts / currentSemCreds) : 0;
                    tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", semGpa));
                    
                    String semName = validSem.getString("semesterName");
                    tvSemGpaSub.setText(semName != null ? semName : "Semester " + (targetPos + 1));
                    tvCumGpaSub.setText("Till " + (semName != null ? semName : "Semester " + (targetPos + 1)));
                    
                    double cumGpa = totalCreds > 0 ? (totalPts / totalCreds) : 0;
                    tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", cumGpa));
                    
                    setupClassStandingChart(cumGpa, false, false);
                    
                    com.google.firebase.Timestamp semTime = validSem.getTimestamp("timestamp");
                    if (semTime != null && !predictionHistory.isEmpty()) {
                        DocumentSnapshot matchingPred = null;
                        for (DocumentSnapshot p : predictionHistory) {
                            com.google.firebase.Timestamp pTime = p.getTimestamp("timestamp");
                            if (pTime != null && pTime.getSeconds() > semTime.getSeconds()) {
                                matchingPred = p;
                                break;
                            }
                        }
                        if (matchingPred == null && !predictionHistory.isEmpty()) {
                            matchingPred = predictionHistory.get(predictionHistory.size() - 1);
                        }
                        
                        currentPredictionDoc = matchingPred;
                        
                        if (matchingPred != null) {
                            Double pGpa = matchingPred.getDouble("predictedGpa");
                            tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", pGpa != null ? pGpa : 0.0));
                            String tip = matchingPred.getString("motivationTip");
                            if (tip != null) tvMotivationTip.setText(tip);
                        }
                    }
                } else {
                    // Fallback
                    tvSemGpa.setText("INC");
                    tvCumGpa.setText("INC");
                    tvPredGpa.setText("0.00");
                    setupClassStandingChart(0.0, true, false);
                }
            }
        });
        rvSemesterSelector.setAdapter(chipAdapter);
        rvSemesterSelector.scrollToPosition(semLabels.size() - 1);
    }

    private void calculateGpasAndPopulateUI(List<DocumentSnapshot> semesters) {
        double totalPoints = 0;
        double totalCredits = 0;
        
        List<Map<String, Object>> latestResults = null;
        List<Entry> gpaTrendEntries = new ArrayList<>();

        // Filter for actual completed semesters (excluding prediction-only documents)
        List<DocumentSnapshot> actualSemesters = new ArrayList<>();
        for (DocumentSnapshot doc : semesters) {
            Boolean isPredOnly = doc.getBoolean("isPredictionOnly");
            if (isPredOnly == null || !isPredOnly) {
                List<Map<String, Object>> mods = (List<Map<String, Object>>) doc.get("modules");
                if (mods != null && !mods.isEmpty()) {
                    boolean hasActualGrades = false;
                    for (Map<String, Object> m : mods) {
                        if (m.get("grade_point") != null) {
                            hasActualGrades = true;
                            break;
                        }
                    }
                    if (hasActualGrades) {
                        actualSemesters.add(doc);
                    }
                }
            }
        }

        if (actualSemesters.isEmpty()) {
            // Student has NO actual completed exam semesters yet (1st Year 1st Semester)
            double predValue = 0.0;
            String tip = "Keep going! Complete a prediction to see your target.";
            
            if (currentPredictionDoc != null) {
                Double pGpa = currentPredictionDoc.getDouble("predictedGpa");
                if (pGpa != null) predValue = pGpa;
                String t = currentPredictionDoc.getString("motivationTip");
                if (t != null) tip = t;
            }

            tvSemGpa.setText("--");
            tvCumGpa.setText("--");
            tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", predValue));
            tvSemGpaSub.setText("Semester I");
            tvCumGpaSub.setText("Till Semester I");
            tvMotivationTip.setText(tip);

            // Display predicted class standing target on the donut chart
            setupClassStandingChart(predValue, false, true);

            // Display single Sem 1 (Pred.) entry on trend chart
            setupSinglePredictionTrendChart(predValue, 1);
            setupPerformanceBarChart(null);
            return;
        }

        semesters = actualSemesters;

        // Find the last valid semester index
        int targetPos = semesters.size() - 1;
        while (targetPos >= 0 && !isSemesterValid(semesters.get(targetPos))) {
            targetPos--;
        }
        
        // The bar chart always displays the very latest semester results
        if (semesters.size() > 0) {
            DocumentSnapshot latest = semesters.get(semesters.size() - 1);
            latestResults = (List<Map<String, Object>>) latest.get("modules");
        }

        // Calculate and build trend entries
        double targetSemGpa = 0;
        double targetCumGpa = 0;
        String targetSemName = "";
        hasSpecialStatusGrade = false;
        
        for (int i = 0; i <= targetPos; i++) {
            DocumentSnapshot sem = semesters.get(i);
            List<Map<String, Object>> modules = (List<Map<String, Object>>) sem.get("modules");
            if (modules == null) continue;
            
            double semPts = 0;
            double semCreds = 0;

            for (Map<String, Object> mod : modules) {
                String grade = (String) mod.get("grade");
                if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC"))) {
                    hasSpecialStatusGrade = true;
                    continue;
                }

                double credits = 0;
                Object credObj = mod.get("credits");
                if (credObj instanceof Double) credits = (Double) credObj;
                else if (credObj instanceof Long) credits = ((Long) credObj).doubleValue();
                
                double points = 0;
                Object ptObj = mod.get("grade_point");
                if (ptObj instanceof Double) points = (Double) ptObj;
                else if (ptObj instanceof Long) points = ((Long) ptObj).doubleValue();

                double pc = points * credits;
                totalPoints += pc;
                totalCredits += credits;
                semPts += pc;
                semCreds += credits;
            }

            double semGpa = semCreds > 0 ? (semPts / semCreds) : 0;
            gpaTrendEntries.add(new Entry(i, (float) semGpa));

            if (i == targetPos) {
                targetSemGpa = semGpa;
                targetCumGpa = totalCredits > 0 ? (totalPoints / totalCredits) : 0;
                calculatedStudentCgpa = targetCumGpa;
                targetSemName = sem.getString("semesterName");
                if (targetSemName == null) targetSemName = "Semester " + (i + 1);
            }
        }

        if (hasSpecialStatusGrade) {
            tvSemGpa.setText("N/A");
            tvCumGpa.setText("N/A");
            calculatedStudentCgpa = 0.0;
            tvSemGpaSub.setText("Special Grade");
            tvCumGpaSub.setText("Action Required");
            tvMotivationTip.setText("⚠️ Special Status Grade (AB, NE, MC, WH, INC) detected. GPA and CGPA cannot be calculated until all special status grades are cleared.");
            setupClassStandingChart(0.0, false, false);
        } else if (targetPos >= 0) {
            tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", targetSemGpa));
            tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", targetCumGpa));
            tvSemGpaSub.setText(targetSemName);
            tvCumGpaSub.setText("Till " + targetSemName);
            setupClassStandingChart(targetCumGpa, false, false);

            // Match prediction for the valid target semester
            DocumentSnapshot validSem = semesters.get(targetPos);
            com.google.firebase.Timestamp semTime = validSem.getTimestamp("timestamp");
            if (semTime != null && !predictionHistory.isEmpty()) {
                DocumentSnapshot matchingPred = null;
                for (DocumentSnapshot p : predictionHistory) {
                    com.google.firebase.Timestamp pTime = p.getTimestamp("timestamp");
                    if (pTime != null && pTime.getSeconds() > semTime.getSeconds()) {
                        matchingPred = p;
                        break;
                    }
                }
                if (matchingPred == null && !predictionHistory.isEmpty()) {
                    matchingPred = predictionHistory.get(predictionHistory.size() - 1);
                }
                
                currentPredictionDoc = matchingPred;
                
                if (matchingPred != null) {
                    Double pGpa = matchingPred.getDouble("predictedGpa");
                    tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", pGpa != null ? pGpa : 0.0));
                    String tip = matchingPred.getString("motivationTip");
                    if (tip != null) tvMotivationTip.setText(tip);
                }
            }
        } else {
            tvSemGpa.setText("INC");
            tvCumGpa.setText("INC");
            tvSemGpaSub.setText("Semester " + semesters.size());
            tvCumGpaSub.setText("Till Semester " + semesters.size());
            setupClassStandingChart(0.0, true, false);
        }

        setupGpaTrendChart(gpaTrendEntries);
        setupPerformanceBarChart(latestResults);
    }

    private void setupSinglePredictionTrendChart(double predGpa, int targetSemNum) {
        List<Entry> predictionEntries = new ArrayList<>();
        predictionEntries.add(new Entry(0, (float) predGpa));

        LineData lineData = new LineData();
        LineDataSet predictionDataSet = new LineDataSet(predictionEntries, "Prediction");
        styleLineDataSet(predictionDataSet, Color.parseColor("#7C3AED"), false);
        predictionDataSet.setCircleRadius(8f);
        predictionDataSet.setDrawValues(true);
        predictionDataSet.setValueTextSize(13f);
        predictionDataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        predictionDataSet.setValueTextColor(Color.parseColor("#7C3AED"));
        predictionDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(java.util.Locale.US, "%.2f", value);
            }
        });
        lineData.addDataSet(predictionDataSet);

        lineChartGpa.setData(lineData);
        lineChartGpa.getDescription().setEnabled(false);
        lineChartGpa.getLegend().setEnabled(false);
        lineChartGpa.setExtraOffsets(12f, 26f, 16f, 12f);

        XAxis xAxis = lineChartGpa.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(0.5f);
        xAxis.setLabelCount(1);
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Sem " + targetSemNum + " (Pred.)";
            }
        });

        setupYAxis(lineChartGpa.getAxisLeft());
        lineChartGpa.getAxisRight().setEnabled(false);
        lineChartGpa.animateY(1000);
        lineChartGpa.invalidate();
    }

    private void updateGpaDisplays(double sem, double cum, double pred, String tip) {
        tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", sem));
        tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", cum));
        tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", pred));
        if (tip != null) tvMotivationTip.setText(tip);
    }

    private void setupGpaTrendChart(List<Entry> historyEntries) {
        if (historyEntries.isEmpty()) historyEntries.add(new Entry(0, 0f));

        LineData lineData = new LineData();
        LineDataSet historyDataSet = new LineDataSet(historyEntries, "GPA History");
        styleLineDataSet(historyDataSet, Color.parseColor("#057BFE"), true);
        historyDataSet.setValueTextSize(11f);
        historyDataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        lineData.addDataSet(historyDataSet);

        String predStr = tvPredGpa.getText().toString();
        float predValue = 0f;
        try { predValue = Float.parseFloat(predStr); } catch (Exception e) {}

        if (predValue > 0) {
            List<Entry> predictionEntries = new ArrayList<>();
            Entry lastHistory = historyEntries.get(historyEntries.size() - 1);
            predictionEntries.add(new Entry(lastHistory.getX(), lastHistory.getY()));
            predictionEntries.add(new Entry(lastHistory.getX() + 1, predValue));

            LineDataSet predictionDataSet = new LineDataSet(predictionEntries, "Prediction");
            styleLineDataSet(predictionDataSet, Color.parseColor("#7C3AED"), false);
            predictionDataSet.enableDashedLine(10f, 10f, 0f);
            predictionDataSet.setCircleRadius(7f);
            predictionDataSet.setDrawValues(true);
            predictionDataSet.setValueTextSize(13f);
            predictionDataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            predictionDataSet.setValueTextColor(Color.parseColor("#7C3AED"));
            final float lastHistoryXVal = lastHistory.getX();
            predictionDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPointLabel(Entry entry) {
                    if (entry.getX() > lastHistoryXVal) {
                        return String.format(java.util.Locale.US, "%.2f", entry.getY());
                    }
                    return "";
                }
                @Override
                public String getFormattedValue(float value) {
                    return String.format(java.util.Locale.US, "%.2f", value);
                }
            });
            lineData.addDataSet(predictionDataSet);
        }

        lineChartGpa.setData(lineData);
        lineChartGpa.getDescription().setEnabled(false);
        lineChartGpa.getLegend().setEnabled(false);
        lineChartGpa.setExtraOffsets(12f, 26f, 16f, 12f);
        
        XAxis xAxis = lineChartGpa.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(historyEntries.size() + (predValue > 0 ? 1 : 0));
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int sem = (int) value + 1;
                return "Sem " + sem + (value >= historyEntries.size() ? " (Pred.)" : "");
            }
        });

        setupYAxis(lineChartGpa.getAxisLeft());
        lineChartGpa.getAxisRight().setEnabled(false);
        lineChartGpa.animateY(1000);
        lineChartGpa.invalidate();
    }

    private void setupPerformanceBarChart(List<Map<String, Object>> results) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (results != null) {
            int validIndex = 0;
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> res = results.get(i);
                double gp = 0;
                Object pt = res.get("grade_point");
                if (pt instanceof Double) gp = (Double) pt;
                else if (pt instanceof Long) gp = ((Long) pt).doubleValue();
                
                String grade = (String) res.get("grade");
                if (pt == null && (grade == null || "N/A".equals(grade) || "INC".equals(grade))) {
                    continue; // Skip non-value / dummy modules in the chart!
                }

                String moduleId = (String) res.get("module_id");
                if (moduleId == null || moduleId.trim().isEmpty()) {
                    moduleId = (String) res.get("moduleName");
                }
                
                entries.add(new BarEntry(validIndex++, (float) gp));
                labels.add(moduleId != null ? moduleId : "Module " + (validIndex));
                
                if (grade != null && (grade.equals("F") || grade.equals("AB") || grade.equals("MC") || grade.equals("NE") || gp < 2.0)) {
                    colors.add(Color.parseColor("#E53E3E")); // Red for Weak/Fail
                } else if (gp >= 3.7) {
                    colors.add(Color.parseColor("#ECC94B")); // Gold
                } else if (gp >= 3.3) {
                    colors.add(Color.parseColor("#48BB78")); // Green
                } else if (gp >= 3.0) {
                    colors.add(Color.parseColor("#4299E1")); // Blue
                } else {
                    colors.add(Color.parseColor("#ED8936")); // Orange
                }
            }
        }

        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0f));
            labels.add("");
            colors.add(Color.TRANSPARENT);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Performance");
        dataSet.setColors(colors);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(java.util.Locale.US, "%.2f", value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        
        barChartPerformance.setData(data);
        barChartPerformance.getDescription().setEnabled(false);
        barChartPerformance.getLegend().setEnabled(false);
        barChartPerformance.setExtraOffsets(12f, 26f, 16f, 12f);
        
        XAxis xAxis = barChartPerformance.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-30);

        setupYAxis(barChartPerformance.getAxisLeft());
        barChartPerformance.getAxisRight().setEnabled(false);
        barChartPerformance.animateY(1200);
        barChartPerformance.invalidate();
    }

    private void setupYAxis(YAxis yAxis) {
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(4.25f);
        yAxis.setLabelCount(9, false);
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));
        yAxis.setDrawAxisLine(true);
        yAxis.setTextSize(11f);
        yAxis.setTextColor(Color.parseColor("#4A5568"));
        
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 4.01f) return "";
                return String.format(java.util.Locale.US, "%.2f", value);
            }
        });
    }

    private void styleLineDataSet(LineDataSet dataSet, int color, boolean isHistory) {
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.15f);
        dataSet.setDrawFilled(isHistory);
        dataSet.setLineWidth(4f);
        dataSet.setCircleRadius(6f);
        dataSet.setCircleColor(color);
        dataSet.setColor(color);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueTextColor(color);
        
        if (isHistory) {
            dataSet.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
                    new int[]{Color.parseColor("#33057BFE"), Color.parseColor("#05FFFFFF")}));
        }
    }
    
    private void setupClassStandingChart(double cgpa, boolean isInc, boolean isTarget) {
        PieChart pieChart = findViewById(R.id.chartDegreeClass);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(75f);
        pieChart.setTransparentCircleRadius(80f);
        pieChart.setDrawEntryLabels(false);

        List<PieEntry> entries = new ArrayList<>();
        PieDataSet dataSet;
        int color;
        String className;

        if (isInc) {
            entries.add(new PieEntry(4.0f, ""));
            dataSet = new PieDataSet(entries, "");
            dataSet.setDrawValues(false);
            
            color = Color.parseColor("#BDBDBD"); // Gray for incomplete
            className = "Incomplete";
            dataSet.setColors(color);
        } else {
            entries.add(new PieEntry((float) cgpa, ""));
            entries.add(new PieEntry((float) (4.0 - cgpa), ""));

            dataSet = new PieDataSet(entries, "");
            dataSet.setDrawValues(false);
            
            if (cgpa >= 3.70) {
                color = Color.parseColor("#FFD700"); // 1st Class
                className = "1st Class";
            } else if (cgpa >= 3.30) {
                color = Color.parseColor("#2DCC70"); // 2nd Upper
                className = "2nd Upper";
            } else if (cgpa >= 3.00) {
                color = Color.parseColor("#057BFE"); // 2nd Lower
                className = "2nd Lower";
            } else if (cgpa >= 2.00) {
                color = Color.parseColor("#F39C12"); // Ordinary Pass
                className = "Ordinary Pass";
            } else {
                color = Color.parseColor("#E74C3C"); // Weak
                className = "Weak";
            }
            dataSet.setColors(color, Color.parseColor("#E0E0E0"));
        }
        
        String centerText = isTarget ? className + "\n(Target)" : className + "\nStanding";
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(12f);
        pieChart.setCenterTextColor(color);
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void showPredictionInsightsDialog() {
        if (currentPredictionDoc == null) {
            android.widget.Toast.makeText(this, "No prediction data available for this semester.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        Double attendance = currentPredictionDoc.getDouble("attendance");
        Double studyHours = currentPredictionDoc.getDouble("studyHours");
        Double sleepHours = currentPredictionDoc.getDouble("sleepHours");
        Double stressLevel = currentPredictionDoc.getDouble("stressLevel");

        if (attendance == null || studyHours == null) {
            android.widget.Toast.makeText(this, "Detailed insights are not available for older predictions.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_prediction_insights, null);
        bottomSheetDialog.setContentView(view);
        
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        TextView tvAttendanceTitle = view.findViewById(R.id.tvAttendanceTitle);
        TextView tvAttendanceDesc = view.findViewById(R.id.tvAttendanceDesc);
        android.widget.LinearLayout llAttendanceBg = view.findViewById(R.id.llAttendanceBg);

        TextView tvStudyTitle = view.findViewById(R.id.tvStudyTitle);
        TextView tvStudyDesc = view.findViewById(R.id.tvStudyDesc);
        android.widget.LinearLayout llStudyBg = view.findViewById(R.id.llStudyBg);

        TextView tvSleepTitle = view.findViewById(R.id.tvSleepTitle);
        TextView tvSleepDesc = view.findViewById(R.id.tvSleepDesc);
        android.widget.LinearLayout llSleepBg = view.findViewById(R.id.llSleepBg);

        TextView tvStressTitle = view.findViewById(R.id.tvStressTitle);
        TextView tvStressDesc = view.findViewById(R.id.tvStressDesc);
        android.widget.LinearLayout llStressBg = view.findViewById(R.id.llStressBg);

        androidx.appcompat.widget.AppCompatButton btnClose = view.findViewById(R.id.btnCloseInsights);
        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Helper to set background colors securely
        int colorGreen = Color.parseColor("#E8F5E9");
        int colorYellow = Color.parseColor("#FFFDE7");
        int colorRed = Color.parseColor("#FFEBEE");

        // 1. Attendance
        tvAttendanceTitle.setText("Attendance: " + attendance + "%");
        if (attendance >= 90) {
            tvAttendanceDesc.setText("Excellent! This is heavily boosting your predicted GPA.");
            llAttendanceBg.setBackgroundColor(colorGreen);
        } else if (attendance >= 80) {
            tvAttendanceDesc.setText("Good, but improving attendance will secure a higher GPA.");
            llAttendanceBg.setBackgroundColor(colorYellow);
        } else {
            tvAttendanceDesc.setText("Critical Warning! Attendance below 80% drastically lowers your predicted GPA.");
            llAttendanceBg.setBackgroundColor(colorRed);
        }

        // 2. Study Time
        tvStudyTitle.setText("Study Time: " + studyHours + "h/week");
        Double targetStudy = currentPredictionDoc.getDouble("targetStudyHours");
        double requiredStudy = (targetStudy != null && targetStudy > 0) ? targetStudy : 25.0; // Fallback to 25 if old data

        if (studyHours >= requiredStudy) {
            tvStudyDesc.setText("Great effort! You are fully meeting your SLQF study requirement.");
            llStudyBg.setBackgroundColor(colorGreen);
        } else if (studyHours >= requiredStudy * 0.6) { // 60% of target
            tvStudyDesc.setText("Moderate effort. Meeting your full SLQF target will directly improve your grade.");
            llStudyBg.setBackgroundColor(colorYellow);
        } else {
            tvStudyDesc.setText("Too low! The AI expects a GPA drop unless you dedicate more time to self-study.");
            llStudyBg.setBackgroundColor(colorRed);
        }

        // 3. Sleep
        tvSleepTitle.setText("Sleep: " + sleepHours + "h/day");
        if (sleepHours >= 7) {
            tvSleepDesc.setText("Healthy sleep habits are keeping your brain sharp.");
            llSleepBg.setBackgroundColor(colorGreen);
        } else {
            tvSleepDesc.setText("Lack of sleep limits your learning efficiency.");
            llSleepBg.setBackgroundColor(colorYellow);
        }

        // 4. Stress
        tvStressTitle.setText("Stress Level: " + stressLevel + "/5");
        if (stressLevel <= 2) {
            tvStressDesc.setText("Low stress is helping you focus.");
            llStressBg.setBackgroundColor(colorGreen);
        } else {
            tvStressDesc.setText("High stress is negatively impacting your prediction. Try to manage it!");
            llStressBg.setBackgroundColor(colorRed);
        }

        bottomSheetDialog.show();

        java.util.List<String> warnings = (java.util.List<String>) currentPredictionDoc.get("acknowledgementsRequired");
        if (warnings != null && !warnings.isEmpty()) {
            AcknowledgementDialogHelper.showWarningDialog(this, warnings, null);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            else if (itemId == R.id.nav_chats) {
                startActivity(new Intent(this, ChatListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, StudentCalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, StudentProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}

package com.example.finalyearprojectnew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
    
    private TextView tvStudentId, tvMotivationTip, tvWelcomeText, tvSpecialStatusNote, tvCurrentDate;
    private TextView tvSemGpa, tvCumGpa, tvPredGpa, btnViewFullReport;
    private TextView tvSemGpaSub, tvCumGpaSub, tvPredGpaSub;
    private androidx.appcompat.widget.AppCompatButton btnRedoPrediction;
    private android.widget.ImageView ivProfile;
    private android.widget.HorizontalScrollView hsvGpaCards;
    private android.widget.ImageView ivScrollIndicator;
    private androidx.cardview.widget.CardView cardPredGpa;

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
            startActivity(new Intent(StudentHomeActivity.this, ManualResultEntryActivity.class));
        });

        btnViewFullReport.setOnClickListener(v -> {
            startActivity(new Intent(StudentHomeActivity.this, PerformanceReportActivity.class));
        });
        
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
                                    allSemesters = resultSnap.getDocuments();
                                    
                                    if (checkFirstSemesterIncomplete(allSemesters)) {
                                         showFirstSemesterIncompleteDialog(allSemesters.get(0).getId());
                                         return;
                                     }

                                    calculateGpasAndPopulateUI(allSemesters);
                                    setupSemesterSelectionBar();
                                } else {
                                    // Redirect to ManualResultEntryActivity if no data is available
                                    Intent intent = new Intent(StudentHomeActivity.this, ManualResultEntryActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                });
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
        List<Map<String, Object>> modules = (List<Map<String, Object>>) semDoc.get("modules");
        if (modules == null) return false;
        for (Map<String, Object> mod : modules) {
            String grade = (String) mod.get("grade");
            if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC"))) {
                return false;
            }
        }
        return true;
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
                    
                    setupClassStandingChart(cumGpa, false);
                    
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
                    setupClassStandingChart(0.0, true);
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
        
        for (int i = 0; i <= targetPos; i++) {
            DocumentSnapshot sem = semesters.get(i);
            List<Map<String, Object>> modules = (List<Map<String, Object>>) sem.get("modules");
            if (modules == null) continue;
            
            double semPts = 0;
            double semCreds = 0;

            for (Map<String, Object> mod : modules) {
                String grade = (String) mod.get("grade");
                if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE") || grade.equals("WH") || grade.equals("INC"))) {
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
                targetSemName = sem.getString("semesterName");
                if (targetSemName == null) targetSemName = "Semester " + (i + 1);
            }
        }

        if (targetPos >= 0) {
            tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", targetSemGpa));
            tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", targetCumGpa));
            tvSemGpaSub.setText(targetSemName);
            tvCumGpaSub.setText("Till " + targetSemName);
            setupClassStandingChart(targetCumGpa, false);

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
            setupClassStandingChart(0.0, true);
        }

        setupGpaTrendChart(gpaTrendEntries);
        setupPerformanceBarChart(latestResults);
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
            lineData.addDataSet(predictionDataSet);
        }

        lineChartGpa.setData(lineData);
        lineChartGpa.getDescription().setEnabled(false);
        lineChartGpa.getLegend().setEnabled(false);
        
        XAxis xAxis = lineChartGpa.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(historyEntries.size() + 1);
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
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> res = results.get(i);
                double gp = 0;
                Object pt = res.get("grade_point");
                if (pt instanceof Double) gp = (Double) pt;
                else if (pt instanceof Long) gp = ((Long) pt).doubleValue();
                
                String moduleId = (String) res.get("module_id");
                String grade = (String) res.get("grade");
                
                entries.add(new BarEntry(i, (float) gp));
                labels.add(moduleId != null ? moduleId : "Module " + (i + 1));
                
                if (grade != null && (grade.equals("F") || grade.equals("AB") || grade.equals("MC") || grade.equals("NE") || gp < 2.0)) {
                    colors.add(Color.parseColor("#FF4B4B"));
                } else {
                    colors.add(Color.parseColor("#057BFE"));
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
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f", value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        
        barChartPerformance.setData(data);
        barChartPerformance.getDescription().setEnabled(false);
        barChartPerformance.getLegend().setEnabled(false);
        
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
        yAxis.setAxisMaximum(4.0f);
        yAxis.setLabelCount(11, true);
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));
        yAxis.setDrawAxisLine(true);
        
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f", value);
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
        dataSet.setDrawValues(isHistory);
        dataSet.setValueTextSize(9f);
        
        if (isHistory) {
            dataSet.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
                    new int[]{Color.parseColor("#33057BFE"), Color.parseColor("#05FFFFFF")}));
        }
    }
    
    private void setupClassStandingChart(double cgpa, boolean isInc) {
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
        
        pieChart.setCenterText(className + "\nStanding");
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
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            else if (itemId == R.id.nav_calendar) {
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

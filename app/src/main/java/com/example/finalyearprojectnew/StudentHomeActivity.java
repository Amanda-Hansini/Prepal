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
    private SemesterChipAdapter chipAdapter;
    
    private TextView tvStudentId, tvMotivationTip, tvWelcomeText, tvSpecialStatusNote, tvCurrentDate;
    private TextView tvSemGpa, tvCumGpa, tvPredGpa, btnViewFullReport;
    private TextView tvSemGpaSub, tvCumGpaSub, tvPredGpaSub;
    private androidx.appcompat.widget.AppCompatButton btnRedoPrediction;
    private android.widget.ImageView ivProfile;

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
        tvSpecialStatusNote = findViewById(R.id.tvSpecialStatusNote);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        
        tvSemGpa = findViewById(R.id.tvSemGpa);
        tvCumGpa = findViewById(R.id.tvCumGpa);
        tvPredGpa = findViewById(R.id.tvPredGpa);
        tvSemGpaSub = findViewById(R.id.tvSemGpaSub);
        tvCumGpaSub = findViewById(R.id.tvCumGpaSub);
        tvPredGpaSub = findViewById(R.id.tvPredGpaSub);
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

    private List<DocumentSnapshot> predictionHistory = new ArrayList<>();

    private void loadStudentAcademicData() {
        db.collection("AllStudents").document(studentId)
                .collection("PredictionHistory")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(predictionSnap -> {
                    predictionHistory = predictionSnap.getDocuments();
                    
                    if (!predictionHistory.isEmpty()) {
                        DocumentSnapshot latest = predictionHistory.get(predictionHistory.size() - 1);
                        String tip = latest.getString("motivationTip");
                        if (tip != null) tvMotivationTip.setText(tip);
                        Double predVal = latest.getDouble("predictedGpa");
                        tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", predVal != null ? predVal : 0.0));
                    } else {
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
                                    calculateGpasAndPopulateUI(allSemesters);
                                    setupSemesterSelectionBar();
                                } else {
                                    updateGpaDisplays(0, 0, 0, null);
                                    setupPerformanceBarChart(null);
                                }
                            });
                });
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
                
                // Check for special statuses to show/hide note
                boolean hasSpecial = checkForSpecialStatuses(modules);
                tvSpecialStatusNote.setVisibility(hasSpecial ? View.VISIBLE : View.GONE);
                
                Double semGpa = sem.getDouble("semesterGpa");
                tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", semGpa != null ? semGpa : 0.0));
                
                String semName = sem.getString("semesterName");
                tvSemGpaSub.setText(semName != null ? semName : "Semester " + (position + 1));
                tvCumGpaSub.setText("Till " + (semName != null ? semName : "Semester " + (position + 1)));
                
                double totalPts = 0;
                double totalCreds = 0;
                for (int i = 0; i <= position; i++) {
                    DocumentSnapshot s = allSemesters.get(i);
                    List<Map<String, Object>> mods = (List<Map<String, Object>>) s.get("modules");
                    if (mods == null) continue;
                    for (Map<String, Object> m : mods) {
                        String grade = (String) m.get("grade");
                        if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE"))) continue;
                        
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
                    }
                }
                // CGPA Formula: Sum of All Quality Points / Total Credit Hours (Sum of all attempted credits)
                double cumGpa = totalCreds > 0 ? (totalPts / totalCreds) : 0;
                tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", cumGpa));
                
                com.google.firebase.Timestamp semTime = sem.getTimestamp("timestamp");
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
                    
                    if (matchingPred != null) {
                        Double pGpa = matchingPred.getDouble("predictedGpa");
                        tvPredGpa.setText(String.format(java.util.Locale.US, "%.2f", pGpa != null ? pGpa : 0.0));
                        String tip = matchingPred.getString("motivationTip");
                        if (tip != null) tvMotivationTip.setText(tip);
                    }
                }
            }
        });
        rvSemesterSelector.setAdapter(chipAdapter);
        rvSemesterSelector.scrollToPosition(semLabels.size() - 1);
    }

    private boolean checkForSpecialStatuses(List<Map<String, Object>> modules) {
        if (modules == null) return false;
        for (Map<String, Object> m : modules) {
            String grade = (String) m.get("grade");
            if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE"))) {
                return true;
            }
        }
        return false;
    }

    private void calculateGpasAndPopulateUI(List<DocumentSnapshot> semesters) {
        double totalPoints = 0;
        double totalCredits = 0;
        double latestSemGpa = 0;
        List<Map<String, Object>> latestResults = null;
        List<Entry> gpaTrendEntries = new ArrayList<>();
        
        for (int i = 0; i < semesters.size(); i++) {
            DocumentSnapshot sem = semesters.get(i);
            List<Map<String, Object>> modules = (List<Map<String, Object>>) sem.get("modules");
            if (modules == null) continue;
            
            if (i == semesters.size() - 1) { 
                latestSemGpa = sem.getDouble("semesterGpa") != null ? sem.getDouble("semesterGpa") : 0.0;
                latestResults = modules;
            }
            
            for (Map<String, Object> mod : modules) {
                String grade = (String) mod.get("grade");
                double credits = 0;
                Object credObj = mod.get("credits");
                if (credObj instanceof Double) credits = (Double) credObj;
                else if (credObj instanceof Long) credits = ((Long) credObj).doubleValue();
                
                double points = 0;
                Object ptObj = mod.get("grade_point");
                if (ptObj instanceof Double) points = (Double) ptObj;
                else if (ptObj instanceof Long) points = ((Long) ptObj).doubleValue();

                if (grade != null && (grade.equals("MC") || grade.equals("AB") || grade.equals("NE"))) continue;
                
                totalPoints += (points * credits);
                totalCredits += credits;
            }
        }

        for (int i = 0; i < semesters.size(); i++) {
            Double gpa = semesters.get(i).getDouble("semesterGpa");
            gpaTrendEntries.add(new Entry(i, gpa != null ? gpa.floatValue() : 0f));
        }

        // CGPA Formula: Sum of All Quality Points / Total Credit Hours
        double cumulativeGpa = totalCredits > 0 ? (totalPoints / totalCredits) : 0;
        
        tvSemGpa.setText(String.format(java.util.Locale.US, "%.2f", latestSemGpa));
        tvCumGpa.setText(String.format(java.util.Locale.US, "%.2f", cumulativeGpa));
        
        if (semesters.size() > 0) {
            DocumentSnapshot latest = semesters.get(semesters.size() - 1);
            String semName = latest.getString("semesterName");
            tvSemGpaSub.setText(semName != null ? semName : "Semester " + semesters.size());
            tvCumGpaSub.setText("Till " + (semName != null ? semName : "Semester " + semesters.size()));
        }
        
        // Initial visibility check for the latest results
        boolean hasSpecial = checkForSpecialStatuses(latestResults);
        tvSpecialStatusNote.setVisibility(hasSpecial ? View.VISIBLE : View.GONE);
        
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
            styleLineDataSet(predictionDataSet, Color.parseColor("#057BFE"), false);
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

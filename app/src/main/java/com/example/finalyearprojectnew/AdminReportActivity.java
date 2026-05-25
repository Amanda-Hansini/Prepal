package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReportActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private AppCompatButton btnExportStudents, btnDownloadLogs;
    private android.widget.TextView tvTotalStudents, tvTotalBatches, tvTotalDegrees, tvTotalModules;
    private com.github.mikephil.charting.charts.BarChart barChartVolume;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private List<com.google.firebase.firestore.DocumentSnapshot> localBatchesList = new ArrayList<>();
    private List<com.google.firebase.firestore.DocumentSnapshot> localStudentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        initViews();
        setupBottomNavigation();
        setupListeners();
        fetchStats();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationAdmin);
        btnExportStudents = findViewById(R.id.btnExportStudents);
        btnDownloadLogs = findViewById(R.id.btnDownloadLogs);

        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvTotalBatches = findViewById(R.id.tvTotalBatches);
        tvTotalDegrees = findViewById(R.id.tvTotalDegrees);
        tvTotalModules = findViewById(R.id.tvTotalModules);
        barChartVolume = findViewById(R.id.barChartVolume);
    }

    private void fetchStats() {
        // Fetch Total Students in real-time
        db.collection("AllStudents").addSnapshotListener((value, error) -> {
            if (error == null && value != null) {
                tvTotalStudents.setText(String.valueOf(value.size()));
                localStudentsList.clear();
                localStudentsList.addAll(value.getDocuments());
                updateBarChart();
            }
        });

        // Fetch Total Batches in real-time
        db.collection("Batches").addSnapshotListener((value, error) -> {
            if (error == null && value != null) {
                tvTotalBatches.setText(String.valueOf(value.size()));
                localBatchesList.clear();
                localBatchesList.addAll(value.getDocuments());
                updateBarChart();
            }
        });

        // Fetch Total Degrees in real-time
        db.collection("Degrees").addSnapshotListener((value, error) -> {
            if (error == null && value != null) {
                tvTotalDegrees.setText(String.valueOf(value.size()));
            }
        });

        // Fetch Total Modules by combining both Web and Mobile subcollections to ensure perfect consistency
        final int[] mobileModulesCount = {0};
        final int[] webModulesCount = {0};

        db.collectionGroup("Module IDs").addSnapshotListener((value, error) -> {
            if (error == null && value != null) {
                mobileModulesCount[0] = value.size();
                tvTotalModules.setText(String.valueOf(mobileModulesCount[0] + webModulesCount[0]));
            }
        });

        db.collectionGroup("Modules").addSnapshotListener((value, error) -> {
            if (error == null && value != null) {
                webModulesCount[0] = value.size();
                tvTotalModules.setText(String.valueOf(mobileModulesCount[0] + webModulesCount[0]));
            }
        });
    }

    private void updateBarChart() {
        if (localBatchesList.isEmpty() || localStudentsList.isEmpty()) {
            return;
        }

        java.util.ArrayList<com.github.mikephil.charting.data.BarEntry> entries = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();

        for (int i = 0; i < localBatchesList.size(); i++) {
            com.google.firebase.firestore.DocumentSnapshot batchDoc = localBatchesList.get(i);
            String batchId = batchDoc.getString("batchId");
            String batchName = batchDoc.getString("batchName");
            if (batchId == null) continue;

            int count = 0;
            for (com.google.firebase.firestore.DocumentSnapshot studentDoc : localStudentsList) {
                String studentBatchId = studentDoc.getString("batchId");
                if (batchId.equalsIgnoreCase(studentBatchId)) {
                    count++;
                }
            }

            entries.add(new com.github.mikephil.charting.data.BarEntry(i, count));
            labels.add(batchName != null ? batchName : batchId);
        }

        com.github.mikephil.charting.data.BarDataSet dataSet = new com.github.mikephil.charting.data.BarDataSet(entries, "Enrolled Students");
        
        int[] colors = {
                android.graphics.Color.parseColor("#057BFE"), // Premium Blue
                android.graphics.Color.parseColor("#7C3AED"), // Purple
                android.graphics.Color.parseColor("#2DCC70"), // Success Green
                android.graphics.Color.parseColor("#FF9F40")  // Orange
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(android.graphics.Color.DKGRAY);
        dataSet.setValueTextSize(12f);

        com.github.mikephil.charting.data.BarData barData = new com.github.mikephil.charting.data.BarData(dataSet);
        barChartVolume.setData(barData);
        barChartVolume.getDescription().setEnabled(false);

        // Customize axes for high-fidelity mobile design
        com.github.mikephil.charting.components.XAxis xAxis = barChartVolume.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        xAxis.setTextColor(android.graphics.Color.DKGRAY);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-45f); // Prevent label overlapping on mobile screens

        // Push the chart bottom up to make comfortable room for rotated X-Axis labels and the legend below
        barChartVolume.setExtraBottomOffset(55f); 

        // Configure legend (Enrolled Students tag) to sit perfectly below the rotated labels
        com.github.mikephil.charting.components.Legend legend = barChartVolume.getLegend();
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(22f); // Move the tag further down below the rotated labels
        legend.setTextColor(android.graphics.Color.DKGRAY);
        legend.setTextSize(12f);

        com.github.mikephil.charting.components.YAxis leftAxis = barChartVolume.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(android.graphics.Color.DKGRAY);
        leftAxis.setTextSize(11f);

        // Disable right Y axis for cleaner look
        barChartVolume.getAxisRight().setEnabled(false);

        // Dynamic 3D chart projection tilt-like entry animation
        barChartVolume.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        barChartVolume.invalidate();
    }

    private void setupBottomNavigation() {
        // Set report selected
        bottomNavigationView.setSelectedItemId(R.id.nav_report);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AdminReportActivity.this, AdminHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_report) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(AdminReportActivity.this, AdminProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        btnExportStudents.setOnClickListener(v -> showExportFilterDialog());
        btnDownloadLogs.setOnClickListener(v -> downloadActivityLogs());
    }

    private void showExportFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_export_filter, null);
        builder.setView(dialogView);

        Spinner spinnerDegree = dialogView.findViewById(R.id.spinnerExportDegree);
        Spinner spinnerBatch = dialogView.findViewById(R.id.spinnerExportBatch);
        AppCompatButton btnExport = dialogView.findViewById(R.id.btnExportConfirm);

        List<DegreeExportInfo> degreeList = new ArrayList<>();
        List<BatchExportInfo> allBatchesList = new ArrayList<>();
        
        // Initial state
        List<String> degreeNames = new ArrayList<>();
        degreeNames.add("All Degrees");
        
        db.collection("Degrees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String id = doc.getString("id");
                String name = doc.getString("name");
                if (id != null && name != null) {
                    degreeList.add(new DegreeExportInfo(id, name));
                    degreeNames.add(name);
                }
            }
            ArrayAdapter<String> degAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, degreeNames);
            degAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerDegree.setAdapter(degAdapter);
        });

        db.collection("Batches").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String pId = doc.getString("programId");
                String bId = doc.getString("batchId");
                String bName = doc.getString("batchName");
                if (pId != null && bName != null) {
                    allBatchesList.add(new BatchExportInfo(pId, bId, bName));
                }
            }
        });

        spinnerDegree.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedDegName = degreeNames.get(position);
                String selectedDegId = null;
                for (DegreeExportInfo d : degreeList) {
                    if (d.name.equals(selectedDegName)) {
                        selectedDegId = d.id;
                        break;
                    }
                }

                List<String> filteredBatchNames = new ArrayList<>();
                filteredBatchNames.add("All Batches");
                
                for (BatchExportInfo b : allBatchesList) {
                    if (selectedDegId == null || b.programId.equalsIgnoreCase(selectedDegId)) {
                        filteredBatchNames.add(b.batchName);
                    }
                }
                
                ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(AdminReportActivity.this, R.layout.spinner_item, filteredBatchNames);
                batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spinnerBatch.setAdapter(batchAdapter);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnExport.setOnClickListener(v -> {
            String selDegName = spinnerDegree.getSelectedItem().toString();
            String selBatchName = spinnerBatch.getSelectedItem().toString();
            
            String selDegId = "All Degrees";
            for (DegreeExportInfo d : degreeList) {
                if (d.name.equals(selDegName)) {
                    selDegId = d.id;
                    break;
                }
            }
            
            dialog.dismiss();
            performStudentExport(selDegId, selBatchName);
        });

        dialog.show();
    }

    private void performStudentExport(String degreeId, String batchName) {
        Toast.makeText(this, "Searching for students...", Toast.LENGTH_SHORT).show();
        db.collectionGroup("Student IDs").get().addOnSuccessListener(queryDocumentSnapshots -> {
            StringBuilder csv = new StringBuilder("Student ID,Name,Email,Batch,Status\n");
            int count = 0;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String sId = doc.getString("studentId");
                String name = doc.getString("fullName"); // Correct field name is fullName
                String email = doc.getString("email");
                String status = doc.getString("status");
                
                // Get batch info from parent doc
                String parentId = doc.getReference().getParent().getParent().getId();
                // Parent ID format is: programId(batchName)
                
                boolean degMatch = degreeId.equals("All Degrees") || parentId.startsWith(degreeId + "(");
                boolean batchMatch = batchName.equals("All Batches") || parentId.contains("(" + batchName + ")");

                if (degMatch && batchMatch) {
                    csv.append(String.format("%s,%s,%s,%s,%s\n", sId, name, email, batchName, status));
                    count++;
                }
            }

            if (count > 0) {
                String fileName = "Students_" + degreeId.replace(" ", "_") + "_" + batchName.replace(" ", "_") + "_" + System.currentTimeMillis() + ".csv";
                saveCsvToDownloads(csv.toString(), fileName);
                ActivityLogger.logAction(this, "Exported Students", "Filters: " + degreeId + ", " + batchName + " (" + count + " records)");
            } else {
                Toast.makeText(this, "No students found matching these filters", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Helper classes for export filtering
    private static class DegreeExportInfo {
        String id, name;
        DegreeExportInfo(String id, String name) { this.id = id; this.name = name; }
    }
    private static class BatchExportInfo {
        String programId, batchId, batchName;
        BatchExportInfo(String pId, String bId, String bName) { this.programId = pId; this.batchId = bId; this.batchName = bName; }
    }

    private void downloadActivityLogs() {
        Toast.makeText(this, "Fetching Logs...", Toast.LENGTH_SHORT).show();
        db.collection("ActivityLogs").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder csv = new StringBuilder("Timestamp,Admin ID,Action,Details\n");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        String time = ts != null ? sdf.format(ts.toDate()) : "N/A";
                        String admin = doc.getString("adminId");
                        String action = doc.getString("action");
                        String details = doc.getString("details");
                        
                        // Clean details of commas to avoid breaking CSV
                        String cleanDetails = details != null ? details.replace(",", ";") : "";
                        csv.append(String.format("%s,%s,%s,%s\n", time, admin, action, cleanDetails));
                    }
                    
                    String fileName = "ActivityLogs_" + System.currentTimeMillis() + ".csv";
                    saveCsvToDownloads(csv.toString(), fileName);
                    Toast.makeText(this, "Logs downloaded successfully", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCsvToDownloads(String csvData, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            }

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(csvData.getBytes());
                    outputStream.close();
                    Toast.makeText(this, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerformanceReportActivity extends AppCompatActivity {

    private RecyclerView rvPerformance;
    private ImageView ivBack;
    private FirebaseFirestore db;
    private String studentId;
    private PerformanceAdapter adapter;

    public static class ReportItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_MODULE = 1;
        public int type;
        public String headerText;
        public String semesterDocId; // Added to identify the semester record
        public Map<String, Object> moduleData;

        public ReportItem(String headerText, String docId) {
            this.type = TYPE_HEADER;
            this.headerText = headerText;
            this.semesterDocId = docId;
        }

        public ReportItem(Map<String, Object> moduleData) {
            this.type = TYPE_MODULE;
            this.moduleData = moduleData;
        }
    }

    private List<ReportItem> reportItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_report);

        db = FirebaseFirestore.getInstance();
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "STU-0000");

        rvPerformance = findViewById(R.id.rvPerformance);
        ivBack = findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> finish());

        rvPerformance.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new PerformanceAdapter(reportItems, new PerformanceAdapter.OnSemesterActionListener() {
            @Override
            public void onEdit(String semesterName) {
                editSemesterResults(semesterName);
            }

            @Override
            public void onDelete(String semesterName) {
                confirmDeleteSemester(semesterName);
            }
        });
        
        rvPerformance.setAdapter(adapter);
        loadAllPerformanceData();
    }

    private void editSemesterResults(String semesterName) {
        // Open ManualResultEntryActivity in Edit Mode
        Intent intent = new Intent(this, ManualResultEntryActivity.class);
        intent.putExtra("isEditMode", true);
        intent.putExtra("semesterDocId", semesterName);
        startActivity(intent);
        // Note: ManualResultEntryActivity will need logic to load existing data
    }

    private void confirmDeleteSemester(String semesterName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm_delete_results, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.TextView tvDeleteMessage = view.findViewById(R.id.tvDeleteMessage);
        tvDeleteMessage.setText("Are you sure you want to delete all results for " + semesterName + "? This will also remove associated AI predictions.");

        view.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            dialog.dismiss();
            deleteSemesterFromFirestore(semesterName);
        });

        dialog.show();
    }

    private void deleteSemesterFromFirestore(String semesterName) {
        // 1. Delete from SemesterResults
        db.collection("AllStudents").document(studentId)
                .collection("SemesterResults").document(semesterName)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Delete ALL from PredictionHistory since the timeline is now invalid
                    db.collection("AllStudents").document(studentId)
                            .collection("PredictionHistory")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                                Toast.makeText(this, "Results deleted successfully", Toast.LENGTH_SHORT).show();
                                
                                // Redirect to ManualResultEntryActivity so the user can re-enter their results
                                Intent intent = new Intent(PerformanceReportActivity.this, ManualResultEntryActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllPerformanceData() {
        db.collection("AllStudents").document(studentId)
                .collection("SemesterResults")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportItems.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("semesterName");
                        String year = doc.getString("semesterYear");
                        String docId = doc.getId(); // This is the semesterName
                        
                        String header = (year != null && name != null) ? (year + " : " + name) : name;
                        if (header == null) header = "Results";
                        
                        // Add Header
                        reportItems.add(new ReportItem(header + " results", docId));
                        
                        List<Map<String, Object>> modules = (List<Map<String, Object>>) doc.get("modules");
                        if (modules != null) {
                            for (Map<String, Object> mod : modules) {
                                reportItems.add(new ReportItem(mod));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (reportItems.isEmpty()) {
                        Toast.makeText(this, "No performance records found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

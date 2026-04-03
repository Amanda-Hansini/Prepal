package com.example.finalyearprojectnew;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

public class ManageSemestersActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerSemesters;
    private LinearLayout llSemesterDetails;
    private TextView tvSelectedBatchId, tvSelectedSemesterId, tvSelectedAcademicYear, tvSelectedSemesterNo;
    private AppCompatButton btnToggleAddSemester, btnSaveSemester;
    private CardView cardAddSemesterForm;
    private TextView tvCancelAddSemester;

    // Semester Fields
    private EditText etBatchId, etSemesterId, etAcademicYear, etSemesterNo;

    // Mock data for semesters
    private List<Semester> semesterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_semesters);

        initViews();
        setupMockData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerSemesters = findViewById(R.id.spinnerSemesters);
        llSemesterDetails = findViewById(R.id.llSemesterDetails);

        tvSelectedBatchId = findViewById(R.id.tvSelectedBatchId);
        tvSelectedSemesterId = findViewById(R.id.tvSelectedSemesterId);
        tvSelectedAcademicYear = findViewById(R.id.tvSelectedAcademicYear);
        tvSelectedSemesterNo = findViewById(R.id.tvSelectedSemesterNo);

        btnToggleAddSemester = findViewById(R.id.btnToggleAddSemester);
        btnSaveSemester = findViewById(R.id.btnSaveSemester);
        cardAddSemesterForm = findViewById(R.id.cardAddSemesterForm);
        tvCancelAddSemester = findViewById(R.id.tvCancelAddSemester);

        etBatchId = findViewById(R.id.etBatchId);
        etSemesterId = findViewById(R.id.etSemesterId);
        etAcademicYear = findViewById(R.id.etAcademicYear);
        etSemesterNo = findViewById(R.id.etSemesterNo);
    }

    private void setupMockData() {
        semesterList = new ArrayList<>();
        semesterList.add(new Semester("B01", "SEM01", "1st Year", "1st Semester"));
        semesterList.add(new Semester("B01", "SEM02", "1st Year", "2nd Semester"));

        List<String> semesterNames = new ArrayList<>();
        semesterNames.add("Select a Semester...");
        for (Semester s : semesterList) {
            semesterNames.add(s.getSemesterId() + " (" + s.getBatchId() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesterNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemesters.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerSemesters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Semester selected = semesterList.get(position - 1);
                    llSemesterDetails.setVisibility(View.VISIBLE);
                    tvSelectedBatchId.setText("Batch ID: " + selected.getBatchId());
                    tvSelectedSemesterId.setText("Semester ID: " + selected.getSemesterId());
                    tvSelectedAcademicYear.setText("Academic Year: " + selected.getAcademicYear());
                    tvSelectedSemesterNo.setText("Semester No: " + selected.getSemesterNo());
                } else {
                    llSemesterDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                llSemesterDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddSemester.setOnClickListener(v -> {
            cardAddSemesterForm.setVisibility(View.VISIBLE);
            btnToggleAddSemester.setVisibility(View.GONE);
        });

        tvCancelAddSemester.setOnClickListener(v -> closeAddSemesterForm());

        btnSaveSemester.setOnClickListener(v -> {
            String batchId = etBatchId.getText().toString().trim();
            String semesterId = etSemesterId.getText().toString().trim();
            String academicYear = etAcademicYear.getText().toString().trim();
            String semesterNo = etSemesterNo.getText().toString().trim();

            if (batchId.isEmpty() || semesterId.isEmpty() || academicYear.isEmpty() || semesterNo.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate saving
            semesterList.add(new Semester(batchId, semesterId, academicYear, semesterNo));

            // Refresh spinner
            List<String> semesterNames = new ArrayList<>();
            semesterNames.add("Select a Semester...");
            for (Semester s : semesterList) {
                semesterNames.add(s.getSemesterId() + " (" + s.getBatchId() + ")");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageSemestersActivity.this,
                    android.R.layout.simple_spinner_item, semesterNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSemesters.setAdapter(adapter);
            spinnerSemesters.setSelection(semesterList.size()); // select the newly added item

            Toast.makeText(this, "Semester saved successfully", Toast.LENGTH_SHORT).show();
            closeAddSemesterForm();
        });
    }

    private void closeAddSemesterForm() {
        cardAddSemesterForm.setVisibility(View.GONE);
        btnToggleAddSemester.setVisibility(View.VISIBLE);

        etBatchId.setText("");
        etSemesterId.setText("");
        etAcademicYear.setText("");
        etSemesterNo.setText("");
    }

    // Simple inner class to hold semester data
    private static class Semester {
        private String batchId;
        private String semesterId;
        private String academicYear;
        private String semesterNo;

        public Semester(String batchId, String semesterId, String academicYear, String semesterNo) {
            this.batchId = batchId;
            this.semesterId = semesterId;
            this.academicYear = academicYear;
            this.semesterNo = semesterNo;
        }

        public String getBatchId() {
            return batchId;
        }

        public String getSemesterId() {
            return semesterId;
        }

        public String getAcademicYear() {
            return academicYear;
        }

        public String getSemesterNo() {
            return semesterNo;
        }
    }
}

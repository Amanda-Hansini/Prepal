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

public class ManageBatchesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerBatches;
    private LinearLayout llBatchDetails;
    private TextView tvSelectedProgramId, tvSelectedBatchId, tvSelectedBatchName, tvSelectedIntakeYear;
    private AppCompatButton btnToggleAddBatch, btnSaveBatch;
    private CardView cardAddBatchForm;
    private TextView tvCancelAddBatch;

    // Batch Fields
    private EditText etProgramId, etBatchId, etBatchName, etIntakeYear;

    // Student Fields
    private EditText etStudentNo, etStudentId, etStudentFullName, etStudentEmail, etStudentPhone;

    // Mock data for batches
    private List<Batch> batchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_batches);

        initViews();
        setupMockData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerBatches = findViewById(R.id.spinnerBatches);
        llBatchDetails = findViewById(R.id.llBatchDetails);

        tvSelectedProgramId = findViewById(R.id.tvSelectedProgramId);
        tvSelectedBatchId = findViewById(R.id.tvSelectedBatchId);
        tvSelectedBatchName = findViewById(R.id.tvSelectedBatchName);
        tvSelectedIntakeYear = findViewById(R.id.tvSelectedIntakeYear);

        btnToggleAddBatch = findViewById(R.id.btnToggleAddBatch);
        btnSaveBatch = findViewById(R.id.btnSaveBatch);
        cardAddBatchForm = findViewById(R.id.cardAddBatchForm);
        tvCancelAddBatch = findViewById(R.id.tvCancelAddBatch);

        etProgramId = findViewById(R.id.etProgramId);
        etBatchId = findViewById(R.id.etBatchId);
        etBatchName = findViewById(R.id.etBatchName);
        etIntakeYear = findViewById(R.id.etIntakeYear);

        etStudentNo = findViewById(R.id.etStudentNo);
        etStudentId = findViewById(R.id.etStudentId);
        etStudentFullName = findViewById(R.id.etStudentFullName);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPhone = findViewById(R.id.etStudentPhone);
    }

    private void setupMockData() {
        batchList = new ArrayList<>();
        batchList.add(new Batch("BIT", "B01", "Intake 1", "2021"));
        batchList.add(new Batch("BSE", "B02", "Intake 2", "2022"));

        List<String> batchNames = new ArrayList<>();
        batchNames.add("Select a Batch...");
        for (Batch b : batchList) {
            batchNames.add(b.getBatchName() + " (" + b.getProgramId() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, batchNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBatches.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerBatches.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Batch selected = batchList.get(position - 1);
                    llBatchDetails.setVisibility(View.VISIBLE);
                    tvSelectedProgramId.setText("Program ID: " + selected.getProgramId());
                    tvSelectedBatchId.setText("Batch ID: " + selected.getBatchId());
                    tvSelectedBatchName.setText("Name: " + selected.getBatchName());
                    tvSelectedIntakeYear.setText("Intake Year: " + selected.getIntakeYear());
                } else {
                    llBatchDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                llBatchDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddBatch.setOnClickListener(v -> {
            cardAddBatchForm.setVisibility(View.VISIBLE);
            btnToggleAddBatch.setVisibility(View.GONE);
        });

        tvCancelAddBatch.setOnClickListener(v -> closeAddBatchForm());

        btnSaveBatch.setOnClickListener(v -> {
            String programId = etProgramId.getText().toString().trim();
            String batchId = etBatchId.getText().toString().trim();
            String batchName = etBatchName.getText().toString().trim();
            String intakeYear = etIntakeYear.getText().toString().trim();

            String studentNo = etStudentNo.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String name = etStudentFullName.getText().toString().trim();
            String email = etStudentEmail.getText().toString().trim();
            String phone = etStudentPhone.getText().toString().trim();

            if (programId.isEmpty() || batchId.isEmpty() || batchName.isEmpty() || intakeYear.isEmpty()
                    || studentNo.isEmpty() || studentId.isEmpty() || name.isEmpty() || email.isEmpty()
                    || phone.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate saving
            batchList.add(new Batch(programId, batchId, batchName, intakeYear));

            // Refresh spinner
            List<String> batchNames = new ArrayList<>();
            batchNames.add("Select a Batch...");
            for (Batch b : batchList) {
                batchNames.add(b.getBatchName() + " (" + b.getProgramId() + ")");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageBatchesActivity.this,
                    android.R.layout.simple_spinner_item, batchNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerBatches.setAdapter(adapter);
            spinnerBatches.setSelection(batchList.size()); // select the newly added item

            Toast.makeText(this, "Batch and Student saved successfully", Toast.LENGTH_SHORT).show();
            closeAddBatchForm();
        });
    }

    private void closeAddBatchForm() {
        cardAddBatchForm.setVisibility(View.GONE);
        btnToggleAddBatch.setVisibility(View.VISIBLE);

        etProgramId.setText("");
        etBatchId.setText("");
        etBatchName.setText("");
        etIntakeYear.setText("");

        etStudentNo.setText("");
        etStudentId.setText("");
        etStudentFullName.setText("");
        etStudentEmail.setText("");
        etStudentPhone.setText("");
    }

    // Simple inner class to hold batch data
    private static class Batch {
        private String programId;
        private String batchId;
        private String batchName;
        private String intakeYear;

        public Batch(String programId, String batchId, String batchName, String intakeYear) {
            this.programId = programId;
            this.batchId = batchId;
            this.batchName = batchName;
            this.intakeYear = intakeYear;
        }

        public String getProgramId() {
            return programId;
        }

        public String getBatchId() {
            return batchId;
        }

        public String getBatchName() {
            return batchName;
        }

        public String getIntakeYear() {
            return intakeYear;
        }
    }
}

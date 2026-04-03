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

public class ManageDegreesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerDegrees;
    private LinearLayout llDegreeDetails;
    private TextView tvSelectedDegreeId, tvSelectedDegreeName;
    private AppCompatButton btnToggleAddProgram, btnSaveProgram;
    private CardView cardAddProgramForm;
    private TextView tvCancelAddProgram;
    private EditText etProgramId, etProgramName;

    // Mock data for degrees
    private List<Degree> degreeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_degrees);

        initViews();
        setupMockData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerDegrees = findViewById(R.id.spinnerDegrees);
        llDegreeDetails = findViewById(R.id.llDegreeDetails);
        tvSelectedDegreeId = findViewById(R.id.tvSelectedDegreeId);
        tvSelectedDegreeName = findViewById(R.id.tvSelectedDegreeName);

        btnToggleAddProgram = findViewById(R.id.btnToggleAddProgram);
        btnSaveProgram = findViewById(R.id.btnSaveProgram);
        cardAddProgramForm = findViewById(R.id.cardAddProgramForm);
        tvCancelAddProgram = findViewById(R.id.tvCancelAddProgram);

        etProgramId = findViewById(R.id.etProgramId);
        etProgramName = findViewById(R.id.etProgramName);
    }

    private void setupMockData() {
        degreeList = new ArrayList<>();
        degreeList.add(new Degree("BIT", "Bachelor of Information Technology"));
        degreeList.add(new Degree("BSE", "Bachelor of Software Engineering"));
        degreeList.add(new Degree("BCS", "Bachelor of Computer Science"));

        List<String> degreeNames = new ArrayList<>();
        degreeNames.add("Select a Degree...");
        for (Degree d : degreeList) {
            degreeNames.add(d.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, degreeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegrees.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerDegrees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Degree selected = degreeList.get(position - 1);
                    llDegreeDetails.setVisibility(View.VISIBLE);
                    tvSelectedDegreeId.setText("ID: " + selected.getId());
                    tvSelectedDegreeName.setText("Name: " + selected.getName());
                } else {
                    llDegreeDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                llDegreeDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddProgram.setOnClickListener(v -> {
            cardAddProgramForm.setVisibility(View.VISIBLE);
            btnToggleAddProgram.setVisibility(View.GONE);
        });

        tvCancelAddProgram.setOnClickListener(v -> closeAddProgramForm());

        btnSaveProgram.setOnClickListener(v -> {
            String id = etProgramId.getText().toString().trim();
            String name = etProgramName.getText().toString().trim();

            if (id.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please completely fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate saving
            degreeList.add(new Degree(id, name));

            // Refresh spinner
            List<String> degreeNames = new ArrayList<>();
            degreeNames.add("Select a Degree...");
            for (Degree d : degreeList) {
                degreeNames.add(d.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageDegreesActivity.this,
                    android.R.layout.simple_spinner_item, degreeNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDegrees.setAdapter(adapter);
            spinnerDegrees.setSelection(degreeList.size()); // select the newly added item

            Toast.makeText(this, "Program saved successfully", Toast.LENGTH_SHORT).show();
            closeAddProgramForm();
        });
    }

    private void closeAddProgramForm() {
        cardAddProgramForm.setVisibility(View.GONE);
        btnToggleAddProgram.setVisibility(View.VISIBLE);
        etProgramId.setText("");
        etProgramName.setText("");
    }

    // Simple inner class to hold degree data
    private static class Degree {
        private String id;
        private String name;

        public Degree(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

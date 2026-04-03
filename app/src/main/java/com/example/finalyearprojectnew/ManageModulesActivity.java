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

public class ManageModulesActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Spinner spinnerModules;
    private LinearLayout llModuleDetails;
    private TextView tvSelectedSemesterId, tvSelectedModuleId, tvSelectedModuleName, tvSelectedCredits;
    private AppCompatButton btnToggleAddModule, btnSaveModule;
    private CardView cardAddModuleForm;
    private TextView tvCancelAddModule;

    // Module Fields
    private EditText etSemesterId, etModuleId, etModuleName, etCredits;

    // Mock data for modules
    private List<Module> moduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_modules);

        initViews();
        setupMockData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        spinnerModules = findViewById(R.id.spinnerModules);
        llModuleDetails = findViewById(R.id.llModuleDetails);

        tvSelectedSemesterId = findViewById(R.id.tvSelectedSemesterId);
        tvSelectedModuleId = findViewById(R.id.tvSelectedModuleId);
        tvSelectedModuleName = findViewById(R.id.tvSelectedModuleName);
        tvSelectedCredits = findViewById(R.id.tvSelectedCredits);

        btnToggleAddModule = findViewById(R.id.btnToggleAddModule);
        btnSaveModule = findViewById(R.id.btnSaveModule);
        cardAddModuleForm = findViewById(R.id.cardAddModuleForm);
        tvCancelAddModule = findViewById(R.id.tvCancelAddModule);

        etSemesterId = findViewById(R.id.etSemesterId);
        etModuleId = findViewById(R.id.etModuleId);
        etModuleName = findViewById(R.id.etModuleName);
        etCredits = findViewById(R.id.etCredits);
    }

    private void setupMockData() {
        moduleList = new ArrayList<>();
        moduleList.add(new Module("SEM01", "CS101", "Introduction to Computer Science", "3"));
        moduleList.add(new Module("SEM01", "MA101", "Calculus I", "4"));
        moduleList.add(new Module("SEM02", "CS102", "Data Structures", "3"));

        List<String> moduleNames = new ArrayList<>();
        moduleNames.add("Select a Module...");
        for (Module m : moduleList) {
            moduleNames.add(m.getModuleId() + " - " + m.getModuleName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, moduleNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModules.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        spinnerModules.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Module selected = moduleList.get(position - 1);
                    llModuleDetails.setVisibility(View.VISIBLE);
                    tvSelectedSemesterId.setText("Semester ID: " + selected.getSemesterId());
                    tvSelectedModuleId.setText("Module ID: " + selected.getModuleId());
                    tvSelectedModuleName.setText("Module Name: " + selected.getModuleName());
                    tvSelectedCredits.setText("Credits: " + selected.getCredits());
                } else {
                    llModuleDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                llModuleDetails.setVisibility(View.GONE);
            }
        });

        btnToggleAddModule.setOnClickListener(v -> {
            cardAddModuleForm.setVisibility(View.VISIBLE);
            btnToggleAddModule.setVisibility(View.GONE);
        });

        tvCancelAddModule.setOnClickListener(v -> closeAddModuleForm());

        btnSaveModule.setOnClickListener(v -> {
            String semesterId = etSemesterId.getText().toString().trim();
            String moduleId = etModuleId.getText().toString().trim();
            String moduleName = etModuleName.getText().toString().trim();
            String credits = etCredits.getText().toString().trim();

            if (semesterId.isEmpty() || moduleId.isEmpty() || moduleName.isEmpty() || credits.isEmpty()) {
                Toast.makeText(this, "Please completely fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate saving
            moduleList.add(new Module(semesterId, moduleId, moduleName, credits));

            // Refresh spinner
            List<String> moduleNames = new ArrayList<>();
            moduleNames.add("Select a Module...");
            for (Module m : moduleList) {
                moduleNames.add(m.getModuleId() + " - " + m.getModuleName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageModulesActivity.this,
                    android.R.layout.simple_spinner_item, moduleNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModules.setAdapter(adapter);
            spinnerModules.setSelection(moduleList.size()); // select the newly added item

            Toast.makeText(this, "Module saved successfully", Toast.LENGTH_SHORT).show();
            closeAddModuleForm();
        });
    }

    private void closeAddModuleForm() {
        cardAddModuleForm.setVisibility(View.GONE);
        btnToggleAddModule.setVisibility(View.VISIBLE);

        etSemesterId.setText("");
        etModuleId.setText("");
        etModuleName.setText("");
        etCredits.setText("");
    }

    // Simple inner class to hold module data
    private static class Module {
        private String semesterId;
        private String moduleId;
        private String moduleName;
        private String credits;

        public Module(String semesterId, String moduleId, String moduleName, String credits) {
            this.semesterId = semesterId;
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.credits = credits;
        }

        public String getSemesterId() {
            return semesterId;
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getCredits() {
            return credits;
        }
    }
}

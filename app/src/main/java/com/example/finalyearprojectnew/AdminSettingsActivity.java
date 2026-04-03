package com.example.finalyearprojectnew;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminSettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private LinearLayout llChangePassword, llHelpFAQ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        llChangePassword = findViewById(R.id.llChangePassword);
        llHelpFAQ = findViewById(R.id.llHelpFAQ);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        llChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show();
            // TODO: Open Change Password Dialog/Activity
        });

        llHelpFAQ.setOnClickListener(v -> {
            Toast.makeText(this, "Help Center clicked", Toast.LENGTH_SHORT).show();
            // TODO: Open Help Center Web or Activity
        });
    }
}

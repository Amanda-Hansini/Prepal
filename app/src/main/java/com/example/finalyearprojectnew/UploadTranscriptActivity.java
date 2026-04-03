package com.example.finalyearprojectnew;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UploadTranscriptActivity extends AppCompatActivity {

    private LinearLayout llUploadPdfField;
    private TextView tvFileName;
    private AppCompatButton btnUpload;
    private ImageView ivUploadIcon;
    private Uri pdfUri = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    pdfUri = result.getData().getData();
                    if (pdfUri != null) {
                        String fileName = getFileName(pdfUri);
                        tvFileName.setText(fileName);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload_transcript);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        startIconAnimation();
    }

    private void startIconAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(ivUploadIcon, "translationY", 0f, 15f, 0f);
        animator.setDuration(1500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    private void initViews() {
        llUploadPdfField = findViewById(R.id.llUploadPdfField);
        tvFileName = findViewById(R.id.tvFileName);
        btnUpload = findViewById(R.id.btnUpload);
        ivUploadIcon = findViewById(R.id.ivUploadIcon);
    }

    private void setupListeners() {
        llUploadPdfField.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Transcript PDF"));
        });

        btnUpload.setOnClickListener(v -> {
            if (pdfUri == null) {
                Toast.makeText(this, "Please select a PDF file first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Navigate forward or handle upload
            Toast.makeText(this, "Transcript Uploaded Successfully", Toast.LENGTH_SHORT).show();
            // TODO: Implement actual upload logic
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Unknown file";
    }
}

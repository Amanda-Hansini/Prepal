package com.example.finalyearprojectnew;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import java.util.List;

public class AcknowledgementDialogHelper {

    /**
     * Displays a prominent Red Warning & Acknowledgement Dialog if any critical academic/lifestyle warnings were triggered.
     */
    public static void showWarningDialog(Context context, List<String> warnings, Runnable onAcknowledge) {
        if (context == null || warnings == null || warnings.isEmpty()) {
            if (onAcknowledge != null) onAcknowledge.run();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Container
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        rootLayout.setPadding(padding, padding, padding, padding);
        rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Red Header Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText("⚠️ ACKNOWLEDGEMENT REQUIRED");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#D32F2F")); // Deep Red
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, padding / 2);
        rootLayout.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(context);
        tvSub.setText("Our AI analysis detected critical academic or lifestyle risk factors that require your attention:");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvSub.setTextColor(Color.parseColor("#424242"));
        tvSub.setPadding(0, 0, 0, padding / 2);
        rootLayout.addView(tvSub);

        // Scrollable list of warnings
        ScrollView scrollView = new ScrollView(context);
        LinearLayout warningsLayout = new LinearLayout(context);
        warningsLayout.setOrientation(LinearLayout.VERTICAL);

        for (String warning : warnings) {
            LinearLayout warningBox = new LinearLayout(context);
            warningBox.setOrientation(LinearLayout.VERTICAL);
            int boxPad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
            warningBox.setPadding(boxPad, boxPad, boxPad, boxPad);
            warningBox.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background

            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            boxParams.setMargins(0, 0, 0, boxPad / 2);
            warningBox.setLayoutParams(boxParams);

            TextView tvWarningText = new TextView(context);
            tvWarningText.setText("• " + warning);
            tvWarningText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvWarningText.setTextColor(Color.parseColor("#B71C1C")); // Dark red text
            tvWarningText.setTypeface(Typeface.DEFAULT_BOLD);
            warningBox.addView(tvWarningText);

            warningsLayout.addView(warningBox);
        }

        scrollView.addView(warningsLayout);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        scrollView.setLayoutParams(scrollParams);
        rootLayout.addView(scrollView);

        // Acknowledge Button
        AppCompatButton btnAcknowledge = new AppCompatButton(context);
        btnAcknowledge.setText("I ACKNOWLEDGE & UNDERSTAND");
        btnAcknowledge.setTextColor(Color.WHITE);
        btnAcknowledge.setBackgroundColor(Color.parseColor("#D32F2F"));
        btnAcknowledge.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, padding, 0, 0);
        btnAcknowledge.setLayoutParams(btnParams);
        rootLayout.addView(btnAcknowledge);

        builder.setView(rootLayout);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnAcknowledge.setOnClickListener(v -> {
            dialog.dismiss();
            if (onAcknowledge != null) {
                onAcknowledge.run();
            }
        });

        dialog.show();
    }
}

package com.example.finalyearprojectnew;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ActivityLogger {

    public static void logAction(Context context, String action, String details) {
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String adminId = prefs.getString("admin_id", "Unknown Admin");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("action", action);
        log.put("details", details);
        log.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("ActivityLogs").add(log);

        String actionType = "SYSTEM";
        String actionLower = action.toLowerCase();
        if (actionLower.contains("degree") || actionLower.contains("batch") || actionLower.contains("module") || actionLower.contains("semester")) {
            actionType = "CURRICULUM";
        } else if (actionLower.contains("student")) {
            actionType = "ENROLLMENT";
        } else if (actionLower.contains("password") || actionLower.contains("login") || actionLower.contains("2fa")) {
            actionType = "SECURITY";
        }

        SystemAlertHelper.queueSystemAlert(actionType, action, "Admin " + adminId + " performed action: " + action + ".\nDetails: " + details);
    }
}

package com.example.finalyearprojectnew;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SystemAlertHelper {

    private static final String TAG = "SystemAlertHelper";

    /**
     * Queues a system alert email/notification for backend processing.
     * A backend Cloud Function or Node.js worker should listen to the "System_Alert_Emails" collection
     * and dispatch emails to all administrators with emailAlertsEnabled = true.
     *
     * @param actionType The category of the alert (e.g., "SECURITY", "CURRICULUM", "ENROLLMENT")
     * @param title      The title of the alert
     * @param message    The detailed message body
     */
    public static void queueSystemAlert(String actionType, String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> alertDoc = new HashMap<>();
        alertDoc.put("actionType", actionType);
        alertDoc.put("title", title);
        alertDoc.put("message", message);
        alertDoc.put("timestamp", System.currentTimeMillis());
        alertDoc.put("status", "pending"); // for the backend worker to pick up

        db.collection("System_Alert_Emails").add(alertDoc)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "System alert queued successfully: " + actionType))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to queue system alert", e));
    }
}

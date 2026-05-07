package com.example.finalyearprojectnew;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StudentCalendarActivity extends AppCompatActivity implements CalendarEventAdapter.OnEventClickListener {

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private TextView tvEmptyState;
    private View btnAddEvent;
    private BottomNavigationView bottomNavigation;
    private CalendarEventAdapter adapter;
    private String selectedDateStr;
    private String studentId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_calendar);

        db = FirebaseFirestore.getInstance();
        studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("student_id", "");

        initViews();
        setupBottomNavigation();
        setupCalendarAndRecyclerView();

        // Initialize with today's date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateStr = sdf.format(new Date(calendarView.getDate()));

        loadEventsForDate(selectedDateStr);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        btnAddEvent.setOnClickListener(v -> showAddEventDialog());
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        rvEvents = findViewById(R.id.rvEvents);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        // Set Calendar as active item
        bottomNavigation.setSelectedItemId(R.id.nav_calendar);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(StudentCalendarActivity.this, StudentHomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                return true;
            } else if (itemId == R.id.nav_chats) {
                // ChatsActivity not implemented yet
                Toast.makeText(this, "Chats coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(StudentCalendarActivity.this, StudentProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupCalendarAndRecyclerView() {
        adapter = new CalendarEventAdapter();
        adapter.setOnEventClickListener(this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Month is 0-indexed
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDateStr = sdf.format(cal.getTime());

            loadEventsForDate(selectedDateStr);
        });
    }

    private void loadEventsForDate(String dateStr) {
        if (studentId.isEmpty()) return;

        db.collection("AllStudents").document(studentId)
                .collection("CalendarEvents")
                .whereEqualTo("dateString", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CalendarEvent> eventsForDay = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CalendarEvent event = doc.toObject(CalendarEvent.class);
                        event.setEventId(doc.getId());
                        eventsForDay.add(event);
                    }
                    adapter.setEvents(eventsForDay);

                    if (eventsForDay.isEmpty()) {
                        rvEvents.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvEvents.setVisibility(View.VISIBLE);
                        tvEmptyState.setVisibility(View.GONE);
                    }
                });
    }

    private void showAddEventDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_add_calendar_event);

        EditText etTitle = dialog.findViewById(R.id.etEventTitle);
        EditText etDesc = dialog.findViewById(R.id.etEventDescription);
        EditText etTime = dialog.findViewById(R.id.etEventTime);
        EditText etReminderTime = dialog.findViewById(R.id.etReminderTime);
        View btnSave = dialog.findViewById(R.id.btnSaveEvent);
        View btnCancel = dialog.findViewById(R.id.btnCancelEvent);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        // Setup time pickers
        if (etTime != null) {
            etTime.setText(timeFormat.format(Calendar.getInstance().getTime()));
            etTime.setOnClickListener(v -> showTimePicker(etTime));
        }
        if (etReminderTime != null) {
            etReminderTime.setOnClickListener(v -> showTimePicker(etReminderTime));
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String reminder = etReminderTime != null ? etReminderTime.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            CalendarEvent newEvent = new CalendarEvent(title, desc, time, selectedDateStr, reminder);
            saveEventToFirestore(newEvent);

            dialog.dismiss();
            loadEventsForDate(selectedDateStr);
            Toast.makeText(this, "Event added to cloud calendar", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onEditClick(CalendarEvent event) {
        showEditEventDialog(event);
    }

    @Override
    public void onDeleteClick(CalendarEvent event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("AllStudents").document(studentId)
                            .collection("CalendarEvents")
                            .document(event.getEventId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(StudentCalendarActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                                loadEventsForDate(selectedDateStr);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditEventDialog(CalendarEvent eventToEdit) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_add_calendar_event);

        EditText etTitle = dialog.findViewById(R.id.etEventTitle);
        EditText etDesc = dialog.findViewById(R.id.etEventDescription);
        EditText etTime = dialog.findViewById(R.id.etEventTime);
        EditText etReminderTime = dialog.findViewById(R.id.etReminderTime);
        android.widget.Button btnSave = dialog.findViewById(R.id.btnSaveEvent);
        View btnCancel = dialog.findViewById(R.id.btnCancelEvent);

        if (btnSave != null) {
            btnSave.setText("Update Event");
        }

        // Prefill existing data
        if (etTitle != null) etTitle.setText(eventToEdit.getTitle());
        if (etDesc != null) etDesc.setText(eventToEdit.getDescription());
        if (etTime != null) etTime.setText(eventToEdit.getTime());
        if (etReminderTime != null) etReminderTime.setText(eventToEdit.getReminderTime());

        // Setup time pickers
        if (etTime != null) {
            etTime.setOnClickListener(v -> showTimePicker(etTime));
        }
        if (etReminderTime != null) {
            etReminderTime.setOnClickListener(v -> showTimePicker(etReminderTime));
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String reminder = etReminderTime != null ? etReminderTime.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            CalendarEvent updatedEvent = new CalendarEvent(title, desc, time, eventToEdit.getDateString(), reminder);

            db.collection("AllStudents").document(studentId)
                    .collection("CalendarEvents")
                    .document(eventToEdit.getEventId())
                    .set(updatedEvent)
                    .addOnSuccessListener(aVoid -> {
                        scheduleNotification(updatedEvent);
                        dialog.dismiss();
                        loadEventsForDate(selectedDateStr);
                        Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showTimePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar timeCalendar = Calendar.getInstance();
                    timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeCalendar.set(Calendar.MINUTE, minuteOfHour);
                    SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    targetEditText.setText(format.format(timeCalendar.getTime()));
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void saveEventToFirestore(CalendarEvent event) {
        if (studentId.isEmpty()) return;

        db.collection("AllStudents").document(studentId)
                .collection("CalendarEvents")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    showImmediateNotification(event);
                    scheduleNotification(event);
                });
    }

    private void showImmediateNotification(CalendarEvent event) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "event_added_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Event Confirmations", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, StudentCalendarActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_calendar_filled)
                .setContentTitle("Event Added Successfully")
                .setContentText("Your event '" + event.getTitle() + "' for " + event.getDateString() + " has been added.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void scheduleNotification(CalendarEvent event) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date eventDate = sdf.parse(event.getDateString());
            if (eventDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventDate);
            
            if (event.getReminderTime() != null && !event.getReminderTime().isEmpty()) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date parsedTime = timeFormat.parse(event.getReminderTime());
                if (parsedTime != null) {
                    Calendar timeCalendar = Calendar.getInstance();
                    timeCalendar.setTime(parsedTime);
                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, 0);
                }
            } else {
                // Notify at 8:00 AM on the day BEFORE the event
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                calendar.set(Calendar.HOUR_OF_DAY, 8);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
            }

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                // If it's already past the time, just skip scheduling for the past
                return;
            }

            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_date", event.getDateString());

            int requestCode = (int) System.currentTimeMillis();
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, requestCode, intent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permission to set exact alarms is missing.", Toast.LENGTH_LONG).show();
        }
    }
}

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StudentCalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private TextView tvEmptyState;
    private View btnAddEvent;
    private BottomNavigationView bottomNavigation;

    private CalendarEventAdapter adapter;

    // Temporary in-memory storage: DateString (yyyy-MM-dd) -> List of Events
    private static HashMap<String, List<CalendarEvent>> eventsDb = new HashMap<>();

    private String selectedDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_calendar);

        initViews();
        setupBottomNavigation();
        setupCalendarAndRecyclerView();

        // Initialize with today's date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateStr = sdf.format(new Date(calendarView.getDate()));

        loadEventsForDate(selectedDateStr);

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
                // ProfileActivity for student not fully implemented
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupCalendarAndRecyclerView() {
        adapter = new CalendarEventAdapter();
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
        List<CalendarEvent> eventsForDay = eventsDb.getOrDefault(dateStr, new ArrayList<>());
        adapter.setEvents(eventsForDay);

        if (eventsForDay.isEmpty()) {
            rvEvents.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEvents.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddEventDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_add_calendar_event);

        EditText etTitle = dialog.findViewById(R.id.etEventTitle);
        EditText etDesc = dialog.findViewById(R.id.etEventDescription);
        EditText etTime = dialog.findViewById(R.id.etEventTime);
        View btnSave = dialog.findViewById(R.id.btnSaveEvent);
        View btnCancel = dialog.findViewById(R.id.btnCancelEvent);

        // Pre-fill time with current time
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        if (etTime != null) {
            etTime.setText(timeFormat.format(Calendar.getInstance().getTime()));
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            CalendarEvent newEvent = new CalendarEvent(title, desc, time, selectedDateStr);
            addEventToDb(newEvent);

            dialog.dismiss();
            loadEventsForDate(selectedDateStr);
            Toast.makeText(this, "Event added to " + selectedDateStr, Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addEventToDb(CalendarEvent event) {
        if (!eventsDb.containsKey(event.getDateString())) {
            eventsDb.put(event.getDateString(), new ArrayList<>());
        }
        eventsDb.get(event.getDateString()).add(event);
    }
}

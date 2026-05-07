package com.example.finalyearprojectnew;

public class CalendarEvent {
    private String title;
    private String description;
    private String time;
    private String dateString; // e.g., "2026-03-06"
    private String reminderTime; // e.g., "05:00 PM"

    @com.google.firebase.firestore.Exclude
    private String eventId;

    public CalendarEvent() {} // Required for Firestore

    public CalendarEvent(String title, String description, String time, String dateString, String reminderTime) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.dateString = dateString;
        this.reminderTime = reminderTime;
    }

    // Constructor without reminderTime for backwards compatibility
    public CalendarEvent(String title, String description, String time, String dateString) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.dateString = dateString;
        this.reminderTime = null;
    }

    @com.google.firebase.firestore.Exclude
    public String getEventId() {
        return eventId;
    }

    @com.google.firebase.firestore.Exclude
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public String getDateString() {
        return dateString;
    }

    public String getReminderTime() {
        return reminderTime;
    }
}

package com.example.finalyearprojectnew;

public class CalendarEvent {
    private String title;
    private String description;
    private String time;
    private String dateString; // e.g., "2026-03-06"

    public CalendarEvent(String title, String description, String time, String dateString) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.dateString = dateString;
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
}

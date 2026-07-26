package com.example.finalyearprojectnew.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class PredictionRequest {
    @SerializedName("student_id")
    public String studentId;

    @SerializedName("attendance")
    public double attendance;

    @SerializedName("module_attendances")
    public Map<String, Double> moduleAttendances;

    @SerializedName("study_hours")
    public double studyHours;

    @SerializedName("sleep_hours")
    public double sleepHours;

    @SerializedName("screen_time")
    public double screenTime;

    @SerializedName("work_hours")
    public double workHours;

    @SerializedName("stress_level")
    public double stressLevel;

    @SerializedName("study_habits")
    public double studyHabits;

    @SerializedName("gpa")
    public double gpa;

    @SerializedName("cgpa")
    public double cgpa;

    @SerializedName("results")
    public List<Map<String, Object>> results;

    @SerializedName("student_type")
    public int studentType; // 1 for returning, 2 for new 1st sem

    @SerializedName("ol_maths")
    public String olMaths;

    @SerializedName("ol_english")
    public String olEnglish;
}

package com.example.finalyearprojectnew.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class PredictionResponse {
    @SerializedName("student_id")
    public String studentId;

    @SerializedName("semester_gpa")
    public double semesterGpa;

    @SerializedName("predicted_future_gpa")
    public double predictedFutureGpa;

    @SerializedName("motivation_tip")
    public String motivationTip;

    @SerializedName("eligible")
    public boolean eligible;

    @SerializedName("error")
    public String error;

    @SerializedName("ab_modules")
    public List<String> abModules;

    @SerializedName("mc_modules")
    public List<String> mcModules;

    @SerializedName("ne_modules")
    public List<String> neModules;

    @SerializedName("extracted_grades")
    public List<Map<String, Object>> extractedGrades;

    @SerializedName("acknowledgements_required")
    public List<String> acknowledgementsRequired;
}

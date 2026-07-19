package com.example.finalyearprojectnew.models;

public class FriendItem {
    private String studentId;
    private String fullName;
    private String email;
    private String batchId;
    private String programId;
    private String profileImageBase64;
    private String status; // "friend", "pending_sent", "pending_received", "none"

    public FriendItem() {
    }

    public FriendItem(String studentId, String fullName, String email, String batchId, String programId, String profileImageBase64, String status) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.batchId = batchId;
        this.programId = programId;
        this.profileImageBase64 = profileImageBase64;
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

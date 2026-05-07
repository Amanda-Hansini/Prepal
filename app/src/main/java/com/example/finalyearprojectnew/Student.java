package com.example.finalyearprojectnew;

public class Student {
    private String studentId;
    private String fullName;
    private String email;
    private String status;
    private String batchId;
    private String programId;

    public Student() {
    }

    public Student(String studentId, String fullName, String email, String status, String batchId, String programId) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.batchId = batchId;
        this.programId = programId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}

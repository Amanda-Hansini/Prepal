package com.example.finalyearprojectnew.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AiChatRequest {

    @SerializedName("user_message")
    private String userMessage;

    @SerializedName("student_context")
    private Map<String, Object> studentContext;

    public AiChatRequest(String userMessage, Map<String, Object> studentContext) {
        this.userMessage = userMessage;
        this.studentContext = studentContext;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Map<String, Object> getStudentContext() {
        return studentContext;
    }

    public void setStudentContext(Map<String, Object> studentContext) {
        this.studentContext = studentContext;
    }
}

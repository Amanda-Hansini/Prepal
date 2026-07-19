package com.example.finalyearprojectnew.models;

import com.google.gson.annotations.SerializedName;

public class AiChatResponse {

    @SerializedName("reply")
    private String reply;

    @SerializedName("status")
    private String status;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

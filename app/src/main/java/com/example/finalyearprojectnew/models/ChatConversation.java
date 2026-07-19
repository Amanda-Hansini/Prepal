package com.example.finalyearprojectnew.models;

import com.google.firebase.Timestamp;

public class ChatConversation {
    private String chatId;
    private String friendId;
    private String friendName;
    private String friendProfileImageBase64;
    private String lastMessage;
    private String lastSenderId;
    private Timestamp lastUpdated;
    private long unreadCount;

    public ChatConversation() {
    }

    public ChatConversation(String chatId, String friendId, String friendName, String friendProfileImageBase64, String lastMessage, String lastSenderId, Timestamp lastUpdated, long unreadCount) {
        this.chatId = chatId;
        this.friendId = friendId;
        this.friendName = friendName;
        this.friendProfileImageBase64 = friendProfileImageBase64;
        this.lastMessage = lastMessage;
        this.lastSenderId = lastSenderId;
        this.lastUpdated = lastUpdated;
        this.unreadCount = unreadCount;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public String getFriendProfileImageBase64() {
        return friendProfileImageBase64;
    }

    public void setFriendProfileImageBase64(String friendProfileImageBase64) {
        this.friendProfileImageBase64 = friendProfileImageBase64;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}

package com.example.ft_hangouts.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String content;
    private String phoneNumber;
    private long timestamp;
    private boolean isSent; // true if message is sent by user, false if received

    public Message() {
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String content, String phoneNumber, boolean isSent) {
        this.content = content;
        this.phoneNumber = phoneNumber;
        this.timestamp = System.currentTimeMillis();
        this.isSent = isSent;
    }

    public Message(String content, String phoneNumber, long timestamp, boolean isSent) {
        this.content = content;
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.isSent = isSent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }
    
    public String getFormattedTime() {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return format.format(new Date(timestamp));
    }
}
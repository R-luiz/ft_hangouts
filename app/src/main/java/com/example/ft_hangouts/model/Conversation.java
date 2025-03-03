package com.example.ft_hangouts.model;

import java.io.Serializable;

/**
 * Represents a conversation with a contact
 */
public class Conversation implements Serializable {
    private Contact contact;
    private String lastMessage;
    private long timestamp;
    private boolean isOutgoing;

    public Conversation(Contact contact, String lastMessage, long timestamp, boolean isOutgoing) {
        this.contact = contact;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.isOutgoing = isOutgoing;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }
}
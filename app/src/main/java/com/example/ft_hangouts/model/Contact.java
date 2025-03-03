package com.example.ft_hangouts.model;

import java.io.Serializable;

public class Contact implements Serializable {
    private long id;
    private String name;
    private String phoneNumber;
    private String email;
    private String address;
    private byte[] photo;
    private String notes;

    // Default constructor
    public Contact() {
    }

    // Constructor without id (for new contacts)
    public Contact(String name, String phoneNumber, String email, String address, byte[] photo, String notes) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.photo = photo;
        this.notes = notes;
    }

    // Full constructor
    public Contact(long id, String name, String phoneNumber, String email, String address, byte[] photo, String notes) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.photo = photo;
        this.notes = notes;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
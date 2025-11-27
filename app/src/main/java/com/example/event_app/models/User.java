package com.example.event_app.models;

import java.util.ArrayList;
import java.util.List;

/**
 * User Model - Represents a user in the LuckySpot system
 * Users are identified by device ID (no password needed)
 *
 * UPDATED: Added favoriteEvents field for marking favorite events
 */
public class User {

    private String userId;           // Firebase Auth UID
    private String deviceId;         // Android device ID
    private String name;
    private String email;
    private String phoneNumber;      // Optional
    private List<String> roles;      // ["entrant"], ["organizer"], or both
    private boolean notificationsEnabled;
    private long createdAt;
    private long updatedAt;

    // NEW: Favorite events
    private List<String> favoriteEvents;  // List of event IDs marked as favorite

    // Empty constructor for Firebase
    public User() {
        this.roles = new ArrayList<>();
        this.favoriteEvents = new ArrayList<>();
    }

    // Constructor for new users
    public User(String userId, String deviceId, String name, String email) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.roles = new ArrayList<>();
        this.favoriteEvents = new ArrayList<>();
        this.notificationsEnabled = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Role helper methods
    public void addRole(String role) {
        if (roles == null) {
            roles = new ArrayList<>();
        }
        if (!roles.contains(role)) {
            roles.add(role);
        }
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("admin");
    }

    public boolean isOrganizer() {
        return hasRole("organizer");
    }

    public boolean isEntrant() {
        return hasRole("entrant");
    }

    // NEW: Favorite helper methods
    public void addFavorite(String eventId) {
        if (favoriteEvents == null) {
            favoriteEvents = new ArrayList<>();
        }
        if (!favoriteEvents.contains(eventId)) {
            favoriteEvents.add(eventId);
        }
    }

    public void removeFavorite(String eventId) {
        if (favoriteEvents != null) {
            favoriteEvents.remove(eventId);
        }
    }

    public boolean isFavorite(String eventId) {
        return favoriteEvents != null && favoriteEvents.contains(eventId);
    }

    // Getters
    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public List<String> getRoles() { return roles; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public List<String> getFavoriteEvents() { return favoriteEvents; }  // NEW

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setFavoriteEvents(List<String> favoriteEvents) { this.favoriteEvents = favoriteEvents; }  // NEW
}
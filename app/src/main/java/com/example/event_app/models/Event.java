package com.example.event_app.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Event Model
 * Represents an event in the LuckySpot system
 */
public class Event {

    // Firestore document ID (set manually when loading from Firestore)
    private String id;

    // Basic event information
    private boolean geolocationEnabled;
    private Map<String, Map<String, Double>> entrantLocations;
    private String eventId;          // optional: if you also store ID inside the document
    private String name;
    private String description;
    private String organizerId;
    private String status;           // "active", "cancelled", "completed"
    private long createdAt;
    private String posterUrl;
    private String location;

    // Registration and Capacity
    private Long capacity;
    private List<String> waitingList;
    private List<String> signedUpUsers;
    private List<String> selectedList;  // Users selected by lottery
    private List<String> declinedUsers;  // Users who declined invitation
    private String organizerName;
    private Date eventDate;
    private int entrantCount;

    // Timestamps
    @ServerTimestamp
    private Date date;
    @ServerTimestamp
    private Date registrationStartDate;
    @ServerTimestamp
    private Date registrationEndDate;


    // Lottery statistics
    private int totalSelected;
    private int totalCancelled;
    private int totalAttending;

    // Empty constructor required for Firebase
    public Event() {}

    // Constructor for creating new events
    public Event(String eventId, String name, String description, String organizerId) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.totalSelected = 0;
        this.totalCancelled = 0;
        this.totalAttending = 0;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOrganizerId() { return organizerId; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public String getPosterUrl() { return posterUrl; }
    public String getLocation() { return location; }
    public Long getCapacity() { return capacity; }
    public List<String> getWaitingList() { return waitingList; }
    public List<String> getSignedUpUsers() { return signedUpUsers; }
    public List<String> getSelectedList() { return selectedList; }
    public Date getDate() { return date; }
    public Date getRegistrationStartDate() { return registrationStartDate; }
    public Date getRegistrationEndDate() { return registrationEndDate; }
    public int getTotalSelected() { return totalSelected; }
    public int getTotalCancelled() { return totalCancelled; }
    public int getTotalAttending() { return totalAttending; }
    public String getOrganizerName() { return organizerName; }
    public Date getEventDate() { return eventDate; }
    public int getEntrantCount() { return entrantCount; }
    public boolean isGeolocationEnabled() { return geolocationEnabled; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public void setLocation(String location) { this.location = location; }
    public void setCapacity(Long capacity) { this.capacity = capacity; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList; }
    public void setSignedUpUsers(List<String> signedUpUsers) { this.signedUpUsers = signedUpUsers; }
    public void setSelectedList(List<String> selectedList) { this.selectedList = selectedList; }
    public void setDate(Date date) { this.date = date; }
    public void setRegistrationStartDate(Date registrationStartDate) { this.registrationStartDate = registrationStartDate; }
    public void setRegistrationEndDate(Date registrationEndDate) { this.registrationEndDate = registrationEndDate; }
    public void setTotalSelected(int totalSelected) { this.totalSelected = totalSelected; }
    public void setTotalCancelled(int totalCancelled) { this.totalCancelled = totalCancelled; }
    public void setTotalAttending(int totalAttending) { this.totalAttending = totalAttending; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }
    public void setEntrantCount(int entrantCount) { this.entrantCount = entrantCount; }
    public void setGeolocationEnabled(boolean geolocationEnabled) { this.geolocationEnabled = geolocationEnabled; }

    public List<String> getDeclinedUsers() { return declinedUsers; }
    public void setDeclinedUsers(List<String> declinedUsers) { this.declinedUsers = declinedUsers; }

    public Map<String, Map<String, Double>> getEntrantLocations() { return entrantLocations; }
    public void setEntrantLocations(Map<String, Map<String, Double>> entrantLocations) {
        this.entrantLocations = entrantLocations;
    }

    // --- Logic Methods ---
    public double getCancellationRate() {
        if (totalSelected == 0) return 0.0;
        return (double) totalCancelled / totalSelected * 100;
    }

    public boolean hasHighCancellationRate() {
        return getCancellationRate() > 30.0;
    }
}
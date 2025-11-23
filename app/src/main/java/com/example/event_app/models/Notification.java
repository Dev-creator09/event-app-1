package com.example.event_app.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Notification Model
 * Represents an in-app notification for entrants
 *
 * Types:
 * - LOTTERY_WON: Selected in lottery
 * - LOTTERY_LOST: Not selected in lottery
 * - EVENT_REMINDER: Reminder before event starts
 * - ORGANIZER_MESSAGE: Custom message from organizer
 * - INVITATION_SENT: Invited to sign up for event
 */
public class Notification {

    private String notificationId;
    private String userId;              // Recipient user ID
    private String eventId;             // Related event ID
    private String eventName;           // For display purposes
    private String type;                // Type of notification
    private String title;               // Notification title
    private String message;             // Notification message
    private boolean read;               // Has user seen this?
    private long createdAt;             // Timestamp

    @ServerTimestamp
    private Date timestamp;

    // Notification types
    public static final String TYPE_LOTTERY_WON = "lottery_won";
    public static final String TYPE_LOTTERY_LOST = "lottery_lost";
    public static final String TYPE_EVENT_REMINDER = "event_reminder";
    public static final String TYPE_ORGANIZER_MESSAGE = "organizer_message";
    public static final String TYPE_INVITATION_SENT = "invitation_sent";
    public static final String TYPE_WAITLIST_JOINED = "waitlist_joined";
    public static final String TYPE_INVITATION_DECLINED = "invitation_declined";

    // Empty constructor for Firebase
    public Notification() {
    }

    /**
     * Constructor for creating new notifications
     */
    public Notification(String userId, String eventId, String eventName, String type,
                        String title, String message) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = false;
        this.createdAt = System.currentTimeMillis();
    }

    // --- Getters ---
    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public long getCreatedAt() { return createdAt; }
    public Date getTimestamp() { return timestamp; }

    // --- Setters ---
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRead(boolean read) { this.read = read; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    /**
     * Helper method to get notification icon based on type
     */
    public String getIcon() {
        switch (type) {
            case TYPE_LOTTERY_WON:
                return "🎉";
            case TYPE_LOTTERY_LOST:
                return "😔";
            case TYPE_EVENT_REMINDER:
                return "⏰";
            case TYPE_ORGANIZER_MESSAGE:
                return "📢";
            case TYPE_INVITATION_SENT:
                return "✉️";
            case TYPE_WAITLIST_JOINED:
                return "✓";
            default:
                return "🔔";
        }
    }

    /**
     * Helper method to determine if notification is important
     */
    public boolean isImportant() {
        return type.equals(TYPE_LOTTERY_WON) ||
                type.equals(TYPE_INVITATION_SENT) ||
                type.equals(TYPE_EVENT_REMINDER);
    }
}

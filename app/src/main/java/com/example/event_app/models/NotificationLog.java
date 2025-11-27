package com.example.event_app.models;

import java.util.Date;

/**
 * NotificationLog - Model for tracking all notifications sent
 * Used for admin auditing and compliance
 */
public class NotificationLog {

    private String logId;
    private String notificationId;
    private String senderId;           // Who sent it (organizer/admin)
    private String senderName;
    private String recipientId;        // Who received it
    private String recipientName;
    private String eventId;            // Related event (if applicable)
    private String eventName;
    private String notificationType;   // "invitation_sent", "selected", "rejected", etc.
    private String title;
    private String message;
    private Date timestamp;
    private String status;             // "sent", "delivered", "failed"

    // Empty constructor for Firebase
    public NotificationLog() {
    }

    public NotificationLog(String logId, String notificationId,
                           String senderId, String senderName,
                           String recipientId, String recipientName,
                           String eventId, String eventName,
                           String notificationType, String title,
                           String message, Date timestamp, String status) {
        this.logId = logId;
        this.notificationId = notificationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.eventId = eventId;
        this.eventName = eventName;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
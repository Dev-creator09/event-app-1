package com.example.event_app.models;

import java.util.Date;

/**
 * NotificationTemplate - Model for managing notification templates
 * Used across the app for consistent messaging
 */
public class NotificationTemplate {

    private String templateId;
    private String name;               // Template name (e.g., "Invitation Sent")
    private String type;               // Template type/category
    private String title;              // Notification title with placeholders
    private String message;            // Notification message with placeholders
    private boolean isActive;          // Whether template is currently in use
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;          // Admin who created it

    // Placeholders that can be used: {userName}, {eventName}, {organizerName}, {date}, etc.

    // Empty constructor for Firebase
    public NotificationTemplate() {
    }

    public NotificationTemplate(String templateId, String name, String type,
                                String title, String message, boolean isActive,
                                Date createdAt, Date updatedAt, String createdBy) {
        this.templateId = templateId;
        this.name = name;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    // Getters and setters
    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Replace placeholders in the template with actual values
     */
    public String applyPlaceholders(String text, String userName, String eventName,
                                    String organizerName, String date) {
        if (text == null) return "";

        return text.replace("{userName}", userName != null ? userName : "")
                .replace("{eventName}", eventName != null ? eventName : "")
                .replace("{organizerName}", organizerName != null ? organizerName : "")
                .replace("{date}", date != null ? date : "");
    }
}
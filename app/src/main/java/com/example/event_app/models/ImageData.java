package com.example.event_app.models;

/**
 * ImageData Model - Represents an uploaded image
 * Used for event posters and profile pictures
 */
public class ImageData {

    private String imageId;
    private String imageUrl;
    private String uploadedBy;      // userId who uploaded
    private String associatedWith;  // eventId or userId
    private String type;            // "event_poster" or "profile_picture"
    private long uploadedAt;
    private String storagePath;     // ✨ Firebase Storage path (e.g., "event_posters/image.jpg")

    // Empty constructor for Firebase
    public ImageData() {
    }

    public ImageData(String imageId, String imageUrl, String uploadedBy, String type) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.uploadedBy = uploadedBy;
        this.type = type;
        this.uploadedAt = System.currentTimeMillis();
    }

    // Getters
    public String getImageId() { return imageId; }
    public String getImageUrl() { return imageUrl; }
    public String getUploadedBy() { return uploadedBy; }
    public String getAssociatedWith() { return associatedWith; }
    public String getType() { return type; }
    public long getUploadedAt() { return uploadedAt; }
    public String getStoragePath() { return storagePath; }

    // Setters
    public void setImageId(String imageId) { this.imageId = imageId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setAssociatedWith(String associatedWith) { this.associatedWith = associatedWith; }
    public void setType(String type) { this.type = type; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    // ✨ Convenience method to get uploader ID (handles both field names)
    public String getUploaderId() {
        return uploadedBy;
    }
}
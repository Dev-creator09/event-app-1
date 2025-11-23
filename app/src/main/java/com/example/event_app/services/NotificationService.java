package com.example.event_app.services;

import android.util.Log;

import com.example.event_app.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService - Handles all notification operations
 *
 * Features:
 * - Send notifications to users
 * - Fetch user notifications
 * - Mark notifications as read
 * - Delete notifications
 * - Get unread count
 * - Respects user notification preferences
 */
public class NotificationService {

    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore db;

    public NotificationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Send a notification to a single user
     * Checks user preferences before sending
     *
     * @param userId Recipient user ID
     * @param eventId Related event ID
     * @param eventName Event name for display
     * @param type Notification type (use Notification.TYPE_* constants)
     * @param title Notification title
     * @param message Notification message
     * @param callback Success/failure callback
     */
    public void sendNotification(String userId, String eventId, String eventName,
                                 String type, String title, String message,
                                 NotificationCallback callback) {

        // ✨ First, check if user has notifications enabled
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");

                        if (notificationsEnabled != null && !notificationsEnabled) {
                            // User has disabled notifications
                            Log.d(TAG, "Notifications disabled for user: " + userId);
                            if (callback != null) {
                                callback.onSuccess(); // Don't treat as failure
                            }
                            return;
                        }
                    }

                    // User has notifications enabled, proceed
                    createAndSendNotification(userId, eventId, eventName, type, title, message, callback);
                })
                .addOnFailureListener(e -> {
                    // If we can't check preference, send anyway (fail-open)
                    Log.w(TAG, "Could not check notification preference, sending anyway", e);
                    createAndSendNotification(userId, eventId, eventName, type, title, message, callback);
                });
    }

    /**
     * ✨ NEW: Internal method to create and send notification
     * Original notification creation logic moved here
     */
    private void createAndSendNotification(String userId, String eventId, String eventName,
                                           String type, String title, String message,
                                           NotificationCallback callback) {
        // Create notification object
        Notification notification = new Notification(userId, eventId, eventName, type, title, message);

        // Generate notification ID
        String notificationId = db.collection(COLLECTION_NOTIFICATIONS).document().getId();
        notification.setNotificationId(notificationId);

        // Save to Firestore
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification sent to user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Send notifications to multiple users
     *
     * @param userIds List of recipient user IDs
     * @param eventId Related event ID
     * @param eventName Event name for display
     * @param type Notification type
     * @param title Notification title
     * @param message Notification message
     * @param callback Batch operation callback
     */
    public void sendBulkNotifications(List<String> userIds, String eventId, String eventName,
                                      String type, String title, String message,
                                      BulkNotificationCallback callback) {

        int total = userIds.size();
        int[] completed = {0};
        int[] failed = {0};

        for (String userId : userIds) {
            sendNotification(userId, eventId, eventName, type, title, message, new NotificationCallback() {
                @Override
                public void onSuccess() {
                    completed[0]++;
                    checkCompletion();
                }

                @Override
                public void onFailure(String error) {
                    completed[0]++;
                    failed[0]++;
                    checkCompletion();
                }

                private void checkCompletion() {
                    if (completed[0] == total) {
                        if (callback != null) {
                            callback.onComplete(total - failed[0], failed[0]);
                        }
                    }
                }
            });
        }
    }

    /**
     * Fetch all notifications for a user
     *
     * @param userId User ID
     * @param callback Callback with list of notifications
     */
    public void getUserNotifications(String userId, NotificationListCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    });

                    Log.d(TAG, "Fetched " + notifications.size() + " notifications for user: " + userId);

                    if (callback != null) {
                        callback.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch notifications", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Get count of unread notifications for a user
     *
     * @param userId User ID
     * @param callback Callback with unread count
     */
    public void getUnreadCount(String userId, UnreadCountCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    Log.d(TAG, "Unread notifications for user " + userId + ": " + count);

                    if (callback != null) {
                        callback.onSuccess(count);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get unread count", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Mark a notification as read
     *
     * @param notificationId Notification ID
     * @param callback Success/failure callback
     */
    public void markAsRead(String notificationId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark notification as read", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Mark all notifications as read for a user
     *
     * @param userId User ID
     * @param callback Success/failure callback
     */
    public void markAllAsRead(String userId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count == 0) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return;
                    }

                    queryDocumentSnapshots.forEach(doc -> {
                        doc.getReference().update("read", true);
                    });

                    Log.d(TAG, "Marked " + count + " notifications as read");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark all as read", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Delete a notification
     *
     * @param notificationId Notification ID
     * @param callback Success/failure callback
     */
    public void deleteNotification(String notificationId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete notification", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Delete all notifications for a user
     *
     * @param userId User ID
     * @param callback Success/failure callback
     */
    public void deleteAllNotifications(String userId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    queryDocumentSnapshots.forEach(doc -> doc.getReference().delete());

                    Log.d(TAG, "Deleted all notifications for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete all notifications", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    // Callback interfaces
    public interface NotificationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(String error);
    }

    public interface UnreadCountCallback {
        void onSuccess(int count);
        void onFailure(String error);
    }

    public interface BulkNotificationCallback {
        void onComplete(int successCount, int failureCount);
    }
}
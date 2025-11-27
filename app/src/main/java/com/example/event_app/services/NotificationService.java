package com.example.event_app.services;

import android.util.Log;

import com.example.event_app.models.Notification;
import com.example.event_app.models.NotificationLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
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
 * - Logs all notifications for admin audit trail
 */
public class NotificationService {

    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_NOTIFICATION_LOGS = "notification_logs";

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

                            // Still log for admin audit (user opted out)
                            logNotification(null, "System", userId,
                                    documentSnapshot.getString("name"),
                                    eventId, eventName, type, title, message, "blocked_user_preference");

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
     * ✨ Internal method to create and send notification
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

                    // ✨ NEW: Log notification for admin audit
                    logNotificationAfterSend(notificationId, userId, eventId, eventName,
                            type, title, message, "sent");

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);

                    // ✨ NEW: Log failed notification attempt
                    logNotification(null, "System", userId, null,
                            eventId, eventName, type, title, message, "failed");

                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * ✨ NEW: Log notification after successful send (with recipient details)
     */
    private void logNotificationAfterSend(String notificationId, String recipientId,
                                          String eventId, String eventName,
                                          String type, String title, String message,
                                          String status) {
        // Fetch recipient details for complete log
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(userDoc -> {
                    String recipientName = userDoc.exists() ? userDoc.getString("name") : "Unknown User";

                    logNotification(null, "System", recipientId, recipientName,
                            eventId, eventName, type, title, message, status, notificationId);
                })
                .addOnFailureListener(e -> {
                    // Log anyway with unknown recipient
                    logNotification(null, "System", recipientId, "Unknown User",
                            eventId, eventName, type, title, message, status, notificationId);
                });
    }

    /**
     * ✨ NEW: Log notification for admin audit trail
     *
     * @param senderId ID of sender (null for system notifications)
     * @param senderName Name of sender
     * @param recipientId ID of recipient user
     * @param recipientName Name of recipient
     * @param eventId Related event ID
     * @param eventName Event name
     * @param type Notification type
     * @param title Notification title
     * @param message Notification message
     * @param status Status: "sent", "failed", "blocked_user_preference"
     */
    private void logNotification(String senderId, String senderName,
                                 String recipientId, String recipientName,
                                 String eventId, String eventName,
                                 String type, String title, String message,
                                 String status) {
        logNotification(senderId, senderName, recipientId, recipientName,
                eventId, eventName, type, title, message, status, null);
    }

    /**
     * ✨ NEW: Log notification for admin audit trail (with notification ID)
     */
    private void logNotification(String senderId, String senderName,
                                 String recipientId, String recipientName,
                                 String eventId, String eventName,
                                 String type, String title, String message,
                                 String status, String notificationId) {

        String logId = db.collection(COLLECTION_NOTIFICATION_LOGS).document().getId();

        NotificationLog log = new NotificationLog(
                logId,
                notificationId,  // Can be null for failed/blocked notifications
                senderId != null ? senderId : "system",
                senderName != null ? senderName : "System",
                recipientId,
                recipientName != null ? recipientName : "Unknown",
                eventId,
                eventName,
                type,
                title,
                message,
                new Date(),
                status
        );

        db.collection(COLLECTION_NOTIFICATION_LOGS)
                .document(logId)
                .set(log)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Notification logged for audit: " + logId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error logging notification for audit", e);
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
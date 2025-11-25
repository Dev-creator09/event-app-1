package com.example.event_app.activities.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.NotificationAdapter;
import com.example.event_app.models.Notification;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsActivity - View all notifications
 *
 * Features:
 * - View all notifications
 * - Mark as read
 * - Delete notifications
 * - Navigate to related events
 *
 * US 01.04.01: Receive notification when chosen
 * US 01.04.02: Receive notification when not chosen
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    // UI Components
    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private MaterialButton btnMarkAllRead, btnClearAll;

    // Data
    private NotificationAdapter adapter;
    private NotificationService notificationService;
    private List<Notification> notifications;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        notificationService = new NotificationService();
        notifications = new ArrayList<>();

        // Initialize views
        initViews();
        setupRecyclerView();
        setupListeners();

        // Load notifications
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAll = findViewById(R.id.btnClearAll);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notifications, new NotificationAdapter.NotificationClickListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                // Mark as read when clicked
                if (!notification.isRead()) {
                    markAsRead(notification);
                }

                // Navigate to event details
                navigateToEvent(notification.getEventId());
            }

            @Override
            public void onDeleteClick(Notification notification) {
                deleteNotification(notification);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());
    }

    private void loadNotifications() {
        showLoading();

        String userId = mAuth.getCurrentUser().getUid();

        notificationService.getUserNotifications(userId, new NotificationService.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> fetchedNotifications) {
                notifications.clear();
                notifications.addAll(fetchedNotifications);

                if (notifications.isEmpty()) {
                    showEmpty();
                } else {
                    showNotifications();
                }

                adapter.notifyDataSetChanged();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load notifications: " + error);
                showEmpty();
            }
        });
    }

    private void markAsRead(Notification notification) {
        notificationService.markAsRead(notification.getNotificationId(),
                new NotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                        updateButtonStates();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to mark as read: " + error);
                    }
                });
    }

    private void markAllAsRead() {
        String userId = mAuth.getCurrentUser().getUid();

        notificationService.markAllAsRead(userId, new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess() {
                // Update local list
                for (Notification notification : notifications) {
                    notification.setRead(true);
                }
                adapter.notifyDataSetChanged();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to mark all as read: " + error);
            }
        });
    }

    private void deleteNotification(Notification notification) {
        notificationService.deleteNotification(notification.getNotificationId(),
                new NotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        notifications.remove(notification);
                        adapter.notifyDataSetChanged();

                        if (notifications.isEmpty()) {
                            showEmpty();
                        }

                        updateButtonStates();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to delete notification: " + error);
                    }
                });
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications? This cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAll() {
        String userId = mAuth.getCurrentUser().getUid();

        notificationService.deleteAllNotifications(userId, new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess() {
                notifications.clear();
                adapter.notifyDataSetChanged();
                showEmpty();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to clear all notifications: " + error);
            }
        });
    }

    private void navigateToEvent(String eventId) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    private void updateButtonStates() {
        boolean hasNotifications = !notifications.isEmpty();
        boolean hasUnread = false;

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                hasUnread = true;
                break;
            }
        }

        btnMarkAllRead.setEnabled(hasUnread);
        btnMarkAllRead.setAlpha(hasUnread ? 1.0f : 0.5f);

        btnClearAll.setEnabled(hasNotifications);
        btnClearAll.setAlpha(hasNotifications ? 1.0f : 0.5f);
    }

    private void showLoading() {
        rvNotifications.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showNotifications() {
        rvNotifications.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        rvNotifications.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }
}

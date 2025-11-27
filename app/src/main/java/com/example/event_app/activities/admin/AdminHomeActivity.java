package com.example.event_app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.ReportExporter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminHomeActivity - Admin dashboard with moderation tools and statistics
 *
 * Features:
 * - Platform statistics (events, users, organizers)
 * - Browse/manage events, users, images
 * - Generate platform reports
 * - Switch back to entrant/organizer view
 * - Flagged content monitoring
 * - Geolocation audit for privacy compliance
 * - Notification logs for compliance tracking
 * - Notification templates management
 *
 * US 03.04.01: Browse events
 * US 03.05.01: Browse profiles
 * US 03.06.01: Browse images
 * US 03.13.01: Export platform usage reports
 *
 * UPDATED: Added geolocation audit, notification logs, and notification templates buttons
 */
public class AdminHomeActivity extends AppCompatActivity {

    private static final String TAG = "AdminHomeActivity";

    // UI Components - Statistics
    private TextView tvEventsCount, tvUsersCount, tvOrganizersCount, tvActiveCount;

    // UI Components - Cards
    private MaterialCardView cardBrowseEvents, cardBrowseUsers, cardBrowseImages;

    // UI Components - Buttons
    private MaterialButton btnGenerateReports, btnFlaggedItems, btnSwitchToUserMode;
    private Button btnGeolocationAudit, btnNotificationLogs, btnNotificationTemplates;

    private View layoutFlaggedSection;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        Log.d(TAG, "AdminHomeActivity created");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check admin access first
        checkAdminAccess();

        // Initialize views
        initViews();

        // Set up button listeners
        setupButtonListeners();

        // Load statistics
        loadStatistics();

        // Load flagged events
        loadFlaggedEvents();
    }

    /**
     * Initialize all view components
     */
    private void initViews() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Statistics TextViews
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvOrganizersCount = findViewById(R.id.tvOrganizersCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        // Action Cards
        cardBrowseEvents = findViewById(R.id.cardBrowseEvents);
        cardBrowseUsers = findViewById(R.id.cardBrowseUsers);
        cardBrowseImages = findViewById(R.id.cardBrowseImages);

        // Buttons
        btnGenerateReports = findViewById(R.id.btnGenerateReports);
        btnFlaggedItems = findViewById(R.id.btnFlaggedItems);
        btnSwitchToUserMode = findViewById(R.id.btnSwitchToUserMode);
        btnGeolocationAudit = findViewById(R.id.btnGeolocationAudit);
        btnNotificationLogs = findViewById(R.id.btnNotificationLogs);
        btnNotificationTemplates = findViewById(R.id.btnNotificationTemplates);

        // Flagged section
        layoutFlaggedSection = findViewById(R.id.layoutFlaggedSection);
    }

    /**
     * Setup click listeners for all buttons and cards
     */
    private void setupButtonListeners() {
        // Browse Events Card
        cardBrowseEvents.setOnClickListener(v -> {
            Log.d(TAG, "Browse Events clicked");
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        // Browse Users Card
        cardBrowseUsers.setOnClickListener(v -> {
            Log.d(TAG, "Browse Users clicked");
            Intent intent = new Intent(this, AdminBrowseUsersActivity.class);
            startActivity(intent);
        });

        // Browse Images Card
        cardBrowseImages.setOnClickListener(v -> {
            Log.d(TAG, "Browse Images clicked");
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        // Generate Reports Button
        if (btnGenerateReports != null) {
            btnGenerateReports.setOnClickListener(v -> {
                Log.d(TAG, "Generate Reports clicked");
                generateAndExportReport();
            });
        }

        // ✨ Geolocation Audit Button
        if (btnGeolocationAudit != null) {
            btnGeolocationAudit.setOnClickListener(v -> {
                Log.d(TAG, "Geolocation Audit clicked");
                startActivity(new Intent(this, AdminGeolocationAuditActivity.class));
            });
        } else {
            Log.e(TAG, "❌ btnGeolocationAudit is null - check XML layout!");
        }

        // ✨ Notification Logs Button
        if (btnNotificationLogs != null) {
            btnNotificationLogs.setOnClickListener(v -> {
                Log.d(TAG, "Notification Logs clicked");
                startActivity(new Intent(this, AdminNotificationLogsActivity.class));
            });
        } else {
            Log.e(TAG, "❌ btnNotificationLogs is null - check XML layout!");
        }

        // ✨ NEW: Notification Templates Button
        if (btnNotificationTemplates != null) {
            btnNotificationTemplates.setOnClickListener(v -> {
                Log.d(TAG, "Notification Templates clicked");
                startActivity(new Intent(this, AdminNotificationTemplatesActivity.class));
            });
        } else {
            Log.e(TAG, "❌ btnNotificationTemplates is null - check XML layout!");
        }

        // Flagged Items Button
        if (btnFlaggedItems != null) {
            btnFlaggedItems.setOnClickListener(v -> {
                Log.d(TAG, "Flagged Items clicked");
                Toast.makeText(this, "Showing flagged events", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.putExtra("showFlaggedOnly", true);
                startActivity(intent);
            });
        }

        // ✨ Switch to User Mode Button
        if (btnSwitchToUserMode != null) {
            btnSwitchToUserMode.setOnClickListener(v -> showRoleSwitchDialog());
        }
    }

    /**
     * Check if current user has admin privileges
     */
    private void checkAdminAccess() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);

                        if (currentUser == null || !currentUser.isAdmin()) {
                            // Not an admin - deny access
                            Toast.makeText(this, "⛔ Admin access required", Toast.LENGTH_LONG).show();
                            Log.w(TAG, "Non-admin user attempted to access admin panel");
                            finish();
                        } else {
                            Log.d(TAG, "✅ Admin access verified for user: " + userId);
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking admin access", e);
                    Toast.makeText(this, "Error verifying access", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Load all platform statistics from Firebase
     */
    private void loadStatistics() {
        Log.d(TAG, "Loading platform statistics...");

        // Load Events Count
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvEventsCount.setText(String.valueOf(count));
                    Log.d(TAG, "Events count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvEventsCount.setText("0");
                    Log.e(TAG, "Error loading events count", e);
                });

        // Load Users Count
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvUsersCount.setText(String.valueOf(count));
                    Log.d(TAG, "Users count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvUsersCount.setText("0");
                    Log.e(TAG, "Error loading users count", e);
                });

        // Load Organizers Count
        db.collection("users")
                .whereArrayContains("roles", "organizer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvOrganizersCount.setText(String.valueOf(count));
                    Log.d(TAG, "Organizers count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvOrganizersCount.setText("0");
                    Log.e(TAG, "Error loading organizers count", e);
                });

        // Load Active Events Count
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvActiveCount.setText(String.valueOf(count));
                    Log.d(TAG, "Active events count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvActiveCount.setText("0");
                    Log.e(TAG, "Error loading active events count", e);
                });
    }

    /**
     * Load and display flagged events (high cancellation rate)
     */
    private void loadFlaggedEvents() {
        Log.d(TAG, "Loading flagged events...");

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int flaggedCount = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event.hasHighCancellationRate()) {
                            flaggedCount++;
                        }
                    }

                    // Update button text with count
                    if (btnFlaggedItems != null) {
                        btnFlaggedItems.setText("View Flagged (" + flaggedCount + ")");
                    }

                    Log.d(TAG, "Found " + flaggedCount + " flagged events");

                    // Show/hide flagged events section
                    if (flaggedCount > 0 && layoutFlaggedSection != null) {
                        layoutFlaggedSection.setVisibility(View.VISIBLE);
                    } else if (layoutFlaggedSection != null) {
                        layoutFlaggedSection.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading flagged events", e);
                    if (btnFlaggedItems != null) {
                        btnFlaggedItems.setText("View Flagged (0)");
                    }
                });
    }

    /**
     * Generate and export platform usage report
     * US 03.13.01: Export platform usage reports
     */
    private void generateAndExportReport() {
        Toast.makeText(this, "Generating report...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting report generation...");

        // Fetch all events
        db.collection("events")
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : eventSnapshots) {
                        events.add(doc.toObject(Event.class));
                    }

                    Log.d(TAG, "Loaded " + events.size() + " events for report");

                    // Fetch all users
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                List<User> users = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : userSnapshots) {
                                    users.add(doc.toObject(User.class));
                                }

                                Log.d(TAG, "Loaded " + users.size() + " users for report");

                                // Export report
                                ReportExporter.exportPlatformReport(this, events, users);
                                Toast.makeText(this, "✅ Report generated!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading users for report", e);
                                Toast.makeText(this, "Error loading users: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events for report", e);
                    Toast.makeText(this, "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✨ Show dialog to switch between admin and user views
     */
    private void showRoleSwitchDialog() {
        if (currentUser == null) return;

        // Build role options
        StringBuilder roleMessage = new StringBuilder();
        roleMessage.append("You have multiple roles. Switch to:\n\n");

        boolean hasOtherRoles = false;

        if (currentUser.isEntrant()) {
            roleMessage.append("📱 Entrant View - Join events, manage waiting lists\n\n");
            hasOtherRoles = true;
        }

        if (currentUser.isOrganizer()) {
            roleMessage.append("🎯 Organizer View - Create and manage events\n\n");
            hasOtherRoles = true;
        }

        if (!hasOtherRoles) {
            Toast.makeText(this, "You only have admin role", Toast.LENGTH_SHORT).show();
            return;
        }

        roleMessage.append("You can return to Admin Panel anytime from the menu.");

        new AlertDialog.Builder(this)
                .setTitle("🔄 Switch View")
                .setMessage(roleMessage.toString())
                .setPositiveButton("Switch to User Mode", (dialog, which) -> switchToUserMode())
                .setNegativeButton("Stay in Admin", null)
                .show();
    }

    /**
     * ✨ Switch to entrant/organizer view (MainActivity)
     */
    private void switchToUserMode() {
        Log.d(TAG, "Switching from Admin to User mode");

        // Go back to MainActivity (normal user interface)
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "👤 Switched to User mode", Toast.LENGTH_SHORT).show();
    }

    /**
     * Reload statistics when activity resumes
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed, reloading statistics");

        // Re-verify admin access
        checkAdminAccess();

        // Reload data
        loadStatistics();
        loadFlaggedEvents();
    }
}
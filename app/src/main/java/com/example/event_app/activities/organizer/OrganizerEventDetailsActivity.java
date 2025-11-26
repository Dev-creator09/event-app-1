package com.example.event_app.activities.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.event_app.R;
import com.example.event_app.activities.organizer.ViewEntrantsActivity;
import com.example.event_app.activities.organizer.ViewEntrantMapActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.Notification;
import com.example.event_app.models.User;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.button.MaterialButton;

/**
 * OrganizerEventDetailsActivity - Comprehensive event management with notifications
 *
 * Features:
 * - Run lottery and select winners (with notifications)
 * - Draw replacement from pool (US 02.05.03)
 * - View entrants in different states
 * - View map of entrant locations
 * - Generate and view QR code
 * - Send notifications to entrants
 * - Export entrant lists to CSV
 * - Update event poster
 * - Send event reminders
 * - Cancel event
 *
 * User Stories:
 * US 02.01.01: Generate QR code
 * US 02.02.01: View waiting list
 * US 02.02.02: View entrant map
 * US 02.04.02: Update poster
 * US 02.05.02: Run lottery (with notifications)
 * US 02.05.03: Draw replacement from pool
 * US 02.06.01-04: Manage entrant lists
 * US 02.06.05: Export CSV
 * US 02.07.01-03: Send notifications
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";

    // UI Elements
    private Toolbar toolbar;
    private TextView tvEventName, tvCapacity;
    private TextView tvWaitingCount, tvSelectedCount, tvAttendingCount;
    private MaterialButton btnRunLottery, btnViewEntrants, btnViewMap, btnGenerateQR;
    private MaterialButton btnSendNotification, btnExportCSV, btnUpdatePoster, btnCancelEvent;
    private MaterialButton btnSendReminder; // Button for event reminders
    private MaterialButton btnDrawReplacement; // ✨ NEW: Replacement lottery button
    private View loadingView;

    // Data
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private NotificationService notificationService;
    private String eventId;
    private Event event;

    // Image picker
    private Uri newPosterUri;
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    newPosterUri = result.getData().getData();
                    updateEventPoster();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_details);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event ID
        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        notificationService = new NotificationService();

        // Initialize views
        initViews();

        // Load event
        loadEventDetails();
    }

    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Text views
        tvEventName = findViewById(R.id.tvEventName);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvAttendingCount = findViewById(R.id.tvAttendingCount);

        // Action buttons
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnViewEntrants = findViewById(R.id.btnViewEntrants);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnSendNotification = findViewById(R.id.btnSendNotification);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);
        btnCancelEvent = findViewById(R.id.btnCancelEvent);
        btnDrawReplacement = findViewById(R.id.btnDrawReplacement); // ✨ NEW

        // Other views
        loadingView = findViewById(R.id.loadingView);

        // Button listeners
        btnRunLottery.setOnClickListener(v -> showLotteryDialog());
        btnViewEntrants.setOnClickListener(v -> openViewEntrantsActivity());
        btnViewMap.setOnClickListener(v -> showEntrantMap());
        btnGenerateQR.setOnClickListener(v -> showQRCode());
        btnSendNotification.setOnClickListener(v -> showMessageDialog());
        btnExportCSV.setOnClickListener(v -> exportToCSV());
        btnUpdatePoster.setOnClickListener(v -> selectNewPoster());
        btnCancelEvent.setOnClickListener(v -> showCancelEventDialog());
        btnDrawReplacement.setOnClickListener(v -> showDrawReplacementDialog()); // ✨ NEW

        if (btnSendReminder != null) {
            btnSendReminder.setOnClickListener(v -> sendEventReminders());
        }
    }

    private void displayEventInfo() {
        tvEventName.setText(event.getName());

        if (event.getCapacity() != null) {
            tvCapacity.setText(String.format("Capacity: %d spots", event.getCapacity()));
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;
        int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));

        btnRunLottery.setEnabled(event.getCapacity() != null && waitingCount > 0);
        btnViewMap.setEnabled(event.isGeolocationEnabled());

        // ✨ NEW: Update button visibility based on lottery state
        updateLotteryButtonVisibility();
    }

    /**
     * ✨ NEW: Update lottery button visibility
     * - Show "Run Lottery" BEFORE lottery
     * - Show "Draw Replacement" AFTER lottery
     */
    private void updateLotteryButtonVisibility() {
        if (event.isLotteryRun()) {
            // Lottery already run - show replacement button
            btnRunLottery.setVisibility(View.GONE);
            btnDrawReplacement.setVisibility(View.VISIBLE);

            // Disable if capacity full or pool empty
            boolean canDrawMore = !event.isCapacityFull() && event.hasReplacementPool();
            btnDrawReplacement.setEnabled(canDrawMore);

            if (!canDrawMore) {
                if (event.isCapacityFull()) {
                    btnDrawReplacement.setText("✅ Capacity Full");
                } else if (!event.hasReplacementPool()) {
                    btnDrawReplacement.setText("❌ Pool Empty");
                }
            } else {
                btnDrawReplacement.setText("🔄 Draw Replacement");
            }
        } else {
            // Lottery not run yet - show run lottery button
            btnRunLottery.setVisibility(View.VISIBLE);
            btnDrawReplacement.setVisibility(View.GONE);
        }
    }

    private void openViewEntrantsActivity() {
        Intent intent = new Intent(this, ViewEntrantsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    private void loadEventDetails() {
        showLoading();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        event = document.toObject(Event.class);
                        if (event != null) {
                            event.setId(document.getId());
                            displayEventInfo();
                        }
                    }
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    /**
     * US 02.02.02: Show map of entrant locations
     */
    private void showEntrantMap() {
        Log.d(TAG, "🗺️ View Map clicked");

        // Check if event is loaded
        if (event == null) {
            Toast.makeText(this, "Please wait for event to load...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check geolocation enabled
        if (!event.isGeolocationEnabled()) {
            Toast.makeText(this, "Geolocation not enabled for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for location data
        if (event.getEntrantLocations() == null || event.getEntrantLocations().isEmpty()) {
            Toast.makeText(this, "No entrant locations available yet. Entrants need to join first!", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "✅ Launching map with " + event.getEntrantLocations().size() + " locations");

        // Launch map activity
        Intent intent = new Intent(this, ViewEntrantMapActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    private void showQRCode() {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(32, 32, 32, 32);

            new AlertDialog.Builder(this)
                    .setTitle("Event QR Code")
                    .setMessage("Entrants can scan this code to join the event")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✨ UPDATED: Check if lottery already run
     */
    private void showLotteryDialog() {
        // ✨ NEW: Prevent running lottery twice
        if (event.isLotteryRun()) {
            Toast.makeText(this, "Lottery already run! Use 'Draw Replacement' instead.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (event.getCapacity() == null) {
            Toast.makeText(this, "No capacity set for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int capacity = event.getCapacity().intValue();

        if (waitingCount == 0) {
            Toast.makeText(this, "No one on waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        int toSelect = Math.min(waitingCount, capacity);

        new AlertDialog.Builder(this)
                .setTitle("Run Lottery")
                .setMessage(String.format(
                        "Select %d winners from %d people on waiting list?\n\n" +
                                "⚠️ This can only be done ONCE.\n" +
                                "Remaining entrants will go into replacement pool.\n\n" +
                                "Notifications will be sent to all participants.",
                        toSelect, waitingCount))
                .setPositiveButton("Run Lottery", (dialog, which) -> runLottery(toSelect))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ UPDATED: Run lottery with notSelectedList
     */
    private void runLottery(int numberOfWinners) {
        btnRunLottery.setEnabled(false);

        List<String> waitingList = new ArrayList<>(event.getWaitingList());
        Collections.shuffle(waitingList);

        // Select winners
        List<String> winners = waitingList.subList(0, Math.min(numberOfWinners, waitingList.size()));

        // ✨ NEW: Get non-winners (replacement pool)
        List<String> notSelected = new ArrayList<>(waitingList);
        notSelected.removeAll(winners);

        if (event.getSelectedList() == null) {
            event.setSelectedList(new ArrayList<>());
        }

        // Add winners to selected list
        for (String winner : winners) {
            if (!event.getSelectedList().contains(winner)) {
                event.getSelectedList().add(winner);
            }
        }

        // ✨ NEW: Update Firebase with notSelectedList and lotteryRun flag
        db.collection("events").document(eventId)
                .update(
                        "selectedList", event.getSelectedList(),
                        "notSelectedList", notSelected,           // ✨ NEW: Save replacement pool
                        "lotteryRun", true,                       // ✨ NEW: Mark lottery as run
                        "lotteryDate", System.currentTimeMillis(), // ✨ NEW: Save when lottery ran
                        "totalSelected", event.getSelectedList().size()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Lottery completed: " + winners.size() + " winners, " +
                            notSelected.size() + " in replacement pool");
                    Toast.makeText(this, winners.size() + " winners selected! Pool: " +
                            notSelected.size(), Toast.LENGTH_LONG).show();

                    // Send notifications to winners and non-winners
                    sendLotteryNotifications(winners, notSelected);

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error running lottery", e);
                    Toast.makeText(this, "Failed to run lottery", Toast.LENGTH_SHORT).show();
                    btnRunLottery.setEnabled(true);
                });
    }

    /**
     * ✨ NEW: Show dialog to draw replacement
     */
    private void showDrawReplacementDialog() {
        int poolSize = event.getNotSelectedList() != null ? event.getNotSelectedList().size() : 0;
        int spotsRemaining = event.getSpotsRemaining();

        if (poolSize == 0) {
            Toast.makeText(this, "No one in replacement pool", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spotsRemaining <= 0) {
            Toast.makeText(this, "Capacity is full!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Draw Replacement")
                .setMessage(String.format(
                        "Draw 1 replacement from pool of %d?\n\n" +
                                "Spots remaining: %d\n" +
                                "Pool available: %d",
                        poolSize, spotsRemaining, poolSize))
                .setPositiveButton("Draw", (dialog, which) -> drawReplacement())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ NEW: Draw one replacement from the pool
     * US 02.05.03: Draw replacement applicant from pooling system
     */
    private void drawReplacement() {
        btnDrawReplacement.setEnabled(false);

        List<String> pool = new ArrayList<>(event.getNotSelectedList());

        if (pool.isEmpty()) {
            Toast.makeText(this, "Replacement pool is empty", Toast.LENGTH_SHORT).show();
            btnDrawReplacement.setEnabled(true);
            return;
        }

        if (event.isCapacityFull()) {
            Toast.makeText(this, "Event is at full capacity", Toast.LENGTH_SHORT).show();
            btnDrawReplacement.setEnabled(true);
            return;
        }

        Collections.shuffle(pool);
        String replacementUserId = pool.get(0);

        // ✨ NEW: Create log entry
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("replacementUserId", replacementUserId);
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("reason", "Manual draw by organizer");

        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayUnion(replacementUserId),
                        "notSelectedList", FieldValue.arrayRemove(replacementUserId),
                        "replacementLog", FieldValue.arrayUnion(logEntry)  // ✨ NEW: Log it
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Replacement drawn and logged: " + replacementUserId);
                    Toast.makeText(this, "Replacement selected! Sending notification...",
                            Toast.LENGTH_SHORT).show();

                    notificationService.sendNotification(
                            replacementUserId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_LOTTERY_WON,
                            "🎉 Good News - You've Been Selected!",
                            "A spot opened up for " + event.getName() +
                                    "! You've been selected from the waiting list. Check your invitations to accept or decline.",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error drawing replacement", e);
                    Toast.makeText(this, "Failed to draw replacement", Toast.LENGTH_SHORT).show();
                    btnDrawReplacement.setEnabled(true);
                });
    }

    /**
     * ✨ NEW: Send notifications to lottery winners and non-winners
     */
    private void sendLotteryNotifications(List<String> winners, List<String> notSelected) {
        String eventName = event.getName();

        // Send winner notifications
        if (!winners.isEmpty()) {
            notificationService.sendBulkNotifications(
                    winners,
                    eventId,
                    eventName,
                    Notification.TYPE_LOTTERY_WON,
                    "🎉 You've Been Selected!",
                    "Congratulations! You've been selected for " + eventName + ". Check your invitations to accept or decline.",
                    (successCount, failureCount) -> {
                        Log.d(TAG, "Sent " + successCount + " winner notifications");
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "Notified " + successCount + " winners!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
            );
        }

        // Send not-selected notifications
        if (!notSelected.isEmpty()) {
            notificationService.sendBulkNotifications(
                    notSelected,
                    eventId,
                    eventName,
                    Notification.TYPE_LOTTERY_LOST,
                    "Lottery Results",
                    "You weren't selected for " + eventName + " this time. You may still have a chance if spots become available!",
                    (successCount, failureCount) -> {
                        Log.d(TAG, "Sent " + successCount + " not-selected notifications");
                    }
            );
        }
    }

    /**
     * US 02.06.05: Export entrants to CSV
     */
    private void exportToCSV() {
        String[] options = {"Waiting List", "Selected", "Attending"};

        new AlertDialog.Builder(this)
                .setTitle("Export List")
                .setItems(options, (dialog, which) -> {
                    String listType = "";
                    String listName = "";
                    switch (which) {
                        case 0:
                            listType = "waiting";
                            listName = "waiting_list";
                            break;
                        case 1:
                            listType = "selected";
                            listName = "selected";
                            break;
                        case 2:
                            listType = "attending";
                            listName = "attending";
                            break;
                    }
                    performExport(listType, listName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performExport(String listType, String listName) {
        btnExportCSV.setEnabled(false);

        List<String> userIdsList = new ArrayList<>();

        switch (listType) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIdsList.addAll(event.getWaitingList());
                }
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIdsList.addAll(event.getSelectedList());
                }
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIdsList.addAll(event.getSignedUpUsers());
                }
                break;
        }

        final List<String> userIds = new ArrayList<>(userIdsList);

        if (userIds.isEmpty()) {
            Toast.makeText(this, "No entrants to export", Toast.LENGTH_SHORT).show();
            btnExportCSV.setEnabled(true);
            return;
        }

        List<User> users = new ArrayList<>();
        final int totalUsers = userIds.size();
        final int[] completed = {0};

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }

                        completed[0]++;

                        if (completed[0] == totalUsers) {
                            createCSVFile(users, listName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user", e);
                        completed[0]++;

                        if (completed[0] == totalUsers) {
                            createCSVFile(users, listName);
                        }
                    });
        }
    }

    private void createCSVFile(List<User> users, String listName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = event.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_" + listName + "_" + timestamp + ".csv";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(csvFile);
            writer.append("Name,Email,Phone\n");

            for (User user : users) {
                writer.append(user.getName() != null ? user.getName() : "").append(",");
                writer.append(user.getEmail() != null ? user.getEmail() : "").append(",");
                writer.append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "").append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "Exported " + users.size() + " entrants to Downloads", Toast.LENGTH_LONG).show();
            Log.d(TAG, "✅ CSV exported: " + csvFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error creating CSV", e);
            Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show();
        }

        btnExportCSV.setEnabled(true);
    }

    private void selectNewPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void updateEventPoster() {
        if (newPosterUri == null) return;

        btnUpdatePoster.setEnabled(false);
        Toast.makeText(this, "Uploading new poster...", Toast.LENGTH_SHORT).show();

        StorageReference posterRef = storage.getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(newPosterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        db.collection("events").document(eventId)
                                .update("posterUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Poster updated successfully!", Toast.LENGTH_SHORT).show();
                                    event.setPosterUrl(uri.toString());
                                    btnUpdatePoster.setEnabled(true);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading poster", e);
                    Toast.makeText(this, "Failed to update poster", Toast.LENGTH_SHORT).show();
                    btnUpdatePoster.setEnabled(true);
                });
    }

    /**
     * US 02.07.01-03: Send message to entrants
     */
    private void showMessageDialog() {
        String[] options = {"Waiting List", "Selected", "Attending"};

        new AlertDialog.Builder(this)
                .setTitle("Send Message To")
                .setItems(options, (dialog, which) -> {
                    String group = "";
                    switch (which) {
                        case 0: group = "waiting"; break;
                        case 1: group = "selected"; break;
                        case 2: group = "attending"; break;
                    }
                    showMessageInputDialog(group);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMessageInputDialog(String group) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_message, null);
        EditText editMessage = dialogView.findViewById(R.id.editMessage);

        new AlertDialog.Builder(this)
                .setTitle("Send Message to " + capitalizeFirst(group))
                .setView(dialogView)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = editMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendMessageToEntrants(message, group);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ UPDATED: Send custom message with actual notifications
     */
    private void sendMessageToEntrants(String message, String group) {
        List<String> userIds = new ArrayList<>();

        switch (group) {
            case "waiting":
                userIds = event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<>();
                break;
            case "selected":
                userIds = event.getSelectedList() != null ? event.getSelectedList() : new ArrayList<>();
                break;
            case "attending":
                userIds = event.getSignedUpUsers() != null ? event.getSignedUpUsers() : new ArrayList<>();
                break;
        }

        if (userIds.isEmpty()) {
            Toast.makeText(this, "No entrants to message", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✨ Send notifications
        notificationService.sendBulkNotifications(
                userIds,
                eventId,
                event.getName(),
                Notification.TYPE_ORGANIZER_MESSAGE,
                "Message from Organizer",
                message,
                (successCount, failureCount) -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Message sent to " + successCount + " entrants!",
                                Toast.LENGTH_LONG).show();
                    });
                    Log.d(TAG, "Message sent: " + message + " to " + successCount + " users");
                }
        );
    }

    /**
     * ✨ NEW: Send event reminders to all attending users
     */
    private void sendEventReminders() {
        String eventName = event.getName();
        List<String> attendees = event.getSignedUpUsers();

        if (attendees == null || attendees.isEmpty()) {
            Toast.makeText(this, "No attendees to remind", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationService.sendBulkNotifications(
                attendees,
                eventId,
                eventName,
                Notification.TYPE_EVENT_REMINDER,
                "⏰ Event Reminder",
                "Don't forget! " + eventName + " is happening soon. We're looking forward to seeing you!",
                (successCount, failureCount) -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Sent reminders to " + successCount + " attendees",
                                Toast.LENGTH_SHORT).show();
                    });
                }
        );
    }

    private void showCancelEventDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Event")
                .setMessage("Are you sure you want to cancel this event? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel Event", (dialog, which) -> cancelEvent())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelEvent() {
        btnCancelEvent.setEnabled(false);

        db.collection("events").document(eventId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cancelling event", e);
                    Toast.makeText(this, "Failed to cancel event", Toast.LENGTH_SHORT).show();
                    btnCancelEvent.setEnabled(true);
                });
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventDetails();
    }
}
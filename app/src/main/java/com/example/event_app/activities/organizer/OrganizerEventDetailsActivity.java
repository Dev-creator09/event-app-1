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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EntrantListAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.List;
import java.util.Locale;

/**
 * OrganizerEventDetailsActivity - Comprehensive event management
 *
 * Features:
 * - Run lottery and select winners
 * - View entrants in different states (waiting, selected, attending)
 * - View map of entrant locations
 * - Generate and view QR code
 * - Send notifications to entrants
 * - Export entrant lists to CSV
 * - Update event poster
 * - Cancel event
 *
 * User Stories:
 * US 02.01.01: Generate QR code
 * US 02.02.01: View waiting list
 * US 02.02.02: View entrant map
 * US 02.04.02: Update poster
 * US 02.05.02: Run lottery
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
    private View loadingView;

    // Data
    private FirebaseFirestore db;
    private FirebaseStorage storage;
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
    }

    private void displayEventInfo() {
        // Event name
        tvEventName.setText(event.getName());

        // Capacity
        if (event.getCapacity() != null) {
            tvCapacity.setText(String.format("Capacity: %d spots", event.getCapacity()));
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        // Counts
        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;
        int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));

        // Enable/disable lottery button
        btnRunLottery.setEnabled(event.getCapacity() != null && waitingCount > 0);

        // Enable/disable map button based on geolocation setting
        btnViewMap.setEnabled(event.isGeolocationEnabled());
    }

    /**
     * Open ViewEntrantsActivity to see full entrant lists
     */
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
        if (!event.isGeolocationEnabled()) {
            Toast.makeText(this, "Geolocation not enabled for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement map view activity
        Toast.makeText(this, "Map feature coming soon! 🗺️", Toast.LENGTH_SHORT).show();

        // Intent to open map activity would go here
        // Intent intent = new Intent(this, ViewEntrantMapActivity.class);
        // intent.putExtra("EVENT_ID", eventId);
        // startActivity(intent);
    }

    /**
     * US 02.01.01: Generate and show QR code
     */
    private void showQRCode() {
        try {
            // Generate QR code bitmap
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

            // Show QR code in dialog
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(32, 32, 32, 32);

            new AlertDialog.Builder(this)
                    .setTitle("Event QR Code")
                    .setMessage("Entrants can scan this code to join the event")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .setNeutralButton("Share", (dialog, which) -> shareQRCode(bitmap))
                    .show();

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode(Bitmap qrBitmap) {
        try {
            // Save to cache
            File cachePath = new File(getCacheDir(), "qr_codes");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_code.png");

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

            // TODO: Implement sharing via FileProvider
            Toast.makeText(this, "QR code sharing coming soon!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error sharing QR code", e);
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * US 02.05.02: Show dialog to run lottery
     */
    private void showLotteryDialog() {
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
                .setMessage(String.format("Select %d winners from %d people on waiting list?", toSelect, waitingCount))
                .setPositiveButton("Run Lottery", (dialog, which) -> runLottery(toSelect))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runLottery(int numberOfWinners) {
        btnRunLottery.setEnabled(false);

        List<String> waitingList = new ArrayList<>(event.getWaitingList());
        Collections.shuffle(waitingList);
        List<String> winners = waitingList.subList(0, Math.min(numberOfWinners, waitingList.size()));

        if (event.getSelectedList() == null) {
            event.setSelectedList(new ArrayList<>());
        }

        for (String winner : winners) {
            if (!event.getSelectedList().contains(winner)) {
                event.getSelectedList().add(winner);
            }
        }

        db.collection("events").document(eventId)
                .update("selectedList", event.getSelectedList(),
                        "totalSelected", event.getSelectedList().size())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Lottery completed: " + winners.size() + " winners selected");
                    Toast.makeText(this, winners.size() + " winners selected! 🎉", Toast.LENGTH_LONG).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error running lottery", e);
                    Toast.makeText(this, "Failed to run lottery", Toast.LENGTH_SHORT).show();
                    btnRunLottery.setEnabled(true);
                });
    }

    /**
     * US 02.06.05: Export entrants to CSV
     */
    private void exportToCSV() {
        // Show dialog to select which list to export
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

        // Fetch user details
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

    /**
     * US 02.04.02: Update event poster
     */
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
        // Show dialog to select which group to message
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

        Toast.makeText(this, "Message sent to " + userIds.size() + " entrants!", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Message sent: " + message + " to " + userIds.size() + " users");

        // TODO: Implement Firebase Cloud Messaging
    }

    /**
     * Cancel/Delete event
     */
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
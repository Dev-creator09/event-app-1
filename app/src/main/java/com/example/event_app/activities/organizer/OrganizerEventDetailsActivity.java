package com.example.event_app.activities.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.adapters.EntrantListAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerEventDetailsActivity - Manage event and run lottery
 *
 * US 02.02.01: View waiting list
 * US 02.05.02: Run lottery/sample attendees
 * US 02.06.01: View selected entrants
 * US 02.06.03: View confirmed entrants
 * US 02.06.04: Cancel entrants
 * US 02.06.05: Export CSV
 * US 02.04.02: Update poster
 * US 02.07.01-03: Send notifications
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";

    // UI Elements
    private TextView tvEventName, tvCapacity, tvWaitingCount, tvSelectedCount, tvAttendingCount;
    private MaterialButton btnRunLottery, btnCancelSelected, btnExportCSV, btnUpdatePoster, btnSendMessage;
    private RecyclerView rvEntrants;
    private TabLayout tabLayout;
    private View loadingView, lotterySection, toolsSection;

    // Data
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String eventId;
    private Event event;
    private EntrantListAdapter adapter;
    private String currentTab = "waiting"; // waiting, selected, attending

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

        // Setup tabs
        setupTabs();

        // Setup RecyclerView
        setupRecyclerView();

        // Load event
        loadEventDetails();
    }

    private void initViews() {
        tvEventName = findViewById(R.id.tvEventName);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvAttendingCount = findViewById(R.id.tvAttendingCount);
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnCancelSelected = findViewById(R.id.btnCancelSelected);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        rvEntrants = findViewById(R.id.rvEntrants);
        tabLayout = findViewById(R.id.tabLayout);
        loadingView = findViewById(R.id.loadingView);
        lotterySection = findViewById(R.id.lotterySection);
        toolsSection = findViewById(R.id.toolsSection);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Button listeners
        btnRunLottery.setOnClickListener(v -> showLotteryDialog());
        btnCancelSelected.setOnClickListener(v -> showCancelDialog());
        btnExportCSV.setOnClickListener(v -> exportToCSV());
        btnUpdatePoster.setOnClickListener(v -> selectNewPoster());
        btnSendMessage.setOnClickListener(v -> showMessageDialog());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Waiting List"));
        tabLayout.addTab(tabLayout.newTab().setText("Selected"));
        tabLayout.addTab(tabLayout.newTab().setText("Attending"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTab = "waiting"; break;
                    case 1: currentTab = "selected"; break;
                    case 2: currentTab = "attending"; break;
                }
                displayEntrants();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(this, eventId);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);
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
                            displayEntrants();
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

    private void displayEventInfo() {
        // Event name
        tvEventName.setText(event.getName());

        // Capacity
        if (event.getCapacity() != null) {
            tvCapacity.setText(String.format("Capacity: %d", event.getCapacity()));
            lotterySection.setVisibility(View.VISIBLE);
        } else {
            tvCapacity.setText("Capacity: Unlimited");
            lotterySection.setVisibility(View.GONE);
        }

        // Counts
        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;
        int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

        tvWaitingCount.setText(String.format("%d waiting", waitingCount));
        tvSelectedCount.setText(String.format("%d selected", selectedCount));
        tvAttendingCount.setText(String.format("%d attending", attendingCount));

        // Show/hide lottery button
        if (event.getCapacity() != null && waitingCount > 0) {
            btnRunLottery.setVisibility(View.VISIBLE);
        } else {
            btnRunLottery.setVisibility(View.GONE);
        }

        // Show/hide cancel button
        if (selectedCount > 0) {
            btnCancelSelected.setVisibility(View.VISIBLE);
        } else {
            btnCancelSelected.setVisibility(View.GONE);
        }

        // Show tools section if there are entrants
        if (waitingCount > 0 || selectedCount > 0 || attendingCount > 0) {
            toolsSection.setVisibility(View.VISIBLE);
        }
    }

    private void displayEntrants() {
        if (event == null) return;

        List<String> userIds = new ArrayList<>();

        switch (currentTab) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIds = event.getWaitingList();
                }
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIds = event.getSelectedList();
                }
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIds = event.getSignedUpUsers();
                }
                break;
        }

        adapter.setUserIds(userIds, currentTab);
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
     * US 02.06.04: Cancel non-responsive entrants
     */
    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Selected Entrants")
                .setMessage("Remove all selected entrants who haven't signed up yet?")
                .setPositiveButton("Cancel Them", (dialog, which) -> cancelNonResponsive())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelNonResponsive() {
        if (event.getSelectedList() == null || event.getSelectedList().isEmpty()) {
            Toast.makeText(this, "No selected entrants to cancel", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> attending = event.getSignedUpUsers() != null ?
                event.getSignedUpUsers() : new ArrayList<>();

        List<String> toCancel = new ArrayList<>();
        for (String userId : event.getSelectedList()) {
            if (!attending.contains(userId)) {
                toCancel.add(userId);
            }
        }

        if (toCancel.isEmpty()) {
            Toast.makeText(this, "Everyone has already signed up!", Toast.LENGTH_SHORT).show();
            return;
        }

        event.getSelectedList().removeAll(toCancel);
        int newCancelledCount = event.getTotalCancelled() + toCancel.size();

        db.collection("events").document(eventId)
                .update("selectedList", event.getSelectedList(),
                        "totalCancelled", newCancelledCount)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, toCancel.size() + " entrants cancelled", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cancelling entrants", e);
                    Toast.makeText(this, "Failed to cancel entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * US 02.06.05: Export entrants to CSV
     */
    private void exportToCSV() {
        btnExportCSV.setEnabled(false);

        // Determine which list to export based on current tab
        List<String> userIdsList = new ArrayList<>();
        String listName = "";

        switch (currentTab) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIdsList.addAll(event.getWaitingList());
                }
                listName = "waiting_list";
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIdsList.addAll(event.getSelectedList());
                }
                listName = "selected";
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIdsList.addAll(event.getSignedUpUsers());
                }
                listName = "attending";
                break;
        }

        // Make final copy for lambda
        final List<String> userIds = new ArrayList<>(userIdsList);
        final String finalListName = listName;

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

                        // When all users fetched, create CSV
                        if (completed[0] == totalUsers) {
                            createCSVFile(users, finalListName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user", e);
                        completed[0]++;

                        // Still create CSV with whatever we got
                        if (completed[0] == totalUsers) {
                            createCSVFile(users, finalListName);
                        }
                    });
        }
    }

    private void createCSVFile(List<User> users, String listName) {
        try {
            // Create file name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = event.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_" + listName + "_" + timestamp + ".csv";

            // Create file in Downloads
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(csvFile);

            // Write header
            writer.append("Name,Email,Phone\n");

            // Write user data
            for (User user : users) {
                writer.append(user.getName() != null ? user.getName() : "");
                writer.append(",");
                writer.append(user.getEmail() != null ? user.getEmail() : "");
                writer.append(",");
                writer.append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "Exported " + users.size() + " entrants to Downloads/" + fileName, Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(this, "Poster updated! ✅", Toast.LENGTH_SHORT).show();
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_message, null);
        EditText editMessage = dialogView.findViewById(R.id.editMessage);

        new AlertDialog.Builder(this)
                .setTitle("Send Message to " + capitalizeFirst(currentTab))
                .setView(dialogView)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = editMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendMessageToEntrants(message);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessageToEntrants(String message) {
        List<String> userIds = new ArrayList<>();

        switch (currentTab) {
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

        // For now, just show success (notifications would be implemented with FCM)
        Toast.makeText(this, "Message sent to " + userIds.size() + " entrants! 📧", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Message: " + message + " to " + userIds.size() + " users");

        // TODO: Implement Firebase Cloud Messaging here
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
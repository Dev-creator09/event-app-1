package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AdminEventDetailsActivity - Shows detailed event information with delete option
 * US 03.01.01: Remove events
 * US 03.04.01: Browse events (detailed view)
 */
public class AdminEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminEventDetails";
    public static final String EXTRA_EVENT_ID = "event_id";

    // Views
    private CardView cardPoster;
    private ImageView ivEventPoster;
    private TextView tvDetailEventName;
    private TextView tvDetailStatus;
    private TextView tvDetailDescription;
    private TextView tvDetailOrganizer;
    private TextView tvDetailEventDate;
    private TextView tvDetailEntrantCount;
    private TextView tvDetailCapacity;
    private TextView tvDetailEventId;
    private CardView cardWarning;
    private TextView tvWarningMessage;
    private LinearLayout layoutCapacity;
    private MaterialButton btnDeleteEvent;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private Event currentEvent;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_details);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Event Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get event ID from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Opening details for event: " + eventId);

        // Initialize views
        initViews();

        // Set up delete button
        setupDeleteButton();

        // Load event data
        loadEventDetails();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        cardPoster = findViewById(R.id.cardPoster);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvDetailEventName = findViewById(R.id.tvDetailEventName);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailOrganizer = findViewById(R.id.tvDetailOrganizer);
        tvDetailEventDate = findViewById(R.id.tvDetailEventDate);
        tvDetailEntrantCount = findViewById(R.id.tvDetailEntrantCount);
        tvDetailCapacity = findViewById(R.id.tvDetailCapacity);
        tvDetailEventId = findViewById(R.id.tvDetailEventId);
        cardWarning = findViewById(R.id.cardWarning);
        tvWarningMessage = findViewById(R.id.tvWarningMessage);
        layoutCapacity = findViewById(R.id.layoutCapacity);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
    }

    /**
     * Setup delete button
     */
    private void setupDeleteButton() {
        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Load event details from Firebase
     */
    private void loadEventDetails() {
        Log.d(TAG, "Loading event details from Firebase...");

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentEvent = documentSnapshot.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(documentSnapshot.getId());
                        displayEventDetails();
                    } else {
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Display event details in UI
     */
    private void displayEventDetails() {
        // Event name
        tvDetailEventName.setText(currentEvent.getName());

        // Status
        String status = currentEvent.getStatus() != null
                ? currentEvent.getStatus().toUpperCase()
                : "ACTIVE";
        tvDetailStatus.setText(status);

        // Status color
        int statusColor;
        switch (status.toLowerCase()) {
            case "active":
                statusColor = android.R.color.holo_green_dark;
                break;
            case "inactive":
                statusColor = android.R.color.darker_gray;
                break;
            case "completed":
                statusColor = android.R.color.holo_blue_dark;
                break;
            default:
                statusColor = android.R.color.holo_orange_dark;
        }
        tvDetailStatus.setBackgroundColor(getColor(statusColor));

        // Description
        String description = currentEvent.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDetailDescription.setText(description);
        } else {
            tvDetailDescription.setText("No description provided");
        }

        // Organizer
        String organizer = currentEvent.getOrganizerName();
        if (organizer != null && !organizer.isEmpty()) {
            tvDetailOrganizer.setText(organizer);
        } else {
            tvDetailOrganizer.setText("Unknown");
        }

        // Event date
        Date eventDate = currentEvent.getEventDate();
        if (eventDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvDetailEventDate.setText(dateFormat.format(eventDate));
        } else {
            tvDetailEventDate.setText("No date specified");
        }

        // Entrant count
        tvDetailEntrantCount.setText(String.valueOf(currentEvent.getEntrantCount()));

        // Capacity (if available)
        Long capacity = currentEvent.getCapacity();
        if (capacity != null && capacity > 0) {
            layoutCapacity.setVisibility(View.VISIBLE);
            tvDetailCapacity.setText(String.valueOf(capacity));
        } else {
            layoutCapacity.setVisibility(View.GONE);
        }

        // Event ID
        tvDetailEventId.setText(currentEvent.getEventId());

        // Poster (if available)
        String posterUrl = currentEvent.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            cardPoster.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivEventPoster);
        } else {
            cardPoster.setVisibility(View.GONE);
        }

        // Warning for high cancellation
        if (currentEvent.hasHighCancellationRate()) {
            cardWarning.setVisibility(View.VISIBLE);
            tvWarningMessage.setText(String.format(
                    "This event has a high cancellation rate of %.1f%% (%d cancelled out of %d selected)",
                    currentEvent.getCancellationRate(),
                    currentEvent.getTotalCancelled(),
                    currentEvent.getTotalSelected()
            ));
        } else {
            cardWarning.setVisibility(View.GONE);
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + currentEvent.getName() + "\"?\n\n" +
                        "This action cannot be undone. All event data, including entrants and waiting lists, will be permanently deleted.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete event from Firebase
     */
    private void deleteEvent() {
        Log.d(TAG, "Deleting event: " + eventId);

        // Show loading
        btnDeleteEvent.setEnabled(false);
        btnDeleteEvent.setText("Deleting...");

        db.collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted successfully");
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();

                    // Return to previous screen
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    Toast.makeText(this, "Error deleting event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Re-enable button
                    btnDeleteEvent.setEnabled(true);
                    btnDeleteEvent.setText("Delete Event");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

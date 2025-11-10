package com.example.event_app.activities.entrant;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EventDetailsActivity - View event details and join waiting list
 *
 * US 01.01.01: Join waiting list
 * US 01.06.01: View event from QR code
 * US 01.05.04: See total entrants count
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    // UI Elements
    private ImageView ivPoster;
    private TextView tvEventName, tvDescription, tvOrganizer, tvLocation;
    private TextView tvEventDate, tvCapacity, tvWaitingListCount;
    private MaterialButton btnJoinWaitingList, btnLeaveWaitingList;
    private View loadingView, contentView, errorView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data
    private String eventId;
    private Event event;
    private boolean isOnWaitingList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);


        // Get event ID from intent
        eventId = getIntent().getStringExtra(Navigator.EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Load event data
        loadEventDetails();
    }

    private void initViews() {
        // Views
        ivPoster = findViewById(R.id.ivEventPoster);
        tvEventName = findViewById(R.id.tvEventName);
        tvDescription = findViewById(R.id.tvEventDescription);
        tvOrganizer = findViewById(R.id.tvOrganizer);
        tvLocation = findViewById(R.id.tvLocation);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvWaitingListCount = findViewById(R.id.tvWaitingListCount);
        btnJoinWaitingList = findViewById(R.id.btnJoinWaitingList);
        btnLeaveWaitingList = findViewById(R.id.btnLeaveWaitingList);
        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);
        errorView = findViewById(R.id.errorView);

        // Button listeners
        btnJoinWaitingList.setOnClickListener(v -> joinWaitingList());
        btnLeaveWaitingList.setOnClickListener(v -> leaveWaitingList());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadEventDetails());
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
                            displayEventDetails();
                            checkWaitingListStatus();
                        }
                    } else {
                        showError("Event not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    showError("Failed to load event");
                });
    }

    private void displayEventDetails() {
        showContent();

        // Event name
        tvEventName.setText(event.getName());

        // Description
        tvDescription.setText(event.getDescription() != null ?
                event.getDescription() : "No description provided");

        // Organizer
        tvOrganizer.setText(event.getOrganizerName() != null ?
                event.getOrganizerName() : "Event Organizer");

        // Location
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            tvLocation.setVisibility(View.VISIBLE);
            tvLocation.setText(event.getLocation());
        } else {
            tvLocation.setVisibility(View.GONE);
        }

        // Event date
        if (event.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else {
            tvEventDate.setText("Date TBA");
        }

        // Capacity
        if (event.getCapacity() != null) {
            tvCapacity.setText(String.format(Locale.getDefault(),
                    "Capacity: %d spots", event.getCapacity()));
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        // Waiting list count (US 01.05.04)
        int waitingListSize = event.getWaitingList() != null ?
                event.getWaitingList().size() : 0;
        tvWaitingListCount.setText(String.format(Locale.getDefault(),
                "%d people on waiting list", waitingListSize));

        // Load poster if available
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getPosterUrl())
                    .centerCrop()
                    .into(ivPoster);
        }
    }

    private void checkWaitingListStatus() {
        String userId = mAuth.getCurrentUser().getUid();

        if (event.getWaitingList() != null && event.getWaitingList().contains(userId)) {
            isOnWaitingList = true;
            btnJoinWaitingList.setVisibility(View.GONE);
            btnLeaveWaitingList.setVisibility(View.VISIBLE);
        } else {
            isOnWaitingList = false;
            btnJoinWaitingList.setVisibility(View.VISIBLE);
            btnLeaveWaitingList.setVisibility(View.GONE);
        }
    }

    /**
     * US 01.01.01: Join waiting list
     */
    private void joinWaitingList() {
        String userId = mAuth.getCurrentUser().getUid();

        btnJoinWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                    isOnWaitingList = true;
                    btnJoinWaitingList.setVisibility(View.GONE);
                    btnLeaveWaitingList.setVisibility(View.VISIBLE);
                    btnLeaveWaitingList.setEnabled(true);
                    loadEventDetails(); // Refresh count
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waiting list", e);
                    Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show();
                    btnJoinWaitingList.setEnabled(true);
                });
    }

    /**
     * US 01.01.02: Leave waiting list
     */
    private void leaveWaitingList() {
        String userId = mAuth.getCurrentUser().getUid();

        btnLeaveWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    isOnWaitingList = false;
                    btnLeaveWaitingList.setVisibility(View.GONE);
                    btnJoinWaitingList.setVisibility(View.VISIBLE);
                    btnJoinWaitingList.setEnabled(true);
                    loadEventDetails(); // Refresh count
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waiting list", e);
                    Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show();
                    btnLeaveWaitingList.setEnabled(true);
                });
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvError)).setText(message);
    }
}

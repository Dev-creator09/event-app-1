package com.example.event_app.activities.entrant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.models.Notification;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.Navigator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * EventDetailsActivity - View event details, join waiting list, accept/decline invitations
 *
 * US 01.01.01: Join waiting list
 * US 01.01.02: Leave waiting list
 * US 01.05.02: Accept invitation
 * US 01.05.03: Decline invitation
 * US 01.06.01: View event from QR code
 * US 01.05.04: See total entrants count
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    // UI Elements
    private ImageView ivPoster;
    private TextView tvEventName, tvDescription, tvOrganizer, tvLocation;
    private TextView tvEventDate, tvCapacity, tvWaitingListCount, tvInvitationStatus;
    private MaterialButton btnJoinWaitingList, btnLeaveWaitingList;
    private MaterialButton btnAcceptInvitation, btnDeclineInvitation;
    private MaterialCardView cardLocation, cardInvitation;
    private View loadingView, contentView, errorView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NotificationService notificationService;

    // Data
    private String eventId;
    private Event event;
    private boolean isOnWaitingList = false;
    private boolean isSelected = false;
    private boolean hasAccepted = false;

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
        notificationService = new NotificationService();

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
        cardLocation = findViewById(R.id.cardLocation);
        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);
        errorView = findViewById(R.id.errorView);

        // Try to find invitation UI elements (add to layout if needed)
        tvInvitationStatus = findViewById(R.id.tvInvitationStatus);
        btnAcceptInvitation = findViewById(R.id.btnAcceptInvitation);
        btnDeclineInvitation = findViewById(R.id.btnDeclineInvitation);
        cardInvitation = findViewById(R.id.cardInvitation);

        // Button listeners
        btnJoinWaitingList.setOnClickListener(v -> joinWaitingList());
        btnLeaveWaitingList.setOnClickListener(v -> leaveWaitingList());

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setOnClickListener(v -> showAcceptConfirmation());
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setOnClickListener(v -> showDeclineConfirmation());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadEventDetails());

        // Location card click listener
        cardLocation.setOnClickListener(v -> openLocationInMaps());
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
                            checkUserStatus();
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
        tvEventName.setText(event.getName());

        tvDescription.setText(event.getDescription() != null ?
                event.getDescription() : "No description available");

        tvOrganizer.setText(event.getOrganizerName() != null ?
                event.getOrganizerName() : "Event Organizer");

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            tvLocation.setText(event.getLocation());
            cardLocation.setVisibility(View.VISIBLE);
        } else {
            cardLocation.setVisibility(View.GONE);
        }

        if (event.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else if (event.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getDate()));
        }

        if (event.getCapacity() != null) {
            tvCapacity.setText("Capacity: " + event.getCapacity() + " spots");
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        tvWaitingListCount.setText(waitingCount + (waitingCount == 1 ? " person" : " people") + " on waiting list");

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getPosterUrl())
                    .centerCrop()
                    .into(ivPoster);
        }

        showContent();
    }

    private void openLocationInMaps() {
        if (event == null || event.getLocation() == null || event.getLocation().isEmpty()) {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String location = event.getLocation();
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening maps", e);
            Toast.makeText(this, "Could not open maps", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check user's status: waiting list, selected, or accepted
     */
    private void checkUserStatus() {
        if (mAuth.getCurrentUser() == null) {
            isOnWaitingList = false;
            isSelected = false;
            hasAccepted = false;
            updateButtonState();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        isOnWaitingList = event.getWaitingList() != null && event.getWaitingList().contains(userId);
        isSelected = event.getSelectedList() != null && event.getSelectedList().contains(userId);
        hasAccepted = event.getSignedUpUsers() != null && event.getSignedUpUsers().contains(userId);

        updateButtonState();
    }

    /**
     * Update UI based on user's status
     */
    private void updateButtonState() {
        // Hide all buttons first
        btnJoinWaitingList.setVisibility(View.GONE);
        btnLeaveWaitingList.setVisibility(View.GONE);
        if (btnAcceptInvitation != null) btnAcceptInvitation.setVisibility(View.GONE);
        if (btnDeclineInvitation != null) btnDeclineInvitation.setVisibility(View.GONE);
        if (cardInvitation != null) cardInvitation.setVisibility(View.GONE);

        if (hasAccepted) {
            // User has accepted - show status
            if (tvInvitationStatus != null) {
                tvInvitationStatus.setText("✅ You're registered for this event!");
                tvInvitationStatus.setVisibility(View.VISIBLE);
            }
        } else if (isSelected) {
            // User is selected - show accept/decline buttons
            if (cardInvitation != null) {
                cardInvitation.setVisibility(View.VISIBLE);
            }
            if (tvInvitationStatus != null) {
                tvInvitationStatus.setText("🎉 You've been selected! Accept or decline your invitation:");
                tvInvitationStatus.setVisibility(View.VISIBLE);
            }
            if (btnAcceptInvitation != null) btnAcceptInvitation.setVisibility(View.VISIBLE);
            if (btnDeclineInvitation != null) btnDeclineInvitation.setVisibility(View.VISIBLE);
        } else if (isOnWaitingList) {
            // User is on waiting list
            btnLeaveWaitingList.setVisibility(View.VISIBLE);
        } else {
            // User can join waiting list
            btnJoinWaitingList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * US 01.01.01: Join waiting list
     */
    private void joinWaitingList() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        btnJoinWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Joined waiting list");
                    Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();

                    // ✨ Send notification
                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_WAITLIST_JOINED,
                            "Joined Waiting List",
                            "You've successfully joined the waiting list for " + event.getName() + ". Good luck!",
                            null
                    );

                    loadEventDetails();
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
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        btnLeaveWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Left waiting list");
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waiting list", e);
                    Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show();
                    btnLeaveWaitingList.setEnabled(true);
                });
    }

    /**
     * Show confirmation dialog before accepting
     */
    private void showAcceptConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Accept Invitation")
                .setMessage("Confirm your registration for " + event.getName() + "?")
                .setPositiveButton("Accept", (dialog, which) -> acceptInvitation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ US 01.05.02: Accept invitation with notification
     */
    private void acceptInvitation() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setEnabled(false);
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setEnabled(false);
        }

        // Move from selectedList to signedUpUsers
        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayRemove(userId),
                        "signedUpUsers", FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation accepted");
                    Toast.makeText(this, "Registration confirmed!", Toast.LENGTH_SHORT).show();

                    // ✨ Send confirmation notification
                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_INVITATION_SENT,
                            "✅ Registration Confirmed",
                            "You're all set for " + event.getName() + "! We're looking forward to seeing you!",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting invitation", e);
                    Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
                });
    }

    /**
     * Show confirmation dialog before declining
     */
    private void showDeclineConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Decline Invitation")
                .setMessage("Are you sure you want to decline this invitation? This cannot be undone.")
                .setPositiveButton("Decline", (dialog, which) -> declineInvitation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ US 01.05.03: Decline invitation with notification
     */
    private void declineInvitation() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setEnabled(false);
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setEnabled(false);
        }

        // Move from selectedList to declinedUsers
        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayRemove(userId),
                        "declinedUsers", FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation declined");
                    Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();

                    // ✨ Send acknowledgment notification
                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_INVITATION_DECLINED,
                            "Invitation Declined",
                            "You've declined the invitation for " + event.getName() + ". Thanks for letting us know!",
                            null
                    );

                    // TODO: Organizer could draw a replacement here

                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error declining invitation", e);
                    Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
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

        TextView tvError = findViewById(R.id.tvError);
        if (tvError != null) {
            tvError.setText(message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (event != null) {
            loadEventDetails();
        }
    }
}
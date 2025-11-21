package com.example.event_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.activities.entrant.SettingsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.activities.organizer.OrganizerEventsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ProfileFragment - User profile with stats and organized actions
 *
 * Features:
 * - Sticky header (Profile + Settings)
 * - User info display
 * - Tappable quick stats (waiting, selected, attending counts)
 * - Single "My Events" button
 * - Organizer actions (always visible)
 *
 * US 01.02.02: Update profile information
 * US 01.02.03: View event history
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Components - User Info
    private ImageButton btnSettings;
    private TextView tvProfileName, tvProfileEmail, tvProfileRole;
    private MaterialButton btnEditProfile;

    // UI Components - Stats (Tappable)
    private LinearLayout statsWaiting, statsSelected, statsAttending;
    private TextView tvWaitingCount, tvSelectedCount, tvAttendingCount;

    // UI Components - Actions
    private MaterialButton btnMyEvents;
    private MaterialButton btnCreateEvent;
    private LinearLayout btnMyOrganizedEvents;

    // Loading
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private User currentUser;
    private int waitingCount = 0;
    private int selectedCount = 0;
    private int attendingCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews(view);

        // Setup listeners
        setupListeners();

        // Load user data
        loadUserProfile();
    }

    /**
     * Initialize all views
     */
    private void initViews(View view) {
        // User Info
        btnSettings = view.findViewById(R.id.btnSettings);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Stats (Tappable)
        statsWaiting = view.findViewById(R.id.statsWaiting);
        statsSelected = view.findViewById(R.id.statsSelected);
        statsAttending = view.findViewById(R.id.statsAttending);
        tvWaitingCount = view.findViewById(R.id.tvWaitingCount);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
        tvAttendingCount = view.findViewById(R.id.tvAttendingCount);

        // Actions
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnMyOrganizedEvents = view.findViewById(R.id.btnMyOrganizedEvents);

        // Loading
        progressBar = view.findViewById(R.id.progressBar);
    }

    /**
     * Setup all click listeners
     */
    private void setupListeners() {
        // Settings
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Edit Profile
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Tappable Stats - Navigate to filtered views
        statsWaiting.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "waiting");
            startActivity(intent);
        });

        statsSelected.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "selected");
            startActivity(intent);
        });

        statsAttending.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "attending");
            startActivity(intent);
        });

        // My Events (all events - no filter)
        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });

        // Create Event
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });

        // My Organized Events
        btnMyOrganizedEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrganizerEventsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Load user profile and calculate stats
     */
    private void loadUserProfile() {
        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "No user is currently signed in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading();

        // Load user info
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            displayUserInfo();
                            // Load event stats
                            loadEventStats(userId);
                        }
                    } else {
                        Log.d(TAG, "No such user document!");
                        hideLoading();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile", e);
                    Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    /**
     * Display user information
     */
    private void displayUserInfo() {
        // Name
        tvProfileName.setText(currentUser.getName() != null ? currentUser.getName() : "User");

        // Email
        tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        // Role
        String roleText;
        if (currentUser.isOrganizer()) {
            roleText = "Entrant & Organizer";
        } else {
            roleText = "Entrant";
        }
        tvProfileRole.setText(roleText);
    }

    /**
     * Load event statistics for this user
     */
    private void loadEventStats(String userId) {
        Log.d(TAG, "Loading event stats for user: " + userId);

        // Load all active events
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waitingCount = 0;
                    selectedCount = 0;
                    attendingCount = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);

                        // Check if user is in waiting list
                        if (event.getWaitingList() != null && event.getWaitingList().contains(userId)) {
                            waitingCount++;
                        }

                        // Check if user is selected
                        if (event.getSelectedList() != null && event.getSelectedList().contains(userId)) {
                            selectedCount++;
                        }

                        // Check if user is attending (signed up)
                        if (event.getSignedUpUsers() != null && event.getSignedUpUsers().contains(userId)) {
                            attendingCount++;
                        }
                    }

                    Log.d(TAG, "Stats - Waiting: " + waitingCount + ", Selected: " + selectedCount + ", Attending: " + attendingCount);

                    // Update UI
                    displayStats();
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event stats", e);
                    hideLoading();
                });
    }

    /**
     * Display statistics
     */
    private void displayStats() {
        // Update counts in stats boxes
        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));
    }

    /**
     * Show loading indicator
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide loading indicator
     */
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile when returning to this fragment
        loadUserProfile();
    }
}
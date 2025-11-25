package com.example.event_app.activities.entrant;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.MyEventsAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * MyEventsActivity - Show user's event history
 *
 * US 01.02.03: View event history with status
 *
 * User sees:
 * - Events they're waiting for
 * - Events they won (selected)
 * - Events they're attending
 * - Events they declined
 */
public class MyEventsActivity extends AppCompatActivity {

    private static final String TAG = "MyEventsActivity";

    // UI Elements
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private MaterialButton btnRetry;
    private View emptyView, errorView;
    private TabLayout tabLayout;

    // Data
    private MyEventsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // Filter
    private String currentFilter = "all"; // all, waiting, selected, attending, declined

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Initialize views
        initViews();

        // Setup tabs
        setupTabs();

        // Setup RecyclerView
        setupRecyclerView();

        // Load events
        loadMyEvents();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnRetry = findViewById(R.id.btnRetry);
        emptyView = findViewById(R.id.emptyView);
        errorView = findViewById(R.id.errorView);
        tabLayout = findViewById(R.id.tabLayout);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Retry button
        btnRetry.setOnClickListener(v -> loadMyEvents());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Waiting"));
        tabLayout.addTab(tabLayout.newTab().setText("Selected"));
        tabLayout.addTab(tabLayout.newTab().setText("Attending"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "all"; break;
                    case 1: currentFilter = "waiting"; break;
                    case 2: currentFilter = "selected"; break;
                    case 3: currentFilter = "attending"; break;
                }
                loadMyEvents();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new MyEventsAdapter(this, userId);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    /**
     * Load all events user is part of
     * US 01.02.03: Event history with status
     */
    private void loadMyEvents() {
        showLoading();

        // Query all events where user is in any list
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> myEvents = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setId(document.getId());

                        // ✅ Check if user is involved in this event (any list)
                        boolean isInWaitingList = event.getWaitingList() != null &&
                                event.getWaitingList().contains(userId);
                        boolean isSelected = event.getSelectedList() != null &&
                                event.getSelectedList().contains(userId);
                        boolean isSignedUp = event.getSignedUpUsers() != null &&
                                event.getSignedUpUsers().contains(userId);
                        boolean isDeclined = event.getDeclinedUsers() != null &&
                                event.getDeclinedUsers().contains(userId);

                        if (isInWaitingList || isSelected || isSignedUp || isDeclined) {
                            // Apply filter
                            String status = getEventStatus(event);
                            if (currentFilter.equals("all") || status.equals(currentFilter)) {
                                myEvents.add(event);
                            }
                        }
                    }

                    if (myEvents.isEmpty()) {
                        showEmpty();
                    } else {
                        showEvents(myEvents);
                    }

                    Log.d(TAG, "Loaded " + myEvents.size() + " events for user");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events", e);
                    showError("Failed to load events. Please try again.");
                });
    }

    /**
     * ✅ Determine user's status for this event (checks all lists!)
     */
    private String getEventStatus(Event event) {
        boolean isInWaitingList = event.getWaitingList() != null &&
                event.getWaitingList().contains(userId);
        boolean isSelected = event.getSelectedList() != null &&
                event.getSelectedList().contains(userId);
        boolean isSignedUp = event.getSignedUpUsers() != null &&
                event.getSignedUpUsers().contains(userId);
        boolean isDeclined = event.getDeclinedUsers() != null &&
                event.getDeclinedUsers().contains(userId);

        // Priority order: attending > declined > selected > waiting
        if (isSignedUp) {
            return "attending";
        } else if (isDeclined) {
            return "declined";
        } else if (isSelected) {
            return "selected";
        } else if (isInWaitingList) {
            return "waiting";
        }
        return "unknown";
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showEvents(List<Event> events) {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        adapter.setEvents(events);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        String message = getEmptyMessage();
        tvEmptyState.setText(message);
    }

    private String getEmptyMessage() {
        switch (currentFilter) {
            case "waiting": return "You're not waiting for any events.\nBrowse events to join!";
            case "selected": return "You haven't been selected for any events yet.";
            case "attending": return "You're not attending any events.\nJoin an event to get started!";
            default: return "You haven't joined any events yet.\nBrowse events to find something you like!";
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload when returning to this screen
        loadMyEvents();
    }
}
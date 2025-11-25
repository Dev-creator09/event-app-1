package com.example.event_app.activities.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.OrganizerEventsAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerEventsActivity - View events created by organizer
 *
 * Organizer can:
 * - See all their events
 * - Tap to manage each event
 * - Create new events
 */
public class OrganizerEventsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventsActivity";

    // UI Elements
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private MaterialButton btnRetry;
    private View emptyView, errorView;
    private FloatingActionButton fabCreateEvent;

    // Data
    private OrganizerEventsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_events);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

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
        fabCreateEvent = findViewById(R.id.fabCreateEvent);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Retry button
        btnRetry.setOnClickListener(v -> loadMyEvents());

        // Create event FAB
        fabCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        adapter = new OrganizerEventsAdapter(this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    /**
     * Load events created by this organizer
     */
    private void loadMyEvents() {
        showLoading();

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("events")
                .whereEqualTo("organizerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setId(document.getId());
                        events.add(event);
                    }

                    if (events.isEmpty()) {
                        showEmpty();
                    } else {
                        showEvents(events);
                    }

                    Log.d(TAG, "Loaded " + events.size() + " events");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events", e);
                    showError("Failed to load events. Please try again.");
                });
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
        tvEmptyState.setText("You haven't created any events yet.\nTap + to create your first event!");
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
        // Reload when returning
        loadMyEvents();
    }
}

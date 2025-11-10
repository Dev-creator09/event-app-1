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
import com.example.event_app.adapters.EventAdapter;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BrowseEventsActivity - Browse all available events
 *
 * US 01.01.03: Browse available events
 * US 01.05.04: See waiting list count for each event
 */
public class BrowseEventsActivity extends AppCompatActivity {

    private static final String TAG = "BrowseEventsActivity";

    // UI Elements
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvErrorState;
    private MaterialButton btnRetry;
    private View emptyView, errorView;

    // Data
    private EventAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load events
        loadEvents();
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvErrorState = findViewById(R.id.tvErrorState);
        btnRetry = findViewById(R.id.btnRetry);
        emptyView = findViewById(R.id.emptyView);
        errorView = findViewById(R.id.errorView);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Retry button
        btnRetry.setOnClickListener(v -> loadEvents());
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void loadEvents() {
        showLoading();

        db.collection("events")
                .whereEqualTo("status", "active")
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
        tvEmptyState.setText("No events available yet.\nCheck back soon!");
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        tvErrorState.setText(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload events when returning to this screen
        loadEvents();
    }
}

package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.AdminEventAdapter;
import com.example.event_app.models.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import android.content.Intent;

/**
 * AdminBrowseEventsActivity - Admin can view, search, filter, and sort all events
 * US 03.04.01: Browse all events
 * Features:
 * - Search by event name or organizer
 * - Filter by status (All, Active, Inactive, Completed)
 * - Sort by date, name, or entrant count
 * - Display: event name, organizer, date, status, entrant count
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private static final String TAG = "AdminBrowseEvents";

    // Sort options
    private enum SortOption {
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        DATE_NEWEST("Date (Newest First)"),
        DATE_OLDEST("Date (Oldest First)"),
        ENTRANTS_HIGH("Entrants (High to Low)"),
        ENTRANTS_LOW("Entrants (Low to High)");

        private final String displayName;

        SortOption(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Views
    private RecyclerView recyclerViewEvents;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyMessage;
    private TextView tvResultsCount;
    private TextInputEditText searchEvents;
    private ChipGroup chipGroupStatus;
    private Chip chipAll, chipActive, chipInactive, chipCompleted;
    private Button btnSort;

    private AdminEventAdapter eventAdapter;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();

    // Current filters and sort
    private String currentSearchQuery = "";
    private String currentStatusFilter = "all";
    private SortOption currentSort = SortOption.NAME_ASC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Events");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Log.d(TAG, "AdminBrowseEventsActivity created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up search
        setupSearch();

        // Set up filters
        setupFilters();

        // Set up sort
        setupSort();

        // Load events
        loadEvents();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        searchEvents = findViewById(R.id.searchEvents);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        chipAll = findViewById(R.id.chipAll);
        chipActive = findViewById(R.id.chipActive);
        chipInactive = findViewById(R.id.chipInactive);
        chipCompleted = findViewById(R.id.chipCompleted);
        btnSort = findViewById(R.id.btnSort);
    }

    /**
     * Set up RecyclerView with AdminEventAdapter
     */
    /**
     * Set up RecyclerView with AdminEventAdapter
     */
    private void setupRecyclerView() {
        // Create admin adapter
        eventAdapter = new AdminEventAdapter();

        // Set click listener
        eventAdapter.setOnEventClickListener(event -> {
            // Open event details activity
            Intent intent = new Intent(this, AdminEventDetailsActivity.class);
            intent.putExtra(AdminEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
            startActivity(intent);
        });

        // Set layout manager and adapter
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(eventAdapter);
    }

    /**
     * Set up search functionality
     */
    private void setupSearch() {
        if (searchEvents == null) {
            Log.w(TAG, "Search box not found in layout");
            return;
        }

        searchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Set up status filter chips
     */
    private void setupFilters() {
        if (chipGroupStatus == null) {
            Log.w(TAG, "Chip group not found in layout");
            return;
        }

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipAll) {
                currentStatusFilter = "all";
            } else if (checkedId == R.id.chipActive) {
                currentStatusFilter = "active";
            } else if (checkedId == R.id.chipInactive) {
                currentStatusFilter = "inactive";
            } else if (checkedId == R.id.chipCompleted) {
                currentStatusFilter = "completed";
            }

            Log.d(TAG, "Status filter changed to: " + currentStatusFilter);
            applyFiltersAndSort();
        });
    }

    /**
     * Set up sort button and dialog
     */
    private void setupSort() {
        if (btnSort == null) {
            Log.w(TAG, "Sort button not found in layout");
            return;
        }

        btnSort.setOnClickListener(v -> showSortDialog());
    }

    /**
     * Show sort options dialog
     */
    private void showSortDialog() {
        String[] sortOptions = new String[SortOption.values().length];
        for (int i = 0; i < SortOption.values().length; i++) {
            sortOptions[i] = SortOption.values()[i].getDisplayName();
        }

        // Find current selection index
        int currentIndex = currentSort.ordinal();

        new AlertDialog.Builder(this)
                .setTitle("Sort Events")
                .setSingleChoiceItems(sortOptions, currentIndex, (dialog, which) -> {
                    currentSort = SortOption.values()[which];
                    Log.d(TAG, "Sort changed to: " + currentSort.getDisplayName());
                    applyFiltersAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Load all events from Firebase
     */
    private void loadEvents() {
        Log.d(TAG, "Loading events from Firebase...");

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        allEvents.add(event);
                    }

                    Log.d(TAG, "Loaded " + allEvents.size() + " events");
                    applyFiltersAndSort();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events", e);
                    Toast.makeText(this, "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI(true);
                });
    }

    /**
     * Apply filters and sort
     */
    private void applyFiltersAndSort() {
        String searchQuery = currentSearchQuery.toLowerCase().trim();
        filteredEvents.clear();

        // Step 1: Apply filters
        for (Event event : allEvents) {
            if (!matchesStatusFilter(event)) {
                continue;
            }

            if (!searchQuery.isEmpty() && !matchesSearchQuery(event, searchQuery)) {
                continue;
            }

            filteredEvents.add(event);
        }

        // Step 2: Apply sort
        sortEvents(filteredEvents);

        Log.d(TAG, "Filtered to " + filteredEvents.size() + " events " +
                "(search: '" + searchQuery + "', status: " + currentStatusFilter +
                ", sort: " + currentSort.getDisplayName() + ")");

        updateUI(false);
    }

    /**
     * Sort events based on current sort option
     */
    private void sortEvents(List<Event> events) {
        switch (currentSort) {
            case NAME_ASC:
                Collections.sort(events, (e1, e2) -> {
                    String name1 = e1.getName() != null ? e1.getName() : "";
                    String name2 = e2.getName() != null ? e2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;

            case NAME_DESC:
                Collections.sort(events, (e1, e2) -> {
                    String name1 = e1.getName() != null ? e1.getName() : "";
                    String name2 = e2.getName() != null ? e2.getName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                break;

            case DATE_NEWEST:
                Collections.sort(events, (e1, e2) -> {
                    Date date1 = e1.getEventDate();
                    Date date2 = e2.getEventDate();
                    if (date1 == null && date2 == null) return 0;
                    if (date1 == null) return 1;
                    if (date2 == null) return -1;
                    return date2.compareTo(date1); // Newest first
                });
                break;

            case DATE_OLDEST:
                Collections.sort(events, (e1, e2) -> {
                    Date date1 = e1.getEventDate();
                    Date date2 = e2.getEventDate();
                    if (date1 == null && date2 == null) return 0;
                    if (date1 == null) return 1;
                    if (date2 == null) return -1;
                    return date1.compareTo(date2); // Oldest first
                });
                break;

            case ENTRANTS_HIGH:
                Collections.sort(events, (e1, e2) ->
                        Integer.compare(e2.getEntrantCount(), e1.getEntrantCount()) // High to low
                );
                break;

            case ENTRANTS_LOW:
                Collections.sort(events, (e1, e2) ->
                        Integer.compare(e1.getEntrantCount(), e2.getEntrantCount()) // Low to high
                );
                break;
        }
    }

    /**
     * Check if event matches the current status filter
     */
    private boolean matchesStatusFilter(Event event) {
        if ("all".equals(currentStatusFilter)) {
            return true;
        }

        String eventStatus = event.getStatus();
        if (eventStatus == null) {
            eventStatus = "active";
        }

        return eventStatus.equalsIgnoreCase(currentStatusFilter);
    }

    /**
     * Check if event matches the search query
     */
    private boolean matchesSearchQuery(Event event, String query) {
        boolean matchesName = event.getName() != null &&
                event.getName().toLowerCase().contains(query);

        boolean matchesOrganizer = event.getOrganizerName() != null &&
                event.getOrganizerName().toLowerCase().contains(query);

        return matchesName || matchesOrganizer;
    }



    /**
     * Update UI based on filtered events
     */
    private void updateUI(boolean isError) {
        if (filteredEvents.isEmpty()) {
            recyclerViewEvents.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvResultsCount.setVisibility(View.GONE);

            if (isError) {
                tvEmptyMessage.setText("Failed to load events");
            } else if (!currentSearchQuery.isEmpty() || !"all".equals(currentStatusFilter)) {
                tvEmptyMessage.setText("No matching events found");
            } else {
                tvEmptyMessage.setText("No events available");
            }
        } else {
            recyclerViewEvents.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            tvResultsCount.setVisibility(View.VISIBLE);
            String countText = filteredEvents.size() == 1
                    ? "Showing 1 event"
                    : "Showing " + filteredEvents.size() + " events";
            tvResultsCount.setText(countText);

            eventAdapter.setEvents(filteredEvents);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed, reloading events");
        loadEvents();
    }
}
package com.example.event_app.fragments;

import android.Manifest;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.BrowseEventsActivity;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.adapters.HorizontalEventAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;
import com.example.event_app.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * HomeFragment - Discovery and Quick Actions
 *
 * Features:
 * - Sticky header with search and notifications
 * - Scan QR code
 * - Happening Soon events (horizontal scroll)
 * - Browse by Category chips
 * - Popular This Week events (horizontal scroll)
 * - Quick actions (My Events, Create Event)
 *
 * US 01.06.01: Scan QR code to view event
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI Components
    private MaterialCardView cardScanQr;
    private ImageButton btnSearch, btnNotifications;
    private TextView btnSeeAllHappeningSoon, btnSeeAllPopular;
    private RecyclerView rvHappeningSoon, rvPopular;
    private LinearLayout emptyHappeningSoon, emptyPopular;
    private MaterialButton btnMyEvents, btnCreateEvent;
    private ProgressBar progressBar;

    // Category chips
    private Chip chipMusic, chipSports, chipArt, chipFood, chipTech, chipWorkshops, chipOther;

    // Adapters
    private HorizontalEventAdapter happeningSoonAdapter;
    private HorizontalEventAdapter popularAdapter;

    // Firebase
    private FirebaseFirestore db;

    // Permission launcher for camera
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchQrScanner();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required to scan QR codes",
                            Toast.LENGTH_SHORT).show();
                }
            });

    // QR scanner launcher
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    // Navigate to event details
                    String eventId = result.getContents();
                    Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
                    intent.putExtra(Navigator.EXTRA_EVENT_ID, eventId);
                    startActivity(intent);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews(view);

        // Setup RecyclerViews
        setupRecyclerViews();

        // Setup listeners
        setupListeners();

        // Load events
        loadHappeningSoonEvents();
        loadPopularEvents();
    }

    /**
     * Initialize all views
     */
    private void initViews(View view) {
        cardScanQr = view.findViewById(R.id.cardScanQr);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnSeeAllHappeningSoon = view.findViewById(R.id.btnSeeAllHappeningSoon);
        btnSeeAllPopular = view.findViewById(R.id.btnSeeAllPopular);
        rvHappeningSoon = view.findViewById(R.id.rvHappeningSoon);
        rvPopular = view.findViewById(R.id.rvPopular);
        emptyHappeningSoon = view.findViewById(R.id.emptyHappeningSoon);
        emptyPopular = view.findViewById(R.id.emptyPopular);
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        progressBar = view.findViewById(R.id.progressBar);

        // Category chips
        chipMusic = view.findViewById(R.id.chipMusic);
        chipSports = view.findViewById(R.id.chipSports);
        chipArt = view.findViewById(R.id.chipArt);
        chipFood = view.findViewById(R.id.chipFood);
        chipTech = view.findViewById(R.id.chipTech);
        chipWorkshops = view.findViewById(R.id.chipWorkshops);
        chipOther = view.findViewById(R.id.chipOther);
    }

    /**
     * Setup horizontal RecyclerViews
     */
    private void setupRecyclerViews() {
        // Happening Soon adapter
        happeningSoonAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvHappeningSoon.setLayoutManager(layoutManager1);
        rvHappeningSoon.setAdapter(happeningSoonAdapter);

        // Popular adapter
        popularAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvPopular.setLayoutManager(layoutManager2);
        rvPopular.setAdapter(popularAdapter);
    }

    /**
     * Setup all click listeners
     */
    private void setupListeners() {
        // Scan QR Card
        cardScanQr.setOnClickListener(v -> handleQrScan());

        // Search button
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        // Notifications button (placeholder for future implementation)
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notifications coming soon! 🔔", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to NotificationsActivity when implemented
        });

        // See All buttons
        btnSeeAllHappeningSoon.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        btnSeeAllPopular.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        // My Events button
        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });

        // Create Event button
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });

        // Category chips - navigate to BrowseEventsActivity with filter
        setupCategoryChips();
    }

    /**
     * Setup category chip listeners
     */
    private void setupCategoryChips() {
        View.OnClickListener categoryListener = v -> {
            // For now, just navigate to BrowseEventsActivity
            // TODO: Pass category as extra to filter events
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);

            // Get category name
            String category = "";
            if (v.getId() == R.id.chipMusic) category = "Music";
            else if (v.getId() == R.id.chipSports) category = "Sports";
            else if (v.getId() == R.id.chipArt) category = "Art";
            else if (v.getId() == R.id.chipFood) category = "Food";
            else if (v.getId() == R.id.chipTech) category = "Tech";
            else if (v.getId() == R.id.chipWorkshops) category = "Workshops";
            else if (v.getId() == R.id.chipOther) category = "Other";

            intent.putExtra("CATEGORY_FILTER", category);
            startActivity(intent);
        };

        chipMusic.setOnClickListener(categoryListener);
        chipSports.setOnClickListener(categoryListener);
        chipArt.setOnClickListener(categoryListener);
        chipFood.setOnClickListener(categoryListener);
        chipTech.setOnClickListener(categoryListener);
        chipWorkshops.setOnClickListener(categoryListener);
        chipOther.setOnClickListener(categoryListener);
    }

    /**
     * Handle QR code scanning
     * US 01.06.01: Scan QR code to view event
     */
    private void handleQrScan() {
        if (PermissionManager.isCameraPermissionGranted(requireActivity())) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Launch QR code scanner
     */
    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }

    /**
     * Load events happening soon (next 7 days)
     */
    private void loadHappeningSoonEvents() {
        Log.d(TAG, "Loading happening soon events...");

        // Calculate date range: today to 7 days from now
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date weekFromNow = calendar.getTime();

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("eventDate", today)
                .whereLessThanOrEqualTo("eventDate", weekFromNow)
                .orderBy("eventDate", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }

                    Log.d(TAG, "Loaded " + events.size() + " happening soon events");

                    if (events.isEmpty()) {
                        showEmptyState(rvHappeningSoon, emptyHappeningSoon);
                    } else {
                        showEvents(rvHappeningSoon, emptyHappeningSoon);
                        happeningSoonAdapter.setEvents(events);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading happening soon events", e);
                    showEmptyState(rvHappeningSoon, emptyHappeningSoon);
                });
    }

    /**
     * Load popular events (sorted by waiting list size)
     */
    private void loadPopularEvents() {
        Log.d(TAG, "Loading popular events...");

        db.collection("events")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }

                    // Sort by waiting list size (most popular first)
                    events.sort((e1, e2) -> {
                        int size1 = e1.getWaitingList() != null ? e1.getWaitingList().size() : 0;
                        int size2 = e2.getWaitingList() != null ? e2.getWaitingList().size() : 0;
                        return Integer.compare(size2, size1); // Descending order
                    });

                    Log.d(TAG, "Loaded " + events.size() + " popular events");

                    if (events.isEmpty()) {
                        showEmptyState(rvPopular, emptyPopular);
                    } else {
                        showEvents(rvPopular, emptyPopular);
                        popularAdapter.setEvents(events);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading popular events", e);
                    showEmptyState(rvPopular, emptyPopular);
                });
    }

    /**
     * Show events in RecyclerView
     */
    private void showEvents(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Show empty state
     */
    private void showEmptyState(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload events when returning to this fragment
        loadHappeningSoonEvents();
        loadPopularEvents();
    }
}
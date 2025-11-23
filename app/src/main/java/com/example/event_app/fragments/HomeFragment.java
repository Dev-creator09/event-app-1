package com.example.event_app.fragments;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.example.event_app.activities.entrant.NotificationsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.adapters.HorizontalEventAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.Navigator;
import com.example.event_app.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
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
 * - Notification badge showing unread count (like real apps!)
 * - Scan QR code
 * - Browse events
 * - Category filtering
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI Components
    private MaterialCardView cardScanQr;
    private ImageButton btnSearch, btnNotifications;
    private TextView tvNotificationBadge; // ✨ Badge showing count
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
    private FirebaseAuth mAuth;

    // Notification service
    private NotificationService notificationService;

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
        mAuth = FirebaseAuth.getInstance();

        // Initialize notification service
        notificationService = new NotificationService();

        // Initialize views
        initViews(view);

        // Setup RecyclerViews
        setupRecyclerViews();

        // Setup listeners
        setupListeners();

        // Load events
        loadHappeningSoonEvents();
        loadPopularEvents();

        // Update notification badge
        updateNotificationBadge();
    }

    private void initViews(View view) {
        cardScanQr = view.findViewById(R.id.cardScanQr);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge); // ✨ Badge

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

    private void setupRecyclerViews() {
        happeningSoonAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvHappeningSoon.setLayoutManager(layoutManager1);
        rvHappeningSoon.setAdapter(happeningSoonAdapter);

        popularAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvPopular.setLayoutManager(layoutManager2);
        rvPopular.setAdapter(popularAdapter);
    }

    private void setupListeners() {
        cardScanQr.setOnClickListener(v -> handleQrScan());

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        // Notifications button - Opens notification center
        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NotificationsActivity.class);
            startActivity(intent);
        });

        btnSeeAllHappeningSoon.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        btnSeeAllPopular.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });

        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });

        setupCategoryChips();
    }

    private void setupCategoryChips() {
        View.OnClickListener categoryListener = v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);

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
     * ✨ Update notification badge - Shows count like real apps!
     */
    private void updateNotificationBadge() {
        if (mAuth.getCurrentUser() == null) {
            hideBadge();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        notificationService.getUnreadCount(userId, new NotificationService.UnreadCountCallback() {
            @Override
            public void onSuccess(int count) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (count > 0) {
                        showBadge(count);
                    } else {
                        hideBadge();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to get unread count: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> hideBadge());
                }
            }
        });
    }

    /**
     * ✨ Show badge with count (like Instagram, Facebook, etc.)
     */
    private void showBadge(int count) {
        if (tvNotificationBadge != null) {
            // Show count (9+ if more than 9)
            String displayText = count > 9 ? "9+" : String.valueOf(count);
            tvNotificationBadge.setText(displayText);
            tvNotificationBadge.setVisibility(View.VISIBLE);

            Log.d(TAG, "Badge shown: " + count + " notifications");
        }
    }

    /**
     * ✨ Hide badge when no notifications
     */
    private void hideBadge() {
        if (tvNotificationBadge != null) {
            tvNotificationBadge.setVisibility(View.GONE);
            Log.d(TAG, "Badge hidden: 0 notifications");
        }
    }

    private void handleQrScan() {
        if (PermissionManager.isCameraPermissionGranted(requireActivity())) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }

    private void loadHappeningSoonEvents() {
        Log.d(TAG, "Loading happening soon events...");

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

                    events.sort((e1, e2) -> {
                        int size1 = e1.getWaitingList() != null ? e1.getWaitingList().size() : 0;
                        int size2 = e2.getWaitingList() != null ? e2.getWaitingList().size() : 0;
                        return Integer.compare(size2, size1);
                    });

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

    private void showEvents(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmptyState(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHappeningSoonEvents();
        loadPopularEvents();

        // ✨ Refresh badge when returning to home
        updateNotificationBadge();
    }
}
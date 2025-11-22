package com.example.event_app.activities.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EntrantListAdapter;
import com.example.event_app.models.Event;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewEntrantsActivity - View all entrants in different states
 *
 * Shows entrants in three tabs:
 * - Waiting List
 * - Selected
 * - Attending
 */
public class ViewEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "ViewEntrants";

    // UI Elements
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvEntrants;
    private TextView tvListCount;
    private LinearLayout emptyView;
    private View loadingView;

    // Data
    private FirebaseFirestore db;
    private String eventId;
    private Event event;
    private EntrantListAdapter adapter;
    private String currentTab = "waiting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entrants);

        // Get event ID
        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Setup tabs
        setupTabs();

        // Setup RecyclerView
        setupRecyclerView();

        // Load event
        loadEventDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvEntrants = findViewById(R.id.rvEntrants);
        tvListCount = findViewById(R.id.tvListCount);
        emptyView = findViewById(R.id.emptyView);
        loadingView = findViewById(R.id.loadingView);

        // Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Waiting List"));
        tabLayout.addTab(tabLayout.newTab().setText("Selected"));
        tabLayout.addTab(tabLayout.newTab().setText("Attending"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTab = "waiting"; break;
                    case 1: currentTab = "selected"; break;
                    case 2: currentTab = "attending"; break;
                }
                displayEntrants();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(this, eventId);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);
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
                            displayEntrants();
                        }
                    }
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    private void displayEntrants() {
        if (event == null) return;

        List<String> userIds = new ArrayList<>();

        switch (currentTab) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIds = event.getWaitingList();
                }
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIds = event.getSelectedList();
                }
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIds = event.getSignedUpUsers();
                }
                break;
        }

        // Update count
        int count = userIds.size();
        tvListCount.setText(count + (count == 1 ? " entrant" : " entrants"));

        // Show/hide empty view
        if (userIds.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rvEntrants.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.setUserIds(userIds, currentTab);
        }
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }
}

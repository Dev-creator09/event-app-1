package com.example.event_app.activities.entrant;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.event_app.R;
import com.example.event_app.adapters.MyEventsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * MyEventsActivity - Unified view of all user's event activity
 *
 * Features:
 * - Tab 1: "Joined" - Events user joined as entrant (with filters)
 * - Tab 2: "Hosting" - Events user created as organizer
 *
 * Supports filter parameter to jump to specific filter in Joined tab:
 * - FILTER: "waiting", "selected", "attending"
 *
 * US 01.02.03: View event history
 */
public class MyEventsActivity_tabbed extends AppCompatActivity {

    private static final String TAG = "MyEventsActivity";

    // UI Components
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // Adapter
    private MyEventsPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events_tabbed);

        // Initialize views
        initViews();

        // Setup ViewPager
        setupViewPager();

        // Setup TabLayout
        setupTabLayout();

        // Handle intent filter (if navigating from Profile stats)
        handleIntentFilter();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Initialize views
     */
    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    /**
     * Setup ViewPager with adapter
     */
    private void setupViewPager() {
        pagerAdapter = new MyEventsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
    }

    /**
     * Connect TabLayout with ViewPager
     */
    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Joined");
                    break;
                case 1:
                    tab.setText("Hosting");
                    break;
            }
        }).attach();
    }

    /**
     * Handle filter parameter from intent
     * If user taps a stat box in Profile, jump to that filter
     */
    private void handleIntentFilter() {
        String filter = getIntent().getStringExtra("FILTER");
        if (filter != null) {
            // Stay on "Joined" tab (position 0) and set the filter
            viewPager.setCurrentItem(0);

            // Wait for fragment to be created, then set filter
            viewPager.post(() -> {
                if (pagerAdapter.getJoinedFragment() != null) {
                    pagerAdapter.getJoinedFragment().setFilter(filter);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fragments will handle their own reloading
    }
}
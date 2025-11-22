package com.example.event_app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.event_app.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * EventsFragment - Main event browsing with tabs
 *
 * Features:
 * - Tab 1: Browse Events - All available events
 * - Tab 2: My Events - Events organized by the user
 */
public class EventsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Setup ViewPager2 with adapter
        EventsPagerAdapter adapter = new EventsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Browse Events");
                    break;
                case 1:
                    tab.setText("My Events");
                    break;
            }
        }).attach();
    }

    /**
     * ViewPager2 Adapter for switching between tabs
     */
    private static class EventsPagerAdapter extends FragmentStateAdapter {

        public EventsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new BrowseEventsTabFragment();
                case 1:
                    return new MyOrganizedEventsTabFragment();
                default:
                    return new BrowseEventsTabFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two tabs
        }
    }
}
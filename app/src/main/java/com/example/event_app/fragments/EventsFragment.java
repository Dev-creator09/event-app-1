package com.example.event_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.BrowseEventsActivity;
import com.google.android.material.button.MaterialButton;

/**
 * EventsFragment - Browse and manage events
 * US 01.01.03: Browse available events
 */
public class EventsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Browse All Events button - TEMP: Shows toast until we build BrowseEventsActivity
        MaterialButton btnBrowseAll = view.findViewById(R.id.btnBrowseAllEvents);
        btnBrowseAll.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BrowseEventsActivity.class);
            startActivity(intent);
        });
    }
}
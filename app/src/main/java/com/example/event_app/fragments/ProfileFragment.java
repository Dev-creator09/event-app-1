package com.example.event_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.SettingsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.activities.organizer.OrganizerEventsActivity;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileFragment - User profile and settings
 * US 01.02.02: Update profile information
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView tvName, tvEmail, tvRole;
    private MaterialButton btnEditProfile, btnCreateEvent, btnMyOrganizerEvents; // <-- Declare btnMyOrganizerEvents here
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvRole = view.findViewById(R.id.tvProfileRole);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnMyOrganizerEvents = view.findViewById(R.id.btnMyOrganizerEvents); // <-- Initialize it here

        // Edit Profile button
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Create Event button click listener
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });

        // Load user data
        loadUserProfile();
    }

    private void loadUserProfile() {
        // Ensure there is a current user to prevent NullPointerException
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "No user is currently signed in.");
            // Optionally, navigate to a login screen
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            // Display user info
                            tvName.setText(user.getName());
                            tvEmail.setText(user.getEmail());

                            // Display roles
                            String roleText = user.isOrganizer() ? "Entrant & Organizer" : "Entrant";
                            tvRole.setText(roleText);

                            // Show/hide buttons based on the user's role
                            if (user.isOrganizer()) {
                                btnCreateEvent.setVisibility(View.VISIBLE);
                                btnMyOrganizerEvents.setVisibility(View.VISIBLE);
                                Log.d(TAG, "✅ User is organizer - showing Create Event and My Events buttons");

                                // Set the click listener for My Organizer Events here
                                btnMyOrganizerEvents.setOnClickListener(v -> {
                                    Intent intent = new Intent(requireContext(), OrganizerEventsActivity.class);
                                    startActivity(intent);
                                });
                            } else {
                                btnCreateEvent.setVisibility(View.GONE);
                                btnMyOrganizerEvents.setVisibility(View.GONE);
                                Log.d(TAG, "❌ User is NOT organizer - hiding Create Event and My Events buttons");
                            }
                        }
                    } else {
                        Log.d(TAG, "No such user document!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile", e);
                    Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile when returning to this fragment
        loadUserProfile();
    }
}

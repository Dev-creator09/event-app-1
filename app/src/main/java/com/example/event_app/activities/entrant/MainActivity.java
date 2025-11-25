package com.example.event_app.activities.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.fragments.EventsFragment;
import com.example.event_app.fragments.HomeFragment;
import com.example.event_app.fragments.ProfileFragment;
import com.example.event_app.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.event_app.utils.AccessibilityHelper;


/**
 * MainActivity - Main app with bottom navigation
 *
 * Features:
 * - Home: Quick actions (scan QR, shortcuts)
 * - Events: Browse and join events
 * - Profile: User settings and info
 * - Admin access: Via Profile menu (if user is admin)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load user data (for admin check)
        loadCurrentUser();

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_events) {
                selectedFragment = new EventsFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }
    }

    /**
     * Load current user to check admin status
     * This is used by ProfileFragment to show admin option
     */
    private void loadCurrentUser() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        Log.d(TAG, "User loaded, isAdmin: " + (currentUser != null && currentUser.isAdmin()));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                });
    }

    /**
     * ✨ Open Admin Panel
     * Called from ProfileFragment when admin clicks the option
     */
    public void openAdminPanel() {
        Intent intent = new Intent(this, AdminHomeActivity.class);
        startActivity(intent);
    }

    /**
     * Get current user (for fragments to check admin status)
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if current user is admin
     */
    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning to activity
        loadCurrentUser();
    }
}
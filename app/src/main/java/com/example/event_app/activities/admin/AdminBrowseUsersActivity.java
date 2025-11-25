package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.UserAdapter;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BrowseUsersActivity - Admin can view all users
 * US 03.05.01: As an administrator, I want to browse all profiles
 * US 03.02.01: As an administrator, I want to remove profiles
 */
public class AdminBrowseUsersActivity extends AppCompatActivity {

    private static final String TAG = "BrowseUsersActivity";

    private RecyclerView recyclerViewUsers;
    private LinearLayout emptyStateLayout;
    private UserAdapter userAdapter;

    private FirebaseFirestore db;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_users);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Users");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        userList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load users
        loadUsers();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        // Create adapter
        userAdapter = new UserAdapter();

        // Set click listener
        userAdapter.setOnUserClickListener(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // For now, just show a toast
                // Later: Open user details screen
                Toast.makeText(AdminBrowseUsersActivity.this,
                        "User: " + user.getName(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(User user) {
                // Show confirmation dialog
                showDeleteConfirmation(user);
            }
        });

        // Set layout manager and adapter
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    /**
     * Load all users from Firebase
     */
    private void loadUsers() {
        Log.d(TAG, "Loading users from Firebase...");

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        userList.add(user);
                    }

                    Log.d(TAG, "Loaded " + userList.size() + " users");

                    // Update UI
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    Toast.makeText(this, "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Show empty state
                    updateUI();
                });
    }

    /**
     * Update UI based on user list
     */
    private void updateUI() {
        if (userList.isEmpty()) {
            // Show empty state
            recyclerViewUsers.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            // Show users
            recyclerViewUsers.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            // Update adapter
            userAdapter.setUsers(userList);
        }
    }

    /**
     * Show confirmation dialog before deleting user
     */
    private void showDeleteConfirmation(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete user \"" + user.getName() + "\"?\n\n" +
                        "This will remove all their data permanently.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteUser(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete user from Firebase
     * US 03.02.01: Remove profiles
     */
    private void deleteUser(User user) {
        Log.d(TAG, "Deleting user: " + user.getUserId());

        db.collection("users")
                .document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User deleted successfully");
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();

                    // Remove from list and update UI
                    userList.remove(user);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user", e);
                    Toast.makeText(this, "Error deleting user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        finish();
        return true;
    }
}

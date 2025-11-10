package com.example.event_app.activities.entrant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.example.event_app.utils.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * SettingsActivity - Edit profile and manage account
 *
 * US 01.02.02: Update profile information
 * US 01.02.04: Delete profile
 * US 01.04.03: Opt out of notifications
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Elements
    private TextInputEditText editName, editEmail, editPhone;
    private SwitchMaterial switchNotifications;
    private MaterialButton btnSave, btnBecomeOrganizer, btnDeleteAccount;
    private View loadingView, contentView, organizerSection;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Initialize views
        initViews();

        // Load user data
        loadUserProfile();
    }

    private void initViews() {
        // Views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSave = findViewById(R.id.btnSave);
        btnBecomeOrganizer = findViewById(R.id.btnBecomeOrganizer);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);
        organizerSection = findViewById(R.id.organizerSection);

        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        btnBecomeOrganizer.setOnClickListener(v -> showBecomeOrganizerDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void loadUserProfile() {
        showLoading();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            displayUserData();
                        }
                    }
                    showContent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    showContent();
                });
    }

    private void displayUserData() {
        // Display user info
        editName.setText(currentUser.getName());
        editEmail.setText(currentUser.getEmail());
        editPhone.setText(currentUser.getPhoneNumber());
        switchNotifications.setChecked(currentUser.isNotificationsEnabled());

        // Show/hide "Become Organizer" button
        if (currentUser.isOrganizer()) {
            organizerSection.setVisibility(View.GONE);
        } else {
            organizerSection.setVisibility(View.VISIBLE);
        }
    }

    /**
     * US 01.02.02: Update profile information
     */
    private void saveProfile() {
        // Get values
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        boolean notificationsEnabled = switchNotifications.isChecked();

        // Validate
        if (!validateInputs(name, email)) {
            return;
        }

        // Disable button
        btnSave.setEnabled(false);

        // Update user object
        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setPhoneNumber(phone);
        currentUser.setNotificationsEnabled(notificationsEnabled);
        currentUser.setUpdatedAt(System.currentTimeMillis());

        // Save to Firestore
        db.collection("users").document(userId)
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private boolean validateInputs(String name, String email) {
        // Validate name
        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            editName.requestFocus();
            return false;
        }

        if (name.length() < 2) {
            editName.setError("Name must be at least 2 characters");
            editName.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            editEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Please enter a valid email");
            editEmail.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Show dialog to become organizer
     */
    private void showBecomeOrganizerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Become an Organizer")
                .setMessage("As an organizer, you'll be able to create and manage events. Continue?")
                .setPositiveButton("Yes, Continue", (dialog, which) -> becomeOrganizer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Upgrade user to organizer role
     */
    private void becomeOrganizer() {
        btnBecomeOrganizer.setEnabled(false);

        // Add organizer role
        currentUser.addRole(UserRole.ORGANIZER);
        currentUser.setUpdatedAt(System.currentTimeMillis());

        // Save to Firestore
        db.collection("users").document(userId)
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You're now an organizer! 🎉", Toast.LENGTH_LONG).show();
                    organizerSection.setVisibility(View.GONE);

                    // Restart activity to show organizer features
                    finish();
                    startActivity(getIntent());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error upgrading to organizer", e);
                    Toast.makeText(this, "Failed to become organizer", Toast.LENGTH_SHORT).show();
                    btnBecomeOrganizer.setEnabled(true);
                });
    }

    /**
     * US 01.02.04: Delete profile
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAccount() {
        btnDeleteAccount.setEnabled(false);

        // Delete user document from Firestore
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete Firebase Auth account
                    mAuth.getCurrentUser().delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                                // Sign out and go back to splash
                                mAuth.signOut();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting auth account", e);
                                Toast.makeText(this, "Account partially deleted. Please contact support.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user document", e);
                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                    btnDeleteAccount.setEnabled(true);
                });
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }
}

package com.example.event_app.activities.entrant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
 * SettingsActivity - Edit profile, manage account, and admin access
 *
 * Features:
 * - US 01.02.02: Update profile information
 * - US 01.02.04: Delete profile
 * - US 01.04.03: Opt out of notifications
 * - Admin code unlock (secret: CMPUT301Lucky_Spot)
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String ADMIN_SECRET_CODE = "CMPUT301Lucky_Spot"; // ✨ Secret code

    // UI Elements - Profile
    private TextInputEditText editName, editEmail, editPhone;
    private SwitchMaterial switchNotifications;
    private MaterialButton btnSave, btnBecomeOrganizer, btnDeleteAccount;
    private View loadingView, contentView, organizerSection;

    // UI Elements - Admin
    private MaterialButton btnUnlockAdmin;
    private TextView tvAdminStatus;
    private View adminSection;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;

    // Tap counter for hidden admin unlock (like Android Developer Options)
    private int tapCount = 0;
    private long lastTapTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Load user data
        loadUserProfile();
    }

    private void initViews() {
        // Profile views
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

        // Admin views
        adminSection = findViewById(R.id.adminSection);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        btnUnlockAdmin = findViewById(R.id.btnUnlockAdmin);

        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        btnBecomeOrganizer.setOnClickListener(v -> showBecomeOrganizerDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        // Admin unlock button
        if (btnUnlockAdmin != null) {
            btnUnlockAdmin.setOnClickListener(v -> showAdminCodeDialog());
        }

        // Hidden admin unlock - tap app version 7 times
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            tvAppVersion.setOnClickListener(v -> handleVersionTap());
        }

        // Notification toggle listener
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser != null) {
                updateNotificationPreference(isChecked);
            }
        });
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
        // Display profile info
        editName.setText(currentUser.getName());
        editEmail.setText(currentUser.getEmail());
        editPhone.setText(currentUser.getPhoneNumber());
        switchNotifications.setChecked(currentUser.isNotificationsEnabled());

        // Hide "Become Organizer" section if already organizer
        organizerSection.setVisibility(View.GONE);

        // Update admin UI
        updateAdminUI();
    }

    /**
     * ✨ Update admin section visibility and status
     */
    private void updateAdminUI() {
        if (adminSection == null || currentUser == null) return;

        if (currentUser.isAdmin()) {
            // User is already admin
            adminSection.setVisibility(View.VISIBLE);
            tvAdminStatus.setText("✅ Admin privileges active");
            tvAdminStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            btnUnlockAdmin.setVisibility(View.GONE);
        } else {
            // User is not admin - show unlock option
            adminSection.setVisibility(View.VISIBLE);
            tvAdminStatus.setText("🔒 Admin access locked");
            tvAdminStatus.setTextColor(getColor(android.R.color.darker_gray));
            btnUnlockAdmin.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ✨ Hidden admin unlock - tap version 7 times rapidly
     */
    private void handleVersionTap() {
        long currentTime = System.currentTimeMillis();

        // Reset if taps are too slow (more than 1 second apart)
        if (currentTime - lastTapTime > 1000) {
            tapCount = 0;
        }

        lastTapTime = currentTime;
        tapCount++;

        if (tapCount >= 7) {
            tapCount = 0;
            showAdminCodeDialog();
            Toast.makeText(this, "Developer mode activated! 🔓", Toast.LENGTH_SHORT).show();
        } else if (tapCount >= 4) {
            Toast.makeText(this, (7 - tapCount) + " more taps to unlock admin", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✨ Show admin code entry dialog
     */
    private void showAdminCodeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_code, null);
        EditText etAdminCode = dialogView.findViewById(R.id.etAdminCode);

        new AlertDialog.Builder(this)
                .setTitle("🔐 Enter Admin Code")
                .setView(dialogView)
                .setPositiveButton("Unlock", (dialog, which) -> {
                    String enteredCode = etAdminCode.getText().toString().trim();
                    verifyAdminCode(enteredCode);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✨ Verify admin code and grant access
     */
    private void verifyAdminCode(String enteredCode) {
        if (enteredCode.equals(ADMIN_SECRET_CODE)) {
            // Correct code - grant admin privileges
            grantAdminAccess();
        } else {
            // Wrong code
            Toast.makeText(this, "❌ Invalid admin code", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Failed admin unlock attempt with code: " + enteredCode);
        }
    }

    /**
     * ✨ Grant admin access to current user
     */
    private void grantAdminAccess() {
        if (currentUser == null || mAuth.getCurrentUser() == null) return;

        btnUnlockAdmin.setEnabled(false);

        // Add admin role
        currentUser.addRole("admin");
        currentUser.setUpdatedAt(System.currentTimeMillis());

        // Update in Firebase
        db.collection("users").document(userId)
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Admin access granted to user: " + userId);
                    Toast.makeText(this, "🎉 Admin access granted!", Toast.LENGTH_LONG).show();

                    // Update UI
                    updateAdminUI();

                    // Show success message
                    showAdminWelcomeMessage();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to grant admin access", e);
                    Toast.makeText(this, "Error granting admin access", Toast.LENGTH_SHORT).show();
                    btnUnlockAdmin.setEnabled(true);
                });
    }

    /**
     * ✨ Show welcome message after admin unlock
     */
    private void showAdminWelcomeMessage() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 Welcome, Administrator!")
                .setMessage("You now have admin privileges. You can:\n\n" +
                        "• Browse and manage all events\n" +
                        "• View and moderate users\n" +
                        "• Remove inappropriate images\n" +
                        "• Monitor system activity\n\n" +
                        "Access the Admin Panel from the main menu.")
                .setPositiveButton("Got it!", null)
                .show();
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
     * Update notification preference
     */
    private void updateNotificationPreference(boolean enabled) {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(userId)
                .update("notificationsEnabled", enabled)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification preference updated: " + enabled);
                    Toast.makeText(this,
                            enabled ? "Notifications enabled" : "Notifications disabled",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating notification preference", e);
                    // Revert switch if update failed
                    switchNotifications.setChecked(!enabled);
                });
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

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data to check for role changes
        if (userId != null) {
            loadUserProfile();
        }
    }
}
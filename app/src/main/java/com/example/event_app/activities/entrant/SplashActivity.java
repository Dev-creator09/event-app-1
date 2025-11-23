package com.example.event_app.activities.entrant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.activities.shared.ProfileSetupActivity;
import com.example.event_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.event_app.activities.admin.AdminHomeActivity;


/**
 * SplashActivity - Uber-Inspired Minimal Launch Screen
 * Shows "LuckySpot" text on black background with fade-in animation
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 1500; // 1.5 seconds

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Make fully immersive
        makeFullScreen();

        // Animate app name text
        animateAppName();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get unique device ID
        deviceId = getUniqueDeviceId();
        Log.d(TAG, "Device ID: " + deviceId);

        // Authenticate after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(
                this::authenticateUser,
                SPLASH_DURATION
        );
    }

    private void makeFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    /**
     * Animate "LuckySpot" text with smooth fade-in
     */
    private void animateAppName() {
        TextView appName = findViewById(R.id.tvAppName);
        if (appName != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(800);
            appName.startAnimation(fadeIn);
        }
    }

    @SuppressLint("HardwareIds")
    private String getUniqueDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    private void authenticateUser() {
        if (mAuth.getCurrentUser() != null) {
            checkUserProfile();
        } else {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "✅ Anonymous sign-in successful");
                    checkUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Sign-in failed", e);
                    navigateToProfileSetup();
                });
    }

    private void checkUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        routeUserByRole(document);
                    } else {
                        navigateToProfileSetup();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking profile", e);
                    navigateToProfileSetup();
                });
    }

    private void routeUserByRole(DocumentSnapshot document) {
        User user = document.toObject(User.class);
        Intent intent;

        if (user != null && user.isAdmin()) {
            intent = new Intent(this, AdminHomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToProfileSetup() {
        Intent intent = new Intent(this, ProfileSetupActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("userId", mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : null);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
package com.example.event_app.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FavoritesManager - Helper class to manage user's favorite events
 *
 * Features:
 * - Add event to favorites
 * - Remove event from favorites
 * - Check if event is favorited
 * - Get all favorite event IDs
 */
public class FavoritesManager {

    private static final String TAG = "FavoritesManager";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public FavoritesManager() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Add event to user's favorites
     */
    public void addFavorite(String eventId, FavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .update("favoriteEvents", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event added to favorites: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add favorite", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Remove event from user's favorites
     */
    public void removeFavorite(String eventId, FavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .update("favoriteEvents", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event removed from favorites: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove favorite", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Check if event is in user's favorites
     */
    public void isFavorite(String eventId, IsFavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onResult(false);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        java.util.List<String> favorites = (java.util.List<String>) document.get("favoriteEvents");
                        boolean isFav = favorites != null && favorites.contains(eventId);
                        callback.onResult(isFav);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check favorite", e);
                    callback.onResult(false);
                });
    }

    /**
     * Callback for add/remove operations
     */
    public interface FavoriteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Callback for checking if favorited
     */
    public interface IsFavoriteCallback {
        void onResult(boolean isFavorite);
    }
}
package com.example.event_app.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.event_app.activities.entrant.EventDetailsActivity;

/**
 * Navigator - Handles app navigation
 */
public class Navigator {

    public static final String EXTRA_EVENT_ID = "com.example.event_app.EVENT_ID";

    /**
     * Navigate to Event Details screen
     * TEMP: Just shows toast until we build EventDetailsActivity
     */
    public void navigateToEventDetails(Context context, String eventId) {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        context.startActivity(intent);
    }

    /**
     * Show error message for invalid QR code
     */
    public void showInvalidQrError(Context context) {
        Toast.makeText(context, "Invalid QR code. Please scan an event QR code.", Toast.LENGTH_LONG).show();
    }

    /**
     * Show generic error message
     */
    public void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show success message
     */
    public void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
package com.example.event_app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * QRService - Processes scanned QR codes
 */
public class QRService {

    private static final String TAG = "QRService";
    private final Navigator navigator;

    /**
     * Default constructor - creates Navigator internally
     */
    public QRService() {
        this.navigator = new Navigator();
    }

    /**
     * Constructor with Navigator dependency injection
     */
    public QRService(Navigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Process scanned QR code content
     */
    public void processQrCode(Context context, String qrContent) {
        Log.d(TAG, "Processing QR code: " + qrContent);

        if (isValidEventId(qrContent)) {
            Log.d(TAG, "Valid event ID, navigating to details");
            navigator.navigateToEventDetails(context, qrContent);
        } else {
            Log.w(TAG, "Invalid QR code format");
            navigator.showInvalidQrError(context);
        }
    }

    /**
     * Validate event ID format
     */
    private boolean isValidEventId(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return false;
        }

        eventId = eventId.trim();

        if (eventId.length() < 10 || eventId.length() > 50) {
            return false;
        }

        if (!eventId.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        return true;
    }
}
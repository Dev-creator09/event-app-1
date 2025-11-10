package com.example.event_app.utils;

import android.Manifest;import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

/**
 * Manages runtime permissions for the application.
 * Use permission manager for items needing permission.
 */
public class PermissionManager {

    /**
     * Checks if the camera permission has been granted.
     */
    public static boolean isCameraPermissionGranted(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}

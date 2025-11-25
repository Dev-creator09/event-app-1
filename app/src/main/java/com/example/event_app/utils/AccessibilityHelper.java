package com.example.event_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

/**
 * AccessibilityHelper - Manages app-wide accessibility settings
 *
 * Features:
 * - Large text mode (increases all text sizes by 30%)
 * - High contrast mode (enables dark theme)
 * - Larger touch targets (increases button sizes by 50%)
 *
 * User Story: As an entrant, I want accessibility options (e.g., large text, high contrast)
 *
 * Usage:
 * 1. In SettingsActivity: Toggle switches save preferences
 * 2. In every Activity onCreate(): Call applyAccessibilitySettings()
 *
 * @author LuckySpot Team
 */
public class AccessibilityHelper {

    private static final String PREFS_NAME = "accessibility_prefs";
    private static final String KEY_LARGE_TEXT = "large_text_enabled";
    private static final String KEY_HIGH_CONTRAST = "high_contrast_enabled";
    private static final String KEY_LARGE_BUTTONS = "large_buttons_enabled";

    // Multipliers for accessibility features
    private static final float TEXT_SIZE_MULTIPLIER = 1.3f;  // 30% larger
    private static final float BUTTON_PADDING_MULTIPLIER = 1.5f;  // 50% more padding
    private static final int MIN_TOUCH_TARGET_DP = 48;  // Android accessibility guidelines

    private final Context context;
    private final SharedPreferences prefs;

    /**
     * Constructor
     */
    public AccessibilityHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    /**
     * Check if large text mode is enabled
     */
    public boolean isLargeTextEnabled() {
        return prefs.getBoolean(KEY_LARGE_TEXT, false);
    }

    /**
     * Enable or disable large text mode
     */
    public void setLargeTextEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply();
    }

    /**
     * Check if high contrast mode is enabled
     */
    public boolean isHighContrastEnabled() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    /**
     * Enable or disable high contrast mode
     */
    public void setHighContrastEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply();
        applyHighContrast();
    }

    /**
     * Check if larger buttons mode is enabled
     */
    public boolean isLargeButtonsEnabled() {
        return prefs.getBoolean(KEY_LARGE_BUTTONS, false);
    }

    /**
     * Enable or disable larger buttons mode
     */
    public void setLargeButtonsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LARGE_BUTTONS, enabled).apply();
    }

    // ========================================================================
    // MAIN APPLY METHOD - Call this in every Activity
    // ========================================================================

    /**
     * Apply all accessibility settings to an activity
     *
     * Call this in onCreate() of each activity AFTER setContentView():
     *
     * Example:
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     setContentView(R.layout.activity_main);
     *
     *     // Apply accessibility
     *     new AccessibilityHelper(this).applyAccessibilitySettings(this);
     *
     *     // Rest of your code...
     * }
     */
    public void applyAccessibilitySettings(AppCompatActivity activity) {
        // Apply high contrast if enabled
        if (isHighContrastEnabled()) {
            applyHighContrast();
        }

        // Apply text/button changes if enabled
        if (isLargeTextEnabled() || isLargeButtonsEnabled()) {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                applyToAllViews(rootView);
            }
        }
    }

    // ========================================================================
    // HIGH CONTRAST MODE
    // ========================================================================

    /**
     * Apply high contrast mode (dark theme)
     */
    private void applyHighContrast() {
        if (isHighContrastEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Get high contrast text color
     */
    public int getHighContrastTextColor() {
        return isHighContrastEnabled() ? Color.WHITE : Color.BLACK;
    }

    /**
     * Get high contrast background color
     */
    public int getHighContrastBackgroundColor() {
        return isHighContrastEnabled() ? Color.BLACK : Color.WHITE;
    }

    // ========================================================================
    // RECURSIVE VIEW PROCESSING
    // ========================================================================

    /**
     * Recursively apply accessibility settings to all views in hierarchy
     */
    private void applyToAllViews(View view) {
        if (view == null) return;

        // Apply to specific view types
        if (view instanceof TextView) {
            applyToTextView((TextView) view);
        }

        if (view instanceof MaterialButton) {
            applyToButton((MaterialButton) view);
        }

        // Recursively process child views
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyToAllViews(child);
            }
        }
    }

    // ========================================================================
    // LARGE TEXT MODE
    // ========================================================================

    /**
     * Apply large text to TextView (and subclasses)
     */
    private void applyToTextView(TextView textView) {
        if (!isLargeTextEnabled()) return;

        try {
            // Get current text size in SP
            float currentSizePx = textView.getTextSize();
            float currentSizeSp = currentSizePx / context.getResources().getDisplayMetrics().scaledDensity;

            // Apply multiplier
            float newSizeSp = currentSizeSp * TEXT_SIZE_MULTIPLIER;

            // Set new size
            textView.setTextSize(newSizeSp);

        } catch (Exception e) {
            // Silently fail - don't break the app if text size adjustment fails
        }
    }

    // ========================================================================
    // LARGER TOUCH TARGETS MODE
    // ========================================================================

    /**
     * Apply larger touch targets to buttons
     */
    private void applyToButton(MaterialButton button) {
        if (!isLargeButtonsEnabled()) return;

        try {
            // Increase padding
            int currentPaddingTop = button.getPaddingTop();
            int currentPaddingBottom = button.getPaddingBottom();
            int currentPaddingStart = button.getPaddingStart();
            int currentPaddingEnd = button.getPaddingEnd();

            int newPaddingTop = (int) (currentPaddingTop * BUTTON_PADDING_MULTIPLIER);
            int newPaddingBottom = (int) (currentPaddingBottom * BUTTON_PADDING_MULTIPLIER);
            int newPaddingStart = (int) (currentPaddingStart * BUTTON_PADDING_MULTIPLIER);
            int newPaddingEnd = (int) (currentPaddingEnd * BUTTON_PADDING_MULTIPLIER);

            button.setPadding(newPaddingStart, newPaddingTop, newPaddingEnd, newPaddingBottom);

            // Ensure minimum touch target size (48dp)
            int minTouchTargetPx = dpToPx(MIN_TOUCH_TARGET_DP);
            button.setMinHeight(minTouchTargetPx);
            button.setMinWidth(minTouchTargetPx);

        } catch (Exception e) {
            // Silently fail
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Convert DP to pixels
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Convert pixels to DP
     */
    private int pxToDp(int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }

    /**
     * Clear all accessibility preferences (for testing)
     */
    public void clearAllSettings() {
        prefs.edit().clear().apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Get summary of current accessibility settings
     */
    public String getAccessibilitySettingsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Accessibility Settings:\n");
        summary.append("- Large Text: ").append(isLargeTextEnabled() ? "ON" : "OFF").append("\n");
        summary.append("- High Contrast: ").append(isHighContrastEnabled() ? "ON" : "OFF").append("\n");
        summary.append("- Larger Buttons: ").append(isLargeButtonsEnabled() ? "ON" : "OFF");
        return summary.toString();
    }
}
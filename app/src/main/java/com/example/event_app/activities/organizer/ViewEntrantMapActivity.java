package com.example.event_app.activities.organizer;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ViewEntrantMapActivity
 *
 * US 02.02.02: As an organizer I want to see on a map where entrants joined
 * my event waiting list from.
 *
 * Features:
 * - Displays map with markers for each entrant location
 * - Clusters markers when multiple entrants are from same location
 * - Shows event location if available
 * - Automatically adjusts camera to show all markers
 * - Handles geolocation-disabled events gracefully
 *
 * Architecture:
 * - Loads event data from Firestore
 * - Processes entrantLocations map: { "userId": { "latitude": X, "longitude": Y } }
 * - Displays markers with circles for visibility
 * - Auto-adjusts camera bounds to show all locations
 */
public class ViewEntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ViewEntrantMap";
    private static final float DEFAULT_ZOOM = 10f;
    private static final float SINGLE_LOCATION_ZOOM = 12f;
    private static final int MARKER_RADIUS_METERS = 500;
    private static final int MAP_PADDING_PX = 120;

    // UI Elements
    private ImageButton btnBack;
    private GoogleMap mMap;

    // Data
    private FirebaseFirestore db;
    private String eventId;
    private Event event;
    private List<LatLng> entrantLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entrant_map);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event ID from intent
        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI
        initializeUI();

        // Initialize map
        initializeMap();

        // Load event data
        loadEventData();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Initialize Google Map fragment
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error initializing map", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Load event data from Firestore
     */
    private void loadEventData() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        event = document.toObject(Event.class);
                        if (event != null) {
                            event.setId(document.getId());

                            // Check if geolocation is enabled
                            if (!event.isGeolocationEnabled()) {
                                Toast.makeText(this, "Geolocation not enabled for this event",
                                        Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            // Process entrant locations
                            processEntrantLocations();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Process entrant locations from event data
     *
     * Expected Firebase structure:
     * entrantLocations: {
     *   "userId1": { "latitude": 53.5, "longitude": -113.5 },
     *   "userId2": { "latitude": 51.0, "longitude": -114.0 }
     * }
     */
    private void processEntrantLocations() {
        entrantLocations.clear();

        Map<String, Map<String, Double>> locations = event.getEntrantLocations();
        if (locations == null || locations.isEmpty()) {
            Log.d(TAG, "No location data available");
            Toast.makeText(this, "No entrant locations to display", Toast.LENGTH_LONG).show();
            // Still allow viewing the map, just without markers
            return;
        }

        // Extract LatLng objects from the map
        int validLocations = 0;
        for (Map.Entry<String, Map<String, Double>> entry : locations.entrySet()) {
            Map<String, Double> coords = entry.getValue();
            if (coords != null && coords.containsKey("latitude") && coords.containsKey("longitude")) {
                Double lat = coords.get("latitude");
                Double lng = coords.get("longitude");

                // Validate coordinates
                if (lat != null && lng != null && isValidCoordinate(lat, lng)) {
                    entrantLocations.add(new LatLng(lat, lng));
                    validLocations++;
                } else {
                    Log.w(TAG, "Invalid coordinates for user " + entry.getKey());
                }
            }
        }

        Log.d(TAG, "Processed " + validLocations + " valid entrant locations out of " + locations.size());

        // Update map if ready
        if (mMap != null) {
            displayLocationsOnMap();
        }
    }

    /**
     * Validate that coordinates are within valid ranges
     */
    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Called when Google Map is ready
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Set map type to normal
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Display locations if already loaded
        if (!entrantLocations.isEmpty()) {
            displayLocationsOnMap();
        } else {
            // Default view (Calgary, Alberta - Dev's location)
            LatLng defaultLocation = new LatLng(51.0447, -114.0719);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        }
    }

    /**
     * Display all entrant locations on the map with markers and circles
     */
    private void displayLocationsOnMap() {
        if (mMap == null) {
            Log.w(TAG, "Map not ready yet");
            return;
        }

        if (entrantLocations.isEmpty()) {
            Log.d(TAG, "No locations to display");
            return;
        }

        // Clear existing markers
        mMap.clear();

        // Add markers for each entrant location
        for (int i = 0; i < entrantLocations.size(); i++) {
            LatLng location = entrantLocations.get(i);

            // Add marker with custom styling
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Entrant #" + (i + 1))
                    .snippet("Joined waiting list from here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Add circle around marker for better visibility
            mMap.addCircle(new CircleOptions()
                    .center(location)
                    .radius(MARKER_RADIUS_METERS)
                    .strokeColor(Color.RED)
                    .strokeWidth(2)
                    .fillColor(Color.argb(70, 255, 0, 0))); // Semi-transparent red
        }

        // Adjust camera to show all markers
        adjustCameraToShowAllMarkers();

        // Show success message
        String message = entrantLocations.size() == 1
                ? "1 entrant location displayed"
                : entrantLocations.size() + " entrant locations displayed";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Adjust camera position to show all markers in view
     */
    private void adjustCameraToShowAllMarkers() {
        if (entrantLocations.size() == 1) {
            // Single location - center on it with appropriate zoom
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    entrantLocations.get(0),
                    SINGLE_LOCATION_ZOOM));
        } else {
            // Multiple locations - fit all in view with bounds
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng location : entrantLocations) {
                builder.include(location);
            }

            try {
                LatLngBounds bounds = builder.build();
                // Animate camera with padding around markers
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX));
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error building bounds", e);
                // Fallback to first location
                if (!entrantLocations.isEmpty()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            entrantLocations.get(0),
                            DEFAULT_ZOOM));
                }
            }
        }
    }
}
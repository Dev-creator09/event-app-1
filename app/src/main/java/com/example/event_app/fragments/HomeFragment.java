package com.example.event_app.fragments;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.BrowseEventsActivity;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.utils.PermissionManager;
import com.google.android.material.card.MaterialCardView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * HomeFragment - Quick actions and shortcuts
 * US 01.06.01: Scan QR code to view event
 */
public class HomeFragment extends Fragment {

    // Permission launcher for camera
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchQrScanner();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    // QR scanner launcher
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    // TEMP: Show toast until we build EventDetailsActivity
                    Toast.makeText(requireContext(), "Scanned: " + result.getContents(), Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Scan QR Card
        MaterialCardView cardScanQR = view.findViewById(R.id.cardScanQR);
        cardScanQR.setOnClickListener(v -> scanQrCode());


        MaterialCardView cardBrowseEvents = view.findViewById(R.id.cardBrowseEvents);
        cardBrowseEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        // My Events Card
        MaterialCardView cardMyEvents = view.findViewById(R.id.cardMyEvents);
        cardMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });
    }

    private void scanQrCode() {
        if (PermissionManager.isCameraPermissionGranted(requireActivity())) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }
}
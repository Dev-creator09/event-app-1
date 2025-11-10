package com.example.event_app.activities.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * CreateEventActivity - Create new events
 *
 * US 02.01.01: Create event and generate QR code
 * US 02.01.04: Set registration period
 * US 02.03.01: Limit number of entrants
 * US 02.04.01: Upload event poster
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Elements
    private TextInputEditText editEventName, editDescription, editLocation, editCapacity;
    private MaterialButton btnSelectPoster, btnSelectEventDate, btnSelectRegStart, btnSelectRegEnd;
    private MaterialButton btnCreateEvent;
    private ImageView ivPosterPreview;
    private View loadingView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Data
    private Uri posterUri;
    private Date eventDate, regStartDate, regEndDate;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    posterUri = result.getData().getData();
                    displayPosterPreview();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        initViews();
    }

    private void initViews() {
        // Input fields
        editEventName = findViewById(R.id.editEventName);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        editCapacity = findViewById(R.id.editCapacity);

        // Buttons
        btnSelectPoster = findViewById(R.id.btnSelectPoster);
        btnSelectEventDate = findViewById(R.id.btnSelectEventDate);
        btnSelectRegStart = findViewById(R.id.btnSelectRegStart);
        btnSelectRegEnd = findViewById(R.id.btnSelectRegEnd);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        // Other views
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        loadingView = findViewById(R.id.loadingView);

        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSelectPoster.setOnClickListener(v -> selectPoster());
        btnSelectEventDate.setOnClickListener(v -> selectEventDate());
        btnSelectRegStart.setOnClickListener(v -> selectRegistrationStart());
        btnSelectRegEnd.setOnClickListener(v -> selectRegistrationEnd());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    /**
     * US 02.04.01: Upload event poster
     */
    private void selectPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void displayPosterPreview() {
        ivPosterPreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(posterUri)
                .centerCrop()
                .into(ivPosterPreview);
        btnSelectPoster.setText("Change Poster");
    }

    /**
     * Select event date
     */
    private void selectEventDate() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            // Now pick time
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                eventDate = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                btnSelectEventDate.setText(sdf.format(eventDate));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * US 02.01.04: Set registration start date
     */
    private void selectRegistrationStart() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            regStartDate = calendar.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            btnSelectRegStart.setText(sdf.format(regStartDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * US 02.01.04: Set registration end date
     */
    private void selectRegistrationEnd() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            regEndDate = calendar.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            btnSelectRegEnd.setText(sdf.format(regEndDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * US 02.01.01: Create event and generate QR code
     */
    private void createEvent() {
        // Get values
        String name = editEventName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String capacityStr = editCapacity.getText().toString().trim();

        // Validate
        if (!validateInputs(name, description)) {
            return;
        }

        // Parse capacity
        Long capacity = null;
        if (!TextUtils.isEmpty(capacityStr)) {
            try {
                capacity = Long.parseLong(capacityStr);
            } catch (NumberFormatException e) {
                editCapacity.setError("Invalid number");
                return;
            }
        }

        // Show loading
        showLoading();

        // Create event ID
        String eventId = db.collection("events").document().getId();

        // Create event object
        Event event = new Event(eventId, name, description, mAuth.getCurrentUser().getUid());
        event.setLocation(location);
        event.setCapacity(capacity);
        event.setEventDate(eventDate);
        event.setRegistrationStartDate(regStartDate);
        event.setRegistrationEndDate(regEndDate);
        event.setWaitingList(new ArrayList<>());
        event.setSignedUpUsers(new ArrayList<>());
        event.setStatus("active");

        // Get organizer name
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String organizerName = userDoc.getString("name");
                        event.setOrganizerName(organizerName);
                    }

                    // Upload poster if selected
                    if (posterUri != null) {
                        uploadPosterAndCreateEvent(eventId, event);
                    } else {
                        saveEventToFirestore(eventId, event);
                    }
                });
    }

    private boolean validateInputs(String name, String description) {
        if (TextUtils.isEmpty(name)) {
            editEventName.setError("Event name is required");
            editEventName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            editDescription.setError("Description is required");
            editDescription.requestFocus();
            return false;
        }

        if (eventDate == null) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Upload poster to Firebase Storage
     */
    private void uploadPosterAndCreateEvent(String eventId, Event event) {
        StorageReference posterRef = storage.getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(posterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        event.setPosterUrl(uri.toString());
                        saveEventToFirestore(eventId, event);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading poster", e);
                    Toast.makeText(this, "Failed to upload poster, creating event without it", Toast.LENGTH_SHORT).show();
                    saveEventToFirestore(eventId, event);
                });
    }

    /**
     * Save event to Firestore and generate QR code
     */
    private void saveEventToFirestore(String eventId, Event event) {
        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Event created successfully");

                    // Generate and upload QR code
                    generateAndUploadQRCode(eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error creating event", e);
                    hideLoading();
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * US 02.01.01: Generate QR code for event
     */
    private void generateAndUploadQRCode(String eventId) {
        try {
            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            // Upload to Firebase Storage
            StorageReference qrRef = storage.getReference()
                    .child("qr_codes")
                    .child(eventId + ".png");

            qrRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "✅ QR code uploaded");
                        hideLoading();
                        showSuccessAndNavigate();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error uploading QR code", e);
                        hideLoading();
                        // Still show success even if QR upload fails
                        showSuccessAndNavigate();
                    });

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            hideLoading();
            showSuccessAndNavigate();
        }
    }

    private void showSuccessAndNavigate() {
        Toast.makeText(this, "Event created successfully! 🎉", Toast.LENGTH_LONG).show();

        // Go back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        btnCreateEvent.setEnabled(false);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        btnCreateEvent.setEnabled(true);
    }
}

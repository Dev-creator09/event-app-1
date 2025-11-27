package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.GeolocationAuditAdapter;
import com.example.event_app.models.GeolocationAudit;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminGeolocationAuditActivity - Admin audit of geolocation usage
 * For privacy compliance monitoring
 */
public class AdminGeolocationAuditActivity extends AppCompatActivity {

    private static final String TAG = "GeolocationAudit";

    private EditText etSearch;
    private RecyclerView recyclerViewAudits;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalRecords;
    private Button btnExportCSV;
    private Button btnClearOldRecords;

    private GeolocationAuditAdapter auditAdapter;
    private FirebaseFirestore db;
    private List<GeolocationAudit> auditList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_geolocation_audit);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geolocation Audit");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        auditList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up buttons
        setupButtons();

        // Load audit records
        loadAuditRecords();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearchAudits);
        recyclerViewAudits = findViewById(R.id.recyclerViewAudits);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnClearOldRecords = findViewById(R.id.btnClearOldRecords);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                auditAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        auditAdapter = new GeolocationAuditAdapter();

        auditAdapter.setOnAuditClickListener(audit -> {
            showAuditDetails(audit);
        });

        recyclerViewAudits.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAudits.setAdapter(auditAdapter);
    }

    private void setupButtons() {
        btnExportCSV.setOnClickListener(v -> exportToCSV());
        btnClearOldRecords.setOnClickListener(v -> showClearOldRecordsDialog());
    }

    private void loadAuditRecords() {
        Log.d(TAG, "Loading geolocation audit records...");
        progressBar.setVisibility(View.VISIBLE);

        db.collection("geolocation_audits")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500)  // Limit to last 500 records for performance
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    auditList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        GeolocationAudit audit = document.toObject(GeolocationAudit.class);
                        auditList.add(audit);
                    }

                    Log.d(TAG, "Loaded " + auditList.size() + " audit records");

                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading audit records", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading records: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    private void updateUI() {
        if (auditList.isEmpty()) {
            recyclerViewAudits.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalRecords.setText("Total: 0 records");
        } else {
            recyclerViewAudits.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalRecords.setText("Total: " + auditList.size() + " records (showing last 500)");

            auditAdapter.setAudits(auditList);
        }
    }

    private void showAuditDetails(GeolocationAudit audit) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());

        String details = "User: " + audit.getUserName() + "\n" +
                "User ID: " + audit.getUserId() + "\n\n" +
                "Event: " + audit.getEventName() + "\n" +
                "Event ID: " + audit.getEventId() + "\n\n" +
                "Action: " + audit.getAction() + "\n" +
                "Location: " + audit.getLocationString() + "\n" +
                "Timestamp: " + (audit.getTimestamp() != null ? sdf.format(audit.getTimestamp()) : "N/A");

        new AlertDialog.Builder(this)
                .setTitle("Geolocation Audit Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("View on Map", (dialog, which) -> {
                    // TODO: Open map view with this location
                    Toast.makeText(this, "Map view coming soon", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void exportToCSV() {
        // TODO: Implement CSV export
        Toast.makeText(this, "CSV export coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showClearOldRecordsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Old Records")
                .setMessage("Delete geolocation records older than 90 days?\n\n" +
                        "This helps maintain privacy compliance by not storing location data indefinitely.")
                .setPositiveButton("Clear Old Records", (dialog, which) -> {
                    clearOldRecords();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearOldRecords() {
        progressBar.setVisibility(View.VISIBLE);

        // Calculate date 90 days ago
        long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
        Date cutoffDate = new Date(ninetyDaysAgo);

        db.collection("geolocation_audits")
                .whereLessThan("timestamp", cutoffDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();

                    if (count == 0) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "No old records to delete", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Delete each document
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Deleted " + count + " old records", Toast.LENGTH_SHORT).show();

                    // Reload
                    loadAuditRecords();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error clearing old records", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
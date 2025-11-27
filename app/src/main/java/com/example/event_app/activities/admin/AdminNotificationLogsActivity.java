package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.NotificationLogAdapter;
import com.example.event_app.models.NotificationLog;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AdminNotificationLogsActivity - Admin review of all notification logs
 * US 03.08.01: Review logs of all notifications sent by organizers
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationLogs";

    private EditText etSearch;
    private Spinner spinnerFilter;
    private RecyclerView recyclerViewLogs;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalLogs;
    private Button btnExportLogs;

    private NotificationLogAdapter logAdapter;
    private FirebaseFirestore db;
    private List<NotificationLog> logList;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Logs");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        logList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up filter
        setupFilter();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up buttons
        setupButtons();

        // Load notification logs
        loadNotificationLogs();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearchLogs);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalLogs = findViewById(R.id.tvTotalLogs);
        btnExportLogs = findViewById(R.id.btnExportLogs);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilter() {
        // Create filter options
        String[] filterOptions = {
                "All Notifications",
                "Organizer Only",
                "Invitation Sent",
                "Selected",
                "Rejected",
                "Waitlist Joined",
                "Today",
                "This Week"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = filterOptions[position];
                applyFilter(selected);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void applyFilter(String filter) {
        Log.d(TAG, "Applying filter: " + filter);
        currentFilter = filter;
        loadNotificationLogs();
    }

    private void setupRecyclerView() {
        logAdapter = new NotificationLogAdapter();

        logAdapter.setOnLogClickListener(log -> {
            showLogDetails(log);
        });

        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLogs.setAdapter(logAdapter);
    }

    private void setupButtons() {
        btnExportLogs.setOnClickListener(v -> exportLogs());
    }

    private void loadNotificationLogs() {
        Log.d(TAG, "Loading notification logs with filter: " + currentFilter);
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500);

        // Apply filters
        if (currentFilter.equals("Organizer Only")) {
            // This would need a field in the log to track sender role
            // For now, load all and filter in adapter
        } else if (currentFilter.equals("Invitation Sent")) {
            query = query.whereEqualTo("notificationType", "invitation_sent");
        } else if (currentFilter.equals("Selected")) {
            query = query.whereEqualTo("notificationType", "selected");
        } else if (currentFilter.equals("Rejected")) {
            query = query.whereEqualTo("notificationType", "rejected");
        } else if (currentFilter.equals("Waitlist Joined")) {
            query = query.whereEqualTo("notificationType", "waitlist_joined");
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    logList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationLog log = document.toObject(NotificationLog.class);
                        logList.add(log);
                    }

                    Log.d(TAG, "Loaded " + logList.size() + " notification logs");

                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notification logs", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading logs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    private void updateUI() {
        if (logList.isEmpty()) {
            recyclerViewLogs.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalLogs.setText("Total: 0 logs");
        } else {
            recyclerViewLogs.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalLogs.setText("Total: " + logList.size() + " logs (showing last 500)");

            logAdapter.setLogs(logList);
        }
    }

    private void showLogDetails(NotificationLog log) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());

        String details = "FROM: " + log.getSenderName() + " (ID: " + log.getSenderId() + ")\n\n" +
                "TO: " + log.getRecipientName() + " (ID: " + log.getRecipientId() + ")\n\n" +
                "EVENT: " + (log.getEventName() != null ? log.getEventName() : "N/A") + "\n\n" +
                "TYPE: " + log.getNotificationType() + "\n\n" +
                "TITLE: " + log.getTitle() + "\n\n" +
                "MESSAGE: " + log.getMessage() + "\n\n" +
                "STATUS: " + log.getStatus() + "\n\n" +
                "TIMESTAMP: " + (log.getTimestamp() != null ? sdf.format(log.getTimestamp()) : "N/A");

        new AlertDialog.Builder(this)
                .setTitle("Notification Log Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void exportLogs() {
        // TODO: Implement CSV export
        Toast.makeText(this, "CSV export coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

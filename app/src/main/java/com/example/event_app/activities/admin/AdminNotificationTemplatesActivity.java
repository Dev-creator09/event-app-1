package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
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
import com.example.event_app.adapters.NotificationTemplateAdapter;
import com.example.event_app.models.NotificationTemplate;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AdminNotificationTemplatesActivity - Manage notification templates
 * Allows admins to create, edit, and manage notification templates used across the app
 */
public class AdminNotificationTemplatesActivity extends AppCompatActivity {

    private static final String TAG = "NotificationTemplates";

    private EditText etSearch;
    private RecyclerView recyclerViewTemplates;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalTemplates;
    private FloatingActionButton fabAddTemplate;

    private NotificationTemplateAdapter templateAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<NotificationTemplate> templateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_templates);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Templates");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize list
        templateList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up FAB
        setupFAB();

        // Load templates
        loadTemplates();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearchTemplates);
        recyclerViewTemplates = findViewById(R.id.recyclerViewTemplates);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalTemplates = findViewById(R.id.tvTotalTemplates);
        fabAddTemplate = findViewById(R.id.fabAddTemplate);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                templateAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        templateAdapter = new NotificationTemplateAdapter();

        templateAdapter.setOnTemplateClickListener(new NotificationTemplateAdapter.OnTemplateClickListener() {
            @Override
            public void onTemplateClick(NotificationTemplate template) {
                showEditTemplateDialog(template);
            }

            @Override
            public void onToggleActive(NotificationTemplate template) {
                toggleTemplateActive(template);
            }

            @Override
            public void onDeleteTemplate(NotificationTemplate template) {
                showDeleteConfirmation(template);
            }
        });

        recyclerViewTemplates.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTemplates.setAdapter(templateAdapter);
    }

    private void setupFAB() {
        fabAddTemplate.setOnClickListener(v -> showCreateTemplateDialog());
    }

    private void loadTemplates() {
        Log.d(TAG, "Loading notification templates...");
        progressBar.setVisibility(View.VISIBLE);

        db.collection("notification_templates")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    templateList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationTemplate template = document.toObject(NotificationTemplate.class);
                        templateList.add(template);
                    }

                    Log.d(TAG, "Loaded " + templateList.size() + " templates");

                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading templates", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading templates: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    private void updateUI() {
        if (templateList.isEmpty()) {
            recyclerViewTemplates.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalTemplates.setText("Total: 0 templates");
        } else {
            recyclerViewTemplates.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalTemplates.setText("Total: " + templateList.size() + " templates");

            templateAdapter.setTemplates(templateList);
        }
    }

    private void showCreateTemplateDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_template, null);

        EditText etName = dialogView.findViewById(R.id.etTemplateName);
        EditText etType = dialogView.findViewById(R.id.etTemplateType);
        EditText etTitle = dialogView.findViewById(R.id.etTemplateTitle);
        EditText etMessage = dialogView.findViewById(R.id.etTemplateMessage);
        TextView tvPlaceholders = dialogView.findViewById(R.id.tvPlaceholders);

        tvPlaceholders.setText("Available placeholders:\n{userName}, {eventName}, {organizerName}, {date}");

        new AlertDialog.Builder(this)
                .setTitle("Create Template")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (name.isEmpty() || title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createTemplate(name, type, title, message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditTemplateDialog(NotificationTemplate template) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_template, null);

        EditText etName = dialogView.findViewById(R.id.etTemplateName);
        EditText etType = dialogView.findViewById(R.id.etTemplateType);
        EditText etTitle = dialogView.findViewById(R.id.etTemplateTitle);
        EditText etMessage = dialogView.findViewById(R.id.etTemplateMessage);
        TextView tvPlaceholders = dialogView.findViewById(R.id.tvPlaceholders);

        // Pre-fill with existing data
        etName.setText(template.getName());
        etType.setText(template.getType());
        etTitle.setText(template.getTitle());
        etMessage.setText(template.getMessage());
        tvPlaceholders.setText("Available placeholders:\n{userName}, {eventName}, {organizerName}, {date}");

        new AlertDialog.Builder(this)
                .setTitle("Edit Template")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (name.isEmpty() || title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateTemplate(template.getTemplateId(), name, type, title, message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTemplate(String name, String type, String title, String message) {
        String templateId = db.collection("notification_templates").document().getId();
        String adminId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        NotificationTemplate template = new NotificationTemplate(
                templateId,
                name,
                type,
                title,
                message,
                true,  // Active by default
                new Date(),
                new Date(),
                adminId
        );

        db.collection("notification_templates")
                .document(templateId)
                .set(template)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Template created successfully");
                    Toast.makeText(this, "Template created!", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating template", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTemplate(String templateId, String name, String type, String title, String message) {
        db.collection("notification_templates")
                .document(templateId)
                .update(
                        "name", name,
                        "type", type,
                        "title", title,
                        "message", message,
                        "updatedAt", new Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Template updated successfully");
                    Toast.makeText(this, "Template updated!", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating template", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleTemplateActive(NotificationTemplate template) {
        boolean newStatus = !template.isActive();

        db.collection("notification_templates")
                .document(template.getTemplateId())
                .update("isActive", newStatus, "updatedAt", new Date())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Template status toggled");
                    Toast.makeText(this,
                            newStatus ? "Template activated" : "Template deactivated",
                            Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error toggling template", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation(NotificationTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Delete \"" + template.getName() + "\"?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTemplate(template);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTemplate(NotificationTemplate template) {
        db.collection("notification_templates")
                .document(template.getTemplateId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Template deleted");
                    Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting template", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

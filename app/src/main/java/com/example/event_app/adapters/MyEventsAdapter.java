package com.example.event_app.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * MyEventsAdapter - Shows user's events with status and action buttons
 *
 * US 01.05.02: Accept invitation
 * US 01.05.03: Decline invitation
 * US 01.05.01: Automatic replacement when someone declines
 */
public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.EventViewHolder> {

    private static final String TAG = "MyEventsAdapter";

    private Context context;
    private List<Event> events;
    private String userId;
    private FirebaseFirestore db;

    public MyEventsAdapter(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.events = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardEvent;
        ImageView ivPoster;
        TextView tvEventName, tvDate, tvStatus;
        LinearLayout buttonContainer;
        MaterialButton btnAccept, btnDecline;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvent = itemView.findViewById(R.id.cardEvent);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            buttonContainer = itemView.findViewById(R.id.buttonContainer);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName());

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Determine status
            String status = getUserStatus(event);
            tvStatus.setText(status);
            setStatusColor(tvStatus, status);

            // Show/hide action buttons based on status
            if (status.equals("🎉 Selected!")) {
                buttonContainer.setVisibility(View.VISIBLE);
                setupActionButtons(event);
            } else {
                buttonContainer.setVisibility(View.GONE);
            }

            // Load poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            }

            // Click listener
            cardEvent.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventDetailsActivity.class);
                intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId());
                context.startActivity(intent);
            });
        }

        /**
         * Determine user's status for this event
         */
        private String getUserStatus(Event event) {
            boolean isInWaitingList = event.getWaitingList() != null &&
                    event.getWaitingList().contains(userId);
            boolean isSelected = event.getSelectedList() != null &&
                    event.getSelectedList().contains(userId);
            boolean isAttending = event.getSignedUpUsers() != null &&
                    event.getSignedUpUsers().contains(userId);
            boolean isDeclined = event.getDeclinedUsers() != null &&
                    event.getDeclinedUsers().contains(userId);

            if (isAttending) {
                return "Attending";
            } else if (isDeclined) {
                return "Declined";
            } else if (isSelected) {
                return "🎉 Selected!";
            } else if (isInWaitingList) {
                return "Waiting";
            }
            return "Unknown";
        }

        private void setStatusColor(TextView textView, String status) {
            int backgroundColor;
            int textColor;

            switch (status) {
                case "Attending":
                    backgroundColor = 0xFF4CAF50; // Green
                    textColor = 0xFFFFFFFF; // White
                    break;
                case "🎉 Selected!":
                    backgroundColor = 0xFF2196F3; // Blue
                    textColor = 0xFFFFFFFF; // White
                    break;
                case "Waiting":
                    backgroundColor = 0xFFFFC107; // Amber
                    textColor = 0xFF000000; // Black
                    break;
                case "Declined":
                    backgroundColor = 0xFFF44336; // Red
                    textColor = 0xFFFFFFFF; // White
                    break;
                default:
                    backgroundColor = 0xFF9E9E9E; // Gray
                    textColor = 0xFFFFFFFF; // White
                    break;
            }

            textView.setBackgroundColor(backgroundColor);
            textView.setTextColor(textColor);
        }

        /**
         * US 01.05.02 & US 01.05.03: Setup Accept/Decline buttons
         */
        private void setupActionButtons(Event event) {
            // Accept button
            btnAccept.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Accept Invitation")
                        .setMessage("Accept invitation to " + event.getName() + "?")
                        .setPositiveButton("Accept", (dialog, which) -> acceptInvitation(event))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Decline button
            btnDecline.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Decline Invitation")
                        .setMessage("Are you sure you want to decline? This cannot be undone.")
                        .setPositiveButton("Decline", (dialog, which) -> declineInvitation(event))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        /**
         * US 01.05.02: Accept invitation - move to attending list
         * FIX: Remove from BOTH selectedList AND waitingList
         */
        private void acceptInvitation(Event event) {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);

            // Initialize lists if null
            if (event.getSignedUpUsers() == null) {
                event.setSignedUpUsers(new ArrayList<>());
            }
            if (event.getSelectedList() == null) {
                event.setSelectedList(new ArrayList<>());
            }
            if (event.getWaitingList() == null) {
                event.setWaitingList(new ArrayList<>());
            }

            // Add to signed up users
            if (!event.getSignedUpUsers().contains(userId)) {
                event.getSignedUpUsers().add(userId);
            }

            // ✅ FIX: Remove from BOTH selected list AND waiting list
            event.getSelectedList().remove(userId);
            event.getWaitingList().remove(userId);

            // Update Firestore
            db.collection("events").document(event.getId())
                    .update(
                            "signedUpUsers", event.getSignedUpUsers(),
                            "selectedList", event.getSelectedList(),
                            "waitingList", event.getWaitingList()  // ✅ Also update waiting list
                    )
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ User accepted invitation");
                        Toast.makeText(context, "You're attending! 🎉", Toast.LENGTH_LONG).show();

                        // Remove this event from the list (will reload)
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            events.remove(position);
                            notifyItemRemoved(position);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error accepting invitation", e);
                        Toast.makeText(context, "Failed to accept. Try again.", Toast.LENGTH_SHORT).show();
                        btnAccept.setEnabled(true);
                        btnDecline.setEnabled(true);
                    });
        }

        /**
         * US 01.05.03: Decline invitation - remove from event
         * US 01.05.01: Automatically draw replacement from waiting list
         */
        private void declineInvitation(Event event) {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);

            // Initialize lists if null
            if (event.getSelectedList() == null) {
                event.setSelectedList(new ArrayList<>());
            }
            if (event.getWaitingList() == null) {
                event.setWaitingList(new ArrayList<>());
            }
            if (event.getDeclinedUsers() == null) {
                event.setDeclinedUsers(new ArrayList<>());
            }

            // Add to declined list (so they can see their history)
            if (!event.getDeclinedUsers().contains(userId)) {
                event.getDeclinedUsers().add(userId);
            }

            // Remove from selected list
            event.getSelectedList().remove(userId);

            // Remove from waiting list
            event.getWaitingList().remove(userId);

            // Increment cancelled count
            int newCancelledCount = event.getTotalCancelled() + 1;

            // ✅ US 01.05.01: Automatically draw replacement if spots available
            // Use final variable for lambda
            final boolean[] drewReplacementArray = {false};

            if (event.getCapacity() != null) {
                int currentSelected = event.getSelectedList().size();
                int capacity = event.getCapacity().intValue();

                // If we're under capacity and there are people waiting
                if (currentSelected < capacity && !event.getWaitingList().isEmpty()) {
                    // Draw one replacement from waiting list
                    List<String> availableEntrants = new ArrayList<>(event.getWaitingList());
                    // Remove already selected people
                    availableEntrants.removeAll(event.getSelectedList());

                    if (!availableEntrants.isEmpty()) {
                        // Randomly select one
                        Collections.shuffle(availableEntrants);
                        String replacement = availableEntrants.get(0);
                        event.getSelectedList().add(replacement);
                        drewReplacementArray[0] = true;
                        Log.d(TAG, "✅ Drew replacement entrant: " + replacement);
                    }
                }
            }

            // Make final copy for lambda
            final boolean drewReplacement = drewReplacementArray[0];

            // Update Firestore
            db.collection("events").document(event.getId())
                    .update(
                            "selectedList", event.getSelectedList(),
                            "waitingList", event.getWaitingList(),
                            "declinedUsers", event.getDeclinedUsers(),
                            "totalCancelled", newCancelledCount
                    )
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ User declined invitation");
                        String message = drewReplacement ?
                                "Invitation declined. Another entrant was selected!" :
                                "Invitation declined";
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                        // Remove this event from the list
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            events.remove(position);
                            notifyItemRemoved(position);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error declining invitation", e);
                        Toast.makeText(context, "Failed to decline. Try again.", Toast.LENGTH_SHORT).show();
                        btnAccept.setEnabled(true);
                        btnDecline.setEnabled(true);
                    });
        }
    }
}
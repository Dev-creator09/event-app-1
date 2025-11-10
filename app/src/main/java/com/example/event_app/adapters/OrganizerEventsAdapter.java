package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.organizer.OrganizerEventDetailsActivity;  // ✅ FIXED IMPORT
import com.example.event_app.models.Event;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerEventsAdapter - Shows organizer's events with stats
 */
public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;

    public OrganizerEventsAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_organizer_event, parent, false);
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
        TextView tvEventName, tvDate, tvWaitingCount, tvSelectedCount, tvAttendingCount;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvent = itemView.findViewById(R.id.cardEvent);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvWaitingCount = itemView.findViewById(R.id.tvWaitingCount);
            tvSelectedCount = itemView.findViewById(R.id.tvSelectedCount);
            tvAttendingCount = itemView.findViewById(R.id.tvAttendingCount);
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

            // Stats
            int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

            tvWaitingCount.setText(String.format(Locale.getDefault(), "%d waiting", waitingCount));
            tvSelectedCount.setText(String.format(Locale.getDefault(), "%d selected", event.getTotalSelected()));
            tvAttendingCount.setText(String.format(Locale.getDefault(), "%d attending", attendingCount));

            // Load poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            }

            // ✅ FIXED: Click listener - navigate to DETAILS screen, not list screen
            cardEvent.setOnClickListener(v -> {
                Log.d("OrganizerAdapter", "Opening details for event: " + event.getId());
                Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                context.startActivity(intent);
            });
        }
    }
}
package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * EventAdapter - RecyclerView adapter for displaying events
 * Used in Browse Events screen
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;

    public EventAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
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

    public void clearEvents() {
        this.events.clear();
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardEvent;
        ImageView ivPoster;
        TextView tvEventName, tvOrganizer, tvDate, tvCapacity, tvWaitingListCount;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvent = itemView.findViewById(R.id.cardEvent);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvWaitingListCount = itemView.findViewById(R.id.tvWaitingListCount);
        }

        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName());

            // Organizer
            tvOrganizer.setText(event.getOrganizerName() != null ?
                    event.getOrganizerName() : "Event Organizer");

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Capacity
            if (event.getCapacity() != null) {
                tvCapacity.setText(String.format(Locale.getDefault(),
                        "%d spots", event.getCapacity()));
            } else {
                tvCapacity.setText("Unlimited");
            }

            // Waiting list count
            int waitingCount = event.getWaitingList() != null ?
                    event.getWaitingList().size() : 0;
            tvWaitingListCount.setText(String.format(Locale.getDefault(),
                    "%d waiting", waitingCount));

            // Load poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            }

            // Click listener - navigate to event details
            cardEvent.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventDetailsActivity.class);
                intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId());
                context.startActivity(intent);
            });
        }
    }
}
package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.FavoritesManager;
import com.example.event_app.utils.Navigator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HorizontalEventAdapter - Displays events in horizontal scrolling lists
 * Used in HomeFragment for "Happening Soon" and "Popular This Week" sections
 *
 * UPDATED: Added favorite heart button functionality
 */
public class HorizontalEventAdapter extends RecyclerView.Adapter<HorizontalEventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private FavoritesManager favoritesManager;

    public HorizontalEventAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
        this.favoritesManager = new FavoritesManager();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_horizontal, parent, false);
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

    /**
     * Update the list of events
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Clear all events
     */
    public void clearEvents() {
        this.events.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for horizontal event cards
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPoster;
        TextView tvEventName, tvDate, tvWaitingCount;
        ImageButton btnFavorite;  // NEW

        private boolean isFavorited = false;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvWaitingCount = itemView.findViewById(R.id.tvWaitingCount);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);  // NEW

            // Click listener - navigate to EventDetailsActivity
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Event event = events.get(position);
                    Intent intent = new Intent(context, EventDetailsActivity.class);
                    intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId() != null ? event.getId() : event.getEventId());
                    context.startActivity(intent);
                }
            });
        }

        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName() != null ? event.getName() : "Untitled Event");

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else if (event.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Waiting list count
            int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            tvWaitingCount.setText(waitingCount + " waiting");

            // Load poster image
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .placeholder(R.color.gray_light)
                        .into(ivPoster);
            } else {
                // Set a default placeholder if no poster
                ivPoster.setImageResource(R.color.gray_light);
            }

            // NEW: Setup favorite button
            setupFavoriteButton(event);
        }

        /**
         * NEW: Setup favorite button with current state
         */
        private void setupFavoriteButton(Event event) {
            String eventId = event.getId() != null ? event.getId() : event.getEventId();

            // Check if event is favorited
            favoritesManager.isFavorite(eventId, isFav -> {
                isFavorited = isFav;
                updateFavoriteIcon();
            });

            // Click listener for favorite button
            btnFavorite.setOnClickListener(v -> {
                // Stop click from propagating to card
                v.setClickable(true);

                // Toggle favorite
                if (isFavorited) {
                    // Remove from favorites
                    favoritesManager.removeFavorite(eventId, new FavoritesManager.FavoriteCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = false;
                            updateFavoriteIcon();
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Add to favorites
                    favoritesManager.addFavorite(eventId, new FavoritesManager.FavoriteCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = true;
                            updateFavoriteIcon();
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        /**
         * NEW: Update heart icon based on favorite state
         */
        private void updateFavoriteIcon() {
            if (isFavorited) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }
        }
    }
}
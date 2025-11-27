package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UserAdapter - Display users with search and event count
 * UPDATED: Added search functionality and events hosted count
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFiltered;  // NEW: For search
    private OnUserClickListener listener;

    public UserAdapter() {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersFiltered.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return usersFiltered.size();
    }

    /**
     * Update the list of users
     */
    public void setUsers(List<User> users) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        notifyDataSetChanged();
    }

    /**
     * NEW: Filter users by search query
     */
    public void filter(String query) {
        usersFiltered.clear();

        if (query.isEmpty()) {
            usersFiltered.addAll(users);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (User user : users) {
                // Search by name or email
                if (user.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        user.getEmail().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    usersFiltered.add(user);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Set the click listener
     */
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder for user items
     */
    class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUserName;
        private TextView tvUserEmail;
        private TextView tvUserRoles;
        private TextView tvEventsHosted;  // NEW
        private MaterialButton btnDeleteUser;
        private MaterialButton btnRemoveOrganizer;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRoles = itemView.findViewById(R.id.tvUserRoles);
            tvEventsHosted = itemView.findViewById(R.id.tvEventsHosted);  // NEW
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
            btnRemoveOrganizer = itemView.findViewById(R.id.btnRemoveOrganizer);
        }

        public void bind(User user, OnUserClickListener listener) {
            // Set user name
            tvUserName.setText(user.getName());

            // Set user email
            tvUserEmail.setText(user.getEmail());

            // Set user roles
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                String rolesText = String.join(", ", user.getRoles());
                tvUserRoles.setText(rolesText.toUpperCase());
            } else {
                tvUserRoles.setText("NO ROLES");
            }

            // NEW: Show events hosted count for organizers
            if (user.isOrganizer()) {
                tvEventsHosted.setVisibility(View.VISIBLE);
                tvEventsHosted.setText("Loading events...");

                // Click to see event details
                tvEventsHosted.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewEventsClick(user);
                    }
                });

                // Trigger loading events count
                if (listener != null) {
                    listener.onLoadEventsCount(user, count -> {
                        if (count == 0) {
                            tvEventsHosted.setText("No events hosted");
                        } else {
                            tvEventsHosted.setText(count + (count == 1 ? " event hosted" : " events hosted") + " - Tap to view");
                        }
                    });
                }
            } else {
                tvEventsHosted.setVisibility(View.GONE);
            }

            // Show/hide "Remove Organizer" button
            if (user.isOrganizer()) {
                btnRemoveOrganizer.setVisibility(View.VISIBLE);
                btnRemoveOrganizer.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveOrganizerClick(user);
                    }
                });
            } else {
                btnRemoveOrganizer.setVisibility(View.GONE);
            }

            // Delete button
            btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(user);
                }
            });

            // Item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }

    /**
     * Interface for handling click events
     */
    public interface OnUserClickListener {
        void onUserClick(User user);
        void onDeleteClick(User user);
        void onRemoveOrganizerClick(User user);
        void onViewEventsClick(User user);  // NEW: View organizer's events
        void onLoadEventsCount(User user, EventsCountCallback callback);  // NEW: Load count
    }

    /**
     * NEW: Callback for events count
     */
    public interface EventsCountCallback {
        void onCountLoaded(int count);
    }
}
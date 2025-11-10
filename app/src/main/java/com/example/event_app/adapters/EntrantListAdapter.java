package com.example.event_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * EntrantListAdapter - Shows list of users (entrants)
 * Used by organizers to view waiting list, selected, attending
 */
public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.EntrantViewHolder> {

    private Context context;
    private List<String> userIds;
    private List<User> users;
    private FirebaseFirestore db;
    private String eventId;
    private String listType; // waiting, selected, attending

    public EntrantListAdapter(Context context, String eventId) {
        this.context = context;
        this.eventId = eventId;
        this.userIds = new ArrayList<>();
        this.users = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        String userId = userIds.get(position);

        // Load user data
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            holder.bind(user);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    public void setUserIds(List<String> userIds, String listType) {
        this.userIds = userIds != null ? userIds : new ArrayList<>();
        this.listType = listType;
        notifyDataSetChanged();
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail, tvPhone;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }

        public void bind(User user) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                tvPhone.setVisibility(View.VISIBLE);
                tvPhone.setText(user.getPhoneNumber());
            } else {
                tvPhone.setVisibility(View.GONE);
            }
        }
    }
}
package com.example.event_app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.Notification;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * NotificationAdapter - Display notifications in RecyclerView
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notifications;
    private final NotificationClickListener listener;

    public interface NotificationClickListener {
        void onNotificationClick(Notification notification);
        void onDeleteClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications,
                               NotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvIcon, tvTitle, tvMessage, tvTime, tvEventName;
        private final ImageButton btnDelete;
        private final View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        public void bind(Notification notification) {
            // Set icon
            tvIcon.setText(notification.getIcon());

            // Set title
            tvTitle.setText(notification.getTitle());

            // Set message
            tvMessage.setText(notification.getMessage());

            // Set event name
            if (notification.getEventName() != null && !notification.getEventName().isEmpty()) {
                tvEventName.setText(notification.getEventName());
                tvEventName.setVisibility(View.VISIBLE);
            } else {
                tvEventName.setVisibility(View.GONE);
            }

            // Set time (relative time)
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ).toString();
            tvTime.setText(timeAgo);

            // Show/hide unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Style based on read status
            if (notification.isRead()) {
                card.setCardBackgroundColor(context.getColor(android.R.color.white));
                tvTitle.setAlpha(0.7f);
                tvMessage.setAlpha(0.7f);
            } else {
                card.setCardBackgroundColor(context.getColor(android.R.color.white));
                tvTitle.setAlpha(1.0f);
                tvMessage.setAlpha(1.0f);
            }

            // Highlight important notifications
            if (notification.isImportant() && !notification.isRead()) {
                card.setStrokeColor(context.getColor(android.R.color.holo_blue_light));
                card.setStrokeWidth(2);
            } else {
                card.setStrokeColor(context.getColor(android.R.color.transparent));
                card.setStrokeWidth(0);
            }

            // Click listeners
            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(notification);
                }
            });
        }
    }
}
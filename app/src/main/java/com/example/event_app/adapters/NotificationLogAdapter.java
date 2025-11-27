package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.event_app.R;
import com.example.event_app.models.NotificationLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogViewHolder> {

    private List<NotificationLog> logs;
    private List<NotificationLog> logsFiltered;
    private OnLogClickListener listener;

    public NotificationLogAdapter() {
        this.logs = new ArrayList<>();
        this.logsFiltered = new ArrayList<>();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NotificationLog log = logsFiltered.get(position);
        holder.bind(log, listener);
    }

    @Override
    public int getItemCount() {
        return logsFiltered.size();
    }

    public void setLogs(List<NotificationLog> logs) {
        this.logs = logs;
        this.logsFiltered = new ArrayList<>(logs);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        logsFiltered.clear();
        if (query.isEmpty()) {
            logsFiltered.addAll(logs);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (NotificationLog log : logs) {
                if (log.getSenderName().toLowerCase().contains(lowerQuery) ||
                        log.getRecipientName().toLowerCase().contains(lowerQuery) ||
                        log.getTitle().toLowerCase().contains(lowerQuery)) {
                    logsFiltered.add(log);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setOnLogClickListener(OnLogClickListener listener) {
        this.listener = listener;
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvRecipientName, tvTitle, tvType, tvTimestamp;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvRecipientName = itemView.findViewById(R.id.tvRecipientName);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvType = itemView.findViewById(R.id.tvType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        public void bind(NotificationLog log, OnLogClickListener listener) {
            tvSenderName.setText("From: " + log.getSenderName());
            tvRecipientName.setText("To: " + log.getRecipientName());
            tvTitle.setText(log.getTitle());
            tvType.setText(log.getNotificationType().replace("_", " ").toUpperCase());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            if (log.getTimestamp() != null) {
                tvTimestamp.setText(sdf.format(log.getTimestamp()));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLogClick(log);
                }
            });
        }
    }

    public interface OnLogClickListener {
        void onLogClick(NotificationLog log);
    }
}

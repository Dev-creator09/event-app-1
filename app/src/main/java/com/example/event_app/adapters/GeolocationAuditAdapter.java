package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.event_app.R;
import com.example.event_app.models.GeolocationAudit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeolocationAuditAdapter extends RecyclerView.Adapter<GeolocationAuditAdapter.AuditViewHolder> {

    private List<GeolocationAudit> audits;
    private List<GeolocationAudit> auditsFiltered;
    private OnAuditClickListener listener;

    public GeolocationAuditAdapter() {
        this.audits = new ArrayList<>();
        this.auditsFiltered = new ArrayList<>();
    }

    @NonNull
    @Override
    public AuditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_geolocation_audit, parent, false);
        return new AuditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditViewHolder holder, int position) {
        GeolocationAudit audit = auditsFiltered.get(position);
        holder.bind(audit, listener);
    }

    @Override
    public int getItemCount() {
        return auditsFiltered.size();
    }

    public void setAudits(List<GeolocationAudit> audits) {
        this.audits = audits;
        this.auditsFiltered = new ArrayList<>(audits);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        auditsFiltered.clear();
        if (query.isEmpty()) {
            auditsFiltered.addAll(audits);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (GeolocationAudit audit : audits) {
                if (audit.getUserName().toLowerCase().contains(lowerQuery) ||
                        audit.getEventName().toLowerCase().contains(lowerQuery)) {
                    auditsFiltered.add(audit);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setOnAuditClickListener(OnAuditClickListener listener) {
        this.listener = listener;
    }

    class AuditViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvEventName, tvLocation, tvTimestamp, tvAction;

        public AuditViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvAction = itemView.findViewById(R.id.tvAction);
        }

        public void bind(GeolocationAudit audit, OnAuditClickListener listener) {
            tvUserName.setText(audit.getUserName());
            tvEventName.setText(audit.getEventName());
            tvLocation.setText(audit.getLocationString());
            tvAction.setText(audit.getAction().replace("_", " ").toUpperCase());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            if (audit.getTimestamp() != null) {
                tvTimestamp.setText(sdf.format(audit.getTimestamp()));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAuditClick(audit);
                }
            });
        }
    }

    public interface OnAuditClickListener {
        void onAuditClick(GeolocationAudit audit);
    }
}
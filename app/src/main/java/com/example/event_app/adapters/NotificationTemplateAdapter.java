package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.event_app.R;
import com.example.event_app.models.NotificationTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationTemplateAdapter extends RecyclerView.Adapter<NotificationTemplateAdapter.TemplateViewHolder> {

    private List<NotificationTemplate> templates;
    private List<NotificationTemplate> templatesFiltered;
    private OnTemplateClickListener listener;

    public NotificationTemplateAdapter() {
        this.templates = new ArrayList<>();
        this.templatesFiltered = new ArrayList<>();
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_template, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        NotificationTemplate template = templatesFiltered.get(position);
        holder.bind(template, listener);
    }

    @Override
    public int getItemCount() {
        return templatesFiltered.size();
    }

    public void setTemplates(List<NotificationTemplate> templates) {
        this.templates = templates;
        this.templatesFiltered = new ArrayList<>(templates);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        templatesFiltered.clear();
        if (query.isEmpty()) {
            templatesFiltered.addAll(templates);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (NotificationTemplate template : templates) {
                if (template.getName().toLowerCase().contains(lowerQuery) ||
                        template.getType().toLowerCase().contains(lowerQuery) ||
                        template.getTitle().toLowerCase().contains(lowerQuery)) {
                    templatesFiltered.add(template);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setOnTemplateClickListener(OnTemplateClickListener listener) {
        this.listener = listener;
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvTitle, tvMessage;
        Switch switchActive;
        ImageButton btnDelete;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTemplateName);
            tvType = itemView.findViewById(R.id.tvTemplateType);
            tvTitle = itemView.findViewById(R.id.tvTemplateTitle);
            tvMessage = itemView.findViewById(R.id.tvTemplateMessage);
            switchActive = itemView.findViewById(R.id.switchActive);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(NotificationTemplate template, OnTemplateClickListener listener) {
            tvName.setText(template.getName());
            tvType.setText(template.getType());
            tvTitle.setText(template.getTitle());
            tvMessage.setText(template.getMessage());
            switchActive.setChecked(template.isActive());

            // Click to edit
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });

            // Toggle active/inactive
            switchActive.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleActive(template);
                }
            });

            // Delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteTemplate(template);
                }
            });
        }
    }

    public interface OnTemplateClickListener {
        void onTemplateClick(NotificationTemplate template);
        void onToggleActive(NotificationTemplate template);
        void onDeleteTemplate(NotificationTemplate template);
    }
}

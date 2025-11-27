package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.ImageData;

import java.util.ArrayList;
import java.util.List;

/**
 * ImageAdapter - Display images in a grid with delete button
 * UPDATED: Shows actual images with delete button overlay (no text labels)
 * Used in AdminBrowseImagesActivity
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageData> images;
    private OnImageClickListener listener;

    public ImageAdapter() {
        this.images = new ArrayList<>();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = images.get(position);
        holder.bind(imageData, listener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Update the list of images
     */
    public void setImages(List<ImageData> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    /**
     * Set the click listener
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder for image items - displays actual image with delete button overlay
     */
    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(ImageData imageData, OnImageClickListener listener) {
            // Load actual image with Glide
            Glide.with(itemView.getContext())
                    .load(imageData.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.gray_light)
                    .error(android.R.drawable.ic_menu_gallery)  // Fallback if load fails
                    .into(ivImage);

            // Delete button click
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(imageData);
                }
            });

            // Optional: Click on image to view details
            ivImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(imageData);
                }
            });
        }
    }

    /**
     * Interface for handling click events
     */
    public interface OnImageClickListener {
        void onImageClick(ImageData imageData);
        void onDeleteClick(ImageData imageData);
    }
}
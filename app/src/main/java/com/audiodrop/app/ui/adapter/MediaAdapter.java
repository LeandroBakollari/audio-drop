package com.audiodrop.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.MediaItem;
import com.audiodrop.app.ui.UiFormat;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {
    public interface OpenListener {
        void onOpenMedia(MediaItem item);
    }

    public interface MoveListener {
        void onMoveMedia(MediaItem item);
    }

    private final OpenListener openListener;
    private final MoveListener moveListener;
    private final List<MediaItem> items = new ArrayList<>();

    public MediaAdapter(OpenListener openListener) {
        this(openListener, null);
    }

    public MediaAdapter(OpenListener openListener, MoveListener moveListener) {
        this.openListener = openListener;
        this.moveListener = moveListener;
    }

    public void submitList(List<MediaItem> nextItems) {
        items.clear();
        items.addAll(nextItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = items.get(position);
        holder.artworkText.setText(item.getArtworkSeed());
        holder.titleText.setText(item.getTitle());
        holder.typeText.setText(item.getType().getLabel());
        holder.durationText.setText(UiFormat.duration(item.getDurationSeconds()));
        holder.cardRoot.setOnClickListener(v -> openListener.onOpenMedia(item));
        holder.moveButton.setVisibility(moveListener == null ? View.GONE : View.VISIBLE);
        holder.moveButton.setOnClickListener(v -> {
            if (moveListener != null) {
                moveListener.onMoveMedia(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView artworkText;
        final TextView titleText;
        final TextView typeText;
        final TextView durationText;
        final ImageButton moveButton;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            artworkText = itemView.findViewById(R.id.artworkText);
            titleText = itemView.findViewById(R.id.titleText);
            typeText = itemView.findViewById(R.id.typeText);
            durationText = itemView.findViewById(R.id.durationText);
            moveButton = itemView.findViewById(R.id.moveButton);
        }
    }
}

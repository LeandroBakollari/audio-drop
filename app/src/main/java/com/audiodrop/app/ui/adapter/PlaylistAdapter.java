package com.audiodrop.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.Playlist;
import com.audiodrop.app.ui.UiFormat;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    public interface Listener {
        void onOpenPlaylist(Playlist playlist);
    }

    public interface CountProvider {
        int itemCount(String playlistId);
    }

    private final Listener listener;
    private final CountProvider countProvider;
    private final List<Playlist> playlists = new ArrayList<>();

    public PlaylistAdapter(Listener listener, CountProvider countProvider) {
        this.listener = listener;
        this.countProvider = countProvider;
    }

    public void submitList(List<Playlist> nextPlaylists) {
        playlists.clear();
        playlists.addAll(nextPlaylists);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.nameText.setText(playlist.getName());
        holder.countText.setText(UiFormat.countLabel(countProvider.itemCount(playlist.getId()), "item", "items"));
        holder.cardRoot.setOnClickListener(v -> listener.onOpenPlaylist(playlist));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView nameText;
        final TextView countText;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            nameText = itemView.findViewById(R.id.nameText);
            countText = itemView.findViewById(R.id.countText);
        }
    }
}

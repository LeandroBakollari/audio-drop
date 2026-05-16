package com.audiodrop.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.Playlist;
import com.audiodrop.app.ui.adapter.PlaylistAdapter;

public class PlaylistsActivity extends BaseActivity {
    private PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);
        setupBottomNavigation(PlaylistsActivity.class);

        RecyclerView playlistRecycler = findViewById(R.id.playlistRecycler);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new PlaylistAdapter(this::openPlaylist, viewModel::playlistItemCount);
        playlistRecycler.setAdapter(playlistAdapter);

        findViewById(R.id.addPlaylistButton).setOnClickListener(v ->
                showFolderNameDialog("New playlist", "", name -> {
                    viewModel.createPlaylist(name);
                    refresh();
                })
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void openPlaylist(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
        startActivity(intent);
    }

    private void refresh() {
        playlistAdapter.submitList(viewModel.getPlaylists());
    }
}

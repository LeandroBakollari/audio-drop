package com.audiodrop.app.ui;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.Playlist;
import com.audiodrop.app.ui.adapter.MediaAdapter;
import com.google.android.material.appbar.MaterialToolbar;

public class PlaylistDetailActivity extends BaseActivity {
    public static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private String playlistId;
    private MediaAdapter mediaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);
        setupBottomNavigation(PlaylistsActivity.class);

        playlistId = getIntent().getStringExtra(EXTRA_PLAYLIST_ID);
        Playlist playlist = viewModel.getPlaylistById(playlistId);
        if (playlist == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(playlist.getName());
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView mediaRecycler = findViewById(R.id.mediaRecycler);
        mediaRecycler.setLayoutManager(new LinearLayoutManager(this));
        mediaAdapter = new MediaAdapter(item -> openPlayer(item.getId(), PlayerActivity.SOURCE_PLAYLIST, playlistId));
        mediaRecycler.setAdapter(mediaAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaAdapter.submitList(viewModel.getMediaInPlaylist(playlistId));
    }
}

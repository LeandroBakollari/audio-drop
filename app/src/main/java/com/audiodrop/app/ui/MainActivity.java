package com.audiodrop.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.ui.adapter.MediaAdapter;

public class MainActivity extends BaseActivity {
    private MediaAdapter recentAdapter;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupBottomNavigation(MainActivity.class);

        findViewById(R.id.openLibraryButton).setOnClickListener(v ->
                startActivity(new Intent(this, LibraryActivity.class))
        );

        emptyText = findViewById(R.id.emptyText);
        RecyclerView recentRecycler = findViewById(R.id.recentRecycler);
        recentRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new MediaAdapter(item -> openPlayer(item.getId()));
        recentRecycler.setAdapter(recentAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        recentAdapter.submitList(viewModel.getRecentMedia());
        emptyText.setVisibility(viewModel.getRecentMedia().isEmpty() ? View.VISIBLE : View.GONE);
    }
}

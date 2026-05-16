package com.audiodrop.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.ui.adapter.FolderAdapter;
import com.audiodrop.app.ui.adapter.MediaAdapter;
import com.google.android.material.textfield.TextInputEditText;

public class LibraryActivity extends BaseActivity implements FolderAdapter.Listener {
    private FolderAdapter folderAdapter;
    private MediaAdapter mediaAdapter;
    private TextView audioTitle;
    private TextInputEditText searchEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        setupBottomNavigation(LibraryActivity.class);

        RecyclerView folderRecycler = findViewById(R.id.folderRecycler);
        folderRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        folderAdapter = new FolderAdapter(this, new FolderAdapter.CountProvider() {
            @Override
            public int itemCount(String folderId) {
                return viewModel.folderItemCount(folderId);
            }

            @Override
            public int childFolderCount(String folderId) {
                return viewModel.childFolderCount(folderId);
            }
        });
        folderRecycler.setAdapter(folderAdapter);

        RecyclerView mediaRecycler = findViewById(R.id.mediaRecycler);
        mediaRecycler.setLayoutManager(new LinearLayoutManager(this));
        mediaAdapter = new MediaAdapter(
                item -> openPlayer(item.getId()),
                item -> showMoveMediaDialog(item, this::refresh)
        );
        mediaRecycler.setAdapter(mediaAdapter);
        audioTitle = findViewById(R.id.audioTitle);

        searchEdit = findViewById(R.id.searchEdit);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.addFolderButton).setOnClickListener(v ->
                showFolderNameDialog("New folder", "", name -> {
                    viewModel.createFolder(null, name);
                    refresh();
                })
        );
        findViewById(R.id.importAudioButton).setOnClickListener(v -> launchAudioImport(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onAudioImportFinished() {
        refresh();
    }

    @Override
    public void onOpenFolder(LibraryFolder folder) {
        Intent intent = new Intent(this, FolderActivity.class);
        intent.putExtra(FolderActivity.EXTRA_FOLDER_ID, folder.getId());
        startActivity(intent);
    }

    @Override
    public void onRenameFolder(LibraryFolder folder) {
        showFolderNameDialog("Rename folder", folder.getName(), name -> {
            viewModel.renameFolder(folder.getId(), name);
            refresh();
        });
    }

    @Override
    public void onMoveFolder(LibraryFolder folder) {
        showMoveFolderDialog(folder, this::refresh);
    }

    @Override
    public void onDeleteFolder(LibraryFolder folder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + folder.getName() + "?")
                .setMessage("Files and subfolders will move up one level.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteFolder(folder.getId());
                    refresh();
                })
                .show();
    }

    private void refresh() {
        String query = searchEdit == null || searchEdit.getText() == null
                ? ""
                : searchEdit.getText().toString();
        if (query.trim().isEmpty()) {
            folderAdapter.submitList(viewModel.getRootFolders());
            mediaAdapter.submitList(viewModel.getAllMedia());
            audioTitle.setText("All Audio Files");
        } else {
            folderAdapter.submitList(viewModel.searchFolders(query));
            mediaAdapter.submitList(viewModel.searchMedia(query));
            audioTitle.setText("Matching Audio Files");
        }
    }
}

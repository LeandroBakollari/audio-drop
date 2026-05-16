package com.audiodrop.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.ui.adapter.FolderAdapter;
import com.audiodrop.app.ui.adapter.MediaAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class FolderActivity extends BaseActivity implements FolderAdapter.Listener {
    public static final String EXTRA_FOLDER_ID = "folder_id";

    private String folderId;
    private FolderAdapter folderAdapter;
    private MediaAdapter mediaAdapter;
    private TextInputEditText searchEdit;
    private boolean canCreateChildren;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);
        setupBottomNavigation(FolderActivity.class);

        folderId = getIntent().getStringExtra(EXTRA_FOLDER_ID);
        LibraryFolder folder = viewModel.getFolderById(folderId);
        if (folder == null) {
            finish();
            return;
        }
        canCreateChildren = viewModel.canCreateChildFolder(folderId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(folder.getName());
        toolbar.setNavigationOnClickListener(v -> openParentOrLibrary(folder));

        RecyclerView folderRecycler = findViewById(R.id.folderRecycler);
        folderRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        folderRecycler.setVisibility(canCreateChildren ? View.VISIBLE : View.GONE);
        findViewById(R.id.foldersTitle).setVisibility(canCreateChildren ? View.VISIBLE : View.GONE);
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
        mediaAdapter = new MediaAdapter(this::openMedia, item -> showMoveMediaDialog(item, this::refresh));
        mediaRecycler.setAdapter(mediaAdapter);

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

        View addFolderButton = findViewById(R.id.addFolderButton);
        addFolderButton.setVisibility(canCreateChildren ? View.VISIBLE : View.GONE);
        addFolderButton.setOnClickListener(v ->
                showFolderNameDialog("New folder", "", name -> {
                    viewModel.createFolder(folderId, name);
                    refresh();
                })
        );
        findViewById(R.id.importAudioButton).setOnClickListener(v -> launchAudioImport(folderId));
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
        intent.putExtra(EXTRA_FOLDER_ID, folder.getId());
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
        if (folderAdapter == null || mediaAdapter == null || searchEdit == null) {
            return;
        }

        String query = searchEdit.getText() == null ? "" : searchEdit.getText().toString().trim();
        if (query.isEmpty()) {
            folderAdapter.submitList(canCreateChildren ? viewModel.getChildFolders(folderId) : new java.util.ArrayList<>());
            mediaAdapter.submitList(viewModel.getMediaInFolder(folderId));
        } else {
            folderAdapter.submitList(canCreateChildren ? viewModel.searchFolders(query) : new java.util.ArrayList<>());
            mediaAdapter.submitList(viewModel.searchMedia(query));
        }
    }

    private void openParentOrLibrary(LibraryFolder folder) {
        String parentId = folder.getParentFolderId();
        if (parentId == null) {
            startActivity(new Intent(this, LibraryActivity.class));
        } else {
            Intent intent = new Intent(this, FolderActivity.class);
            intent.putExtra(EXTRA_FOLDER_ID, parentId);
            startActivity(intent);
        }
        finish();
    }

    private void openMedia(com.audiodrop.app.model.MediaItem item) {
        String query = searchEdit == null || searchEdit.getText() == null
                ? ""
                : searchEdit.getText().toString().trim();
        if (query.isEmpty()) {
            openPlayer(item.getId(), PlayerActivity.SOURCE_FOLDER, folderId);
        } else {
            openPlayer(item.getId());
        }
    }
}

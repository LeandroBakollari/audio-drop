package com.audiodrop.app.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.audiodrop.app.R;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.model.MediaItem;
import com.audiodrop.app.playback.PlaybackController;
import com.audiodrop.app.viewmodel.AudioDropViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Shared Activity behavior: theme loading, explicit navigation, and reusable dialogs.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT_AUDIO = 8103;

    protected AudioDropViewModel viewModel;
    protected PlaybackController playbackController;

    private String pendingImportFolderId;
    private View miniPlayerBar;
    private TextView miniPlayerTitle;
    private MaterialButton miniPreviousButton;
    private MaterialButton miniPlayPauseButton;
    private MaterialButton miniNextButton;
    private final PlaybackController.PlaybackListener miniPlayerListener = this::refreshMiniPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("audio_drop_data", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(
                prefs.getBoolean("dark_mode", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AudioDropViewModel.class);
        playbackController = PlaybackController.getInstance(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        attachMiniPlayerIfPossible();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playbackController != null) {
            playbackController.addListener(miniPlayerListener);
        }
    }

    @Override
    protected void onPause() {
        if (playbackController != null) {
            playbackController.removeListener(miniPlayerListener);
        }
        super.onPause();
    }

    protected void setupBottomNavigation(Class<?> currentScreen) {
        bindNavButton(R.id.navHome, MainActivity.class, currentScreen);
        bindNavButton(R.id.navLibrary, LibraryActivity.class, currentScreen);
        bindNavButton(R.id.navPlaylists, PlaylistsActivity.class, currentScreen);
        bindNavButton(R.id.navSettings, SettingsActivity.class, currentScreen);
    }

    protected void openPlayer(String mediaId) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_ID, mediaId);
        startActivity(intent);
    }

    protected void openPlayer(String mediaId, String sourceType, String sourceId) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_ID, mediaId);
        intent.putExtra(PlayerActivity.EXTRA_SOURCE_TYPE, sourceType);
        intent.putExtra(PlayerActivity.EXTRA_SOURCE_ID, sourceId);
        startActivity(intent);
    }

    protected void showFolderNameDialog(String title, String currentValue, FolderNameCallback callback) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setText(currentValue == null ? "" : currentValue);
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(editText)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> callback.onName(editText.getText().toString()))
                .show();
    }

    protected void showMoveMediaDialog(MediaItem item, Runnable afterMove) {
        List<LibraryFolder> folders = viewModel.getFoldersForMediaType(item.getType());
        if (folders.isEmpty()) {
            return;
        }

        String[] labels = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            labels[i] = viewModel.folderPath(folders.get(i).getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("Move To Folder")
                .setItems(labels, (dialog, which) -> {
                    viewModel.moveMediaToFolder(item.getId(), folders.get(which).getId());
                    if (afterMove != null) {
                        afterMove.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void showMoveFolderDialog(LibraryFolder folder, Runnable afterMove) {
        List<LibraryFolder> folders = viewModel.getFolderMoveTargets(folder.getId());
        if (folders.isEmpty()) {
            return;
        }

        String[] labels = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            labels[i] = viewModel.folderPath(folders.get(i).getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("Move Folder")
                .setItems(labels, (dialog, which) -> {
                    viewModel.moveFolder(folder.getId(), folders.get(which).getId());
                    if (afterMove != null) {
                        afterMove.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void launchAudioImport(String folderId) {
        pendingImportFolderId = folderId;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_AUDIO);
    }

    protected void onAudioImportFinished() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_AUDIO || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        int importedCount = 0;
        int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                if (importAudioUri(clipData.getItemAt(i).getUri(), flags)) {
                    importedCount++;
                }
            }
        } else if (data.getData() != null && importAudioUri(data.getData(), flags)) {
            importedCount++;
        }

        if (importedCount > 0) {
            Toast.makeText(this, importedCount == 1 ? "Imported audio" : "Imported " + importedCount + " audio files", Toast.LENGTH_SHORT).show();
            onAudioImportFinished();
        }
    }

    protected interface FolderNameCallback {
        void onName(String name);
    }

    private void bindNavButton(int id, Class<?> target, Class<?> currentScreen) {
        MaterialButton button = findViewById(id);
        if (button == null) {
            return;
        }
        button.setEnabled(target != currentScreen);
        button.setOnClickListener(v -> {
            if (target != currentScreen) {
                Intent intent = new Intent(this, target);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private boolean importAudioUri(Uri uri, int flags) {
        if (uri == null) {
            return false;
        }
        if (flags != 0) {
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (SecurityException ignored) {
            }
        }
        return viewModel.importDeviceAudio(uri, pendingImportFolderId) != null;
    }

    private void attachMiniPlayerIfPossible() {
        View bottomBar = findViewById(R.id.bottomBar);
        if (bottomBar == null) {
            return;
        }

        ViewGroup contentRoot = findViewById(android.R.id.content);
        if (contentRoot == null || contentRoot.getChildCount() == 0) {
            return;
        }

        View rootView = contentRoot.getChildAt(0);
        if (!(rootView instanceof ConstraintLayout)) {
            return;
        }

        ConstraintLayout rootLayout = (ConstraintLayout) rootView;
        miniPlayerBar = rootLayout.findViewById(R.id.miniPlayerBar);
        if (miniPlayerBar == null) {
            miniPlayerBar = LayoutInflater.from(this).inflate(R.layout.view_mini_player, rootLayout, false);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    0,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToTop = R.id.bottomBar;
            rootLayout.addView(miniPlayerBar, params);
        }

        miniPlayerTitle = miniPlayerBar.findViewById(R.id.miniPlayerTitle);
        miniPreviousButton = miniPlayerBar.findViewById(R.id.miniPreviousButton);
        miniPlayPauseButton = miniPlayerBar.findViewById(R.id.miniPlayPauseButton);
        miniNextButton = miniPlayerBar.findViewById(R.id.miniNextButton);

        miniPlayerBar.setOnClickListener(v -> openCurrentPlayer());
        miniPreviousButton.setOnClickListener(v -> playbackController.previousTrack());
        miniPlayPauseButton.setOnClickListener(v -> playbackController.togglePlayPause());
        miniNextButton.setOnClickListener(v -> playbackController.nextTrack());
        refreshMiniPlayer();
    }

    private void refreshMiniPlayer() {
        if (miniPlayerBar == null || playbackController == null) {
            return;
        }

        MediaItem currentItem = playbackController.getCurrentItem();
        if (currentItem == null) {
            miniPlayerBar.setVisibility(View.GONE);
            return;
        }

        miniPlayerBar.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(currentItem.getTitle());
        miniPreviousButton.setEnabled(playbackController.hasPreviousTrack());
        miniNextButton.setEnabled(playbackController.hasNextTrack());
        miniPlayPauseButton.setEnabled(playbackController.isPlayable());
        miniPlayPauseButton.setIconResource(
                playbackController.isPlaying()
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );
        miniPlayPauseButton.setContentDescription(playbackController.isPlaying() ? "Pause" : "Play");
    }

    private void openCurrentPlayer() {
        MediaItem currentItem = playbackController.getCurrentItem();
        if (currentItem == null) {
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_ID, currentItem.getId());
        intent.putExtra(PlayerActivity.EXTRA_START_PLAYBACK, false);
        if (playbackController.getSourceType() != null && playbackController.getSourceId() != null) {
            intent.putExtra(PlayerActivity.EXTRA_SOURCE_TYPE, playbackController.getSourceType());
            intent.putExtra(PlayerActivity.EXTRA_SOURCE_ID, playbackController.getSourceId());
        }
        startActivity(intent);
    }
}

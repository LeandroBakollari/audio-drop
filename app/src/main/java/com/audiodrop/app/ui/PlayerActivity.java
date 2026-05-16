package com.audiodrop.app.ui;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.audiodrop.app.R;
import com.audiodrop.app.model.AudioType;
import com.audiodrop.app.model.Chapter;
import com.audiodrop.app.model.MediaItem;
import com.audiodrop.app.model.Playlist;
import com.audiodrop.app.playback.PlaybackController;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends BaseActivity implements SensorEventListener, PlaybackController.PlaybackListener {
    public static final String EXTRA_MEDIA_ID = "media_id";
    public static final String EXTRA_SOURCE_TYPE = "source_type";
    public static final String EXTRA_SOURCE_ID = "source_id";
    public static final String EXTRA_START_PLAYBACK = "start_playback";
    public static final String SOURCE_PLAYLIST = PlaybackController.SOURCE_PLAYLIST;
    public static final String SOURCE_FOLDER = PlaybackController.SOURCE_FOLDER;

    private final String[] repeatLabels = {"Repeat Off", "Repeat All", "Repeat One"};

    private MediaItem mediaItem;
    private Slider progressSlider;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private TextView currentSpeedText;
    private TextView sleepTimerText;
    private TextView currentChapterText;
    private View chapterCard;
    private MaterialButton playPauseButton;
    private MaterialButton shuffleButton;
    private MaterialButton repeatButton;
    private MaterialButton previousTrackButton;
    private MaterialButton nextTrackButton;
    private int jumpSeconds = 10;
    private String sourceType;
    private String sourceId;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        String mediaId = getIntent().getStringExtra(EXTRA_MEDIA_ID);
        sourceType = getIntent().getStringExtra(EXTRA_SOURCE_TYPE);
        sourceId = getIntent().getStringExtra(EXTRA_SOURCE_ID);

        boolean shouldStartPlayback = getIntent().getBooleanExtra(EXTRA_START_PLAYBACK, true);
        MediaItem requestedItem = mediaId == null ? null : viewModel.getMediaById(mediaId);
        if (requestedItem == null) {
            requestedItem = playbackController.getCurrentItem();
        }
        if (requestedItem == null) {
            finish();
            return;
        }

        if (shouldStartPlayback || playbackController.getCurrentItem() == null) {
            playbackController.play(requestedItem, sourceType, sourceId);
        }
        if (!syncCurrentItem()) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindHeader();
        bindTimeline();
        bindTransportControls();
        bindChapterControls();
        bindPlaybackTools();
        bindSpeedControls();
        bindAccelerometer();
        bindPlaylistControls();
        findViewById(R.id.shareButton).setOnClickListener(v -> shareTrack());
        refreshFullPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playbackController.addListener(this);
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        playbackController.persistCurrentProgress();
        playbackController.removeListener(this);
        super.onPause();
    }

    @Override
    public void onPlaybackChanged() {
        if (progressSlider == null || !syncCurrentItem()) {
            return;
        }
        refreshFullPlayer();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0] / SensorManager.GRAVITY_EARTH;
        float y = event.values[1] / SensorManager.GRAVITY_EARTH;
        float z = event.values[2] / SensorManager.GRAVITY_EARTH;
        double force = Math.sqrt(x * x + y * y + z * z);
        long now = System.currentTimeMillis();

        if (force > 2.7f && now - lastShakeTime > 900L) {
            lastShakeTime = now;
            playbackController.togglePlayPause();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean syncCurrentItem() {
        MediaItem currentItem = playbackController.getCurrentItem();
        if (currentItem == null) {
            return false;
        }

        boolean changedTrack = mediaItem == null || !mediaItem.getId().equals(currentItem.getId());
        mediaItem = currentItem;
        sourceType = playbackController.getSourceType();
        sourceId = playbackController.getSourceId();
        jumpSeconds = mediaItem.getType() == AudioType.AUDIOBOOK ? 30 : 10;
        if (changedTrack && progressSlider != null) {
            bindHeader();
            refreshTimelineDuration();
            updateChapterVisibility();
        }
        return true;
    }

    private void bindHeader() {
        TextView artworkText = findViewById(R.id.artworkText);
        TextView titleText = findViewById(R.id.titleText);
        TextView typeText = findViewById(R.id.typeText);

        artworkText.setText(mediaItem.getArtworkSeed());
        titleText.setText(mediaItem.getTitle());
        typeText.setText(mediaItem.getType().getLabel() + " - " + UiFormat.duration(mediaItem.getDurationSeconds()));
    }

    private void bindTimeline() {
        progressSlider = findViewById(R.id.progressSlider);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);

        progressSlider.setValueFrom(0);
        progressSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                playbackController.seekTo(Math.round(value));
            }
        });
        refreshTimelineDuration();
    }

    private void bindTransportControls() {
        playPauseButton = findViewById(R.id.playPauseButton);
        MaterialButton skipBackButton = findViewById(R.id.skipBackButton);
        MaterialButton skipForwardButton = findViewById(R.id.skipForwardButton);
        previousTrackButton = findViewById(R.id.previousTrackButton);
        nextTrackButton = findViewById(R.id.nextTrackButton);

        skipBackButton.setText("-" + jumpSeconds + "s");
        skipForwardButton.setText("+" + jumpSeconds + "s");
        previousTrackButton.setOnClickListener(v -> playbackController.previousTrack());
        nextTrackButton.setOnClickListener(v -> playbackController.nextTrack());
        skipBackButton.setOnClickListener(v -> playbackController.skipBy(-jumpSeconds));
        skipForwardButton.setOnClickListener(v -> playbackController.skipBy(jumpSeconds));
        playPauseButton.setOnClickListener(v -> playbackController.togglePlayPause());
    }

    private void bindChapterControls() {
        chapterCard = findViewById(R.id.chapterCard);
        currentChapterText = findViewById(R.id.currentChapterText);
        MaterialButton previousChapterButton = findViewById(R.id.previousChapterButton);
        MaterialButton nextChapterButton = findViewById(R.id.nextChapterButton);
        MaterialButton showChaptersButton = findViewById(R.id.showChaptersButton);

        previousChapterButton.setOnClickListener(v -> openChapterOffset(-1));
        nextChapterButton.setOnClickListener(v -> openChapterOffset(1));
        showChaptersButton.setOnClickListener(v -> showChaptersDialog());
        updateChapterVisibility();
    }

    private void bindPlaybackTools() {
        shuffleButton = findViewById(R.id.shuffleButton);
        repeatButton = findViewById(R.id.repeatButton);
        sleepTimerText = findViewById(R.id.sleepTimerText);

        shuffleButton.setOnClickListener(v -> playbackController.shuffleNextTrack());
        repeatButton.setOnClickListener(v -> {
            int nextRepeatMode = (playbackController.getRepeatModeIndex() + 1) % repeatLabels.length;
            playbackController.setRepeatModeIndex(nextRepeatMode);
        });
    }

    private void bindSpeedControls() {
        currentSpeedText = findViewById(R.id.currentSpeedText);
        findViewById(R.id.speed05Button).setOnClickListener(v -> playbackController.setPlaybackSpeed(0.5f));
        findViewById(R.id.speed1Button).setOnClickListener(v -> playbackController.setPlaybackSpeed(1f));
        findViewById(R.id.speed125Button).setOnClickListener(v -> playbackController.setPlaybackSpeed(1.25f));
        findViewById(R.id.speed15Button).setOnClickListener(v -> playbackController.setPlaybackSpeed(1.5f));
        findViewById(R.id.speed2Button).setOnClickListener(v -> playbackController.setPlaybackSpeed(2f));
    }

    private void bindPlaylistControls() {
        View playlistsButton = findViewById(R.id.playlistsButton);
        if (mediaItem.getType() == AudioType.AUDIOBOOK) {
            playlistsButton.setVisibility(View.GONE);
            return;
        }
        playlistsButton.setOnClickListener(v -> showPlaylistDialog());
    }

    private void bindAccelerometer() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void refreshFullPlayer() {
        updateTransportButtons();
        updatePlaybackToolButtons();
        updateSpeedLabel();
        updateSleepTimerLabel();
        updateProgressLabels();
        updateChapterLabel();
    }

    private void refreshTimelineDuration() {
        progressSlider.setValueTo(Math.max(1, mediaItem.getDurationSeconds()));
        totalTimeText.setText(UiFormat.duration(mediaItem.getDurationSeconds()));
        updateProgressLabels();
    }

    private void updateProgressLabels() {
        int progressSeconds = Math.min(playbackController.getCurrentProgressSeconds(), mediaItem.getDurationSeconds());
        if (progressSlider != null) {
            progressSlider.setValue(progressSeconds);
        }
        currentTimeText.setText(UiFormat.duration(progressSeconds));
    }

    private void updateChapterVisibility() {
        if (chapterCard == null) {
            return;
        }
        chapterCard.setVisibility(mediaItem.getChapters().isEmpty() ? View.GONE : View.VISIBLE);
        updateChapterLabel();
    }

    private void updateChapterLabel() {
        if (currentChapterText == null || mediaItem.getChapters().isEmpty()) {
            return;
        }

        int chapterIndex = mediaItem.currentChapterIndex();
        Chapter chapter = mediaItem.getChapters().get(chapterIndex);
        currentChapterText.setText(chapter.getTitle() + " - starts at " + UiFormat.duration(chapter.getStartSeconds()));
    }

    private void updateTransportButtons() {
        if (!playbackController.isPlayable()) {
            playPauseButton.setText("No File");
            playPauseButton.setIcon(null);
            playPauseButton.setEnabled(false);
        } else {
            playPauseButton.setText("");
            playPauseButton.setEnabled(true);
            playPauseButton.setIconResource(
                    playbackController.isPlaying()
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play
            );
            playPauseButton.setContentDescription(playbackController.isPlaying() ? "Pause" : "Play");
        }
        previousTrackButton.setEnabled(playbackController.hasPreviousTrack());
        nextTrackButton.setEnabled(playbackController.hasNextTrack());
    }

    private void updatePlaybackToolButtons() {
        shuffleButton.setVisibility(mediaItem.getType() == AudioType.AUDIOBOOK ? View.GONE : View.VISIBLE);
        shuffleButton.setText("Shuffle");
        shuffleButton.setEnabled(playbackController.canShuffle());
        repeatButton.setText(repeatLabels[playbackController.getRepeatModeIndex()]);
    }

    private void updateSpeedLabel() {
        currentSpeedText.setText("Current speed: " + playbackController.getPlaybackSpeed() + "x");
    }

    private void updateSleepTimerLabel() {
        int minutes = viewModel.getSleepTimerMinutes();
        if (minutes == 0) {
            sleepTimerText.setText("Sleep timer: Off");
        } else if (playbackController.isSleepTimerActive()) {
            sleepTimerText.setText("Sleep timer: running for " + minutes + " min");
        } else {
            sleepTimerText.setText("Sleep timer: " + minutes + " min selected");
        }
    }

    private void openChapterOffset(int offset) {
        List<Chapter> chapters = mediaItem.getChapters();
        if (chapters.isEmpty()) {
            return;
        }

        int nextIndex = Math.max(0, Math.min(mediaItem.currentChapterIndex() + offset, chapters.size() - 1));
        playbackController.seekTo(chapters.get(nextIndex).getStartSeconds());
    }

    private void showChaptersDialog() {
        List<Chapter> chapters = mediaItem.getChapters();
        String[] labels = new String[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            labels[i] = chapter.getTitle() + "  " + UiFormat.duration(chapter.getStartSeconds());
        }

        new AlertDialog.Builder(this)
                .setTitle("Chapters")
                .setItems(labels, (dialog, which) -> playbackController.seekTo(chapters.get(which).getStartSeconds()))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPlaylistDialog() {
        List<Playlist> playlists = viewModel.getPlaylists();
        String[] labels = new String[playlists.size() + 1];
        labels[0] = "Music";
        int checkedIndex = 0;
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            labels[i + 1] = playlist.getName();
            if (playlist.getId().equals(mediaItem.getFolderId())) {
                checkedIndex = i + 1;
            }
        }

        int[] selectedIndex = {checkedIndex};
        new AlertDialog.Builder(this)
                .setTitle("Move To Playlist")
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> selectedIndex[0] = which)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Move", (dialog, which) -> {
                    List<String> nextPlaylistIds = new ArrayList<>();
                    if (selectedIndex[0] > 0) {
                        nextPlaylistIds.add(playlists.get(selectedIndex[0] - 1).getId());
                    }
                    viewModel.updateMediaPlaylists(mediaItem.getId(), nextPlaylistIds);
                    MediaItem movedItem = viewModel.getMediaById(mediaItem.getId());
                    String nextSourceType = selectedIndex[0] > 0 ? SOURCE_PLAYLIST : SOURCE_FOLDER;
                    String nextSourceId = selectedIndex[0] > 0
                            ? playlists.get(selectedIndex[0] - 1).getId()
                            : "music";
                    playbackController.play(movedItem, nextSourceType, nextSourceId);
                })
                .show();
    }

    private void shareTrack() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Listening to " + mediaItem.getTitle() + " in AudioDrop.");
        startActivity(Intent.createChooser(sendIntent, "Share audio"));
    }
}

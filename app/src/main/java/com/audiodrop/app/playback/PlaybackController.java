package com.audiodrop.app.playback;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.audiodrop.app.data.AudioRepository;
import com.audiodrop.app.model.AudioType;
import com.audiodrop.app.model.MediaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlaybackController {
    public static final String SOURCE_PLAYLIST = "playlist";
    public static final String SOURCE_FOLDER = "folder";

    private static PlaybackController instance;

    private final Context appContext;
    private final AudioRepository repository;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private final List<PlaybackListener> listeners = new ArrayList<>();
    private final List<MediaItem> playbackQueue = new ArrayList<>();
    private final Random random = new Random();

    private MediaPlayer mediaPlayer;
    private MediaItem currentItem;
    private boolean isPlayable = false;
    private boolean isPlaying = false;
    private int repeatModeIndex = 0;
    private float playbackSpeed = 1f;
    private String sourceType;
    private String sourceId;
    private int queueIndex = -1;
    private boolean sleepTimerActive = false;
    private long sleepTimerEndsAtMs = 0L;

    private PlaybackController(Context context) {
        appContext = context.getApplicationContext();
        repository = AudioRepository.getInstance(appContext);
    }

    public static synchronized PlaybackController getInstance(Context context) {
        if (instance == null) {
            instance = new PlaybackController(context);
        }
        return instance;
    }

    public void addListener(PlaybackListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        listener.onPlaybackChanged();
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public void play(String mediaId, String nextSourceType, String nextSourceId) {
        MediaItem item = repository.getMediaById(mediaId);
        if (item != null) {
            play(item, nextSourceType, nextSourceId);
        }
    }

    public void play(MediaItem item, String nextSourceType, String nextSourceId) {
        if (item == null) {
            return;
        }

        MediaItem repositoryItem = repository.getMediaById(item.getId());
        if (repositoryItem == null) {
            return;
        }

        if (currentItem != null && currentItem.getId().equals(repositoryItem.getId()) && mediaPlayer != null) {
            sourceType = nextSourceType;
            sourceId = nextSourceId;
            rebuildPlaybackQueue();
            notifyPlaybackChanged();
            return;
        }

        persistCurrentProgress();
        releaseMediaPlayer();
        currentItem = repositoryItem;
        sourceType = nextSourceType;
        sourceId = nextSourceId;
        if (currentItem.getType() != AudioType.AUDIOBOOK) {
            currentItem.setProgressSeconds(0);
        }
        rebuildPlaybackQueue();
        prepareCurrentItem();
    }

    public void togglePlayPause() {
        if (!isPlayable || mediaPlayer == null) {
            return;
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            persistCurrentProgress();
            sleepTimerHandler.removeCallbacksAndMessages(null);
        } else {
            mediaPlayer.start();
            isPlaying = true;
            scheduleProgressUpdates();
            scheduleSleepTimer();
        }
        notifyPlaybackChanged();
    }

    public void pause() {
        if (mediaPlayer == null || !isPlayable || !mediaPlayer.isPlaying()) {
            return;
        }
        mediaPlayer.pause();
        isPlaying = false;
        persistCurrentProgress();
        sleepTimerHandler.removeCallbacksAndMessages(null);
        notifyPlaybackChanged();
    }

    public void seekTo(int seconds) {
        if (currentItem == null) {
            return;
        }

        int clamped = Math.max(0, Math.min(seconds, currentItem.getDurationSeconds()));
        currentItem.setProgressSeconds(clamped);
        if (currentItem.getType() == AudioType.AUDIOBOOK) {
            repository.updateProgress(currentItem.getId(), clamped);
        }
        if (mediaPlayer != null && isPlayable) {
            mediaPlayer.seekTo(clamped * 1000);
        }
        notifyPlaybackChanged();
    }

    public void skipBy(int seconds) {
        seekTo(getCurrentProgressSeconds() + seconds);
    }

    public void previousTrack() {
        MediaItem previousItem = getQueueNeighbor(-1);
        if (previousItem != null) {
            play(previousItem, sourceType, sourceId);
        }
    }

    public void nextTrack() {
        MediaItem nextItem = getQueueNeighbor(1);
        if (nextItem != null) {
            play(nextItem, sourceType, sourceId);
        }
    }

    public void shuffleNextTrack() {
        if (!canShuffle()) {
            return;
        }

        MediaItem nextItem;
        do {
            nextItem = playbackQueue.get(random.nextInt(playbackQueue.size()));
        } while (nextItem.getId().equals(currentItem.getId()));
        play(nextItem, sourceType, sourceId);
    }

    public void setRepeatModeIndex(int repeatModeIndex) {
        this.repeatModeIndex = Math.max(0, Math.min(repeatModeIndex, 2));
        notifyPlaybackChanged();
    }

    public int getRepeatModeIndex() {
        return repeatModeIndex;
    }

    public void setPlaybackSpeed(float speed) {
        playbackSpeed = speed;
        applyPlaybackSpeed();
        notifyPlaybackChanged();
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void refreshSleepTimer() {
        if (sleepTimerActive) {
            sleepTimerEndsAtMs = SystemClock.elapsedRealtime() + repository.getSleepTimerMinutes() * 60_000L;
        }
        scheduleSleepTimer();
        notifyPlaybackChanged();
    }

    public void startSleepTimer() {
        int minutes = repository.getSleepTimerMinutes();
        if (minutes <= 0) {
            cancelSleepTimer();
            return;
        }

        sleepTimerActive = true;
        sleepTimerEndsAtMs = SystemClock.elapsedRealtime() + minutes * 60_000L;
        scheduleSleepTimer();
        notifyPlaybackChanged();
    }

    public void cancelSleepTimer() {
        sleepTimerActive = false;
        sleepTimerEndsAtMs = 0L;
        sleepTimerHandler.removeCallbacksAndMessages(null);
        notifyPlaybackChanged();
    }

    public boolean isSleepTimerActive() {
        return sleepTimerActive;
    }

    public void persistCurrentProgress() {
        if (currentItem == null) {
            return;
        }

        if (mediaPlayer != null && isPlayable) {
            currentItem.setProgressSeconds(mediaPlayer.getCurrentPosition() / 1000);
        }
        if (currentItem.getType() == AudioType.AUDIOBOOK) {
            repository.updateProgress(currentItem.getId(), currentItem.getProgressSeconds());
        }
    }

    public MediaItem getCurrentItem() {
        return currentItem;
    }

    public int getCurrentProgressSeconds() {
        if (mediaPlayer != null && isPlayable) {
            return mediaPlayer.getCurrentPosition() / 1000;
        }
        return currentItem == null ? 0 : currentItem.getProgressSeconds();
    }

    public boolean hasCurrentItem() {
        return currentItem != null;
    }

    public boolean isPlayable() {
        return isPlayable;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean canShuffle() {
        return currentItem != null && currentItem.getType() != AudioType.AUDIOBOOK && hasQueue() && playbackQueue.size() > 1;
    }

    public boolean hasPreviousTrack() {
        return hasQueue() && playbackQueue.size() > 1;
    }

    public boolean hasNextTrack() {
        return hasQueue() && playbackQueue.size() > 1;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    private void prepareCurrentItem() {
        boolean hasAssetPath = currentItem != null && currentItem.getAssetPath() != null && !currentItem.getAssetPath().trim().isEmpty();
        boolean hasSourceUri = currentItem != null && currentItem.getSourceUri() != null && !currentItem.getSourceUri().trim().isEmpty();
        if (!hasAssetPath && !hasSourceUri) {
            isPlayable = false;
            isPlaying = false;
            notifyPlaybackChanged();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            if (hasSourceUri) {
                mediaPlayer.setDataSource(appContext, Uri.parse(currentItem.getSourceUri()));
            } else {
                try (AssetFileDescriptor descriptor = appContext.getAssets().openFd(currentItem.getAssetPath())) {
                    mediaPlayer.setDataSource(
                            descriptor.getFileDescriptor(),
                            descriptor.getStartOffset(),
                            descriptor.getLength()
                    );
                }
            }
            mediaPlayer.setOnCompletionListener(player -> handleTrackCompleted());
            mediaPlayer.prepare();

            int actualDurationSeconds = Math.max(1, mediaPlayer.getDuration() / 1000);
            currentItem.setDurationSeconds(actualDurationSeconds);
            mediaPlayer.seekTo(currentItem.getProgressSeconds() * 1000);
            isPlayable = true;
            applyPlaybackSpeed();
            mediaPlayer.start();
            isPlaying = true;
            scheduleProgressUpdates();
            scheduleSleepTimer();
            notifyPlaybackChanged();
        } catch (Exception exception) {
            releaseMediaPlayer();
            isPlayable = false;
            isPlaying = false;
            notifyPlaybackChanged();
        }
    }

    private void releaseMediaPlayer() {
        progressHandler.removeCallbacksAndMessages(null);
        sleepTimerHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlayable = false;
        isPlaying = false;
    }

    private void handleTrackCompleted() {
        persistCurrentProgress();
        if (repeatModeIndex == 2 && mediaPlayer != null) {
            seekTo(0);
            mediaPlayer.start();
            isPlaying = true;
            scheduleProgressUpdates();
            scheduleSleepTimer();
            notifyPlaybackChanged();
            return;
        }

        if (hasQueue() && playbackQueue.size() > 1 && queueIndex < playbackQueue.size() - 1) {
            play(playbackQueue.get(queueIndex + 1), sourceType, sourceId);
        } else if (hasQueue() && playbackQueue.size() > 1 && repeatModeIndex == 1) {
            play(playbackQueue.get(0), sourceType, sourceId);
        } else {
            stopAtEnd();
        }
    }

    private void stopAtEnd() {
        isPlaying = false;
        if (mediaPlayer != null && currentItem != null && currentItem.getType() != AudioType.AUDIOBOOK) {
            mediaPlayer.seekTo(0);
            currentItem.setProgressSeconds(0);
        } else if (currentItem != null) {
            currentItem.setProgressSeconds(currentItem.getDurationSeconds());
            repository.updateProgress(currentItem.getId(), currentItem.getProgressSeconds());
        }
        sleepTimerActive = false;
        sleepTimerEndsAtMs = 0L;
        sleepTimerHandler.removeCallbacksAndMessages(null);
        notifyPlaybackChanged();
    }

    private void scheduleProgressUpdates() {
        progressHandler.removeCallbacksAndMessages(null);
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer == null || !isPlayable || currentItem == null) {
                    return;
                }
                if (mediaPlayer.isPlaying()) {
                    currentItem.setProgressSeconds(mediaPlayer.getCurrentPosition() / 1000);
                    notifyPlaybackChanged();
                    progressHandler.postDelayed(this, 1000L);
                }
            }
        }, 1000L);
    }

    private void scheduleSleepTimer() {
        sleepTimerHandler.removeCallbacksAndMessages(null);
        int minutes = repository.getSleepTimerMinutes();
        if (minutes <= 0) {
            sleepTimerActive = false;
            sleepTimerEndsAtMs = 0L;
            return;
        }
        if (!sleepTimerActive || mediaPlayer == null || !isPlayable || !isPlaying) {
            return;
        }

        long remainingMillis = sleepTimerEndsAtMs - SystemClock.elapsedRealtime();
        if (remainingMillis <= 0L) {
            pauseForSleepTimer();
            return;
        }
        sleepTimerHandler.postDelayed(this::pauseForSleepTimer, remainingMillis);
    }

    private void pauseForSleepTimer() {
        sleepTimerActive = false;
        sleepTimerEndsAtMs = 0L;
        pause();
    }

    private void applyPlaybackSpeed() {
        if (mediaPlayer == null || !isPlayable) {
            return;
        }
        try {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        } catch (IllegalStateException ignored) {
        }
    }

    private void rebuildPlaybackQueue() {
        playbackQueue.clear();
        queueIndex = -1;
        if (currentItem == null) {
            return;
        }

        List<MediaItem> candidates = new ArrayList<>();
        if (SOURCE_PLAYLIST.equals(sourceType) && sourceId != null) {
            candidates.addAll(repository.getMediaInPlaylist(sourceId));
        } else if (SOURCE_FOLDER.equals(sourceType) && sourceId != null) {
            candidates.addAll(repository.getMediaInFolder(sourceId));
        } else {
            candidates.addAll(repository.getAllMedia());
        }

        for (MediaItem item : candidates) {
            if (item.getType() == currentItem.getType()) {
                playbackQueue.add(item);
            }
        }

        for (int i = 0; i < playbackQueue.size(); i++) {
            if (playbackQueue.get(i).getId().equals(currentItem.getId())) {
                queueIndex = i;
                return;
            }
        }

        playbackQueue.add(currentItem);
        queueIndex = playbackQueue.size() - 1;
    }

    private boolean hasQueue() {
        return queueIndex >= 0 && !playbackQueue.isEmpty();
    }

    private MediaItem getQueueNeighbor(int direction) {
        if (!hasQueue() || playbackQueue.size() <= 1) {
            return null;
        }

        int nextIndex = queueIndex + direction;
        if (nextIndex < 0) {
            nextIndex = playbackQueue.size() - 1;
        } else if (nextIndex >= playbackQueue.size()) {
            nextIndex = 0;
        }
        return playbackQueue.get(nextIndex);
    }

    private void notifyPlaybackChanged() {
        List<PlaybackListener> snapshot = new ArrayList<>(listeners);
        for (PlaybackListener listener : snapshot) {
            listener.onPlaybackChanged();
        }
    }

    public interface PlaybackListener {
        void onPlaybackChanged();
    }
}

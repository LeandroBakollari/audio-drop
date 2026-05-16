package com.audiodrop.app.model;

import java.util.ArrayList;
import java.util.List;

public class MediaItem {
    private final String id;
    private final String title;
    private final String artist;
    private final AudioType type;
    private String folderId;
    private int durationSeconds;
    private int progressSeconds;
    private final String artworkSeed;
    private final List<String> playlistIds;
    private final List<Chapter> chapters;
    private final String assetPath;
    private final String sourceUri;

    public MediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            int progressSeconds,
            String artworkSeed,
            List<String> playlistIds
    ) {
        this(id, title, artist, type, folderId, durationSeconds, progressSeconds, artworkSeed, playlistIds, null, null, null);
    }

    public MediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            int progressSeconds,
            String artworkSeed,
            List<String> playlistIds,
            String assetPath
    ) {
        this(id, title, artist, type, folderId, durationSeconds, progressSeconds, artworkSeed, playlistIds, null, assetPath, null);
    }

    public MediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            int progressSeconds,
            String artworkSeed,
            List<String> playlistIds,
            List<Chapter> chapters
    ) {
        this(id, title, artist, type, folderId, durationSeconds, progressSeconds, artworkSeed, playlistIds, chapters, null, null);
    }

    public MediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            int progressSeconds,
            String artworkSeed,
            List<String> playlistIds,
            List<Chapter> chapters,
            String assetPath
    ) {
        this(id, title, artist, type, folderId, durationSeconds, progressSeconds, artworkSeed, playlistIds, chapters, assetPath, null);
    }

    public MediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            int progressSeconds,
            String artworkSeed,
            List<String> playlistIds,
            List<Chapter> chapters,
            String assetPath,
            String sourceUri
    ) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.type = type;
        this.folderId = folderId;
        this.durationSeconds = durationSeconds;
        this.progressSeconds = progressSeconds;
        this.artworkSeed = artworkSeed;
        this.playlistIds = new ArrayList<>(playlistIds);
        this.chapters = chapters == null || chapters.isEmpty()
                ? defaultChapters(type, durationSeconds)
                : new ArrayList<>(chapters);
        this.assetPath = assetPath;
        this.sourceUri = sourceUri;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public AudioType getType() {
        return type;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        boolean shouldRegenerateChapters = type == AudioType.AUDIOBOOK && hasGeneratedChapters();
        this.durationSeconds = Math.max(0, durationSeconds);
        if (shouldRegenerateChapters) {
            chapters.clear();
            chapters.addAll(defaultChapters(type, this.durationSeconds));
        }
        setProgressSeconds(progressSeconds);
    }

    public int getProgressSeconds() {
        return progressSeconds;
    }

    public void setProgressSeconds(int progressSeconds) {
        this.progressSeconds = Math.max(0, Math.min(progressSeconds, durationSeconds));
    }

    public String getArtworkSeed() {
        return artworkSeed;
    }

    public List<String> getPlaylistIds() {
        return new ArrayList<>(playlistIds);
    }

    public void setPlaylistIds(List<String> nextPlaylistIds) {
        playlistIds.clear();
        playlistIds.addAll(nextPlaylistIds);
    }

    public List<Chapter> getChapters() {
        return new ArrayList<>(chapters);
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public int currentChapterIndex() {
        if (chapters.isEmpty()) {
            return -1;
        }

        int currentIndex = 0;
        for (int i = 0; i < chapters.size(); i++) {
            if (progressSeconds >= chapters.get(i).getStartSeconds()) {
                currentIndex = i;
            }
        }
        return currentIndex;
    }

    private static List<Chapter> defaultChapters(AudioType type, int durationSeconds) {
        List<Chapter> generated = new ArrayList<>();
        if (type != AudioType.AUDIOBOOK || durationSeconds <= 0) {
            return generated;
        }

        int chapterCount = Math.max(1, Math.min(12, (int) Math.ceil(durationSeconds / 600.0)));
        int chapterLength = Math.max(1, durationSeconds / chapterCount);
        for (int i = 0; i < chapterCount; i++) {
            generated.add(new Chapter("Chapter " + (i + 1), i * chapterLength));
        }
        return generated;
    }

    private boolean hasGeneratedChapters() {
        if (chapters.isEmpty()) {
            return true;
        }
        for (int i = 0; i < chapters.size(); i++) {
            if (!chapters.get(i).getTitle().equals("Chapter " + (i + 1))) {
                return false;
            }
        }
        return true;
    }
}

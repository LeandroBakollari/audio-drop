package com.audiodrop.app.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import com.audiodrop.app.model.AudioType;
import com.audiodrop.app.model.Chapter;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.model.MediaItem;
import com.audiodrop.app.model.Playlist;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the app. It keeps the in-memory library fast for UI use
 * and writes every folder/settings change back to SharedPreferences.
 */
public class AudioRepository {
    private static final String PREFS_NAME = "audio_drop_data";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_FOLDERS = "folders";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_PLAYLISTS = "playlists";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_SLEEP_TIMER = "sleep_timer";
    private static final String MUSIC_FOLDER_ID = "music";
    private static final String AUDIOBOOK_FOLDER_ID = "audiobooks";
    private static AudioRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final List<LibraryFolder> folders = new ArrayList<>();
    private final List<MediaItem> mediaItems = new ArrayList<>();
    private final List<Playlist> playlists = new ArrayList<>();

    private AudioRepository(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            seedDefaults();
            saveAll();
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
        } else {
            loadAll();
        }
    }

    public static AudioRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AudioRepository(context);
        }
        return instance;
    }

    public List<LibraryFolder> getRootFolders() {
        List<LibraryFolder> result = new ArrayList<>();
        for (LibraryFolder folder : folders) {
            if (folder.getParentFolderId() == null) {
                result.add(folder);
            }
        }
        return result;
    }

    public List<LibraryFolder> getChildFolders(String parentFolderId) {
        List<LibraryFolder> result = new ArrayList<>();
        for (LibraryFolder folder : folders) {
            if (same(folder.getParentFolderId(), parentFolderId)) {
                result.add(folder);
            }
        }
        return result;
    }

    public List<LibraryFolder> searchFolders(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return getRootFolders();
        }

        List<LibraryFolder> result = new ArrayList<>();
        for (LibraryFolder folder : folders) {
            if (matches(folder.getName(), normalized)
                    || matches(folder.getDescription(), normalized)
                    || matches(folderName(folder.getParentFolderId()), normalized)) {
                result.add(folder);
            }
        }
        return result;
    }

    public List<MediaItem> getMediaInFolder(String folderId) {
        List<MediaItem> result = new ArrayList<>();
        for (MediaItem item : mediaItems) {
            if (same(item.getFolderId(), folderId)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<MediaItem> searchMedia(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        List<MediaItem> result = new ArrayList<>();
        for (MediaItem item : mediaItems) {
            if (matches(item.getTitle(), normalized)
                    || matches(item.getArtist(), normalized)
                    || matches(item.getType().getLabel(), normalized)
                    || matches(folderName(item.getFolderId()), normalized)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<MediaItem> getRecentMedia() {
        return new ArrayList<>(mediaItems);
    }

    public List<MediaItem> getAllMedia() {
        return new ArrayList<>(mediaItems);
    }

    public List<MediaItem> getMediaInPlaylist(String playlistId) {
        if (playlistId == null || !isPlaylistFolder(playlistId)) {
            return new ArrayList<>();
        }

        List<MediaItem> result = new ArrayList<>();
        for (MediaItem item : mediaItems) {
            if (item.getType() == AudioType.MUSIC && same(item.getFolderId(), playlistId)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<Playlist> getPlaylists() {
        return buildPlaylistsFromMusicFolders();
    }

    public MediaItem getMediaById(String mediaId) {
        for (MediaItem item : mediaItems) {
            if (item.getId().equals(mediaId)) {
                return item;
            }
        }
        return null;
    }

    public MediaItem getAdjacentMedia(String mediaId, int direction) {
        if (mediaItems.isEmpty()) {
            return null;
        }

        int currentIndex = -1;
        for (int i = 0; i < mediaItems.size(); i++) {
            if (mediaItems.get(i).getId().equals(mediaId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) {
            return null;
        }

        int nextIndex = (currentIndex + direction) % mediaItems.size();
        if (nextIndex < 0) {
            nextIndex = mediaItems.size() - 1;
        }
        return mediaItems.get(nextIndex);
    }

    public void updateProgress(String mediaId, int progressSeconds) {
        MediaItem item = getMediaById(mediaId);
        if (item == null) {
            return;
        }
        item.setProgressSeconds(item.getType() == AudioType.AUDIOBOOK ? progressSeconds : 0);
        saveMedia();
    }

    public void updateMediaPlaylists(String mediaId, List<String> playlistIds) {
        MediaItem item = getMediaById(mediaId);
        if (item == null || item.getType() == AudioType.AUDIOBOOK) {
            return;
        }

        String nextFolderId = MUSIC_FOLDER_ID;
        if (playlistIds != null && !playlistIds.isEmpty()) {
            LibraryFolder playlistFolder = getFolderById(playlistIds.get(0));
            if (playlistFolder != null && same(topLevelFolderId(playlistFolder.getId()), MUSIC_FOLDER_ID)) {
                nextFolderId = playlistFolder.getId();
            }
        }
        item.setFolderId(nextFolderId);
        item.setPlaylistIds(new ArrayList<>());
        saveMedia();
    }

    public void moveMediaToFolder(String mediaId, String folderId) {
        MediaItem item = getMediaById(mediaId);
        LibraryFolder folder = getFolderById(folderId);
        if (item == null || folder == null || !folderAcceptsMediaType(folderId, item.getType())) {
            return;
        }

        item.setFolderId(folderId);
        saveMedia();
    }

    public MediaItem importDeviceAudio(Uri sourceUri, String folderId) {
        if (sourceUri == null) {
            return null;
        }

        String uriValue = sourceUri.toString();
        String targetFolderId = resolveImportFolderId(folderId);
        AudioType type = audioTypeForFolder(targetFolderId);
        for (MediaItem item : mediaItems) {
            if (uriValue.equals(item.getSourceUri())) {
                if (folderAcceptsMediaType(targetFolderId, item.getType())) {
                    item.setFolderId(targetFolderId);
                    saveMedia();
                }
                return item;
            }
        }

        try {
            appContext.getContentResolver().takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }

        String fileName = queryDisplayName(sourceUri);
        AudioMetadata metadata = readDeviceAudioMetadata(sourceUri);
        String fallbackTitle = fileName == null || fileName.trim().isEmpty()
                ? "Imported Audio"
                : titleFromFileName(fileName);
        String title = metadata.title.isEmpty() ? fallbackTitle : metadata.title;
        String artist = metadata.artist.isEmpty() ? "Device file" : metadata.artist;
        int durationSeconds = metadata.durationSeconds > 0 ? metadata.durationSeconds : 1;

        MediaItem item = new MediaItem(
                uniqueMediaId(title),
                title,
                artist,
                type,
                targetFolderId,
                durationSeconds,
                0,
                initials(title),
                new ArrayList<>(),
                null,
                null,
                uriValue
        );
        mediaItems.add(item);
        saveMedia();
        return item;
    }

    public LibraryFolder getFolderById(String folderId) {
        for (LibraryFolder folder : folders) {
            if (folder.getId().equals(folderId)) {
                return folder;
            }
        }
        return null;
    }

    public Playlist getPlaylistById(String playlistId) {
        LibraryFolder folder = getFolderById(playlistId);
        if (folder == null || !same(folder.getParentFolderId(), MUSIC_FOLDER_ID)) {
            return null;
        }
        return new Playlist(folder.getId(), folder.getName());
    }

    public Playlist createPlaylist(String name) {
        String cleanName = name == null || name.trim().isEmpty() ? "New Playlist" : name.trim();
        LibraryFolder folder = createFolder(MUSIC_FOLDER_ID, cleanName);
        return new Playlist(folder.getId(), folder.getName());
    }

    public LibraryFolder createFolder(String parentFolderId, String name) {
        if (!canCreateChildFolder(parentFolderId)) {
            return null;
        }

        String cleanName = name == null || name.trim().isEmpty() ? "New Folder" : name.trim();
        LibraryFolder folder = new LibraryFolder(
                uniqueFolderId(cleanName),
                cleanName,
                "Custom folder",
                initials(cleanName),
                false,
                false,
                parentFolderId
        );
        folders.add(folder);
        saveFolders();
        return folder;
    }

    public void renameFolder(String folderId, String newName) {
        LibraryFolder folder = getFolderById(folderId);
        if (folder == null || folder.isSystemFolder() || newName == null || newName.trim().isEmpty()) {
            return;
        }
        folder.setName(newName.trim());
        saveFolders();
    }

    public void deleteFolder(String folderId) {
        LibraryFolder folder = getFolderById(folderId);
        if (folder == null || folder.isSystemFolder()) {
            return;
        }

        String fallbackFolderId = folder.getParentFolderId() != null ? folder.getParentFolderId() : MUSIC_FOLDER_ID;
        for (MediaItem item : mediaItems) {
            if (same(item.getFolderId(), folderId)) {
                item.setFolderId(fallbackFolderId);
            }
        }
        for (LibraryFolder child : folders) {
            if (same(child.getParentFolderId(), folderId)) {
                child.setParentFolderId(folder.getParentFolderId());
            }
        }

        folders.remove(folder);
        saveAll();
    }

    public void moveFolder(String folderId, String nextParentFolderId) {
        LibraryFolder folder = getFolderById(folderId);
        LibraryFolder nextParent = getFolderById(nextParentFolderId);
        if (folder == null || nextParent == null || folder.isSystemFolder() || isPlaylistFolder(nextParentFolderId)) {
            return;
        }

        Set<String> blockedIds = descendantFolderIds(folderId);
        blockedIds.add(folderId);
        if (blockedIds.contains(nextParentFolderId) || same(folder.getParentFolderId(), nextParentFolderId)) {
            return;
        }

        String currentRoot = topLevelFolderId(folderId);
        if ((same(currentRoot, MUSIC_FOLDER_ID) || same(currentRoot, AUDIOBOOK_FOLDER_ID))
                && !same(topLevelFolderId(nextParentFolderId), currentRoot)) {
            return;
        }

        folder.setParentFolderId(nextParentFolderId);
        saveFolders();
    }

    public int folderItemCount(String folderId) {
        Set<String> ids = descendantFolderIds(folderId);
        ids.add(folderId);

        int count = 0;
        for (MediaItem item : mediaItems) {
            if (ids.contains(item.getFolderId())) {
                count++;
            }
        }
        return count;
    }

    public int childFolderCount(String folderId) {
        int count = 0;
        for (LibraryFolder folder : folders) {
            if (same(folder.getParentFolderId(), folderId)) {
                count++;
            }
        }
        return count;
    }

    public int playlistItemCount(String playlistId) {
        return getMediaInPlaylist(playlistId).size();
    }

    public List<LibraryFolder> getFoldersForMediaType(AudioType type) {
        String rootId = type == AudioType.AUDIOBOOK ? AUDIOBOOK_FOLDER_ID : MUSIC_FOLDER_ID;
        List<LibraryFolder> result = new ArrayList<>();
        for (LibraryFolder folder : folders) {
            if (same(folder.getId(), rootId) || same(topLevelFolderId(folder.getId()), rootId)) {
                result.add(folder);
            }
        }
        return result;
    }

    public List<LibraryFolder> getFolderMoveTargets(String folderId) {
        List<LibraryFolder> result = new ArrayList<>();
        LibraryFolder folder = getFolderById(folderId);
        if (folder == null || folder.isSystemFolder()) {
            return result;
        }

        Set<String> blockedIds = descendantFolderIds(folderId);
        blockedIds.add(folderId);
        String currentRoot = topLevelFolderId(folderId);
        for (LibraryFolder candidate : folders) {
            if (blockedIds.contains(candidate.getId()) || same(candidate.getId(), folder.getParentFolderId())) {
                continue;
            }
            if ((same(currentRoot, MUSIC_FOLDER_ID) || same(currentRoot, AUDIOBOOK_FOLDER_ID))
                    && !same(topLevelFolderId(candidate.getId()), currentRoot)) {
                continue;
            }
            if (isPlaylistFolder(candidate.getId())) {
                continue;
            }
            result.add(candidate);
        }
        return result;
    }

    public String folderPath(String folderId) {
        List<String> names = new ArrayList<>();
        String currentId = folderId;
        while (currentId != null) {
            LibraryFolder folder = getFolderById(currentId);
            if (folder == null) {
                break;
            }
            names.add(0, folder.getName());
            currentId = folder.getParentFolderId();
        }
        return names.isEmpty() ? "" : String.join(" / ", names);
    }

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public int getSleepTimerMinutes() {
        return prefs.getInt(KEY_SLEEP_TIMER, 0);
    }

    public void setSleepTimerMinutes(int minutes) {
        prefs.edit().putInt(KEY_SLEEP_TIMER, minutes).apply();
    }

    public String folderName(String folderId) {
        if (folderId == null) {
            return "";
        }
        LibraryFolder folder = getFolderById(folderId);
        return folder == null ? "" : folder.getName();
    }

    public boolean canCreateChildFolder(String parentFolderId) {
        return parentFolderId == null || !isPlaylistFolder(parentFolderId);
    }

    private void seedDefaults() {
        folders.clear();
        mediaItems.clear();
        playlists.clear();

        folders.add(new LibraryFolder("music", "Music", "Albums, singles, and focus mixes", "MU", true, true, null));
        folders.add(new LibraryFolder("audiobooks", "Audiobooks", "Long-form listening", "AB", true, true, null));
        folders.add(new LibraryFolder("focus", "Focus Queue", "Music for coding and studying", "FO", false, false, "music"));
        folders.add(new LibraryFolder("bedtime", "Bedtime", "Music for winding down", "BE", false, false, "music"));
        folders.add(new LibraryFolder("commute", "Commute", "Music for moving around", "CO", false, false, "music"));
        folders.add(new LibraryFolder("study-notes", "Study Notes", "Lessons and narrated notes", "SN", false, false, "audiobooks"));

        addBundledMusicItems();
        addBundledAudiobookItems();
    }

    private void loadAll() {
        folders.clear();
        mediaItems.clear();
        playlists.clear();

        try {
            JSONArray folderArray = new JSONArray(prefs.getString(KEY_FOLDERS, "[]"));
            for (int i = 0; i < folderArray.length(); i++) {
                JSONObject object = folderArray.getJSONObject(i);
                folders.add(new LibraryFolder(
                        object.getString("id"),
                        object.getString("name"),
                        object.optString("description", ""),
                        object.optString("iconSeed", initials(object.getString("name"))),
                        object.optBoolean("systemFolder", false),
                        object.optBoolean("pinnedOnHome", false),
                        object.isNull("parentFolderId") ? null : object.optString("parentFolderId")
                ));
            }

            JSONArray mediaArray = new JSONArray(prefs.getString(KEY_MEDIA, "[]"));
            for (int i = 0; i < mediaArray.length(); i++) {
                JSONObject object = mediaArray.getJSONObject(i);
                JSONArray playlistArray = object.optJSONArray("playlistIds");
                List<String> ids = new ArrayList<>();
                if (playlistArray != null) {
                    for (int j = 0; j < playlistArray.length(); j++) {
                        ids.add(playlistArray.getString(j));
                    }
                }
                JSONArray chapterArray = object.optJSONArray("chapters");
                List<Chapter> chapters = new ArrayList<>();
                if (chapterArray != null) {
                    for (int j = 0; j < chapterArray.length(); j++) {
                        JSONObject chapterObject = chapterArray.getJSONObject(j);
                        chapters.add(new Chapter(
                                chapterObject.optString("title", "Chapter " + (j + 1)),
                                chapterObject.optInt("startSeconds", 0)
                        ));
                    }
                }
                mediaItems.add(new MediaItem(
                        object.getString("id"),
                        object.getString("title"),
                        object.optString("artist", ""),
                        AudioType.valueOf(object.getString("type")),
                        object.getString("folderId"),
                        object.optInt("durationSeconds", 0),
                        object.optInt("progressSeconds", 0),
                        object.optString("artworkSeed", "AD"),
                        ids,
                        chapters,
                        jsonStringOrNull(object, "assetPath"),
                        jsonStringOrNull(object, "sourceUri")
                ));
            }

            JSONArray playlistArray = new JSONArray(prefs.getString(KEY_PLAYLISTS, "[]"));
            for (int i = 0; i < playlistArray.length(); i++) {
                JSONObject object = playlistArray.getJSONObject(i);
                playlists.add(new Playlist(object.getString("id"), object.getString("name")));
            }

            boolean mediaChanged = removeUnavailableMedia();
            mediaChanged |= migrateLegacyPlaylistsToMusicFolders();
            mediaChanged |= flattenPlaylistFolders();
            mediaChanged |= addBundledMusicItems();
            mediaChanged |= addBundledAudiobookItems();
            if (mediaChanged) {
                saveMedia();
            }
            if (!playlists.isEmpty()) {
                playlists.clear();
                savePlaylists();
            }
        } catch (JSONException exception) {
            seedDefaults();
            saveAll();
        }
    }

    private void saveAll() {
        saveFolders();
        saveMedia();
        savePlaylists();
    }

    private void saveFolders() {
        JSONArray array = new JSONArray();
        try {
            for (LibraryFolder folder : folders) {
                JSONObject object = new JSONObject();
                object.put("id", folder.getId());
                object.put("name", folder.getName());
                object.put("description", folder.getDescription());
                object.put("iconSeed", folder.getIconSeed());
                object.put("systemFolder", folder.isSystemFolder());
                object.put("pinnedOnHome", folder.isPinnedOnHome());
                object.put("parentFolderId", folder.getParentFolderId() == null ? JSONObject.NULL : folder.getParentFolderId());
                array.put(object);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply();
    }

    private void saveMedia() {
        JSONArray array = new JSONArray();
        try {
            for (MediaItem item : mediaItems) {
                JSONObject object = new JSONObject();
                object.put("id", item.getId());
                object.put("title", item.getTitle());
                object.put("artist", item.getArtist());
                object.put("type", item.getType().name());
                object.put("folderId", item.getFolderId());
                object.put("durationSeconds", item.getDurationSeconds());
                object.put("progressSeconds", item.getProgressSeconds());
                object.put("artworkSeed", item.getArtworkSeed());
                object.put("playlistIds", new JSONArray(item.getPlaylistIds()));
                object.put("assetPath", item.getAssetPath() == null ? JSONObject.NULL : item.getAssetPath());
                object.put("sourceUri", item.getSourceUri() == null ? JSONObject.NULL : item.getSourceUri());
                JSONArray chapterArray = new JSONArray();
                for (Chapter chapter : item.getChapters()) {
                    JSONObject chapterObject = new JSONObject();
                    chapterObject.put("title", chapter.getTitle());
                    chapterObject.put("startSeconds", chapter.getStartSeconds());
                    chapterArray.put(chapterObject);
                }
                object.put("chapters", chapterArray);
                array.put(object);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString(KEY_MEDIA, array.toString()).apply();
    }

    private void savePlaylists() {
        JSONArray array = new JSONArray();
        try {
            for (Playlist playlist : playlists) {
                JSONObject object = new JSONObject();
                object.put("id", playlist.getId());
                object.put("name", playlist.getName());
                array.put(object);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply();
    }

    private List<Playlist> buildPlaylistsFromMusicFolders() {
        List<Playlist> result = new ArrayList<>();
        for (LibraryFolder folder : folders) {
            if (same(folder.getParentFolderId(), MUSIC_FOLDER_ID)) {
                result.add(new Playlist(folder.getId(), folder.getName()));
            }
        }
        return result;
    }

    private boolean migrateLegacyPlaylistsToMusicFolders() {
        if (playlists.isEmpty()) {
            return false;
        }

        Map<String, String> legacyPlaylistFolderIds = new HashMap<>();
        boolean foldersChanged = false;
        for (Playlist playlist : playlists) {
            LibraryFolder existingFolder = getFolderById(playlist.getId());
            if (existingFolder != null && same(existingFolder.getParentFolderId(), MUSIC_FOLDER_ID)) {
                legacyPlaylistFolderIds.put(playlist.getId(), existingFolder.getId());
                continue;
            }

            String folderId = existingFolder == null ? playlist.getId() : uniqueFolderId(playlist.getName());
            folders.add(new LibraryFolder(
                    folderId,
                    playlist.getName(),
                    "Playlist",
                    initials(playlist.getName()),
                    false,
                    false,
                    MUSIC_FOLDER_ID
            ));
            legacyPlaylistFolderIds.put(playlist.getId(), folderId);
            foldersChanged = true;
        }

        boolean mediaChanged = false;
        for (MediaItem item : mediaItems) {
            if (item.getType() != AudioType.MUSIC || item.getPlaylistIds().isEmpty()) {
                continue;
            }

            for (String playlistId : item.getPlaylistIds()) {
                String folderId = legacyPlaylistFolderIds.get(playlistId);
                if (folderId != null) {
                    item.setFolderId(folderId);
                    mediaChanged = true;
                    break;
                }
            }
            item.setPlaylistIds(new ArrayList<>());
            mediaChanged = true;
        }

        if (foldersChanged) {
            saveFolders();
        }
        return mediaChanged;
    }

    private boolean flattenPlaylistFolders() {
        boolean changed = false;
        for (int i = folders.size() - 1; i >= 0; i--) {
            LibraryFolder folder = folders.get(i);
            String parentFolderId = folder.getParentFolderId();
            if (parentFolderId == null || !isPlaylistFolder(parentFolderId)) {
                continue;
            }

            for (MediaItem item : mediaItems) {
                if (same(item.getFolderId(), folder.getId())) {
                    item.setFolderId(parentFolderId);
                    changed = true;
                }
            }
            for (LibraryFolder child : folders) {
                if (same(child.getParentFolderId(), folder.getId())) {
                    child.setParentFolderId(parentFolderId);
                    changed = true;
                }
            }
            folders.remove(i);
            changed = true;
        }

        if (changed) {
            saveFolders();
        }
        return changed;
    }

    private boolean addBundledMusicItems() {
        boolean added = false;
        List<String> musicPaths = new ArrayList<>();
        collectAudioAssets("music", musicPaths);
        String[] playlistFolderIds = {"focus", "bedtime", "commute"};
        for (int i = 0; i < musicPaths.size(); i++) {
            String assetPath = musicPaths.get(i);
            String fileName = assetPath.substring(assetPath.lastIndexOf('/') + 1);
            String title = titleFromFileName(fileName);
            AudioMetadata metadata = readAudioMetadata(assetPath);
            String displayTitle = metadata.title.isEmpty() ? title : metadata.title;
            String displayArtist = metadata.artist.isEmpty() ? "Music" : metadata.artist;
            int durationSeconds = metadata.durationSeconds > 0 ? metadata.durationSeconds : 180;
            String folderId = getFolderById(playlistFolderIds[i % playlistFolderIds.length]) == null
                    ? MUSIC_FOLDER_ID
                    : playlistFolderIds[i % playlistFolderIds.length];
            added |= upsertBundledMediaItem(
                    "song-" + slug(assetPath),
                    displayTitle,
                    displayArtist,
                    AudioType.MUSIC,
                    folderId,
                    durationSeconds,
                    initials(displayTitle),
                    assetPath,
                    new ArrayList<>()
            );
        }
        return added;
    }

    private boolean addBundledAudiobookItems() {
        boolean added = false;
        List<String> audiobookPaths = new ArrayList<>();
        collectAudioAssets("audiobooks", audiobookPaths);
        collectAudioAssets("audiobook", audiobookPaths);
        for (String assetPath : audiobookPaths) {
            String fileName = assetPath.substring(assetPath.lastIndexOf('/') + 1);
            String title = titleFromFileName(fileName);
            AudioMetadata metadata = readAudioMetadata(assetPath);
            String displayTitle = metadata.title.isEmpty() ? title : metadata.title;
            String displayArtist = metadata.artist.isEmpty() ? "Audiobook" : metadata.artist;
            int durationSeconds = metadata.durationSeconds > 0 ? metadata.durationSeconds : 3600;
            added |= upsertBundledMediaItem(
                    "audiobook-" + slug(assetPath),
                    displayTitle,
                    displayArtist,
                    AudioType.AUDIOBOOK,
                    AUDIOBOOK_FOLDER_ID,
                    durationSeconds,
                    initials(displayTitle),
                    assetPath,
                    new ArrayList<>()
            );
        }
        return added;
    }

    private boolean upsertBundledMediaItem(
            String id,
            String title,
            String artist,
            AudioType type,
            String folderId,
            int durationSeconds,
            String artworkSeed,
            String assetPath,
            List<String> playlistIds
    ) {
        for (int i = 0; i < mediaItems.size(); i++) {
            MediaItem existingItem = mediaItems.get(i);
            if (!existingItem.getId().equals(id)) {
                continue;
            }

            if (existingItem.getType() == type
                    && title.equals(existingItem.getTitle())
                    && artist.equals(existingItem.getArtist())
                    && durationSeconds == existingItem.getDurationSeconds()
                    && artworkSeed.equals(existingItem.getArtworkSeed())
                    && assetPath.equals(existingItem.getAssetPath())
                    && (type != AudioType.AUDIOBOOK || existingItem.getPlaylistIds().isEmpty())) {
                return false;
            }

            List<String> savedPlaylistIds = type == AudioType.AUDIOBOOK
                    ? new ArrayList<>()
                    : existingItem.getPlaylistIds().isEmpty()
                    ? playlistIds
                    : existingItem.getPlaylistIds();
            mediaItems.set(i, new MediaItem(
                    id,
                    title,
                    artist,
                    type,
                    existingItem.getFolderId(),
                    durationSeconds,
                    existingItem.getProgressSeconds(),
                    artworkSeed,
                    savedPlaylistIds,
                    assetPath
            ));
            return true;
        }

        mediaItems.add(new MediaItem(
                id,
                title,
                artist,
                type,
                folderId,
                durationSeconds,
                0,
                artworkSeed,
                playlistIds,
                assetPath
        ));
        return true;
    }

    private boolean removeUnavailableMedia() {
        boolean removed = false;
        for (int i = mediaItems.size() - 1; i >= 0; i--) {
            MediaItem item = mediaItems.get(i);
            boolean available = item.getSourceUri() == null
                    ? assetExists(item.getAssetPath())
                    : sourceUriExists(item.getSourceUri());
            if (!available) {
                mediaItems.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private boolean assetExists(String assetPath) {
        if (assetPath == null || assetPath.trim().isEmpty()) {
            return false;
        }
        try (AssetFileDescriptor ignored = appContext.getAssets().openFd(assetPath)) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean sourceUriExists(String sourceUri) {
        if (sourceUri == null || sourceUri.trim().isEmpty()) {
            return false;
        }
        try (ParcelFileDescriptor ignored = appContext.getContentResolver().openFileDescriptor(Uri.parse(sourceUri), "r")) {
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void collectAudioAssets(String folder, List<String> result) {
        try {
            String[] children = appContext.getAssets().list(folder);
            if (children == null) {
                return;
            }
            for (String child : children) {
                String path = folder + "/" + child;
                if (isSupportedAudioFile(child)) {
                    result.add(path);
                } else {
                    collectAudioAssets(path, result);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private AudioMetadata readAudioMetadata(String assetPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (AssetFileDescriptor descriptor = appContext.getAssets().openFd(assetPath)) {
            retriever.setDataSource(
                    descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(),
                    descriptor.getLength()
            );
            String durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String title = cleanMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            String artist = cleanMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            int durationSeconds = durationMillis == null ? 0 : (int) (Long.parseLong(durationMillis) / 1000L);
            return new AudioMetadata(title, artist, durationSeconds);
        } catch (Exception ignored) {
            return new AudioMetadata("", "", 0);
        } finally {
            try {
                retriever.release();
            } catch (IOException | RuntimeException ignored) {
            }
        }
    }

    private AudioMetadata readDeviceAudioMetadata(Uri sourceUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(appContext, sourceUri);
            String durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String title = cleanMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            String artist = cleanMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            int durationSeconds = durationMillis == null ? 0 : (int) (Long.parseLong(durationMillis) / 1000L);
            return new AudioMetadata(title, artist, durationSeconds);
        } catch (Exception ignored) {
            return new AudioMetadata("", "", 0);
        } finally {
            try {
                retriever.release();
            } catch (IOException | RuntimeException ignored) {
            }
        }
    }

    private String queryDisplayName(Uri sourceUri) {
        try (Cursor cursor = appContext.getContentResolver().query(
                sourceUri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isSupportedAudioFile(String fileName) {
        String lower = normalize(fileName);
        return lower.endsWith(".aac")
                || lower.endsWith(".flac")
                || lower.endsWith(".m4a")
                || lower.endsWith(".m4b")
                || lower.endsWith(".mp3")
                || lower.endsWith(".ogg")
                || lower.endsWith(".wav");
    }

    private static String cleanMetadata(String value) {
        return value == null ? "" : value.trim();
    }

    private static String titleFromFileName(String fileName) {
        String withoutExtension = fileName.replaceFirst("\\.[^.]+$", "");
        return withoutExtension.replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String slug(String value) {
        String slug = normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "audio" : slug;
    }

    private static class AudioMetadata {
        final String title;
        final String artist;
        final int durationSeconds;

        AudioMetadata(String title, String artist, int durationSeconds) {
            this.title = title;
            this.artist = artist;
            this.durationSeconds = durationSeconds;
        }
    }

    private String uniqueFolderId(String name) {
        String base = normalize(name).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) {
            base = "folder";
        }

        String candidate = base;
        int suffix = 2;
        while (getFolderById(candidate) != null) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String uniqueMediaId(String title) {
        String base = "device-" + normalize(title).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.equals("device-")) {
            base = "device-audio";
        }

        String candidate = base;
        int suffix = 2;
        while (getMediaById(candidate) != null) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String resolveImportFolderId(String folderId) {
        LibraryFolder folder = getFolderById(folderId);
        if (folder == null) {
            return MUSIC_FOLDER_ID;
        }
        String rootId = topLevelFolderId(folder.getId());
        return same(rootId, AUDIOBOOK_FOLDER_ID) || same(rootId, MUSIC_FOLDER_ID)
                ? folder.getId()
                : MUSIC_FOLDER_ID;
    }

    private AudioType audioTypeForFolder(String folderId) {
        return same(topLevelFolderId(folderId), AUDIOBOOK_FOLDER_ID) ? AudioType.AUDIOBOOK : AudioType.MUSIC;
    }

    private boolean isPlaylistFolder(String folderId) {
        LibraryFolder folder = getFolderById(folderId);
        return folder != null && same(folder.getParentFolderId(), MUSIC_FOLDER_ID);
    }

    private boolean folderAcceptsMediaType(String folderId, AudioType type) {
        String requiredRoot = type == AudioType.AUDIOBOOK ? AUDIOBOOK_FOLDER_ID : MUSIC_FOLDER_ID;
        return same(folderId, requiredRoot) || same(topLevelFolderId(folderId), requiredRoot);
    }

    private String topLevelFolderId(String folderId) {
        LibraryFolder folder = getFolderById(folderId);
        if (folder == null) {
            return null;
        }

        while (folder.getParentFolderId() != null) {
            LibraryFolder parent = getFolderById(folder.getParentFolderId());
            if (parent == null) {
                break;
            }
            folder = parent;
        }
        return folder.getId();
    }

    private Set<String> descendantFolderIds(String folderId) {
        Set<String> ids = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(folderId);

        while (!pending.isEmpty()) {
            String parentId = pending.removeFirst();
            for (LibraryFolder folder : folders) {
                if (same(folder.getParentFolderId(), parentId) && ids.add(folder.getId())) {
                    pending.add(folder.getId());
                }
            }
        }
        return ids;
    }

    private static String initials(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase(Locale.US);
        }
        return clean.toUpperCase(Locale.US);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private static boolean matches(String value, String query) {
        return normalize(value).contains(query);
    }

    private static boolean same(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static String jsonStringOrNull(JSONObject object, String key) {
        return object.isNull(key) ? null : object.optString(key, null);
    }
}

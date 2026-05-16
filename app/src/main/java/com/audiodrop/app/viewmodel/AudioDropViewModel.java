package com.audiodrop.app.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.audiodrop.app.data.AudioRepository;
import com.audiodrop.app.model.AudioType;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.model.MediaItem;
import com.audiodrop.app.model.Playlist;

import java.util.List;

/**
 * ViewModel layer used by Activities so UI screens do not read or write storage directly.
 */
public class AudioDropViewModel extends AndroidViewModel {
    private final AudioRepository repository;

    public AudioDropViewModel(@NonNull Application application) {
        super(application);
        repository = AudioRepository.getInstance(application);
    }

    public List<LibraryFolder> getRootFolders() {
        return repository.getRootFolders();
    }

    public List<LibraryFolder> getChildFolders(String parentFolderId) {
        return repository.getChildFolders(parentFolderId);
    }

    public List<LibraryFolder> searchFolders(String query) {
        return repository.searchFolders(query);
    }

    public List<MediaItem> getMediaInFolder(String folderId) {
        return repository.getMediaInFolder(folderId);
    }

    public List<MediaItem> searchMedia(String query) {
        return repository.searchMedia(query);
    }

    public List<MediaItem> getRecentMedia() {
        return repository.getRecentMedia();
    }

    public List<MediaItem> getAllMedia() {
        return repository.getAllMedia();
    }

    public List<Playlist> getPlaylists() {
        return repository.getPlaylists();
    }

    public List<MediaItem> getMediaInPlaylist(String playlistId) {
        return repository.getMediaInPlaylist(playlistId);
    }

    public MediaItem getMediaById(String mediaId) {
        return repository.getMediaById(mediaId);
    }

    public MediaItem getAdjacentMedia(String mediaId, int direction) {
        return repository.getAdjacentMedia(mediaId, direction);
    }

    public void updateProgress(String mediaId, int progressSeconds) {
        repository.updateProgress(mediaId, progressSeconds);
    }

    public void updateMediaPlaylists(String mediaId, List<String> playlistIds) {
        repository.updateMediaPlaylists(mediaId, playlistIds);
    }

    public void moveMediaToFolder(String mediaId, String folderId) {
        repository.moveMediaToFolder(mediaId, folderId);
    }

    public MediaItem importDeviceAudio(Uri sourceUri, String folderId) {
        return repository.importDeviceAudio(sourceUri, folderId);
    }

    public LibraryFolder getFolderById(String folderId) {
        return repository.getFolderById(folderId);
    }

    public Playlist getPlaylistById(String playlistId) {
        return repository.getPlaylistById(playlistId);
    }

    public Playlist createPlaylist(String name) {
        return repository.createPlaylist(name);
    }

    public LibraryFolder createFolder(String parentFolderId, String name) {
        return repository.createFolder(parentFolderId, name);
    }

    public boolean canCreateChildFolder(String parentFolderId) {
        return repository.canCreateChildFolder(parentFolderId);
    }

    public void renameFolder(String folderId, String newName) {
        repository.renameFolder(folderId, newName);
    }

    public void deleteFolder(String folderId) {
        repository.deleteFolder(folderId);
    }

    public void moveFolder(String folderId, String parentFolderId) {
        repository.moveFolder(folderId, parentFolderId);
    }

    public List<LibraryFolder> getFoldersForMediaType(AudioType type) {
        return repository.getFoldersForMediaType(type);
    }

    public List<LibraryFolder> getFolderMoveTargets(String folderId) {
        return repository.getFolderMoveTargets(folderId);
    }

    public String folderPath(String folderId) {
        return repository.folderPath(folderId);
    }

    public int folderItemCount(String folderId) {
        return repository.folderItemCount(folderId);
    }

    public int childFolderCount(String folderId) {
        return repository.childFolderCount(folderId);
    }

    public int playlistItemCount(String playlistId) {
        return repository.playlistItemCount(playlistId);
    }

    public boolean isDarkModeEnabled() {
        return repository.isDarkModeEnabled();
    }

    public void setDarkModeEnabled(boolean enabled) {
        repository.setDarkModeEnabled(enabled);
    }

    public int getSleepTimerMinutes() {
        return repository.getSleepTimerMinutes();
    }

    public void setSleepTimerMinutes(int minutes) {
        repository.setSleepTimerMinutes(minutes);
    }
}

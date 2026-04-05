package com.audiodrop.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.audiodrop.app.data.SampleData
import com.audiodrop.app.model.AudioType
import com.audiodrop.app.model.LibraryFolder
import com.audiodrop.app.model.LibrarySortMode
import com.audiodrop.app.model.LibraryViewMode
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.PlaylistUi
import com.audiodrop.app.model.RepeatMode
import com.audiodrop.app.model.SleepTimerOption
import com.audiodrop.app.model.durationFromFraction

private const val YOUTUBE_FOLDER_ID = "youtube"
private const val DOWNLOADS_FOLDER_ID = "downloads"

@Stable
class AudioDropState(
    initialFolders: List<LibraryFolder>,
    initialMediaItems: List<MediaItemUi>,
    initialPlaylists: List<PlaylistUi>
) {
    val folders = mutableStateListOf<LibraryFolder>().apply { addAll(initialFolders) }
    val mediaItems = mutableStateListOf<MediaItemUi>().apply { addAll(initialMediaItems) }
    val playlists = mutableStateListOf<PlaylistUi>().apply { addAll(initialPlaylists) }

    var activeMediaId by mutableStateOf(mediaItems.firstOrNull()?.id)
        private set

    var isPlaying by mutableStateOf(true)
        private set

    var homeSearchQuery by mutableStateOf("")
    var librarySearchQuery by mutableStateOf("")
    var quickPasteUrl by mutableStateOf("")
        private set
    var quickPasteMessage by mutableStateOf<String?>(null)
        private set

    var activeFolderId by mutableStateOf<String?>(null)
        private set
    var activeTypeFilter by mutableStateOf<AudioType?>(null)
        private set
    var librarySortMode by mutableStateOf(LibrarySortMode.RECENT)
        private set
    var libraryViewMode by mutableStateOf(LibraryViewMode.LIST)
        private set

    var selectionMode by mutableStateOf(false)
        private set
    val selectedItemIds = mutableStateListOf<String>()

    var playbackSpeed by mutableStateOf(1f)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var sleepTimerOption by mutableStateOf(SleepTimerOption.OFF)
        private set

    private var generatedYoutubeCount by mutableIntStateOf(1)

    val currentItem: MediaItemUi?
        get() = mediaItems.firstOrNull { it.id == activeMediaId } ?: mediaItems.firstOrNull()

    val pinnedFolders: List<LibraryFolder>
        get() {
            val query = homeSearchQuery.trim().lowercase()
            return folders
                .filter { it.pinnedOnHome }
                .filter { folder ->
                    query.isBlank() ||
                        folder.name.lowercase().contains(query) ||
                        folder.description.lowercase().contains(query)
                }
        }

    val recentItems: List<MediaItemUi>
        get() {
            val query = homeSearchQuery.trim().lowercase()
            return mediaItems.filter { itemMatchesQuery(it, query) }.take(6)
        }

    val visibleLibraryItems: List<MediaItemUi>
        get() {
            val query = librarySearchQuery.trim().lowercase()
            val filteredItems = mediaItems
                .asSequence()
                .filter { activeFolderId == null || it.folderId == activeFolderId }
                .filter { activeTypeFilter == null || it.type == activeTypeFilter }
                .filter { itemMatchesQuery(it, query) }
                .toList()

            return when (librarySortMode) {
                LibrarySortMode.RECENT -> filteredItems
                LibrarySortMode.NAME -> filteredItems.sortedBy { it.title.lowercase() }
                LibrarySortMode.DATE_ADDED -> filteredItems.sortedByDescending { it.dateAddedOrder }
                LibrarySortMode.DURATION -> filteredItems.sortedByDescending { it.durationSeconds }
            }
        }

    fun openItem(itemId: String, autoPlay: Boolean = true) {
        activeMediaId = itemId
        isPlaying = autoPlay
        markRecent(itemId)
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun skipBy(deltaSeconds: Int) {
        val item = currentItem ?: return
        updateItem(item.id) {
            it.copy(
                progressSeconds = (it.progressSeconds + deltaSeconds)
                    .coerceIn(0, it.durationSeconds)
            )
        }
    }

    fun seekTo(fraction: Float) {
        val item = currentItem ?: return
        updateItem(item.id) {
            it.copy(progressSeconds = durationFromFraction(it.durationSeconds, fraction))
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerOption = option
    }

    fun updateQuickPasteUrl(url: String) {
        quickPasteUrl = url
        quickPasteMessage = null
    }

    fun submitQuickPasteUrl(): String? {
        val sanitizedUrl = quickPasteUrl.trim()
        if (!sanitizedUrl.contains("youtube.com/") && !sanitizedUrl.contains("youtu.be/")) {
            quickPasteMessage = "Paste a valid YouTube link to prepare audio-only playback."
            return null
        }

        val importNumber = mediaItems.count { it.type == AudioType.YOUTUBE } + generatedYoutubeCount
        generatedYoutubeCount += 1
        val newItem = MediaItemUi(
            id = "yt-import-$importNumber",
            title = "YouTube Import $importNumber",
            supportingText = "Quick paste ready",
            type = AudioType.YOUTUBE,
            folderId = YOUTUBE_FOLDER_ID,
            durationSeconds = 2940,
            progressSeconds = 0,
            artworkSeed = "YP",
            hasArtwork = false,
            lastPlayedLabel = "Ready to play",
            dateAddedLabel = "Today",
            dateAddedOrder = (mediaItems.maxOfOrNull { it.dateAddedOrder } ?: 0) + 1,
            sourceUrl = sanitizedUrl,
            playCount = 0,
            downloadStateLabel = "Audio-Only Stream",
            metadataLabel = "Queued"
        )

        mediaItems.add(0, newItem)
        activeMediaId = newItem.id
        isPlaying = true
        quickPasteMessage = "Link saved in YouTube Audio and opened in the player."
        quickPasteUrl = ""
        return newItem.id
    }

    fun setActiveFolder(folderId: String?) {
        activeFolderId = folderId
    }

    fun updateActiveTypeFilter(type: AudioType?) {
        activeTypeFilter = type
    }

    fun updateLibrarySortMode(mode: LibrarySortMode) {
        librarySortMode = mode
    }

    fun updateLibraryViewMode(mode: LibraryViewMode) {
        libraryViewMode = mode
    }

    fun createFolder(name: String) {
        val cleanName = name.trim().ifEmpty { "New Folder" }
        val folder = LibraryFolder(
            id = uniqueFolderId(cleanName),
            name = cleanName,
            description = "Custom folder",
            iconSeed = cleanName.take(2).uppercase(),
            isSystemFolder = false,
            pinnedOnHome = folders.count { it.pinnedOnHome } < 6
        )
        folders.add(folder)
        activeFolderId = folder.id
    }

    fun renameFolder(folderId: String, newName: String) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1) return

        val folder = folders[index]
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return

        folders[index] = folder.copy(
            name = cleanName,
            iconSeed = cleanName.take(2).uppercase()
        )
    }

    fun deleteFolder(folderId: String) {
        val index = folders.indexOfFirst { it.id == folderId }
        if (index == -1 || folders[index].isSystemFolder) return

        mediaItems.indices.forEach { mediaIndex ->
            val item = mediaItems[mediaIndex]
            if (item.folderId == folderId) {
                mediaItems[mediaIndex] = item.copy(folderId = DOWNLOADS_FOLDER_ID)
            }
        }

        folders.removeAt(index)
        if (activeFolderId == folderId) {
            activeFolderId = DOWNLOADS_FOLDER_ID
        }
    }

    fun toggleSelectionMode() {
        selectionMode = !selectionMode
        if (!selectionMode) {
            selectedItemIds.clear()
        }
    }

    fun beginSelectionWith(itemId: String) {
        if (!selectionMode) {
            selectionMode = true
        }
        toggleItemSelection(itemId)
    }

    fun toggleItemSelection(itemId: String) {
        if (selectedItemIds.contains(itemId)) {
            selectedItemIds.remove(itemId)
        } else {
            selectedItemIds.add(itemId)
        }

        if (selectedItemIds.isEmpty()) {
            selectionMode = false
        }
    }

    fun clearSelection() {
        selectedItemIds.clear()
        selectionMode = false
    }

    fun moveItemToFolder(itemId: String, folderId: String) {
        updateItem(itemId) { it.copy(folderId = folderId) }
    }

    fun moveSelectedItemsToFolder(folderId: String) {
        selectedItemIds.toList().forEach { itemId ->
            moveItemToFolder(itemId, folderId)
        }
        clearSelection()
        activeFolderId = folderId
    }

    fun deleteSelectedItems() {
        val deletingCurrentItem = activeMediaId in selectedItemIds
        mediaItems.removeAll { it.id in selectedItemIds }
        clearSelection()

        if (deletingCurrentItem) {
            activeMediaId = mediaItems.firstOrNull()?.id
        }
    }

    fun toggleFavorite(itemId: String) {
        updateItem(itemId) { item ->
            item.copy(isFavorite = !item.isFavorite)
        }
    }

    fun togglePlaylist(playlistId: String, itemId: String? = currentItem?.id) {
        val targetItemId = itemId ?: return
        updateItem(targetItemId) { item ->
            val nextPlaylists = if (playlistId in item.playlistIds) {
                item.playlistIds - playlistId
            } else {
                item.playlistIds + playlistId
            }
            item.copy(playlistIds = nextPlaylists)
        }
    }

    fun folderName(folderId: String): String {
        return folders.firstOrNull { it.id == folderId }?.name ?: "Library"
    }

    fun folderItemCount(folderId: String): Int {
        return mediaItems.count { it.folderId == folderId }
    }

    fun playlistItemCount(playlistId: String): Int {
        return mediaItems.count { playlistId in it.playlistIds }
    }

    fun itemSubtitle(item: MediaItemUi): String {
        return "${item.supportingText} • ${folderName(item.folderId)}"
    }

    private fun markRecent(itemId: String) {
        updateItem(itemId) { item ->
            item.copy(
                lastPlayedLabel = "Just now",
                playCount = item.playCount + 1
            )
        }

        val index = mediaItems.indexOfFirst { it.id == itemId }
        if (index > 0) {
            val item = mediaItems.removeAt(index)
            mediaItems.add(0, item)
        }
    }

    private fun itemMatchesQuery(item: MediaItemUi, query: String): Boolean {
        if (query.isBlank()) return true

        return buildList {
            add(item.title)
            add(item.supportingText)
            add(item.type.label)
            add(item.lastPlayedLabel)
            add(folderName(item.folderId))
            addAll(item.playlistIds.mapNotNull { id -> playlists.firstOrNull { it.id == id }?.name })
        }.any { value ->
            value.lowercase().contains(query)
        }
    }

    private fun updateItem(itemId: String, transform: (MediaItemUi) -> MediaItemUi) {
        val index = mediaItems.indexOfFirst { it.id == itemId }
        if (index == -1) return
        mediaItems[index] = transform(mediaItems[index])
    }

    private fun uniqueFolderId(name: String): String {
        val normalized = name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "folder" }

        var candidate = normalized
        var suffix = 2
        while (folders.any { it.id == candidate }) {
            candidate = "$normalized-$suffix"
            suffix += 1
        }
        return candidate
    }
}

@Composable
fun rememberAudioDropState(): AudioDropState {
    return remember {
        AudioDropState(
            initialFolders = SampleData.folders,
            initialMediaItems = SampleData.mediaItems,
            initialPlaylists = SampleData.playlists
        )
    }
}

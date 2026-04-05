package com.audiodrop.app.model

import kotlin.math.roundToInt

enum class AudioType(val label: String) {
    MUSIC("Music"),
    AUDIOBOOK("Audiobook"),
    YOUTUBE("YouTube Audio")
}

enum class LibrarySortMode(val label: String) {
    RECENT("Recent"),
    NAME("Name"),
    DATE_ADDED("Date Added"),
    DURATION("Duration")
}

enum class LibraryViewMode(val label: String) {
    LIST("List"),
    GRID("Grid")
}

enum class RepeatMode(val label: String) {
    OFF("Repeat Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

enum class SleepTimerOption(val label: String, val minutes: Int?) {
    OFF("Off", null),
    FIFTEEN("15m", 15),
    THIRTY("30m", 30),
    FORTY_FIVE("45m", 45)
}

data class MediaItemUi(
    val id: String,
    val title: String,
    val supportingText: String,
    val type: AudioType,
    val folderId: String,
    val durationSeconds: Int,
    val progressSeconds: Int,
    val artworkSeed: String,
    val hasArtwork: Boolean,
    val lastPlayedLabel: String,
    val dateAddedLabel: String,
    val dateAddedOrder: Int,
    val sourceUrl: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val playlistIds: List<String> = emptyList(),
    val downloadStateLabel: String = "Offline Ready",
    val chapterLabel: String? = null,
    val metadataLabel: String? = null
)

data class LibraryFolder(
    val id: String,
    val name: String,
    val description: String,
    val iconSeed: String,
    val isSystemFolder: Boolean = false,
    val pinnedOnHome: Boolean = false
)

data class PlaylistUi(
    val id: String,
    val name: String
)

val MediaItemUi.progressFraction: Float
    get() = if (durationSeconds == 0) 0f else progressSeconds.toFloat() / durationSeconds.toFloat()

val MediaItemUi.progressLabel: String
    get() = formatDuration(progressSeconds)

val MediaItemUi.totalDurationLabel: String
    get() = formatDuration(durationSeconds)

val MediaItemUi.remainingLabel: String
    get() = formatDuration((durationSeconds - progressSeconds).coerceAtLeast(0))

fun durationFromFraction(durationSeconds: Int, fraction: Float): Int {
    return (durationSeconds * fraction.coerceIn(0f, 1f)).roundToInt()
}

fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

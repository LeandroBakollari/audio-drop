package com.audiodrop.app.data

import com.audiodrop.app.model.AudioType
import com.audiodrop.app.model.LibraryFolder
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.PlaylistUi

object SampleData {
    val folders = listOf(
        LibraryFolder(
            id = "music",
            name = "Music",
            description = "Albums, singles, and focus mixes",
            iconSeed = "MU",
            isSystemFolder = true,
            pinnedOnHome = true
        ),
        LibraryFolder(
            id = "audiobooks",
            name = "Audiobooks",
            description = "Long-form listening with resume support",
            iconSeed = "AB",
            isSystemFolder = true,
            pinnedOnHome = true
        ),
        LibraryFolder(
            id = "youtube",
            name = "YouTube Audio",
            description = "Saved links and streaming history",
            iconSeed = "YT",
            isSystemFolder = true,
            pinnedOnHome = true
        ),
        LibraryFolder(
            id = "favorites",
            name = "Favorites",
            description = "Quick access picks",
            iconSeed = "FV",
            isSystemFolder = true,
            pinnedOnHome = true
        ),
        LibraryFolder(
            id = "downloads",
            name = "Downloads",
            description = "Offline local files",
            iconSeed = "DL",
            isSystemFolder = true
        ),
        LibraryFolder(
            id = "language",
            name = "Language Notes",
            description = "Lessons and study clips",
            iconSeed = "LN"
        )
    )

    val playlists = listOf(
        PlaylistUi(id = "focus", name = "Focus Queue"),
        PlaylistUi(id = "bedtime", name = "Bedtime"),
        PlaylistUi(id = "commute", name = "Commute")
    )

    val mediaItems = listOf(
        MediaItemUi(
            id = "deep-focus",
            title = "Deep Focus Mix",
            supportingText = "Midnight Tape",
            type = AudioType.MUSIC,
            folderId = "music",
            durationSeconds = 250,
            progressSeconds = 102,
            artworkSeed = "DF",
            hasArtwork = true,
            lastPlayedLabel = "12 min ago",
            dateAddedLabel = "Today",
            dateAddedOrder = 6,
            isFavorite = true,
            playCount = 14,
            playlistIds = listOf("focus", "commute"),
            metadataLabel = "AAC"
        ),
        MediaItemUi(
            id = "atomic-habits-c4",
            title = "Atomic Habits - Chapter 4",
            supportingText = "James Clear",
            type = AudioType.AUDIOBOOK,
            folderId = "audiobooks",
            durationSeconds = 1680,
            progressSeconds = 1040,
            artworkSeed = "AH",
            hasArtwork = true,
            lastPlayedLabel = "Yesterday",
            dateAddedLabel = "Mar 18",
            dateAddedOrder = 5,
            playCount = 5,
            playlistIds = listOf("bedtime"),
            chapterLabel = "Chapter 4 of 18",
            metadataLabel = "M4A"
        ),
        MediaItemUi(
            id = "lofi-stream",
            title = "Lo-fi Coding Stream",
            supportingText = "Pasted YouTube Link",
            type = AudioType.YOUTUBE,
            folderId = "youtube",
            durationSeconds = 3150,
            progressSeconds = 554,
            artworkSeed = "LS",
            hasArtwork = false,
            lastPlayedLabel = "2 days ago",
            dateAddedLabel = "Mar 16",
            dateAddedOrder = 4,
            sourceUrl = "https://youtube.com/watch?v=example",
            playCount = 8,
            playlistIds = listOf("focus"),
            downloadStateLabel = "Streaming",
            metadataLabel = "Audio Only"
        ),
        MediaItemUi(
            id = "night-train",
            title = "Night Train",
            supportingText = "City Echoes",
            type = AudioType.MUSIC,
            folderId = "favorites",
            durationSeconds = 266,
            progressSeconds = 0,
            artworkSeed = "NT",
            hasArtwork = true,
            lastPlayedLabel = "Last week",
            dateAddedLabel = "Mar 10",
            dateAddedOrder = 2,
            isFavorite = true,
            playCount = 19,
            playlistIds = listOf("commute"),
            metadataLabel = "MP3"
        ),
        MediaItemUi(
            id = "deep-work-ritual",
            title = "Deep Work Rituals",
            supportingText = "Narrated notes",
            type = AudioType.AUDIOBOOK,
            folderId = "language",
            durationSeconds = 4020,
            progressSeconds = 1815,
            artworkSeed = "DW",
            hasArtwork = false,
            lastPlayedLabel = "3 days ago",
            dateAddedLabel = "Mar 12",
            dateAddedOrder = 3,
            chapterLabel = "Lesson 7 of 12",
            metadataLabel = "FLAC"
        ),
        MediaItemUi(
            id = "field-recording",
            title = "Rain on Glass",
            supportingText = "Local import",
            type = AudioType.MUSIC,
            folderId = "downloads",
            durationSeconds = 534,
            progressSeconds = 221,
            artworkSeed = "RG",
            hasArtwork = false,
            lastPlayedLabel = "5 days ago",
            dateAddedLabel = "Mar 08",
            dateAddedOrder = 1,
            playCount = 3,
            playlistIds = listOf("bedtime"),
            metadataLabel = "WAV"
        )
    )
}

package com.audiodrop.app.data

import com.audiodrop.app.model.AudioType
import com.audiodrop.app.model.LibraryFolder
import com.audiodrop.app.model.MediaItemUi

object SampleData {
    val recentItems = listOf(
        MediaItemUi(
            id = "1",
            title = "Deep Focus Mix",
            subtitle = "Downloaded music",
            type = AudioType.MUSIC,
            progressLabel = "01:42",
            totalDurationLabel = "04:10",
            artworkEmoji = "M"
        ),
        MediaItemUi(
            id = "2",
            title = "Atomic Habits - Chapter 4",
            subtitle = "Audiobook",
            type = AudioType.AUDIOBOOK,
            progressLabel = "17:20",
            totalDurationLabel = "28:00",
            artworkEmoji = "A"
        ),
        MediaItemUi(
            id = "3",
            title = "Lo-fi coding stream",
            subtitle = "YouTube audio",
            type = AudioType.YOUTUBE,
            progressLabel = "09:14",
            totalDurationLabel = "52:30",
            artworkEmoji = "Y",
            sourceUrl = "https://youtube.com/watch?v=example"
        )
    )

    val folders = listOf(
        LibraryFolder(id = "music", name = "Music", itemCount = 24),
        LibraryFolder(id = "audiobooks", name = "Audiobooks", itemCount = 8),
        LibraryFolder(id = "youtube", name = "YouTube Audio", itemCount = 11),
        LibraryFolder(id = "favorites", name = "Favorites", itemCount = 13)
    )
}

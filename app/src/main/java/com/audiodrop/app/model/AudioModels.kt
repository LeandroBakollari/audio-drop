package com.audiodrop.app.model

enum class AudioType {
    MUSIC,
    AUDIOBOOK,
    YOUTUBE
}

data class MediaItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: AudioType,
    val progressLabel: String,
    val totalDurationLabel: String,
    val artworkEmoji: String,
    val sourceUrl: String? = null
)

data class LibraryFolder(
    val id: String,
    val name: String,
    val itemCount: Int
)

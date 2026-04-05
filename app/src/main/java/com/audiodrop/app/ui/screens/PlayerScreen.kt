package com.audiodrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audiodrop.app.model.AudioType
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.SleepTimerOption
import com.audiodrop.app.model.progressFraction
import com.audiodrop.app.model.progressLabel
import com.audiodrop.app.model.remainingLabel
import com.audiodrop.app.model.totalDurationLabel
import com.audiodrop.app.ui.AudioDropState
import com.audiodrop.app.ui.components.AudioArtwork
import com.audiodrop.app.ui.components.AudioTypeChip
import com.audiodrop.app.ui.components.EmptyStateCard
import com.audiodrop.app.ui.components.SectionTitle

private val speedOptions = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
private val folderHighlightIds = listOf("music", "audiobooks", "youtube", "favorites", "downloads")

@Composable
fun PlayerScreen(
    state: AudioDropState,
    onBack: () -> Unit
) {
    val item = state.currentItem

    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                title = "Nothing selected",
                subtitle = "Open a track from Home or Library to start playback."
            )
        }
        return
    }

    val jumpSeconds = if (item.type == AudioType.AUDIOBOOK) 30 else 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = { state.toggleFavorite(item.id) }) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Outlined.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = "Toggle favorite"
                )
            }
        }

        PlayerHero(item = item, subtitle = state.itemSubtitle(item))

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(
                        title = "Timeline",
                        subtitle = "Playback position is saved automatically."
                    )
                    Text(
                        text = "-${item.remainingLabel}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Slider(
                    value = item.progressFraction,
                    onValueChange = state::seekTo
                )
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.progressLabel)
                    Text(item.totalDurationLabel)
                }
            }
        }

        PlayerControls(
            jumpSeconds = jumpSeconds,
            isPlaying = state.isPlaying,
            onSkipBack = { state.skipBy(-jumpSeconds) },
            onPlayPause = state::togglePlayPause,
            onSkipForward = { state.skipBy(jumpSeconds) }
        )

        PlayerToolsCard(state = state, item = item)

        SpeedCard(
            currentSpeed = state.playbackSpeed,
            onSpeedSelected = state::updatePlaybackSpeed
        )

        PlaylistCard(
            state = state,
            item = item
        )

        FolderCard(
            state = state,
            item = item
        )

        SleepTimerCard(
            selectedOption = state.sleepTimerOption,
            onOptionSelected = state::setSleepTimer
        )

        ContentModeCard(item = item)
    }
}

@Composable
private fun PlayerHero(
    item: MediaItemUi,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AudioArtwork(
                item = item,
                modifier = Modifier.size(220.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudioTypeChip(type = item.type)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = listOfNotNull(
                        item.chapterLabel,
                        item.metadataLabel,
                        item.downloadStateLabel
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    jumpSeconds: Int,
    isPlaying: Boolean,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkipButton(
                label = "Back $jumpSeconds s",
                onClick = onSkipBack
            )
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(86.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Play or pause",
                    modifier = Modifier.size(42.dp)
                )
            }
            SkipButton(
                label = "Forward $jumpSeconds s",
                onClick = onSkipForward
            )
        }
    }
}

@Composable
private fun SkipButton(
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.height(72.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlayerToolsCard(
    state: AudioDropState,
    item: MediaItemUi
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Playback Tools",
                subtitle = "Tune the player for music, audiobooks, or long-form sessions."
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.shuffleEnabled,
                    onClick = state::toggleShuffle,
                    label = { Text(if (state.shuffleEnabled) "Shuffle On" else "Shuffle") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Shuffle, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = state.repeatMode != com.audiodrop.app.model.RepeatMode.OFF,
                    onClick = state::cycleRepeatMode,
                    label = { Text(state.repeatMode.label) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Repeat, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = item.isFavorite,
                    onClick = { state.toggleFavorite(item.id) },
                    label = { Text("Favorite") },
                    leadingIcon = {
                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun SpeedCard(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Speed Control",
                subtitle = "Especially useful for audiobooks and lecture-style listening."
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                speedOptions.forEach { speed ->
                    FilterChip(
                        selected = currentSpeed == speed,
                        onClick = { onSpeedSelected(speed) },
                        label = { Text("${speed}x") }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    state: AudioDropState,
    item: MediaItemUi
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Add To Playlist",
                subtitle = "Keep mixed content together in a single listening queue."
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.playlists.forEach { playlist ->
                    FilterChip(
                        selected = playlist.id in item.playlistIds,
                        onClick = { state.togglePlaylist(playlist.id, item.id) },
                        label = {
                            Text("${playlist.name} (${state.playlistItemCount(playlist.id)})")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    state: AudioDropState,
    item: MediaItemUi
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Add To Folder",
                subtitle = "Refile items quickly without leaving the player."
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.folders
                    .filter { it.id in folderHighlightIds || it.id == item.folderId }
                    .distinctBy { it.id }
                    .forEach { folder ->
                        FilterChip(
                            selected = folder.id == item.folderId,
                            onClick = { state.moveItemToFolder(item.id, folder.id) },
                            label = { Text(folder.name) }
                        )
                    }
            }
        }
    }
}

@Composable
private fun SleepTimerCard(
    selectedOption: SleepTimerOption,
    onOptionSelected: (SleepTimerOption) -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Sleep Timer",
                subtitle = "Handy for bedtime listening and long audiobook sessions."
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SleepTimerOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedOption == option,
                        onClick = { onOptionSelected(option) },
                        label = { Text(option.label) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Timer, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentModeCard(item: MediaItemUi) {
    val title: String
    val description: String

    when (item.type) {
        AudioType.MUSIC -> {
            title = "Music Mode"
            description = "Focuses on quick play, simple queue control, favorites, and playlist support."
        }

        AudioType.AUDIOBOOK -> {
            title = "Audiobook Mode"
            description = "Prioritizes resume position, chapter progress, speed control, and sleep timer handling."
        }

        AudioType.YOUTUBE -> {
            title = "YouTube Audio Mode"
            description = "Keeps the source link close, emphasizes streaming reliability, and stores pasted history."
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = title,
                subtitle = description
            )

            if (item.sourceUrl != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null)
                    Text(
                        text = item.sourceUrl,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

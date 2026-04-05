package com.audiodrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.audiodrop.app.model.LibraryFolder
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.PlaylistUi
import com.audiodrop.app.model.progressFraction
import com.audiodrop.app.model.progressLabel
import com.audiodrop.app.model.totalDurationLabel
import com.audiodrop.app.ui.AudioDropState
import com.audiodrop.app.ui.components.AudioArtwork
import com.audiodrop.app.ui.components.AudioTypeChip
import com.audiodrop.app.ui.components.EmptyStateCard
import com.audiodrop.app.ui.components.SearchField
import com.audiodrop.app.ui.components.SectionTitle

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    state: AudioDropState,
    onOpenPlayer: (String) -> Unit,
    onOpenLibrary: (String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeroCard(
                currentTitle = state.currentItem?.title,
                onResumePlayer = {
                    state.currentItem?.id?.let(onOpenPlayer)
                },
                onOpenLibrary = { onOpenLibrary(null) }
            )
        }

        item {
            SearchField(
                value = state.homeSearchQuery,
                onValueChange = { state.homeSearchQuery = it },
                placeholder = "Search recent items, folders, or playlists"
            )
        }

        item {
            QuickPasteCard(
                url = state.quickPasteUrl,
                message = state.quickPasteMessage,
                onUrlChange = state::updateQuickPasteUrl,
                onPaste = {
                    state.submitQuickPasteUrl()?.let(onOpenPlayer)
                },
                onOpenYoutubeFolder = { onOpenLibrary("youtube") }
            )
        }

        item {
            SectionTitle(
                title = "Folder Overview",
                subtitle = "Jump into the parts of the library you use most."
            )
        }

        item {
            if (state.pinnedFolders.isEmpty()) {
                EmptyStateCard(
                    title = "No pinned folders yet",
                    subtitle = "Create or pin folders from the library to keep them close on Home."
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.pinnedFolders) { folder ->
                        FolderOverviewCard(
                            folder = folder,
                            itemCount = state.folderItemCount(folder.id),
                            onClick = { onOpenLibrary(folder.id) }
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "Playlists",
                subtitle = "Mixed queues that work across music, audiobooks, and YouTube audio."
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        itemCount = state.playlistItemCount(playlist.id)
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "Recently Played",
                subtitle = "Resume exactly where you left off."
            )
        }

        if (state.recentItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nothing in history",
                    subtitle = "Play a file or paste a YouTube link to start building your listening history."
                )
            }
        } else {
            items(state.recentItems) { item ->
                RecentItemCard(
                    item = item,
                    subtitle = state.itemSubtitle(item),
                    onClick = { onOpenPlayer(item.id) },
                    onToggleFavorite = { state.toggleFavorite(item.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeroCard(
    currentTitle: String?,
    onResumePlayer: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AudioDrop",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "A cleaner home for music, audiobooks, and pasted YouTube audio.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenLibrary) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Library")
                }
                FilledTonalButton(
                    onClick = onResumePlayer,
                    enabled = currentTitle != null
                ) {
                    Icon(Icons.Outlined.Headphones, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentTitle != null) "Resume Player" else "Nothing Playing")
                }
            }
        }
    }
}

@Composable
private fun QuickPasteCard(
    url: String,
    message: String?,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onOpenYoutubeFolder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                }
                Column {
                    Text(
                        text = "Quick Paste Link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Paste a YouTube URL and prepare it for audio-only playback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("https://youtube.com/watch?v=...") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPaste) {
                    Text("Process Link")
                }
                FilledTonalButton(onClick = onOpenYoutubeFolder) {
                    Text("Open YouTube Audio")
                }
            }

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun FolderOverviewCard(
    folder: LibraryFolder,
    itemCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(196.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = folder.iconSeed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = folder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Open folder")
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistUi,
    itemCount: Int
) {
    Card(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlaylistPlay, contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun RecentItemCard(
    item: MediaItemUi,
    subtitle: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioArtwork(
                item = item,
                modifier = Modifier.size(72.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioTypeChip(type = item.type)
                    Text(
                        text = item.lastPlayedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${item.progressLabel} / ${item.totalDurationLabel}",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) {
                            Icons.Outlined.Star
                        } else {
                            Icons.Outlined.StarBorder
                        },
                        contentDescription = "Toggle favorite"
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Open player"
                )
            }
        }
    }
}

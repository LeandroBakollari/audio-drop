package com.audiodrop.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.audiodrop.app.model.AudioType
import com.audiodrop.app.model.LibraryFolder
import com.audiodrop.app.model.LibrarySortMode
import com.audiodrop.app.model.LibraryViewMode
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.progressLabel
import com.audiodrop.app.model.totalDurationLabel
import com.audiodrop.app.ui.AudioDropState
import com.audiodrop.app.ui.components.AudioArtwork
import com.audiodrop.app.ui.components.AudioTypeChip
import com.audiodrop.app.ui.components.EmptyStateCard
import com.audiodrop.app.ui.components.SearchField
import com.audiodrop.app.ui.components.SectionTitle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    state: AudioDropState,
    onOpenItem: (String) -> Unit
) {
    var createFolderDialogOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<LibraryFolder?>(null) }
    var deleteTarget by remember { mutableStateOf<LibraryFolder?>(null) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            LibraryHeader(
                selectionMode = state.selectionMode,
                onCreateFolder = { createFolderDialogOpen = true },
                onToggleSelectionMode = state::toggleSelectionMode
            )
        }

        item {
            SearchField(
                value = state.librarySearchQuery,
                onValueChange = { state.librarySearchQuery = it },
                placeholder = "Search title, folder, author, source"
            )
        }

        item {
            LibraryControlsCard(state = state)
        }

        item {
            SectionTitle(
                title = "Folders",
                subtitle = "System folders stay protected. Custom folders can be renamed or removed."
            )
        }

        item {
            FolderFilterRow(state = state)
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.folders.forEach { folder ->
                    FolderCard(
                        folder = folder,
                        itemCount = state.folderItemCount(folder.id),
                        isActive = state.activeFolderId == folder.id,
                        onClick = { state.setActiveFolder(folder.id) },
                        onRename = {
                            if (!folder.isSystemFolder) {
                                renameTarget = folder
                            }
                        },
                        onDelete = {
                            if (!folder.isSystemFolder) {
                                deleteTarget = folder
                            }
                        }
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = state.activeFolderId?.let { "Items In ${state.folderName(it)}" } ?: "All Items",
                subtitle = "Long press to start multi-select, move items, or delete in bulk."
            )
        }

        if (state.selectionMode) {
            item {
                BulkActionsCard(state = state)
            }
        }

        if (state.visibleLibraryItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No items match these filters",
                    subtitle = "Try another folder, change the sort/filter options, or create a new import."
                )
            }
        } else if (state.libraryViewMode == LibraryViewMode.LIST) {
            items(state.visibleLibraryItems, key = { it.id }) { item ->
                LibraryListItem(
                    item = item,
                    subtitle = state.itemSubtitle(item),
                    isSelected = item.id in state.selectedItemIds,
                    selectionMode = state.selectionMode,
                    onClick = {
                        if (state.selectionMode) {
                            state.toggleItemSelection(item.id)
                        } else {
                            onOpenItem(item.id)
                        }
                    },
                    onLongPress = { state.beginSelectionWith(item.id) },
                    onToggleFavorite = { state.toggleFavorite(item.id) }
                )
            }
        } else {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.visibleLibraryItems.forEach { item ->
                        LibraryGridItem(
                            item = item,
                            isSelected = item.id in state.selectedItemIds,
                            selectionMode = state.selectionMode,
                            onClick = {
                                if (state.selectionMode) {
                                    state.toggleItemSelection(item.id)
                                } else {
                                    onOpenItem(item.id)
                                }
                            },
                            onLongPress = { state.beginSelectionWith(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (createFolderDialogOpen) {
        FolderNameDialog(
            title = "Create Folder",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { createFolderDialogOpen = false },
            onConfirm = { name ->
                state.createFolder(name)
                createFolderDialogOpen = false
            }
        )
    }

    renameTarget?.let { folder ->
        FolderNameDialog(
            title = "Rename Folder",
            initialValue = folder.name,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                state.renameFolder(folder.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${folder.name}?") },
            text = {
                Text("Items in this folder will move to Downloads so nothing is lost.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.deleteFolder(folder.id)
                        deleteTarget = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LibraryHeader(
    selectionMode: Boolean,
    onCreateFolder: () -> Unit,
    onToggleSelectionMode: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(
                title = "Library",
                subtitle = "A lightweight explorer for music, audiobooks, downloads, and YouTube audio."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onCreateFolder) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                    Text(" New Folder")
                }
                OutlinedButton(onClick = onToggleSelectionMode) {
                    Icon(
                        imageVector = if (selectionMode) {
                            Icons.Outlined.Close
                        } else {
                            Icons.Outlined.DoneAll
                        },
                        contentDescription = null
                    )
                    Text(if (selectionMode) " Finish" else " Select")
                }
            }
        }
    }
}

@Composable
private fun LibraryControlsCard(state: AudioDropState) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(
                title = "Sort And Filter",
                subtitle = "Switch between recent, name, date added, duration, type, and layout."
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibrarySortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.librarySortMode == mode,
                        onClick = { state.updateLibrarySortMode(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = {
                            Icon(Icons.Outlined.FilterList, contentDescription = null)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = state.activeTypeFilter == null,
                    onClick = { state.updateActiveTypeFilter(null) },
                    label = { Text("All Types") }
                )
                AudioType.entries.forEach { type ->
                    FilterChip(
                        selected = state.activeTypeFilter == type,
                        onClick = { state.updateActiveTypeFilter(type) },
                        label = { Text(type.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = state.libraryViewMode == LibraryViewMode.LIST,
                    onClick = { state.updateLibraryViewMode(LibraryViewMode.LIST) },
                    label = { Text("List") },
                    leadingIcon = {
                        Icon(Icons.Outlined.List, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = state.libraryViewMode == LibraryViewMode.GRID,
                    onClick = { state.updateLibraryViewMode(LibraryViewMode.GRID) },
                    label = { Text("Grid") },
                    leadingIcon = {
                        Icon(Icons.Outlined.GridView, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderFilterRow(state: AudioDropState) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = state.activeFolderId == null,
            onClick = { state.setActiveFolder(null) },
            label = { Text("All") }
        )
        state.folders.forEach { folder ->
            FilterChip(
                selected = state.activeFolderId == folder.id,
                onClick = { state.setActiveFolder(folder.id) },
                label = { Text("${folder.name} (${state.folderItemCount(folder.id)})") }
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: LibraryFolder,
    itemCount: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.width(184.dp),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (folder.isSystemFolder) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                    } else {
                        Text(
                            text = folder.iconSeed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (folder.isSystemFolder) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "System folder")
                } else {
                    Row {
                        IconButton(onClick = onRename) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename folder")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete folder")
                        }
                    }
                }
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
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun BulkActionsCard(state: AudioDropState) {
    Card(
        shape = RoundedCornerShape(26.dp),
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
                title = "${state.selectedItemIds.size} item(s) selected",
                subtitle = "Move the selection to a folder or remove it from the app library."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = state::clearSelection) {
                    Text("Clear")
                }
                FilledTonalButton(onClick = state::deleteSelectedItems) {
                    Text("Delete")
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.folders.forEach { folder ->
                    FilterChip(
                        selected = false,
                        onClick = { state.moveSelectedItemsToFolder(folder.id) },
                        label = { Text("Move to ${folder.name}") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryListItem(
    item: MediaItemUi,
    subtitle: String,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }

            AudioArtwork(
                item = item,
                modifier = Modifier.size(68.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.progressLabel} / ${item.totalDurationLabel}",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFavorite) {
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGridItem(
    item: MediaItemUi,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }

            AudioArtwork(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
            )
            AudioTypeChip(type = item.type)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.totalDurationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Folder Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

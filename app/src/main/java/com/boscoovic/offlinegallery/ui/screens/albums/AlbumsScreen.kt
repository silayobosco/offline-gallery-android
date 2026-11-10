package com.boscoovic.offlinegallery.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.ui.theme.FavoriteRed

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Album) -> Unit,
    onFavoritesClick: () -> Unit,
    onSecureFolderClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var newAlbumName by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Bar with Search & Add actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { viewModel.openCreateDialog() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_album),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Samsung One UI Reachability Viewing Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.albums),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${uiState.albums.size} albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Shortcuts Section (Favorites, Secure Folder, Recycle Bin)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ShortcutCard(
                                icon = Icons.Default.Favorite,
                                iconTint = FavoriteRed,
                                title = stringResource(R.string.favorites),
                                count = uiState.favoritesCount,
                                onClick = onFavoritesClick,
                                modifier = Modifier.weight(1f)
                            )
                            ShortcutCard(
                                icon = Icons.Default.Lock,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = stringResource(R.string.secure_folder),
                                count = uiState.secureCount,
                                onClick = onSecureFolderClick,
                                modifier = Modifier.weight(1f)
                            )
                            ShortcutCard(
                                icon = Icons.Default.Delete,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                title = stringResource(R.string.recycle_bin),
                                count = uiState.trashCount,
                                onClick = onRecycleBinClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Album items
                    items(uiState.albums, key = { "${it.isCustom}_${it.id}" }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }

        // Create Album Dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.closeCreateDialog() },
                title = { Text(stringResource(R.string.create_album)) },
                text = {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text(stringResource(R.string.album_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createCustomAlbum(newAlbumName)
                            newAlbumName = ""
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeCreateDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.coverUri)
                        .crossfade(true)
                        .size(400)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${album.itemCount} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

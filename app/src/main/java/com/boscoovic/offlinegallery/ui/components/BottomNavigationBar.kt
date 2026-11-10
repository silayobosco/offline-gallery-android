package com.boscoovic.offlinegallery.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.ui.navigation.Screen

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Pictures.route,
            onClick = { onNavigate(Screen.Pictures) },
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.pictures)) },
            label = { Text(stringResource(R.string.pictures)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Albums.route,
            onClick = { onNavigate(Screen.Albums) },
            icon = { Icon(Icons.Default.PhotoAlbum, contentDescription = stringResource(R.string.albums)) },
            label = { Text(stringResource(R.string.albums)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

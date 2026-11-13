package com.boscoovic.offlinegallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumDetailScreen
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumDetailViewModel
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumsViewModel
import com.boscoovic.offlinegallery.ui.screens.editor.EditorScreen
import com.boscoovic.offlinegallery.ui.screens.editor.EditorViewModel
import com.boscoovic.offlinegallery.ui.screens.favorites.FavoritesScreen
import com.boscoovic.offlinegallery.ui.screens.favorites.FavoritesViewModel
import com.boscoovic.offlinegallery.ui.screens.main.MainGalleryScreen
import com.boscoovic.offlinegallery.ui.screens.pictures.PicturesViewModel
import com.boscoovic.offlinegallery.ui.screens.recyclebin.RecycleBinScreen
import com.boscoovic.offlinegallery.ui.screens.recyclebin.RecycleBinViewModel
import com.boscoovic.offlinegallery.ui.screens.search.SearchScreen
import com.boscoovic.offlinegallery.ui.screens.search.SearchViewModel
import com.boscoovic.offlinegallery.ui.screens.secure.SecureFolderScreen
import com.boscoovic.offlinegallery.ui.screens.secure.SecureFolderViewModel
import com.boscoovic.offlinegallery.ui.screens.settings.SettingsScreen
import com.boscoovic.offlinegallery.ui.screens.settings.SettingsViewModel
import com.boscoovic.offlinegallery.ui.screens.slideshow.SlideshowScreen
import com.boscoovic.offlinegallery.ui.screens.stories.StoriesScreen
import com.boscoovic.offlinegallery.ui.screens.stories.StoriesViewModel
import com.boscoovic.offlinegallery.ui.screens.viewer.MediaViewerScreen
import com.boscoovic.offlinegallery.ui.screens.viewer.MediaViewerViewModel

@Composable
fun GalleryNavGraph(
    navController: NavHostController,
    repository: MediaRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Pictures.route,
        modifier = modifier
    ) {
        // Main Screen with Top Tabs [Pictures(367)] [Albums(12)], Collapsible Reachability Area, and Zero Bottom Bar
        composable(Screen.Pictures.route) {
            val picturesViewModel: PicturesViewModel = hiltViewModel()
            val albumsViewModel: AlbumsViewModel = hiltViewModel()
            MainGalleryScreen(
                picturesViewModel = picturesViewModel,
                albumsViewModel = albumsViewModel,
                onMediaClick = { item ->
                    navController.navigate(Screen.Viewer.createRoute(item.id, "ALL", ""))
                },
                onAlbumClick = { album ->
                    navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.name, album.isCustom))
                },
                onFavoritesClick = {
                    navController.navigate(Screen.Favorites.route)
                },
                onStoriesClick = {
                    navController.navigate(Screen.Stories.route)
                },
                onSecureFolderClick = {
                    navController.navigate(Screen.SecureFolder.route)
                },
                onRecycleBinClick = {
                    navController.navigate(Screen.RecycleBin.route)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

            // Album Detail
            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(
                    navArgument("albumId") { type = NavType.StringType },
                    navArgument("albumName") { type = NavType.StringType },
                    navArgument("isCustom") { type = NavType.StringType }
                )
            ) {
                val viewModel: AlbumDetailViewModel = hiltViewModel()
                AlbumDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { item ->
                        val contextType = if (viewModel.uiState.value.isCustom) "CUSTOM_ALBUM" else "ALBUM"
                        navController.navigate(Screen.Viewer.createRoute(item.id, contextType, viewModel.uiState.value.albumId.toString()))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onSlideshowClick = { contextType, contextArg ->
                        navController.navigate(Screen.Slideshow.createRoute(contextType, contextArg))
                    }
                )
            }

            // Stories Tab
            composable(Screen.Stories.route) {
                val viewModel: StoriesViewModel = hiltViewModel()
                StoriesScreen(
                    viewModel = viewModel,
                    onStoryClick = { story ->
                        if (story.items.isNotEmpty()) {
                            navController.navigate(Screen.Viewer.createRoute(story.coverItem.id, "ALL", ""))
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Search Tab
            composable(Screen.Search.route) {
                val viewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel = viewModel,
                    onMediaClick = { item ->
                        navController.navigate(Screen.Viewer.createRoute(item.id, "ALL", ""))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Favorites Screen
            composable(Screen.Favorites.route) {
                val viewModel: FavoritesViewModel = hiltViewModel()
                FavoritesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { item ->
                        navController.navigate(Screen.Viewer.createRoute(item.id, "FAVORITES", ""))
                    },
                    onSlideshowClick = {
                        navController.navigate(Screen.Slideshow.createRoute("FAVORITES", ""))
                    }
                )
            }

            // Full-screen Viewer
            composable(
                route = Screen.Viewer.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("contextType") { type = NavType.StringType; defaultValue = "ALL" },
                    navArgument("contextArg") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                val viewModel: MediaViewerViewModel = hiltViewModel()
                MediaViewerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.Editor.createRoute(id))
                    },
                    onSlideshowClick = {
                        navController.navigate(Screen.Slideshow.createRoute())
                    }
                )
            }

            // Editor
            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType }
                )
            ) {
                val viewModel: EditorViewModel = hiltViewModel()
                EditorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // Recycle Bin
            composable(Screen.RecycleBin.route) {
                val viewModel: RecycleBinViewModel = hiltViewModel()
                RecycleBinScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Secure Folder
            composable(Screen.SecureFolder.route) {
                val viewModel: SecureFolderViewModel = hiltViewModel()
                SecureFolderScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { item ->
                        navController.navigate(Screen.Viewer.createRoute(item.id, "SECURE", ""))
                    }
                )
            }

            // Slideshow
            composable(
                route = Screen.Slideshow.route,
                arguments = listOf(
                    navArgument("contextType") { type = NavType.StringType; defaultValue = "ALL" },
                    navArgument("contextArg") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val contextType = backStackEntry.arguments?.getString("contextType") ?: "ALL"
                val contextArg = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("contextArg") ?: "", "UTF-8")
                SlideshowScreen(
                    repository = repository,
                    contextType = contextType,
                    contextArg = contextArg,
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }



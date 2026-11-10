package com.boscoovic.offlinegallery.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import com.boscoovic.offlinegallery.ui.components.PermissionHandler
import com.boscoovic.offlinegallery.ui.navigation.GalleryNavGraph
import com.boscoovic.offlinegallery.ui.theme.OfflineGalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var repository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OfflineGalleryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionHandler(
                        onPermissionsGranted = {
                            // MediaStore observer reactively refreshes the gallery
                        }
                    ) {
                        val navController = rememberNavController()
                        GalleryNavGraph(
                            navController = navController,
                            repository = repository
                        )
                    }
                }
            }
        }
    }
}


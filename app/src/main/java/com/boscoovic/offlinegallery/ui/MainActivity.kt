package com.boscoovic.offlinegallery.ui

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import com.boscoovic.offlinegallery.data.settings.SettingsManager
import com.boscoovic.offlinegallery.data.settings.ThemeMode
import com.boscoovic.offlinegallery.ui.components.PermissionHandler
import com.boscoovic.offlinegallery.ui.navigation.GalleryNavGraph
import com.boscoovic.offlinegallery.ui.theme.OfflineGalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val dynamicColor by settingsManager.dynamicColor.collectAsState()

            val systemRequestLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) {
                repository.notifySystemRequestCompleted()
            }

            LaunchedEffect(repository) {
                repository.pendingSystemRequest.collect { intentSender ->
                    systemRequestLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            OfflineGalleryTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
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


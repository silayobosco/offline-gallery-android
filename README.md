# Lumina Gallery (Offline Android Gallery)

A modern, native, strictly offline Android gallery app inspired by Samsung Gallery and One UI design principles. Built with **Jetpack Compose**, **Room**, and **MediaStore**.

---

## Key Features

- **Strictly Offline**: Zero internet permissions (`android.permission.INTERNET` and `ACCESS_NETWORK_STATE` are omitted from the manifest). No analytics, no ads, no cloud sync, and no third-party tracking.
- **Pictures Timeline**:
  - Grouped by day with sticky date headers.
  - Smooth pinch-to-zoom to switch grid density dynamically between 2 and 6 columns.
  - Video duration pills and favorite indicators.
  - Filter chips: All, Photos, Videos, Favorites.
  - Multi-select mode with batch actions (Share, Favorite, Move, Hide, Delete).
- **Albums & Auto-Buckets**:
  - Device albums automatically grouped from MediaStore (Camera, Screenshots, Downloads, Movies).
  - Virtual custom albums: create, rename, delete, add/remove items.
  - Quick shortcuts for Favorites, Secure Folder, and Recycle Bin.
- **Full-Screen Media Viewer**:
  - Horizontal swipe pager with context preservation (All, Album, Favorites).
  - Gesture support: pinch-to-zoom, pan, and double-tap to reset/zoom.
  - Media3 ExoPlayer integration for offline video playback with custom controls.
  - Swipe-up / Info details panel displaying full EXIF data (camera model, aperture, shutter speed, ISO, focal length, GPS coordinates).
- **Recycle Bin**:
  - App-managed trash bin with automatic 30-day retention.
  - Daily background purge worker powered by AndroidX WorkManager.
  - Single and multi-select restore or permanent delete, plus "Empty Bin".
- **Secure Folder (Hidden Items)**:
  - Hidden photos/videos moved into encrypted app-private storage via AndroidX Security (`EncryptedFile` + AES-256 GCM).
  - Access protected by biometric authentication (fingerprint/face) or device credentials via AndroidX Biometric.
- **Photo Editor**:
  - Non-destructive editing pipeline in memory.
  - Transform: 90° rotation and horizontal flip.
  - Adjustments: Brightness, contrast, saturation, warmth.
  - Preset Filters: Vivid, Warm, Cool, Black & White, Sepia, Dramatic.
  - Freehand drawing tool with custom stroke colors.
  - Save as a new copy with EXIF metadata preservation.
- **Stories & Auto Collections**:
  - Automatic time and location clustering for monthly memory collections.
- **Slideshow**:
  - Full-screen slideshow with customizable intervals (2s, 3s, 5s) and play/pause controls.
- **Settings & Privacy**:
  - Dark / Light / System default themes.
  - Grid customization.
  - Transparent privacy disclosure confirming offline status.

---

## Architecture & Tech Stack

```
com.boscoovic.offlinegallery/
├── GalleryApp.kt                         # Application class, Coil setup, WorkManager scheduler
├── data/
│   ├── model/                            # Domain entities (MediaItem, Album, ExifData, MediaType)
│   ├── mediastore/                       # MediaStoreFetcher & MediaStoreObserver (ContentObserver)
│   ├── local/                            # Room database, entities (MediaMeta, Trash, CustomAlbum) & DAOs
│   ├── repository/                       # MediaRepository, SecureStorageManager, TrashPurgeWorker
│   └── di/                               # Hilt modules (DatabaseModule, RepositoryModule)
└── ui/
    ├── MainActivity.kt                   # Single activity entry point with edge-to-edge Compose
    ├── theme/                            # Material 3 colors, typography, and shape definitions
    ├── components/                       # Reusable UI widgets (MediaThumbnail, StickyHeader, SelectionBar)
    ├── navigation/                       # Screen routes and GalleryNavGraph
    └── screens/
        ├── pictures/                     # Main grid timeline & PicturesViewModel
        ├── albums/                       # Albums grid & AlbumDetailScreen
        ├── viewer/                       # Full-screen viewer, ZoomableImage, VideoPlayerView, Details
        ├── editor/                       # Image editor with adjustments, filters, rotate, flip
        ├── recyclebin/                   # Recycle Bin with 30-day purge & days-remaining badges
        ├── secure/                       # Biometric-gated secure folder
        ├── search/                       # Offline instant search & recent history
        ├── stories/                      # Auto-generated memory collections
        ├── slideshow/                    # Presentation slideshow
        └── settings/                     # Appearance, grid settings, and privacy statement
```

- **UI**: Jetpack Compose with Material 3 (One UI layout paradigms).
- **Database**: Room for app-managed metadata (favorites, custom albums, trash, hidden items).
- **Source of Truth**: Android `MediaStore` with real-time `ContentObserver`.
- **Dependency Injection**: Hilt.
- **Async & Concurrency**: Kotlin Coroutines + Flow.
- **Image/Video Loading**: Coil 2.7 with `VideoFrameDecoder`.
- **Video Playback**: AndroidX Media3 ExoPlayer.
- **Security & Biometrics**: AndroidX Biometric + AndroidX Security Crypto (`EncryptedFile`).
- **Scheduled Background Tasks**: AndroidX WorkManager.

---

## Verifying Offline Privacy

To verify that the application has zero access to the internet, inspect `app/src/main/AndroidManifest.xml`:
- Neither `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE` is present.
- The app operates completely self-contained on the device.

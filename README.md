# J-Gallery (Offline Android Gallery)

A modern, native, strictly offline Android gallery app inspired by Samsung Gallery and One UI design principles. Built with **Jetpack Compose**, **Room**, **Hilt**, **Media3**, and **MediaStore**.

---

## Key Features

- **Strictly Offline**: Zero internet permissions (`android.permission.INTERNET` and `ACCESS_NETWORK_STATE` are omitted from the manifest). No analytics, no ads, no cloud sync, and no third-party tracking.
- **Pictures Timeline**:
  - Grouped by day with sticky date headers and a fast-scroll scrubber (`FastScrollScrubber`).
  - Smooth pinch-to-zoom to switch grid density dynamically between 2 and 5 columns.
  - Long-press & drag-to-select (`DragSelectGrid`) with batch actions (Share, Favorite, Move/Add to Album, Hide, Delete) and Back-to-exit selection mode.
  - Filter chips: All, Photos, Videos, Favorites.
- **Albums & Auto-Buckets**:
  - 2×2 top shortcuts grid for **Favorites**, **Stories**, **Secure Folder**, and **Recycle Bin**.
  - Device albums automatically grouped from MediaStore (Camera, Screenshots, Downloads, Movies).
  - Virtual custom albums: create, rename, delete, and add/remove items.
- **Full-Screen Media Viewer & Video Player**:
  - Configurable **Vertical** (TikTok-style) or **Horizontal** swipe pager with context preservation (`All`, `Album`, `Custom Album`, `Favorites`, `Secure`), toggleable bottom **Filmstrip**, and swipe-down to dismiss (in horizontal mode).
  - Gesture support: pinch-to-zoom, pan, double-tap to zoom/like, and swipe-up/left for full EXIF details (camera model, aperture, shutter speed, ISO, focal length, GPS).
  - **Media3 ExoPlayer** video playback: 2× edge-hold speed & slide-down-to-lock, double-tap left/right edges to seek ±10s, interactive progress scrub bar, mute/speed controls, and configurable end-of-video behavior (Loop, Next, Stop).
  - **Set as Wallpaper** (Home screen, Lock screen, Both, or Other apps).
- **Photo Editor**:
  - **Crop & Rotate**: Interactive crop box mapped to rendered image bounds with rule-of-thirds grid, 4 draggable corner handles, aspect ratio presets (`Free`, `1:1`, `4:3`, `16:9`, `9:16`), 90° rotate, horizontal flip, and **Straighten** (`-45°` to `+45°`).
  - **Adjustments**: Brightness, Contrast, Saturation, Warmth, and Vignette.
  - **Filters & Draw**: Preset filters (`Vivid`, `Warm`, `Cool`, `B&W`, `Sepia`, `Dramatic`) and a freehand **Draw** tool with a 7-color palette and brush size slider.
  - **Hold-to-Compare** original preview, Undo/Redo stack, and Save as Copy (with EXIF preservation) or Overwrite Original.
- **Stories & Context-Aware Slideshow**:
  - Offline curated **Stories**: **On This Day**, **Favorites Reel**, **Event Highlights** (48-hour burst clustering), **Monthly Memories**, and **Year in Review**.
  - Tapping a story, album, or viewer slideshow launches **SlideshowScreen** scoped to that exact context (`Story`, `Album`, `Custom Album`, `Favorites`, `Secure`, or `All`) with **Ken Burns** zoom/pan animation, progress indicator, shuffle, and customizable intervals.
- **Offline Instant Search**:
  - Searches filenames, albums, formatted dates/months/years, MIME formats, paths, and orientations (`portrait`, `landscape`, `panorama`, `gif`).
  - Quick filter chips (`All`, `Photos`, `Videos`, `Screenshots`, `Camera`, `Favorites`, `GIFs`, `Portrait`, `Landscape`, `Large files`) and recent search history.
- **Secure Folder (Hidden Items)**:
  - Hidden photos/videos moved into encrypted app-private storage via AndroidX Security (`EncryptedFile` + AES-256 GCM).
  - Biometric / device credential lock via AndroidX Biometric, `FLAG_SECURE` screenshot & recents protection, automatic re-lock when backgrounded (`ON_STOP`), and long-press multi-select batch restore.
- **Recycle Bin (System Trash Sync)**:
  - Connected to **Android System Trash** on Android 11+ (`MediaStore.createTrashRequest` / `MediaStore.createDeleteRequest` & `IS_TRASHED`), keeping local trashed photos and videos in sync with Google Gallery and Google Photos' device trash.
  - 30-day automatic retention, single and multi-select restore or permanent delete, "Empty Bin", plus app-private trash fallback on Android 10.
- **Settings & Customization**:
  - Theme (`System`, `Light`, `Dark`), Dynamic Color, Default Grid Columns (`2`–`5 cols`), Viewer Scroll Direction (`Vertical` / `Horizontal`), Filmstrip toggle, Video autoplay & end action, cache size inspection/clearing, and transparent privacy disclosure.

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
│   ├── settings/                         # SettingsManager (SharedPreferences / StateFlows)
│   └── di/                               # Hilt modules (DatabaseModule, RepositoryModule)
└── ui/
    ├── MainActivity.kt                   # Single activity entry point with edge-to-edge Compose
    ├── theme/                            # Material 3 colors, typography, and shape definitions
    ├── components/                       # Reusable UI widgets (DragSelectGrid, FastScrollScrubber, MediaThumbnail, StickyHeader, SelectionBar)
    ├── navigation/                       # Screen routes and GalleryNavGraph
    └── screens/
        ├── main/                         # MainGalleryScreen (Pictures & Albums tabs + onboarding)
        ├── pictures/                     # Main grid timeline & PicturesViewModel
        ├── albums/                       # Albums grid & AlbumDetailScreen
        ├── viewer/                       # Full-screen viewer, ZoomableImage, VideoPlayerView, MediaDetailsSheet
        ├── editor/                       # Photo editor (Crop, Straighten, Adjust, Filters, Draw)
        ├── recyclebin/                   # Recycle Bin with 30-day purge & days-remaining badges
        ├── secure/                       # Biometric-gated secure folder with FLAG_SECURE & multi-select
        ├── search/                       # Offline instant search & quick filters
        ├── stories/                      # Offline curated memory collections
        ├── slideshow/                    # Context-aware Ken Burns slideshow
        └── settings/                     # Appearance, viewer, grid, video, and privacy settings
```

- **UI**: Jetpack Compose with Material 3 (One UI layout paradigms).
- **Database**: Room for app-managed metadata (favorites, custom albums, trash, hidden items, recent searches).
- **Source of Truth**: Android `MediaStore` with real-time `ContentObserver`.
- **Dependency Injection**: Hilt.
- **Async & Concurrency**: Kotlin Coroutines + Flow.
- **Image/Video Loading**: Coil 2.7 with `VideoFrameDecoder`.
- **Video Playback**: AndroidX Media3 ExoPlayer.
- **Security & Biometrics**: AndroidX Biometric + AndroidX Security Crypto (`EncryptedFile`).
- **Scheduled Background Tasks**: AndroidX WorkManager.

---

## Build & Test

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

---

## Verifying Offline Privacy

To verify that the application has zero access to the internet, inspect `app/src/main/AndroidManifest.xml`:
- Neither `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE` is present.
- The app operates completely self-contained on the device.

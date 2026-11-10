package com.boscoovic.offlinegallery.ui.navigation

sealed class Screen(val route: String) {
    data object Pictures : Screen("pictures")
    data object Albums : Screen("albums")
    data object Stories : Screen("stories")
    data object Search : Screen("search")
    data object RecycleBin : Screen("recycle_bin")
    data object SecureFolder : Screen("secure_folder")
    data object Settings : Screen("settings")

    data object AlbumDetail : Screen("album_detail/{albumId}/{albumName}/{isCustom}") {
        fun createRoute(albumId: Long, albumName: String, isCustom: Boolean) =
            "album_detail/$albumId/${java.net.URLEncoder.encode(albumName, "UTF-8")}/$isCustom"
    }

    data object Viewer : Screen("viewer/{mediaId}?contextType={contextType}&contextArg={contextArg}") {
        fun createRoute(mediaId: Long, contextType: String = "ALL", contextArg: String = "") =
            "viewer/$mediaId?contextType=$contextType&contextArg=${java.net.URLEncoder.encode(contextArg, "UTF-8")}"
    }

    data object Editor : Screen("editor/{mediaId}") {
        fun createRoute(mediaId: Long) = "editor/$mediaId"
    }

    data object Slideshow : Screen("slideshow?contextType={contextType}&contextArg={contextArg}") {
        fun createRoute(contextType: String = "ALL", contextArg: String = "") =
            "slideshow?contextType=$contextType&contextArg=${java.net.URLEncoder.encode(contextArg, "UTF-8")}"
    }
}

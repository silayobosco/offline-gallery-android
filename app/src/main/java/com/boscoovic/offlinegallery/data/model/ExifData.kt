package com.boscoovic.offlinegallery.data.model

data class ExifData(
    val make: String? = null,
    val model: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val dateTimeOriginal: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val width: Int = 0,
    val height: Int = 0,
    val orientation: Int = 0
) {
    val formattedCamera: String?
        get() {
            return if (!make.isNullOrBlank() || !model.isNullOrBlank()) {
                listOfNotNull(make, model).distinct().joinToString(" ")
            } else null
        }

    val formattedExposure: String?
        get() {
            val parts = mutableListOf<String>()
            focalLength?.let { parts.add(it) }
            fNumber?.let { parts.add("f/$it") }
            exposureTime?.let { parts.add("${it}s") }
            iso?.let { parts.add("ISO $it") }
            return if (parts.isNotEmpty()) parts.joinToString("  •  ") else null
        }
}


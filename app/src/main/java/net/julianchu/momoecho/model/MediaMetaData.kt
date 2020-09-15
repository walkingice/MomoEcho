package net.julianchu.momoecho.model

import android.net.Uri

data class MediaMetaData(
    val title: String?,
    val album: String?,
    val author: String?,
    val mimeType: String,
    val thumbnailFilePath: String?,
    val duration: Long
)

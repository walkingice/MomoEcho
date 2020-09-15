package net.julianchu.momoecho.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val uri: Uri,
    val md5: String,
    val createdAt: Date = Date(),
    var filename: String = uri.path ?: "",
    var title: String = filename,
    val album: String = "",
    val mimeType: String = "",
    val author: String = "",
    val thumbnailFilePath: String? = null,
    var duration: Long = -1
) {
    @Ignore
    val displayTitle = when {
        title.isNotEmpty() -> title
        filename.isNotEmpty() -> filename
        else -> uri.path ?: ""
    }

    @Ignore
    var clipsNumber = 0
}

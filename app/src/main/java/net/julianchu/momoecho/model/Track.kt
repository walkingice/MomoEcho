package net.julianchu.momoecho.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val uri: Uri,
    val createdAt: Date = Date(),
    var name: String = "${uri.path}",
    var displayName: String = name,
    var duration: Long = -1
) {
    @Ignore
    val displayText =
        if (displayName.isNotEmpty())
            displayName
        else (if (name.isNotEmpty())
            name
        else "${uri.path}")

    @Ignore
    var clipsNumber = 0
}
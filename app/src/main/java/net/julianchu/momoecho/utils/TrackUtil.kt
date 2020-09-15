package net.julianchu.momoecho.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.model.MediaMetaData
import java.io.File

object TrackUtil {
    suspend fun resolveFilename(
        context: Context,
        uri: Uri
    ): String? = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            name
        } else {
            null
        }
    }

    suspend fun retrieveMetaData(
        context: Context,
        audioSrcUri: Uri,
        audioSrcFileMd5: String
    ): MediaMetaData = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, audioSrcUri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val thumbnail: String? = retriever.embeddedPicture?.let {
            val thumbDir = File(context.cacheDir, "thumbnails")
            if (!thumbDir.exists()) {
                thumbDir.mkdirs()
            }
            val file = File(thumbDir, "${audioSrcFileMd5}.thumbnail")
            file.writeBytes(it)
            file.path
        }

        MediaMetaData(
            title = title,
            album = album,
            author = author,
            mimeType = mimeType,
            thumbnailFilePath = thumbnail,
            duration = duration.toLong()
        )
    }
}

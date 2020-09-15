package net.julianchu.momoecho.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.utils.toReadableShort
import java.io.File
import java.io.FileInputStream

class TrackAdapter(
    val context: Context,
    private val coroutineScope: CoroutineScope,
    private val data: MutableList<Track>,
    private val clickListener: (View) -> Unit = {},
    private val longClickListener: (View) -> Unit = {}
) : RecyclerView.Adapter<TrackViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val v = inflater.inflate(R.layout.list_item_track, parent, false)
        return TrackViewHolder(v, clickListener, longClickListener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = data[position]
        holder.itemView.tag = track
        holder.title.text = track.displayTitle
        holder.filename.text = "${track.filename}"
        holder.clips.text = "${track.clipsNumber} clips"
        holder.duration.text = track.duration.toReadableShort()
        holder.album.text = if (track.album.isEmpty()) "Unknown album" else track.album

        coroutineScope.launch {
            track.thumbnailFilePath?.let {
                val bitmap = decodeBitmap(it)
                if (bitmap != null) {
                    holder.icon.setImageBitmap(bitmap)
                }
            }
        }
    }

    private suspend fun decodeBitmap(
        thumbnailPath: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (thumbnailPath != null) {
            try {
                val file = File(thumbnailPath)
                if (file.exists() && file.canRead()) {
                    val inputStream = FileInputStream(file)
                    val byteArray = inputStream.readBytes()
                    return@withContext BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}

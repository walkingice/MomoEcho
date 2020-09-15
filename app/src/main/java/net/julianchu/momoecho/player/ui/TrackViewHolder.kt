package net.julianchu.momoecho.player.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.julianchu.momoecho.R

class TrackViewHolder(
    itemView: View,
    clickListener: (View) -> Unit = {},
    longClickListener: (View) -> Unit = {}
) : RecyclerView.ViewHolder(itemView) {

    val title: TextView = itemView.findViewById(R.id.track_title)
    val album: TextView = itemView.findViewById(R.id.track_album)
    val filename: TextView = itemView.findViewById(R.id.track_filename)
    val clips : TextView = itemView.findViewById(R.id.track_clips)
    val duration : TextView = itemView.findViewById(R.id.track_duration)
    val icon: ImageView = itemView.findViewById(R.id.track_thumbnail)

    init {
        itemView.setOnClickListener {
            clickListener(itemView)
        }

        itemView.setOnLongClickListener {
            longClickListener(itemView)
            true
        }
    }
}

package net.julianchu.momoecho.player.ui

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackViewHolder(
    itemView: View,
    clickListener: (View) -> Unit = {},
    longClickListener: (View) -> Unit = {}
) : RecyclerView.ViewHolder(itemView) {

    val title: TextView = itemView.findViewById(android.R.id.title)
    val clips: TextView = itemView.findViewById(android.R.id.text1)

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
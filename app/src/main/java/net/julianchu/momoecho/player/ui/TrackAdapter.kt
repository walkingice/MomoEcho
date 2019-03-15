package net.julianchu.momoecho.player.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.Track

class TrackAdapter(
    val context: Context,
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
        holder.title.text = track.displayText
        holder.clips.text = "${track.clipsNumber} clips"
    }
}
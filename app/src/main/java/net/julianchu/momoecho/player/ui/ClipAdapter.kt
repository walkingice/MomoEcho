package net.julianchu.momoecho.player.ui

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.Clip

class ClipAdapter(
    context: Context,
    private val data: MutableList<Clip>,
    private val toggleListener: (View) -> Unit,
    private val clickListener: (View) -> Unit,
    private val longClickListener: (View) -> Unit
) :
    RecyclerView.Adapter<ClipViewHolder>() {
    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val v = inflater.inflate(R.layout.list_item_clip, parent, false)
        return ClipViewHolder(v, toggleListener, clickListener, longClickListener)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        val clip = data[position]
        holder.itemView.elevation = if (clip.isPlaying) 40f else 15f
        holder.content.text = clip.content
        holder.extra.visibility = if (clip.isExpanding) View.VISIBLE else View.GONE
        holder.title.text = clip.displayText
        holder.itemView.tag = clip
        holder.checkbox.isChecked = clip.isEnabled
        holder.title.setTypeface(
            null,
            if (clip.content.isNotBlank()) Typeface.BOLD else Typeface.NORMAL
        )
        holder.hint.isEnabled = clip.isPlaying
    }
}

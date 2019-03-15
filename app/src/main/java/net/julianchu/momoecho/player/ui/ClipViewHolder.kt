package net.julianchu.momoecho.player.ui

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.Clip

class ClipViewHolder(
    itemView: View,
    toggleListener: (View) -> Unit,
    clickListener: (View) -> Unit,
    longClickListener: (View) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    val title = itemView.findViewById<TextView>(android.R.id.title)!!
    val checkbox = itemView.findViewById<CheckBox>(android.R.id.checkbox)!!
    val hint = itemView.findViewById<View>(R.id.list_item_clip_hint)!!
    val extra = itemView.findViewById<View>(android.R.id.extractArea)!!
    val content = itemView.findViewById<TextView>(android.R.id.content)!!

    init {
        itemView.setOnClickListener {
            var expanding = false
            if (extra.visibility == View.VISIBLE) {
                extra.visibility = View.GONE
            } else {
                extra.visibility = View.VISIBLE
                expanding = true
            }
            (it.tag as? Clip)?.let { clip ->
                clip.isExpanding = expanding
            }
            clickListener(itemView)
        }

        itemView.setOnLongClickListener {
            longClickListener(itemView)
            true
        }

        checkbox.setOnClickListener {
            toggleListener(itemView)
        }
    }
}

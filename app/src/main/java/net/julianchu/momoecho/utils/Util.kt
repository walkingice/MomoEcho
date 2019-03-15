package net.julianchu.momoecho.utils

import net.julianchu.momoecho.model.Clip

fun sortClips(clips: MutableList<Clip>) {
    clips.sortBy { it.startTime }
}

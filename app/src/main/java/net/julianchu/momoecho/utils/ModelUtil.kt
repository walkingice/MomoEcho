package net.julianchu.momoecho.utils

import android.os.Bundle
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.model.Clip


fun Bundle.setClip(clip: Clip): Bundle {
    this.putParcelable(Const.EXTRA_KEY_CLIP, clip)
    return this
}

fun Bundle.getClip(): Clip? {
    return this.getParcelable(Const.EXTRA_KEY_CLIP)
}

package net.julianchu.momoecho.utils

import android.content.Context
import android.util.DisplayMetrics

object ViewUtil {
    fun convertDpToPixel(context: Context, dp: Float): Float {
        val densityDpi = context.resources.displayMetrics.densityDpi
        return dp * (densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    }
}

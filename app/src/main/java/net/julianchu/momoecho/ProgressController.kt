package net.julianchu.momoecho

import android.widget.ProgressBar
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ProgressController(
    private val bar: ProgressBar
) {
    private var timer: Timer? = null

    fun startAt(position: Int) {
        timer?.cancel()
        bar.progress = position
        timer = buildTimer(position)
    }

    fun pauseAt(position: Int) {
        timer?.cancel()
        timer = null
        bar.progress = position
    }

    fun stop() {
        timer?.cancel()
        timer = null
        bar.progress = 0
    }

    private fun buildTimer(start: Int): Timer {
        val period = 200
        var current = start
        return fixedRateTimer(
            daemon = true,
            period = period.toLong(),
            action = {
                current += period
                bar.progress = current
            }
        )
    }
}
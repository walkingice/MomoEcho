package net.julianchu.momoecho

import android.widget.ProgressBar
import net.julianchu.momoecho.widget.VerticalSeekBar
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ProgressController(
    private val progressBar: ProgressBar? = null,
    private val seekBar: VerticalSeekBar? = null
) {
    private var timer: Timer? = null

    fun startAt(position: Int) {
        timer?.cancel()
        progressBar?.progress = position
        seekBar?.progress = position
        timer = buildTimer(position)
    }

    fun pauseAt(position: Int) {
        timer?.cancel()
        timer = null
        progressBar?.progress = position
        seekBar?.progress = position
    }

    fun stop() {
        timer?.cancel()
        timer = null
        progressBar?.progress = 0
        seekBar?.progress = 0
    }

    private fun buildTimer(start: Int): Timer {
        val period = 200
        var current = start
        return fixedRateTimer(
            daemon = true,
            period = period.toLong(),
            action = {
                current += period
                progressBar?.progress = current
                seekBar?.progress = current
            }
        )
    }
}

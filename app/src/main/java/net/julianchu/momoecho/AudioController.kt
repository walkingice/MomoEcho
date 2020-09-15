package net.julianchu.momoecho

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.*
import net.julianchu.momoecho.model.Clip
import java.util.*

class AudioController(private val player: MediaPlayer) {

    init {
        player.setOnErrorListener { player, _, _ ->
            playerReset()
            true
        }
    }

    private val playbackScope = CoroutineScope(newSingleThreadContext("playback"))
    private var scheduledCallbackJob: Job? = null
    private var progressTimer: Timer? = null
    private var currentClip: Clip? = null
    private var clipPlaybackListener: (() -> Unit) = {}
    private var state = State.IDLE

    fun setDataSource(context: Context, uri: Uri) {
        if (state != State.IDLE) {
            cleanUp()
        }
        playerPrepare(context, uri)
    }

    fun startPlayback(
        clip: Clip,
        finishCallback: () -> Unit = {}
    ) {
        currentClip = clip
        player.seekTo(clip.startTime)
        playerStart()
        changeState(State.STARTED)
        val length = (clip.endTime - clip.startTime).toLong()
        scheduledCallbackJob = launchDelayCallback(playbackScope, length) {
            pausePlayback()
            clipPlaybackListener()
            finishCallback()
        }
    }

    fun stopPlayback() {
        cancelTimer()
        currentClip = null
        playerStop()
    }

    fun resumePlayback(
        finishCallback: () -> Unit = {}
    ) {
        cancelTimer()
        val clip = currentClip
        if (clip != null) {
            val length = (clip.endTime - player.currentPosition).toLong()
            scheduledCallbackJob = launchDelayCallback(playbackScope, length) {
                pausePlayback()
                clipPlaybackListener()
                finishCallback()
            }
        }
        playerResume()
    }

    fun pausePlayback() {
        cancelTimer()
        playerPause()
    }

    fun setClipPlaybackListener(listener: (() -> Unit)?) {
        clipPlaybackListener = listener ?: {}
    }

    fun getCurrentPosition(): Int = player.currentPosition

    fun getDuration(): Int = player.duration

    private fun cleanUp() {
        cancelTimer()
        playerReset()
    }

    private fun cancelTimer() {
        if (scheduledCallbackJob?.isActive == true) {
            scheduledCallbackJob?.cancel()
        }
        scheduledCallbackJob = null

        if (progressTimer != null) {
            progressTimer?.cancel()
        }
        progressTimer = null
    }

    /* Player control functions */

    private fun playerReset() {
        player.reset()
        changeState(State.IDLE)
    }

    private fun playerPrepare(context: Context, uri: Uri) {
        player.setDataSource(context, uri)
        player.prepare()
        changeState(State.PREPARED)
    }

    private fun playerStart() {
        if (state == State.PREPARED || state == State.PAUSED) {
            player.start()
            changeState(State.STARTED)
        }
    }

    private fun playerPause() {
        if (state == State.STARTED) {
            player.pause()
            changeState(State.PAUSED)
        }
    }

    private fun playerResume() {
        if (state == State.PAUSED) {
            player.start()
            changeState(State.STARTED)
        }
    }

    // not literal stop
    private fun playerStop() {
        if (state == State.STARTED) {
            player.pause()
        }
        player.seekTo(0)
        changeState(State.PREPARED)
    }

    private fun changeState(newState: State) {
        if (state != newState) {
            state = newState
        }
    }

    private fun launchDelayCallback(
        scope: CoroutineScope,
        period: Long,
        callback: () -> Unit = {}
    ): Job {
        return scope.launch {
            delay(period)
            launch(context = Dispatchers.Main) {
                callback()
            }
        }
    }

    enum class State {
        PREPARED,
        STARTED,
        PAUSED,
        IDLE
    }
}

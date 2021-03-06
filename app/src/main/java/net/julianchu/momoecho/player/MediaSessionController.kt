package net.julianchu.momoecho.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.julianchu.momoecho.AudioController
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.media.PlaybackState
import net.julianchu.momoecho.media.PlaybackState.PAUSED
import net.julianchu.momoecho.media.PlaybackState.PLAYING
import net.julianchu.momoecho.media.PlaybackState.SKIPPING_TO_NEXT
import net.julianchu.momoecho.media.PlaybackState.STOPPED
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.utils.getClip
import net.julianchu.momoecho.utils.sortClips

class MediaSessionController(
    private val context: Context,
    private val mediaSession: MediaSessionCompat,
    private val funcStartFgService: () -> Unit,
    private val funcStopFgService: () -> Unit
) : MediaSessionCompat.Callback() {

    private val audioCtrl: AudioController = AudioController(MediaPlayer())
    private var current: Clip? = null
    private var currentTrackId = -1L
    private val clips = mutableListOf<Clip>()
    private var scheduleNextJob: Job? = null

    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val noisyReceiver = BecomingNoisyReceiver()
    private var noisyReceiverRegistered = false

    private val store: StoreDispatcher =
        Room.databaseBuilder(
            context.applicationContext,
            RoomStore::class.java,
            RoomStore.DB_NAME
        )
            .build()
            .let { StoreDispatcher(it) }

    private val clipPlaybackListener: (() -> Unit) = {
        val nextClip = findNextClip(current, clips)
        if (nextClip == null) {
            onStop()
        } else {
            var period = 0
            current?.let { c -> period = c.endTime - c.startTime }
            current = nextClip
            mediaSession.setPlaybackState(SKIPPING_TO_NEXT.asCompat(nextClip.startTime.toLong()))
            // TODO: avoid using global scope
            scheduleNextJob = GlobalScope.launch {
                delay(period.toLong())
                scheduleNextJob = null
                mediaSession.setPlaybackState(PLAYING.asCompat(nextClip.startTime.toLong()))
                audioCtrl.startPlayback(nextClip)
            }
        }
    }

    override fun onPlay() {
        super.onPlay()
        if (!noisyReceiverRegistered) {
            noisyReceiverRegistered = true
            context.registerReceiver(noisyReceiver, intentFilter)
        }

        audioCtrl.setClipPlaybackListener(clipPlaybackListener)
        if (current == null) {
            findNextClip(current, clips)?.let {
                current = it
                audioCtrl.startPlayback(it)
                mediaSession.setPlaybackState(PLAYING.asCompat(position = it.startTime.toLong()))
                funcStartFgService()
            }
        } else {
            audioCtrl.resumePlayback()
            mediaSession.setPlaybackState(PLAYING.asCompat(audioCtrl.getCurrentPosition().toLong()))
        }

    }

    override fun onStop() {
        super.onStop()
        if (noisyReceiverRegistered) {
            noisyReceiverRegistered = false
            context.unregisterReceiver(noisyReceiver)
        }

        audioCtrl.stopPlayback()
        scheduleNextJob?.let {
            // TODO: avoid using global scope
            GlobalScope.launch { it.cancel() }
        }
        audioCtrl.setClipPlaybackListener(null)
        current = null
        mediaSession.setPlaybackState(STOPPED.asCompat())
        funcStopFgService()
    }

    override fun onPause() {
        super.onPause()
        pausePlayback()
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        super.onPrepareFromUri(uri, extras)
        uri ?: return
        audioCtrl.setDataSource(context, uri)
        mediaSession.setPlaybackState(STOPPED.asCompat())
        resetCurrentTrack(extras)
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        when (command) {
            Const.COMMAND_PLAYBACK_ONE_CLIP -> extras?.let { playbackOneClip(it) }
            Const.COMMAND_REFRESH_CLIP -> extras?.let { refreshClip(it) }
            Const.COMMAND_UPDATE_CLIP -> extras?.let { updateClip(it) }
            Const.COMMAND_REMOVE_CLIP -> extras?.let { removeClip(it) }
            Const.COMMAND_SET_TRACK -> extras?.let { updateClips(it) }
            Const.COMMAND_GET_INFO -> sendInfo()
        }
    }

    private fun resetCurrentTrack(extras: Bundle?) {
        if (extras == null || !extras.containsKey(Const.EXTRA_KEY_TRACK)) {
            return
        }
        currentTrackId = extras.getLong(Const.EXTRA_KEY_TRACK)
        resetClipsByTrack(currentTrackId)
    }

    private fun resetClipsByTrack(trackId: Long) {
        GlobalScope.launch {
            val queriedClips = store.queryClips(trackId)
            clips.clear()
            clips.addAll(queriedClips)
            sortClips(clips)
            sendInfo()
        }
    }

    private fun removeClip(extras: Bundle) {
        val id = extras.getLong(Const.EXTRA_KEY_CLIP)
        val found = pickClip(id) ?: return
        clips.remove(found)
        sortClips(clips)
        mediaSession.sendSessionEvent(
            Const.EVENT_UPDATE_CLIP,
            createBundle(id)
        )
    }

    private fun playbackOneClip(extras: Bundle) {
        val clip = extras.getClip() ?: return
        onStop()
        mediaSession.setPlaybackState(PLAYING.asCompat(clip.startTime.toLong()))
        audioCtrl.startPlayback(clip) {
            mediaSession.setPlaybackState(PAUSED.asCompat(clip.endTime.toLong()))
        }
    }

    private fun refreshClip(extras: Bundle) {
        if (!extras.containsKey(Const.EXTRA_KEY_CLIP)) {
            return
        }
        val id = extras.getLong(Const.EXTRA_KEY_CLIP, -1)
        GlobalScope.launch {
            val clip = store.getClip(id)
            val found = clip?.let { pickClip(it.id) }
            if (clip != null && found != null) {
                found.merge(clip)
                mediaSession.sendSessionEvent(
                    Const.EVENT_UPDATE_CLIP,
                    createBundle(found.id)
                )
            }
        }
    }

    private fun updateClip(extras: Bundle) {
        val id = extras.getLong(Const.EXTRA_KEY_CLIP, -1)
        if (id != -1L) {
            GlobalScope.launch {
                val clip = store.getClip(id)
                if (clip != null) {
                    val found = pickClip(clip.id)
                    if (found != null) {
                        found.merge(clip)
                    } else {
                        clips.add(clip)
                    }
                    sortClips(clips)
                    mediaSession.sendSessionEvent(
                        Const.EVENT_UPDATE_CLIP,
                        createBundle(clip.id)
                    )
                }
            }
        }
    }

    private fun updateClips(extras: Bundle) {
        GlobalScope.launch {
            val queriedClips = store.queryClips(extras.getLong(Const.EXTRA_KEY_TRACK))
            clips.clear()
            for (clip in queriedClips) {
                clips.add(clip)
            }
            sortClips(clips)
            mediaSession.sendSessionEvent(
                Const.EVENT_UPDATE_CLIPS,
                createBundle(null)
            )
        }
    }

    private fun sendInfo() {
        val extras = createBundle(null)
        extras.putInt(Const.EXTRA_KEY_INFO_DURATION, audioCtrl.getDuration())
        mediaSession.sendSessionEvent(
            Const.EVENT_UPDATE_INFO,
            extras
        )
    }

    private fun pausePlayback() {
        audioCtrl.setClipPlaybackListener(null)
        audioCtrl.pausePlayback()
        mediaSession.setPlaybackState(PAUSED.asCompat(audioCtrl.getCurrentPosition().toLong()))
    }

    private fun pickClip(id: Long): Clip? {
        for (c in clips) {
            if (c.id == id) {
                return c
            }
        }
        return null
    }

    @VisibleForTesting
    internal fun findNextClip(current: Clip?, list: MutableList<Clip>): Clip? {
        if (current == null || !list.contains(current)) {
            return list.firstOrNull { it.isEnabled }
        }

        val enabled = list.filter { it.isEnabled }
        when (enabled.size) {
            0 -> return null
            1 -> return enabled[0]
        }

        // fresh = not played
        val fresh = enabled.filter { it.startTime >= current.startTime && it != current }
        return when (fresh.size) {
            0 -> enabled[0] // reach end, return first clip to play
            else -> fresh[0]
        }
    }

    private fun createBundle(clipId: Long?): Bundle {
        return Bundle().also {
            it.putLong(Const.EXTRA_KEY_TRACK, currentTrackId)
            if (clipId != null) {
                it.putLong(Const.EXTRA_KEY_CLIP, clipId)
            }
        }
    }

    private fun PlaybackState.asCompat(
        position: Long = 0,
        playbackSpeed: Float = 1.0f
    ): PlaybackStateCompat {
        val clipId: Long = current?.id ?: -1
        val builder = PlaybackStateCompat.Builder()
            .setState(this.compatInt, position, playbackSpeed)
            .setExtras(createBundle(clipId))

        return builder.build()
    }

    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onStop()
            }
        }
    }
}

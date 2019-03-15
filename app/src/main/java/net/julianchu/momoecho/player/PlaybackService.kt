package net.julianchu.momoecho.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import net.julianchu.momoecho.MainActivity
import net.julianchu.momoecho.R

private const val TAG = "PlaybackService"
private const val MEDIA_ID_ROOT = "media_id_root"
private const val FG_NOTIFICATION_ID = 0xDEFA
private const val INTENT_REQ_CODE = 0x9527
private const val CHANNEL_ID = "main_channel_id"

class PlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private var currentFg = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, TAG)
        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }
        val callback = MediaSessionController(
            this,
            mediaSession,
            { startToForeground() },
            { stopFromForeground() }
        )
        mediaSession.setCallback(callback)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
        mediaSession.isActive = true
        this.sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        result.sendResult(mediaItems)
    }

    private fun startToForeground() {
        if (currentFg) {
            return
        }
        currentFg = true

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configForegroundChannel(this)
            CHANNEL_ID
        } else {
            "not_used_notification_id"
        }

        val intent = PendingIntent.getActivity(
            applicationContext,
            INTENT_REQ_CODE,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MomoEcho")
            .setSmallIcon(R.drawable.app_logo)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(intent)
            .build()

        startForeground(FG_NOTIFICATION_ID, notification)

        val pm: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "momoecho:playservice")
        wakeLock?.acquire()
    }

    private fun stopFromForeground() {
        if (!currentFg) {
            return
        }
        currentFg = false
        stopForeground(true)
        wakeLock?.release()
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configForegroundChannel(context: Context) {
        val mgr: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName: String = context.getString(R.string.app_name)
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        mgr.createNotificationChannel(notificationChannel)
    }

    private class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let {
                val action = state.actions
                val state = state.playbackState
                val s = state.toString()
            }
        }
    }
}

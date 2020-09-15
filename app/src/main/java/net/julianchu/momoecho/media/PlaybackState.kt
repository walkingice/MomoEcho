package net.julianchu.momoecho.media

import android.support.v4.media.session.PlaybackStateCompat

enum class PlaybackState(val compatInt: Int) {
    PLAYING(PlaybackStateCompat.STATE_PLAYING),
    PAUSED(PlaybackStateCompat.STATE_PAUSED),
    STOPPED(PlaybackStateCompat.STATE_STOPPED),
    SKIPPING_TO_NEXT(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT),
    NONE(PlaybackStateCompat.STATE_NONE);
}

package net.julianchu.momoecho

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track

/**
 * A wrapper to run DB operation in another thread, and invoke callback in main thread
 */
class StoreDispatcher(
    private val store: RoomStore
) {
    private val dbScope = CoroutineScope(newSingleThreadContext("db"))

    fun addTrack(
        track: Track,
        callback: (Boolean) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.addTrack(track)
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun getTracks(
        callback: (MutableList<Track>) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.getTracks()
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun removeTrack(
        trackId: Long,
        callback: (Boolean) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.removeTrack(trackId)
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun upsertClip(
        clip: Clip,
        callback: (Boolean) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.upsertClip(clip)
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun removeClip(
        id: Long,
        callback: () -> Unit = {}
    ) {
        dbScope.launch {
            store.removeClip(id)
            launch(context = Dispatchers.Main) {
                callback()
            }
        }
    }

    fun removeClipsOfTrack(
        trackId: Long,
        callback: () -> Unit = {}
    ) {
        dbScope.launch {
            store.removeClipsOfTrack(trackId)
            launch(context = Dispatchers.Main) {
                callback()
            }
        }
    }

    fun getClip(
        id: Long,
        callback: (Clip?) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.getClip(id)
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun getClips(
        callback: (List<Clip>) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.getClips()
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun queryClips(
        trackId: Long,
        callback: (List<Clip>) -> Unit = {}
    ) {
        dbScope.launch {
            val result = store.queryClips(trackId)
            launch(context = Dispatchers.Main) {
                callback(result)
            }
        }
    }
}
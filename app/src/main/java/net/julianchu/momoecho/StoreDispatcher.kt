package net.julianchu.momoecho

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.AmplitudeDiagram
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track

/**
 * A wrapper to run DB operation in another thread, and invoke callback in main thread
 */
class StoreDispatcher(private val store: RoomStore) {

    suspend fun addTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        store.addTrack(track)
    }

    suspend fun getTracks(): MutableList<Track> = withContext(Dispatchers.IO) {
        store.getTracks()
    }

    suspend fun removeTrack(trackId: Long): Boolean = withContext(Dispatchers.IO) {
        store.removeTrack(trackId)
    }

    suspend fun upsertClip(clip: Clip): Boolean = withContext(Dispatchers.IO) {
        store.upsertClip(clip)
    }

    suspend fun removeClip(id: Long) = withContext(Dispatchers.IO) {
        store.removeClip(id)
    }

    suspend fun removeClipsOfTrack(trackId: Long) = withContext(Dispatchers.IO) {
        store.removeClipsOfTrack(trackId)
    }

    suspend fun getClip(id: Long): Clip? = withContext(Dispatchers.IO) {
        store.getClip(id)
    }

    suspend fun getClips(): List<Clip> = withContext(Dispatchers.IO) {
        store.getClips()
    }

    suspend fun queryClips(trackId: Long): List<Clip> = withContext(Dispatchers.IO) {
        store.queryClips(trackId)
    }

    suspend fun addAmplitude(
        amplitudeDiagram: AmplitudeDiagram
    ): Boolean = withContext(Dispatchers.IO) {
        store.addAmplitude(amplitudeDiagram)
    }

    suspend fun getAmplitude(md5: String): AmplitudeDiagram? = withContext(Dispatchers.IO) {
        store.getAmplitude(md5)
    }

    suspend fun getAmplitudesMd5(): List<String> = withContext(Dispatchers.IO) {
        store.getAmplitudesMd5()
    }

    suspend fun removeAmplitude(md5: String) = withContext(Dispatchers.IO) {
        store.removeAmplitude(md5)
    }
}

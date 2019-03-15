package net.julianchu.momoecho.db.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track

@Database(
    entities = arrayOf(
        Track::class,
        Clip::class
    ),
    version = 1
)
@TypeConverters(TypeConverter::class)
abstract class RoomStore : RoomDatabase() {

    private val trackDao = getTrackDao()
    private val clipDao = getClipDao()

    fun addTrack(track: Track): Boolean {
        trackDao.addTrack(track)
        return true
    }

    fun getTracks(): MutableList<Track> {
        return trackDao.getAll().toMutableList()
    }

    fun removeTrack(trackId: Long): Boolean {
        val removed = trackDao.removeTrack(trackId)
        return removed > 0
    }

    fun upsertClip(clip: Clip): Boolean {
        return clip
            .apply { clipDao.addClip(this) }
            .run { clipDao.updateClip(this) != -1 }
    }

    fun removeClip(id: Long) {
        clipDao.removeClip(id)
    }

    fun removeClipsOfTrack(trackId: Long) {
        clipDao.removeClipsOfTrack(trackId)
    }

    fun getClip(id: Long): Clip {
        return clipDao.getClip(id)
    }

    fun getClips(): List<Clip> {
        return clipDao.getClips()
    }

    fun queryClips(trackId: Long): List<Clip> {
        return clipDao.queryClips(trackId)
    }

    abstract fun getTrackDao(): TrackDao
    abstract fun getClipDao(): ClipDao

    companion object {
        const val DB_NAME = "room_db"
    }
}

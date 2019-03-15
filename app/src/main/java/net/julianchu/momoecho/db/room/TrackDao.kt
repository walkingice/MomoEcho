package net.julianchu.momoecho.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.julianchu.momoecho.model.Track

@Dao
interface TrackDao {
    @Query("DELETE FROM tracks WHERE id = :trackId")
    fun removeTrack(trackId: Long): Int

    @Query("SELECT * from tracks")
    fun getAll(): List<Track>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addTrack(track: Track)
}
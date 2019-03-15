package net.julianchu.momoecho.db.room

import androidx.room.*
import net.julianchu.momoecho.model.Clip

@Dao
interface ClipDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addClip(clip: Clip): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun updateClip(clip: Clip): Int

    @Query("DELETE FROM clips WHERE id = :id")
    fun removeClip(id: Long): Int

    @Query("DELETE FROM clips WHERE trackId = :trackId")
    fun removeClipsOfTrack(trackId: Long): Int

    @Query("SELECT * FROM clips WHERE id= :id")
    fun getClip(id: Long): Clip

    @Query("SELECT * FROM clips")
    fun getClips(): List<Clip>

    @Query("SELECT * FROM clips WHERE trackId = :trackId")
    fun queryClips(trackId: Long): List<Clip>
}
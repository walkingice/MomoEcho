package net.julianchu.momoecho.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.julianchu.momoecho.model.AmplitudeDiagram

@Dao
interface AmplitudeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addAmplitudeDiagram(amplitude: AmplitudeDiagram)

    @Query("SELECT * FROM amplitudes_diagram WHERE md5 = :md5")
    fun getAmplitudeDiagram(md5: String): AmplitudeDiagram?

    @Query("SELECT md5 FROM amplitudes_diagram")
    fun getAmplitudeDiagramsMd5(): List<String>

    @Query("DELETE FROM amplitudes_diagram WHERE md5 = :md5")
    fun removeAmplitudeDiagram(md5: String)
}

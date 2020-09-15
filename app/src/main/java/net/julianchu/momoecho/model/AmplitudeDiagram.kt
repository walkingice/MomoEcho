package net.julianchu.momoecho.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// audio length = windowLength * left.size
@Entity(tableName = "amplitudes_diagram")
class AmplitudeDiagram(
    @PrimaryKey
    val md5: String,

    val windowLength: Float,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val left: IntArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val right: IntArray,

    val createdAt: Date = Date()
)

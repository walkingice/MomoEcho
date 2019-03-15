package net.julianchu.momoecho.db.room

import android.net.Uri
import androidx.room.TypeConverter
import java.util.Date

class TypeConverter {
    @TypeConverter
    fun timestamp2Date(timestamp: Long): Date = Date(timestamp)

    @TypeConverter
    fun date2Timestamp(date: Date): Long = date.time

    @TypeConverter
    fun string2Uri(str: String): Uri = Uri.parse(str)

    @TypeConverter
    fun uri2String(uri: Uri): String = uri.toString()
}

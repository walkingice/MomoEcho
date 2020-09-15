package net.julianchu.momoecho.db.room

import android.net.Uri
import androidx.room.TypeConverter
import java.nio.ByteBuffer
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

    @TypeConverter
    fun intArray2ByteArray(intArray: IntArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(intArray.size * 4)
        val intBuffer = byteBuffer.asIntBuffer()
        val byteArray = ByteArray(byteBuffer.limit())
        intBuffer.put(intArray)
        byteBuffer.get(byteArray)
        return byteArray
    }

    @TypeConverter
    fun byteArray2IntArray(byteArray: ByteArray): IntArray {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        val intBuffer = byteBuffer.asIntBuffer()
        val intArray = IntArray(intBuffer.limit())
        intBuffer.get(intArray)
        return intArray
    }
}

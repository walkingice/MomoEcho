package net.julianchu.momoecho.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import net.julianchu.momoecho.utils.toReadable

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class Clip(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val trackId: Long,
    startTime: Int,
    endTime: Int,
    var isEnabled: Boolean = false
) : Parcelable {
    var startTime = startTime
        set(value) {
            field = value
            updateText()
        }
    var endTime = endTime
        set(value) {
            field = value
            updateText()
        }

    var content: String = ""

    @Ignore
    var displayText = "${startTime.toReadable()} - ${endTime.toReadable()}"

    @Ignore
    var isPlaying = false

    @Ignore
    var isExpanding = false

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    ) {
        content = parcel.readString()
        displayText = parcel.readString()!!
        isPlaying = parcel.readByte() != 0.toByte()
    }

    fun merge(clip: Clip) {
        startTime = clip.startTime
        endTime = clip.endTime
        isEnabled = clip.isEnabled
    }

    private fun updateText() {
        displayText = "${startTime.toReadable()} - ${endTime.toReadable()}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(trackId)
        parcel.writeInt(startTime)
        parcel.writeInt(endTime)
        parcel.writeByte(if (isEnabled) 1 else 0)
        parcel.writeString(content)
        parcel.writeString(displayText)
        parcel.writeByte(if (isPlaying) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Clip> {
        override fun createFromParcel(parcel: Parcel): Clip {
            return Clip(parcel)
        }

        override fun newArray(size: Int): Array<Clip?> {
            return arrayOfNulls(size)
        }
    }
}

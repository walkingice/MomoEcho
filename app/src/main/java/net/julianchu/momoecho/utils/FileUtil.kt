package net.julianchu.momoecho.utils

import android.content.Context
import android.net.Uri
import net.julianchu.momoecho.BuildConfig
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.lang.Boolean.parseBoolean
import java.lang.Integer.parseInt
import java.lang.Long.parseLong
import java.security.MessageDigest
import java.text.SimpleDateFormat

val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")

private const val META = "meta"
private const val TRACK = "track"
private const val CLIP = "clip"

private val regexId = Regex("id=(\\d*)")
private val regexTrackId = Regex("trackId=(\\d*)")
private val regexStartTime = Regex("startTime=(\\d*)")
private val regexEndTime = Regex("endTime=(\\d*)")
private val regexContent = Regex("content=((.|\\r\\n|\\n|\\r|)*)")
private val regexIsEnabled = Regex("isEnabled=(.*)")
private val regexUri = Regex("uri=(.*)")
private val regexCreatedAt = Regex("createdAt=(.*)")
private val regexFilename = Regex("filename=(.*)")
private val regexTitle = Regex("title=(.*)")
private val regexMd5 = Regex("md5=(.*)")
private val regexDuration = Regex("duration=(.*)")

fun calculateMd5(context: Context, uri: Uri): String? {
    val inputStream = context.contentResolver.openInputStream(uri)
    val buffer = ByteArray(1024)
    var len: Int = 0
    try {
        val digest = MessageDigest.getInstance("MD5")
        while (inputStream.read(buffer, 0, 1024).also({ len = it }) != -1) {
            digest.update(buffer, 0, len)
        }
        inputStream.close()
        return digest.digest().toHexString()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

private fun bytesToHexString(src: ByteArray?): String? {
    val stringBuilder = StringBuilder("")
    if (src == null || src.isEmpty()) {
        return null
    }
    for (i in src.indices) {
        val value: Int = src[i].toInt()
        val v: Int = value and 0xFF
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

fun writeCsv(
    file: File,
    tracks: List<Track>?,
    clips: List<Clip>?
): Boolean {
    try {
        file.bufferedWriter().use { writer ->
            // create meta data
            val csv = CSVPrinter(writer, CSVFormat.DEFAULT)
            addMeta(csv)
            tracks?.forEach { writeTrack(csv, it) }
            clips?.forEach { writeClip(csv, it) }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        return false
    }
    return true
}

fun readCsv(file: File): Triple<String, List<Track>, List<Clip>> {
    var meta = ""
    val tracks = mutableListOf<Track>()
    val clips = mutableListOf<Clip>()
    file.bufferedReader().use { reader ->
        val csv = CSVParser(reader, CSVFormat.DEFAULT)
        for (record in csv) {
            when (record[0]) {
                META -> meta = readMeta(record)
                CLIP -> clips.add(readClip(record))
                TRACK -> tracks.add(readTrack(record))
            }
        }
    }
    return Triple(meta, tracks, clips)
}


private fun addMeta(csv: CSVPrinter) {
    csv.printRecord(
        META,
        BuildConfig.APPLICATION_ID,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE
    )
}

private fun writeTrack(csv: CSVPrinter, track: Track) {
    csv.printRecord(
        TRACK,
        "id=${track.id}",
        "uri=${track.uri}",
        "createdAt=${dateFormat.format(track.createdAt)}",
        "filename=${track.filename}",
        "title=${track.title}",
        "duration=${track.duration}",
        "md5=${track.md5}"
    )
}

private fun writeClip(csv: CSVPrinter, clip: Clip) {
    csv.printRecord(
        CLIP,
        "id=${clip.id}",
        "trackId=${clip.trackId}",
        "startTime=${clip.startTime}",
        "endTime=${clip.endTime}",
        "content=${clip.content}",
        "isEnabled=${clip.isEnabled}"
    )
}

private fun readMeta(record: CSVRecord): String {
    return record.toString()
}

// XXX: this is awful implementation, it should be improved
private fun readTrack(record: CSVRecord): Track {
    val idIdx = 1
    val uriIdx = 2
    val createdAtIdx = 3
    val filenameIdx = 4
    val titleIdx = 5
    val durationIdx = 6
    val md5Idx = 7

    val id = parseLong(regexId.find(record[idIdx])!!.groups[1]!!.value)
    val uri = Uri.parse(regexUri.find(record[uriIdx])!!.groups[1]!!.value)
    val createdAt = dateFormat.parse(regexCreatedAt.find(record[createdAtIdx])!!.groups[1]!!.value)
    val filename = regexFilename.find(record[filenameIdx])!!.groups[1]!!.value
    val title = regexTitle.find(record[titleIdx])!!.groups[1]!!.value
    val md5 = regexMd5.find(record[md5Idx])!!.groups[1]!!.value
    val duration = parseLong(regexDuration.find(record[durationIdx])!!.groups[1]!!.value)
    return Track(
        id = id,
        uri = uri,
        createdAt = createdAt,
        filename = filename,
        title = title,
        duration = duration,
        md5 = md5
    )
}

// XXX: this is awful implementation, it should be improved
private fun readClip(record: CSVRecord): Clip {
    val idIdx = 1
    val trackIdIdx = 2
    val startTimeIdx = 3
    val endTimeIdx = 4
    val contentIdx = 5
    val isEnabledIdx = 6

    val id = parseLong(regexId.find(record[idIdx])!!.groups[1]!!.value)
    val trackId = parseLong(regexTrackId.find(record[trackIdIdx])!!.groups[1]!!.value)
    val startTime = parseInt(regexStartTime.find(record[startTimeIdx])!!.groups[1]!!.value)
    val endTime = parseInt(regexEndTime.find(record[endTimeIdx])!!.groups[1]!!.value)
    val content = regexContent.find(record[contentIdx])?.groups?.get(1)?.value
    val isEnabled = parseBoolean(regexIsEnabled.find(record[isEnabledIdx])!!.groups[1]!!.value)
    return Clip(
        id = id,
        trackId = trackId,
        startTime = startTime,
        endTime = endTime,
        isEnabled = isEnabled
    ).also {
        it.content = content ?: ""
    }
}


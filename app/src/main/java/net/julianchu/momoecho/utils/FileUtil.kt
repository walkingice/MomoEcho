package net.julianchu.momoecho.utils

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
private val regexName = Regex("name=(.*)")
private val regexDisplayName = Regex("displayName=(.*)")
private val regexDuration = Regex("duration=(.*)")

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
        "name=${track.name}",
        "displayName=${track.displayName}",
        "duration=${track.duration}"
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
    val nameIdx = 4
    val displayNameIdx = 5
    val durationIdx = 6

    val id = parseLong(regexId.find(record[idIdx])!!.groups[1]!!.value)
    val uri = Uri.parse(regexUri.find(record[uriIdx])!!.groups[1]!!.value)
    val createdAt = dateFormat.parse(regexCreatedAt.find(record[createdAtIdx])!!.groups[1]!!.value)
    val name = regexName.find(record[nameIdx])!!.groups[1]!!.value
    val displayName = regexDisplayName.find(record[displayNameIdx])!!.groups[1]!!.value
    val duration = parseLong(regexDuration.find(record[durationIdx])!!.groups[1]!!.value)
    return Track(
        id = id,
        uri = uri,
        createdAt = createdAt,
        name = name,
        displayName = displayName,
        duration = duration
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


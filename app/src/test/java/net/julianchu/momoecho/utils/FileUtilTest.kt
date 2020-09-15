package net.julianchu.momoecho.utils

import android.net.Uri
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.text.SimpleDateFormat

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FileUtilTest {

    private lateinit var tmpFolder: TemporaryFolder
    private lateinit var tmpDir: File
    private val tracks = mutableListOf<Track>()
    private val clips = mutableListOf<Clip>()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        tmpFolder = TemporaryFolder()
        tmpFolder.create()
        tmpDir = tmpFolder.newFolder()

        val format = SimpleDateFormat("yyyy-MM-dd")
        tracks.add(
            Track(
                md5="md5",
                id = 100L,
                uri = Uri.parse("file://1"),
                createdAt = format.parse("1997-08-29"),
                filename = "Foobar",
                title = "title",
                duration = 123456L
            )
        )
        tracks.add(Track(200L, Uri.parse("file://2"), "md5_track2"))
        tracks.add(Track(300L, Uri.parse("file://3"), "md5_track3"))
        tracks.add(Track(400L, Uri.parse("file://4"), "md5_track4"))

        val trackId = tracks[0].id
        clips.add(Clip(id = 0L, trackId = trackId, startTime = 1000, endTime = 9999, isEnabled = true))
        clips.add(Clip(id = 1L, trackId = trackId, startTime = 2000, endTime = 9999, isEnabled = true))
        clips.add(Clip(id = 2L, trackId = trackId, startTime = 3000, endTime = 9999, isEnabled = false))
        Clip(id = 3L, trackId = trackId, startTime = 3000, endTime = 9999, isEnabled = false)
            .also {
                it.content = """
                    this
                    is
                    multiline
                    文句
                """.trimIndent()
                clips.add(it)
            }
    }

    @After
    @Throws(Exception::class)
    fun cleanUp() {
        tmpFolder.delete()
    }

    @Test
    fun testTempDir() {
        assertTrue(tmpDir.exists())
    }

    @Test
    fun testCsvReadWrite() {
        val file = File(tmpDir, "output.csv")
        writeCsv(file, tracks, clips)
        val result = readCsv(file)
        val storedTracks = result.second
        assertEquals(tracks.size, storedTracks.size)
        storedTracks[0].let {
            assertEquals(tracks[0].id, it.id)
            assertEquals(tracks[0].uri, it.uri)
            assertEquals(tracks[0].createdAt, it.createdAt)
            assertEquals(tracks[0].filename, it.filename)
            assertEquals(tracks[0].title, it.title)
            assertEquals(tracks[0].duration, it.duration)
        }

        val storedClips = result.third
        assertEquals(clips.size, storedClips.size)

        assertEquals(clips[0].id, storedClips[0].id)
        assertEquals(clips[0].trackId, storedClips[0].trackId)
        assertEquals(clips[0].startTime, storedClips[0].startTime)
        assertEquals(clips[0].endTime, storedClips[0].endTime)
        assertEquals(clips[0].content, storedClips[0].content)
        assertEquals(clips[3].id, storedClips[3].id)
        assertEquals(clips[3].trackId, storedClips[3].trackId)
        assertEquals(clips[3].startTime, storedClips[3].startTime)
        assertEquals(clips[3].endTime, storedClips[3].endTime)
        assertEquals(clips[3].content, storedClips[3].content)
    }
}

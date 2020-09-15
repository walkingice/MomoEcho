package net.julianchu.momoecho.player

import android.support.v4.media.session.MediaSessionCompat
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.utils.sortClips
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MediaSessionControllerTest {

    private val context = RuntimeEnvironment.application
    private lateinit var ctrl: MediaSessionController
    private val trackId = 123L
    private val clips = mutableListOf<Clip>()

    @Before
    fun onBefore() {
        val mockMediaSession = mock(MediaSessionCompat::class.java)
        ctrl = MediaSessionController(context, mockMediaSession, {}, {})
        clips.add(Clip(id = 0L, trackId = trackId, startTime = 1000, endTime = 9999, isEnabled = true))
        clips.add(Clip(id = 1L, trackId = trackId, startTime = 2000, endTime = 9999, isEnabled = true))
        clips.add(Clip(id = 2L, trackId = trackId, startTime = 3000, endTime = 9999, isEnabled = false))
        clips.add(Clip(id = 3L, trackId = trackId, startTime = 4000, endTime = 9999, isEnabled = true))
        clips.add(Clip(id = 4L, trackId = trackId, startTime = 5000, endTime = 9999, isEnabled = false))
        clips.add(Clip(id = 5L, trackId = trackId, startTime = 6000, endTime = 9999, isEnabled = false))
        clips.add(Clip(id = 6L, trackId = trackId, startTime = 7000, endTime = 9999, isEnabled = true))
    }

    @After
    fun onAfter() {
        clips.clear()
    }

    @Test
    fun testSort() {
        clips.add(Clip(id = 10L, trackId = trackId, startTime = 1500, endTime = 9999))
        sortClips(clips)
        assertEquals(0L, clips[0].id)
        assertEquals(10L, clips[1].id)
        assertEquals(1L, clips[2].id)
        assertEquals(2L, clips[3].id)
        assertEquals(3L, clips[4].id)
    }

    @Test
    fun testFindNext() {
        val outer = Clip(id = 10L, trackId = trackId, startTime = 1500, endTime = 9999)
        assertEquals(0L, ctrl.findNextClip(outer, clips)?.id)
        assertEquals(0L, ctrl.findNextClip(null, clips)?.id)
        assertEquals(1L, ctrl.findNextClip(clips[0], clips)?.id)
        assertEquals(3L, ctrl.findNextClip(clips[1], clips)?.id)
        assertEquals(6L, ctrl.findNextClip(clips[3], clips)?.id)
        // reach end, find fist
        assertEquals(0L, ctrl.findNextClip(clips[6], clips)?.id)

        clips[0].isEnabled = false
        clips[1].isEnabled = false
        clips[2].isEnabled = false
        clips[3].isEnabled = false
        clips[4].isEnabled = false
        clips[5].isEnabled = false
        clips[6].isEnabled = false
        assertNull(ctrl.findNextClip(outer, clips))
        assertNull(ctrl.findNextClip(clips[0], clips))
        assertNull(ctrl.findNextClip(clips[1], clips))
        assertNull(ctrl.findNextClip(clips[2], clips))
        assertNull(ctrl.findNextClip(clips[3], clips))
        assertNull(ctrl.findNextClip(clips[4], clips))
        assertNull(ctrl.findNextClip(clips[5], clips))
        assertNull(ctrl.findNextClip(clips[6], clips))
    }
}

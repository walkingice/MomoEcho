package net.julianchu.momoecho.utils

import android.content.res.Resources
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.MediaFormatData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class AudioUtilTest {

    private val mediaFormatData = MediaFormatData(2, 44100)
    private lateinit var res: Resources
    private lateinit var rawBuffer: ShortArray

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val appContext = InstrumentationRegistry.getTargetContext()
        res = appContext.resources
        val rawFd = res.openRawResourceFd(R.raw.sinesweepraw)
        val bis = BufferedInputStream(rawFd.createInputStream())
        rawBuffer = ShortArray((rawFd.length / 2).toInt())
        for (i in rawBuffer.indices) {
            // PCM is little endian, so low byte is read first
            val lo = bis.read()
            var hi = bis.read()
            if (hi >= 128) {
                hi -= 256
            }
            val sample = hi * 256 + lo
            rawBuffer[i] = sample.toShort()
        }
        rawFd.close()
        bis.close()
    }

    @Test
    fun testDecode() {
        val job = GlobalScope.launch {
            val mp3Fd = res.openRawResourceFd(R.raw.sinesweepmp3lame)
            val decodedArray = AudioUtil.decodeToAmplitude(mp3Fd)
            mp3Fd.close()
            assertNotNull(decodedArray)
            assertEquals(rawBuffer.size, decodedArray!!.size)
            var error: Long = 0
            for (i in rawBuffer.indices) {
                val diff = rawBuffer[i] - decodedArray[i]
                error += diff * diff
            }
            val rmse = sqrt((error / decodedArray.size).toDouble())
            val maxError = 804f
            assertTrue(rmse < maxError)
        }
        runBlocking {
            job.join()
        }
        assertTrue(rawBuffer.size > 0)
    }

    @Test
    fun testSplitAmplitudeToDualChannel() {
        val byteArrayPcm: ByteArray = byteArrayOf(0x11, 0x22, 0x66, 0x55, 0x11, 0x22, 0x66, 0x55)
        val shortArrayAmplitude = AudioUtil.pcmToAmplitude(byteArrayPcm)
        assertEquals(shortArrayAmplitude[0], 0x2211.toShort())
        assertEquals(shortArrayAmplitude[1], 0x5566.toShort())
        assertEquals(shortArrayAmplitude[2], 0x2211.toShort())
        assertEquals(shortArrayAmplitude[3], 0x5566.toShort())
        val pair = AudioUtil.splitAmplitudeToDualChannel(mediaFormatData, shortArrayAmplitude)
        val left = pair.first
        val right = pair.second
        assertEquals(left[0], 0x2211.toShort())
        assertEquals(left[1], 0x2211.toShort())
        assertEquals(right[0], 0x5566.toShort())
        assertEquals(right[1], 0x5566.toShort())
    }

    @Test
    fun testAmplitudeToPcm() {
        val amplitude =
            shortArrayOf(0x1122, 32767 /*0x7FFF*/, -32768/*0x8000*/, -32514 /*0x80FE*/)
        val pcmData = AudioUtil.amplitudeToPcm(amplitude)
        assertEquals(pcmData[0], 0x22.toByte())
        assertEquals(pcmData[1], 0x11.toByte())
        assertEquals(pcmData[2], (-1).toByte()) // 0xFF
        assertEquals(pcmData[3], 0x7F.toByte())
        assertEquals(pcmData[4], 0x00.toByte())
        assertEquals(pcmData[5], 0x80.toByte())
        assertEquals(pcmData[6], 0xFE.toByte())
        assertEquals(pcmData[7], 0x80.toByte())
    }

    @Test
    fun testPcmToAmplitude() {
        val pcm =
            byteArrayOf(0x22, 0x11, -1, 0x7F, 0x00, 0x80.toByte(), 0xFE.toByte(), 0x80.toByte())
        val amplitude = AudioUtil.pcmToAmplitude(pcm)
        assertEquals(amplitude[0], 0x1122.toShort())
        assertEquals(amplitude[1], 32767.toShort())
        assertEquals(amplitude[2], (-32768).toShort())
        assertEquals(amplitude[3], (-32514).toShort())
    }

    // @Test
    fun testGetMediaFormData() = runBlocking {
        val mp3Fd = res.openRawResourceFd(R.raw.sinesweepmp3lame)
        val mediaFormData = AudioUtil.getMediaFormatData(mp3Fd)
        assertNotNull(mediaFormData)
    }
}

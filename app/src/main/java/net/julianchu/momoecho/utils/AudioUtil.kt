package net.julianchu.momoecho.utils

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodec.CodecException
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.model.AmplitudeDiagram
import net.julianchu.momoecho.model.MediaFormatData
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

private const val TAG = "AudioUtil"

// 0.01 second is good enough for our case
private const val WINDOW_LENGTH = 0.01f

private typealias ProgressCallback = (Float) -> Unit

object AudioUtil {

    fun amplitudeToDiagram(
        md5: String,
        format: MediaFormatData,
        amplitudeData: ShortArray,
        windowLength: Float = WINDOW_LENGTH
    ): AmplitudeDiagram {
        val pair = splitAmplitudeToDualChannel(format, amplitudeData)
        val leftChannelPcmBuffer = pair.first
        val rightChannelPcmBuffer = pair.second
        val samplesPerWindow = (format.sampleRate * windowLength).toInt()

        val leftChannel = getMaxMinFromChannel(leftChannelPcmBuffer, samplesPerWindow)
        val rightChannel = getMaxMinFromChannel(rightChannelPcmBuffer, samplesPerWindow)
        return AmplitudeDiagram(
            md5 = md5,
            windowLength = windowLength,
            left = leftChannel,
            right = rightChannel
        )
    }

    private fun getMaxMinFromChannel(channelData: ShortArray, samplesPerWindow: Int): IntArray {
        // for each windows, we save lowest and highest value
        val bufferSize = (channelData.size / samplesPerWindow) * 2
        val channel = IntArray(bufferSize)
        for (i in channel.indices step 2) {
            var max = Integer.MIN_VALUE
            var min = Integer.MAX_VALUE
            for (j in 0 until samplesPerWindow) {
                val current = channelData[j + (i / 2) * samplesPerWindow].toInt()
                max = max(max, current)
                min = min(min, current)
            }
            channel[i] = max
            channel[i + 1] = min
        }
        return channel
    }

    fun pcmToAmplitude(pcm: ByteArray): ShortArray {
        val byteBuffer = ByteBuffer.wrap(pcm)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        return shortArray
    }

    fun amplitudeToPcm(amplitude: ShortArray): ByteArray {
        //val byteBuffer = ByteBuffer.allocate(it.size * 2)
        //byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        //val shortBuffer = byteBuffer.asShortBuffer()
        //shortBuffer.put(amplitudeData)
        //val channel = FileOutputStream(cacheFile).channel
        //channel.write(byteBuffer)
        //channel.close()

        val outputStream = ByteArrayOutputStream(amplitude.size * 2)
        for (i in amplitude.indices) {
            val short = amplitude[i]
            var hi = (short.toInt() and 0xFF00) shr 8
            val lo = short.toInt() and 0xFF
            // PCM is little endian, write low byte of Amplitude first
            outputStream.write(lo)
            outputStream.write(hi)
        }
        return outputStream.toByteArray()
    }

    /**
     *  Split Dual Channel PCM data
     */
    @VisibleForTesting
    fun splitAmplitudeToDualChannel(
        format: MediaFormatData,
        amplitudeData: ShortArray
    ): Pair<ShortArray, ShortArray> {
        val isDualChannel = format.channelCount == 2 // FIXME: this might be wrong.
        val byteBuffer = ByteBuffer.allocate(amplitudeData.size * 2)
        byteBuffer.asShortBuffer().put(amplitudeData)
        val shortBuffer = byteBuffer.asShortBuffer()

        val channelPcmBufferSize =
            if (isDualChannel) (shortBuffer.capacity() / 2) else shortBuffer.capacity()
        val leftChannelPcmBuffer = ShortArray(channelPcmBufferSize)
        val rightChannelPcmBuffer = ShortArray(channelPcmBufferSize)
        for (i in leftChannelPcmBuffer.indices) {
            leftChannelPcmBuffer[i] = shortBuffer.get()
            if (isDualChannel) {
                rightChannelPcmBuffer[i] = shortBuffer.get()
            } else {
                rightChannelPcmBuffer[i] = leftChannelPcmBuffer[i]
            }
        }

        return Pair(
            first = leftChannelPcmBuffer,
            second = rightChannelPcmBuffer
        )
    }

    suspend fun getMediaFormatData(
        asset: AssetFileDescriptor
    ): MediaFormatData? = withContext(Dispatchers.IO) {
        val extractor = createMediaExtractor(asset)
        val firstAudioTrack = getFirstAudioTrack(extractor)
        val format = extractor.getTrackFormat(firstAudioTrack)
        extractor.release()
        MediaFormatData(
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        )
    }

    suspend fun getMediaFormatData(
        context: Context,
        uri: Uri
    ): MediaFormatData? = withContext(Dispatchers.IO) {
        val extractor = createMediaExtractor(context, uri)
        val firstAudioTrack = getFirstAudioTrack(extractor)
        val format = extractor.getTrackFormat(firstAudioTrack)
        extractor.release()
        MediaFormatData(
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        )
    }

    suspend fun decodeToAmplitude(
        asset: AssetFileDescriptor
    ): ShortArray? = withContext(Dispatchers.IO) {
        try {
            // XXX: this function is just for unit test, set fake duration
            val duration = 1000L
            val extractor = createMediaExtractor(asset)
            val amplitudes = decode(extractor, duration)
            extractor.release()
            amplitudes
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun decodeToAmplitude(
        context: Context,
        srcFile: Uri,
        progressCallback: ProgressCallback? = null
    ): ShortArray? = withContext(Dispatchers.IO) {
        try {
            val retriever = createMediaMetaDataRetriever(context, srcFile)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val extractor = createMediaExtractor(context, srcFile)
            val amplitudes = decode(extractor, duration.toLong(), progressCallback)
            extractor.release()
            amplitudes
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createMediaExtractor(asset: AssetFileDescriptor): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
        return extractor
    }

    private fun createMediaExtractor(context: Context, srcFile: Uri): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, srcFile, emptyMap())
        return extractor
    }

    private fun createMediaMetaDataRetriever(
        context: Context,
        srcFile: Uri
    ): MediaMetadataRetriever {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, srcFile)
        return retriever
    }

    // Return array of amplitude data
    private suspend fun decode(
        extractor: MediaExtractor,
        duration: Long,
        progressCallback: ProgressCallback? = null
    ): ShortArray? = withContext(Dispatchers.IO) {
        val audioTrack = getFirstAudioTrack(extractor)
        if (audioTrack == -1) {
            null
        } else {
            decodeToMemoryAsync(extractor, audioTrack, duration, progressCallback)
            //decodeToMemorySync(extractor, audioTrack, duration)
        }
    }

    private fun getFirstAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio")) {
                return i
            }
        }
        return -1
    }

    private suspend fun decodeToMemoryAsync(
        extractor: MediaExtractor,
        audioTrackIdx: Int,
        duration: Long,
        progressCallback: ProgressCallback? = null
    ): ShortArray? {
        val format = extractor.getTrackFormat(audioTrackIdx)
        extractor.selectTrack(audioTrackIdx)
        val mime = format.getString(MediaFormat.KEY_MIME)
        val mediaCodec: MediaCodec = MediaCodec.createDecoderByType(mime)
        var decoded = ShortArray(0)
        var decodedIdx = 0

        var sawInputEOS = false
        var sawOutputEOS = false

        var currentSampleTime: Long = 0
        var lastSampleTime: Long = 0
        val durationMicroseconds = duration * 1000

        val timeout = 5000L
        var timeoutCount = 0L
        val checkPeriod = 100L
        mediaCodec.setCallback(object : MediaCodec.Callback() {

            override fun onInputBufferAvailable(codec: MediaCodec, inputBufIndex: Int) {
                if (sawInputEOS || inputBufIndex < 0) {
                    return
                }
                val dstBuf = codec.getInputBuffer(inputBufIndex)
                var sampleSize = extractor.readSampleData(dstBuf!!, 0)
                val presentationTime = if (sampleSize < 0) 0 else extractor.sampleTime
                currentSampleTime += (presentationTime - lastSampleTime)
                lastSampleTime = presentationTime

                if (progressCallback != null) {
                    // progress = 0 ~ 1
                    val progress = (currentSampleTime.toFloat() / durationMicroseconds.toFloat())
                    //Log.d(TAG, "sample: $currentSampleTime/ $durationMicroseconds = $progress %")
                    progressCallback(progress)
                }

                sawInputEOS = sawInputEOS or (sampleSize < 0)
                if (sampleSize < 0) {
                    sampleSize = 0
                }
                val flag = if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTime, flag)
                if (!sawInputEOS) {
                    extractor.advance()
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                outputBufferId: Int,
                info: BufferInfo
            ) {
                timeoutCount = 0
                if (outputBufferId < 0) {
                    return
                }
                // this is Little endian Buf
                val buf = codec.getOutputBuffer(outputBufferId)
                if (decodedIdx + (info.size / 2) >= decoded.size) {
                    decoded = decoded.copyOf(decodedIdx + (info.size / 2))
                }
                for (i in 0 until info.size step 2) {
                    // get.Short from Little endian buffer
                    // so we got amplitude data, not PCM raw data
                    decoded[decodedIdx++] = buf!!.getShort(i)
                }

                mediaCodec.releaseOutputBuffer(outputBufferId, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                }
            }

            //void onOutputFormatChanged(MediaCodec mc, MediaFormat format)
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "output format has changed to ${codec.outputFormat}")
            }

            override fun onError(codec: MediaCodec, e: CodecException) {
                Log.d(TAG, "decode error: $e")
                sawOutputEOS = true
            }
        })
        mediaCodec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */)
        mediaCodec.start();
        // wait for processing to complete
        while (!sawOutputEOS && timeoutCount < timeout) {
            delay(checkPeriod)
            timeoutCount += checkPeriod
        }
        if (timeoutCount >= timeout) {
            Log.w(TAG, "Timeout, stop")
        }
        mediaCodec.stop();
        mediaCodec.release();
        return decoded
    }

    // https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/DecoderTest.java
    private fun decodeToMemorySync(extractor: MediaExtractor, audioTrackIdx: Int): ShortArray? {
        var decoded = ShortArray(0)
        var decodedIdx = 0
        extractor.selectTrack(audioTrackIdx)

        val format = extractor.getTrackFormat(audioTrackIdx)
        val mime = format.getString(MediaFormat.KEY_MIME)
        val codec: MediaCodec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */)
        codec.start()

        val info = MediaCodec.BufferInfo()
        val kTimeOutUs: Long = 5000
        var sawInputEOS = false
        var sawOutputEOS = false
        var noOutputCounter = 0
        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++
            if (!sawInputEOS) {
                val inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs)
                if (inputBufIndex >= 0) {
                    val dstBuf = codec.getInputBuffer(inputBufIndex)
                    var sampleSize = extractor.readSampleData(dstBuf!!, 0)
                    val presentationTime = if (sampleSize < 0) 0 else extractor.sampleTime
                    sawInputEOS = sawInputEOS or (sampleSize < 0)
                    if (sampleSize < 0) {
                        sampleSize = 0
                    }
                    val flag = if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTime, flag)
                    if (!sawInputEOS) {
                        extractor.advance()
                    }
                }
            }
            val res = codec.dequeueOutputBuffer(info, kTimeOutUs)
            when {
                res >= 0 -> {
                    if (info.size > 0) {
                        noOutputCounter = 0
                    }
                    // this is Little endian Buf
                    val buf = codec.getOutputBuffer(res)
                    if (decodedIdx + (info.size / 2) >= decoded.size) {
                        decoded = decoded.copyOf(decodedIdx + (info.size / 2))
                    }
                    for (i in 0 until info.size step 2) {
                        // get.Short from Little endian buffer
                        // so we got amplitude data, not PCM raw data
                        decoded[decodedIdx++] = buf!!.getShort(i)
                    }

                    codec.releaseOutputBuffer(res, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
                res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                    Log.d(TAG, "output format has changed to ${codec.outputFormat}")
                else ->
                    Log.d(TAG, "dequeueOutputBuffer returned $res")
            }
        }
        codec.stop()
        codec.release()
        return decoded
    }
}

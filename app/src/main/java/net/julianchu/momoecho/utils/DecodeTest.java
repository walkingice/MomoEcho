package net.julianchu.momoecho.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DecodeTest {

    byte[] decodeToMemory(MediaExtractor extractor, boolean reconfigure, int audioTrack) throws IOException {
        byte[] decoded = new byte[0];
        int decodedIdx = 0;
        MediaCodec codec;

        MediaFormat format = extractor.getTrackFormat(audioTrack);
        String mime = format.getString(MediaFormat.KEY_MIME);
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        if (reconfigure) {
            codec.stop();
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();
        }
        extractor.selectTrack(audioTrack);
        // start decoding
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codec.getInputBuffer(inputBufIndex);
                    int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }
            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
            if (res >= 0) {
                //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0;
                }
                if (info.size > 0 && reconfigure) {
                    // once we've gotten some data out of the decoder, reconfigure it again
                    reconfigure = false;
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    sawInputEOS = false;
                    codec.stop();
                    codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
                    codec.start();
                    continue;
                }
                int outputBufIndex = res;
                ByteBuffer buf = codec.getOutputBuffer(outputBufIndex);
                MediaFormat outputFormat = codec.getOutputFormat(outputBufIndex);
                if (decodedIdx + (info.size) >= decoded.length) {
                    decoded = Arrays.copyOf(decoded, decodedIdx + (info.size));
                }
                for (int i = 0; i < info.size; i ++) {
                    decoded[decodedIdx++] = buf.get(i);
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
            } else {
            }
        }
        codec.stop();
        codec.release();
        return decoded;
    }

}

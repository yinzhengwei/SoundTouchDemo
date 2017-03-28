package com.smp.soundtouchandroid;

import android.media.AudioTrack;

public class AudioTrackAudioSink extends AudioTrack implements AudioSink {
    public AudioTrackAudioSink(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) throws IllegalArgumentException {
        super(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);
    }

    @Override
    public void close() {
        stop();
        flush();
        release();
    }
}

package com.smp.soundtouchandroid;

public interface OnProgressChangedListener {
    void onProgressChanged(int track, double currentPercentage, long position);

    void onTrackEnd(int track);

    void onExceptionThrown(String string);
}
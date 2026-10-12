package vn.pmgteam.games.audio;

import static org.lwjgl.openal.AL10.*;

/**
 * PCM data đã decode + OpenAL buffer handle. Immutable sau khi tạo.
 */
public class SoundBuffer {

    public final int   alBuffer;
    public final int   channels;
    public final int   sampleRate;
    public final float duration;   // seconds
    public final String name;

    public SoundBuffer(int alBuffer, int channels, int sampleRate,
                       float duration, String name) {
        this.alBuffer   = alBuffer;
        this.channels   = channels;
        this.sampleRate = sampleRate;
        this.duration   = duration;
        this.name       = name;
    }

    public void cleanup() {
        if (alBuffer != 0) alDeleteBuffers(alBuffer);
    }
}
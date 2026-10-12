package vn.pmgteam.games.audio;

import static org.lwjgl.openal.AL10.*;

import org.joml.Vector3f;

/**
 * 1 nguồn âm thanh trong không gian 3D (hoặc 2D nếu setRelative(true)).
 * Có thể tái sử dụng — set buffer, play, stop, rồi dùng lại.
 */
public class AudioSource {

    public final int alSource;

    private SoundBuffer buffer = null;
    private SoundCategory category = SoundCategory.SFX;
    private float baseGain = 1f;

    public AudioSource() {
        alSource = alGenSources();
    }

    public void setBuffer(SoundBuffer buf) {
        this.buffer = buf;
        if (buf != null) {
            alSourcei(alSource, AL_BUFFER, buf.alBuffer);
        } else {
            alSourcei(alSource, AL_BUFFER, 0);
        }
    }

    public SoundBuffer getBuffer() { return buffer; }

    public void play() {
        alSourcePlay(alSource);
    }

    public void pause() { alSourcePause(alSource); }
    public void stop()  { alSourceStop(alSource); }

    public boolean isPlaying() {
        return alGetSourcei(alSource, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void setLooping(boolean loop) {
        alSourcei(alSource, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
    }

    public void setPitch(float pitch) {
        alSourcef(alSource, AL_PITCH, pitch);
    }

    /**
     * Set base gain của source (0..1). Gain thực = baseGain × category × master.
     * Gọi applyGain() sau khi đổi category/master.
     */
    public void setBaseGain(float gain) {
        this.baseGain = Math.max(0f, gain);
        applyGain();
    }

    public float getBaseGain() { return baseGain; }

    public void setCategory(SoundCategory cat) {
        this.category = cat;
        applyGain();
    }

    public SoundCategory getCategory() { return category; }

    public void applyGain() {
        float master = AudioMaster.getCategoryVolume(SoundCategory.MASTER);
        float catVol = AudioMaster.getCategoryVolume(category);
        alSourcef(alSource, AL_GAIN, baseGain * catVol * master);
    }

    public void setPosition(Vector3f pos) {
        alSource3f(alSource, AL_POSITION, pos.x, pos.y, pos.z);
    }

    public void setPosition(float x, float y, float z) {
        alSource3f(alSource, AL_POSITION, x, y, z);
    }

    public void setVelocity(Vector3f v) {
        alSource3f(alSource, AL_VELOCITY, v.x, v.y, v.z);
    }

    public void setRelative(boolean rel) {
        alSourcei(alSource, AL_SOURCE_RELATIVE, rel ? AL_TRUE : AL_FALSE);
    }

    /** refDist: khoảng cách bắt đầu giảm âm. maxDist: ngưỡng im lặng. */
    public void setDistanceModel(float refDist, float maxDist, float rolloff) {
        alSourcef(alSource, AL_REFERENCE_DISTANCE, refDist);
        alSourcef(alSource, AL_MAX_DISTANCE, maxDist);
        alSourcef(alSource, AL_ROLLOFF_FACTOR, rolloff);
    }

    public void cleanup() {
        alDeleteSources(alSource);
    }
}
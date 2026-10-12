package vn.pmgteam.games.audio;

/**
 * Music player có crossfade. Chỉ 1 track phát 1 lúc, fade cũ → mới.
 */
public class MusicPlayer {

    private AudioSource current;
    private AudioSource fadingOut;

    private float currentFadeTarget = 1f;
    private float currentFadeTime   = 0f;
    private float currentFadeDur    = 0f;

    private float outFadeTime = 0f;
    private float outFadeDur  = 0f;
    private float outFadeStart = 1f;

    public MusicPlayer() {
        current   = new AudioSource();
        fadingOut = new AudioSource();
        current.setCategory(SoundCategory.MUSIC);
        fadingOut.setCategory(SoundCategory.MUSIC);
        current.setRelative(true);
        fadingOut.setRelative(true);
        current.setPosition(0f, 0f, 0f);
        fadingOut.setPosition(0f, 0f, 0f);
    }

    /** Phát track mới, fade out track cũ nếu có. */
    public void play(SoundBuffer buf, float fadeIn, float fadeOutOld) {
        if (buf == null) return;

        // Nếu đang phát chính track đó → không restart
        if (current.getBuffer() == buf && current.isPlaying()) return;

        // Chuyển current → fadingOut
        if (current.isPlaying()) {
            // Nếu fadingOut đang có track cũ, stop luôn
            if (fadingOut.isPlaying()) fadingOut.stop();

            // Swap: current → fadingOut
            AudioSource tmp = fadingOut;
            fadingOut = current;
            current = tmp;

            outFadeStart = fadingOut.getBaseGain();
            outFadeTime  = 0f;
            outFadeDur   = fadeOutOld;
        }

        // Setup new current
        current.stop();
        current.setBuffer(buf);
        current.setLooping(true);
        current.setBaseGain(0f);       // bắt đầu từ 0, fade in
        current.play();

        currentFadeTime   = 0f;
        currentFadeDur    = Math.max(0.01f, fadeIn);
        currentFadeTarget = 1f;
    }

    /** Fade out và stop. */
    public void stop(float fadeOut) {
        if (!current.isPlaying()) return;

        if (fadingOut.isPlaying()) fadingOut.stop();
        AudioSource tmp = fadingOut;
        fadingOut = current;
        current = tmp;

        outFadeStart = fadingOut.getBaseGain();
        outFadeTime  = 0f;
        outFadeDur   = fadeOut;
    }

    public void update(float dt) {
        // Fade in current
        if (current.isPlaying() && currentFadeTime < currentFadeDur) {
            currentFadeTime += dt;
            float t = Math.min(1f, currentFadeTime / currentFadeDur);
            float gain = currentFadeTarget * t;
            current.setBaseGain(gain);
        }

        // Fade out fadingOut
        if (fadingOut.isPlaying() && outFadeDur > 0f) {
            outFadeTime += dt;
            float t = Math.min(1f, outFadeTime / outFadeDur);
            float gain = outFadeStart * (1f - t);
            fadingOut.setBaseGain(gain);
            if (t >= 1f) {
                fadingOut.stop();
                fadingOut.setBaseGain(0f);
            }
        }
    }

    public void applyGain() {
        current.applyGain();
        fadingOut.applyGain();
    }

    public void cleanup() {
        current.cleanup();
        fadingOut.cleanup();
    }
}
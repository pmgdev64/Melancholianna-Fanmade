package vn.pmgteam.games.audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import vn.pmgteam.games.world.Camera3D;

/**
 * Singleton quản lý OpenAL device/context + listener + pool source.
 */
public final class AudioMaster {

    private static long device = NULL;
    private static long context = NULL;
    private static boolean initialized = false;

    private static final Map<SoundCategory, Float> categoryVolumes =
            new EnumMap<>(SoundCategory.class);

    /** Pool source để tái sử dụng cho SFX. */
    private static final Deque<AudioSource> sourcePool = new ArrayDeque<>();
    private static final int POOL_SIZE = 32;

    /** Source đang phát — sẽ tự recycle khi xong. */
    private static final java.util.List<AudioSource> activeSources =
            new java.util.ArrayList<>();

    private static MusicPlayer music;

    // Listener state — để không update thừa
    private static final Vector3f lastListenerPos = new Vector3f(Float.NaN);

    private AudioMaster() {}

    // ==================== LIFECYCLE ====================

    public static void init() {
        if (initialized) return;

        device = alcOpenDevice((java.nio.ByteBuffer) null);
        if (device == NULL) {
            System.err.println("[Audio] Không mở được OpenAL device — audio tắt.");
            return;
        }

        context = alcCreateContext(device, (int[]) null);
        if (context == NULL || !alcMakeContextCurrent(context)) {
            System.err.println("[Audio] Không tạo được OpenAL context.");
            alcCloseDevice(device);
            device = NULL;
            return;
        }

        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);

        // Default volumes
        for (SoundCategory cat : SoundCategory.values()) {
            categoryVolumes.put(cat, cat.getVolume());
        }

        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);

        // Pre-allocate source pool
        for (int i = 0; i < POOL_SIZE; i++) {
            AudioSource s = new AudioSource();
            s.setRelative(false);
            sourcePool.push(s);
        }

        music = new MusicPlayer();
        initialized = true;

        System.out.println("[Audio] OpenAL initialized: "
                + alcGetString(device, ALC_DEVICE_SPECIFIER));
    }

    public static void cleanup() {
        if (!initialized) return;

        if (music != null) music.cleanup();
        for (AudioSource s : sourcePool)    s.cleanup();
        for (AudioSource s : activeSources) s.cleanup();
        sourcePool.clear();
        activeSources.clear();

        if (context != NULL) {
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            context = NULL;
        }
        if (device != NULL) {
            alcCloseDevice(device);
            device = NULL;
        }
        initialized = false;
    }

    public static boolean isInitialized() { return initialized; }

    // ==================== LISTENER ====================

    /**
     * Update listener từ camera — gọi mỗi frame trong Game.update().
     * Skip nếu vị trí không đổi đáng kể.
     */
    public static void setListener(Camera3D camera) {
        if (!initialized) return;

        Vector3f pos = camera.position;
        if (lastListenerPos.equals(pos)) {
            // Chỉ update orientation nếu cần (camera có thể xoay mà pos không đổi)
        }
        lastListenerPos.set(pos);

        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListener3f(AL_VELOCITY, 0f, 0f, 0f);

        Vector3f fwd = camera.getForward();
        float[] orient = {
            fwd.x, fwd.y, fwd.z,   // at
            0f, 1f, 0f             // up
        };
        alListenerfv(AL_ORIENTATION, orient);
    }

    // ==================== VOLUME ====================

    public static float getCategoryVolume(SoundCategory cat) {
        return categoryVolumes.getOrDefault(cat, 1f);
    }

    public static void setCategoryVolume(SoundCategory cat, float vol) {
        categoryVolumes.put(cat, Math.max(0f, Math.min(1f, vol)));
        cat.setVolume(vol);

        // Re-apply cho tất cả source đang phát
        for (AudioSource s : activeSources) s.applyGain();
        for (AudioSource s : sourcePool)    s.applyGain();
        if (music != null) music.applyGain();
    }

    // ==================== PLAYBACK ====================

    /**
     * Play SFX 2D (UI, effects). Trả về AudioSource để caller có thể set pitch/loop.
     * Source sẽ tự recycle khi phát xong.
     */
    public static AudioSource playSound(SoundBuffer buf, SoundCategory cat) {
        if (!initialized || buf == null) return null;

        AudioSource s = sourcePool.poll();
        if (s == null) {
            // Pool cạn — recycle source đã xong
            recycleFinished();
            s = sourcePool.poll();
            if (s == null) {
                // Vẫn cạn — bỏ qua, tránh spam
                return null;
            }
        }

        s.setBuffer(buf);
        s.setCategory(cat);
        s.setRelative(true);       // UI/sfx 2D
        s.setPosition(0f, 0f, 0f);
        s.setLooping(false);
        s.setPitch(1f);
        s.applyGain();
        s.play();

        activeSources.add(s);
        return s;
    }

    /**
     * Play SFX 3D tại vị trí world. Source tự recycle khi xong.
     */
    public static AudioSource playSound3D(SoundBuffer buf, SoundCategory cat,
                                          float x, float y, float z) {
        if (!initialized || buf == null) return null;

        AudioSource s = sourcePool.poll();
        if (s == null) {
            recycleFinished();
            s = sourcePool.poll();
            if (s == null) return null;
        }

        s.setBuffer(buf);
        s.setCategory(cat);
        s.setRelative(false);
        s.setPosition(x, y, z);
        s.setVelocity(new Vector3f(0f, 0f, 0f));
        s.setLooping(false);
        s.setPitch(1f);
        s.setDistanceModel(1.5f, 40f, 1f);
        s.applyGain();
        s.play();

        activeSources.add(s);
        return s;
    }

    /** Gọi mỗi frame — recycle source đã phát xong. */
    public static void update(float dt) {
        if (!initialized) return;
        recycleFinished();
        if (music != null) music.update(dt);
    }

    private static void recycleFinished() {
        for (int i = activeSources.size() - 1; i >= 0; i--) {
            AudioSource s = activeSources.get(i);
            if (!s.isPlaying()) {
                s.setBuffer(null);
                s.setRelative(false);
                sourcePool.push(s);
                activeSources.remove(i);
            }
        }
    }

    // ==================== MUSIC ====================

    public static MusicPlayer getMusic() { return music; }
}
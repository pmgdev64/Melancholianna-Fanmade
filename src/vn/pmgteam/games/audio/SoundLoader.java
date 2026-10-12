package vn.pmgteam.games.audio;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Load OGG (qua stb_vorbis) và WAV (parser thủ công) → SoundBuffer.
 * Nếu file không tồn tại → trả null, không throw.
 */
public final class SoundLoader {

    private SoundLoader() {}

    /** Load từ classpath resource. Trả null nếu không tìm thấy. */
    public static SoundBuffer loadResource(String resourcePath) {
        try (InputStream is = SoundLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[Audio] Không tìm thấy: " + resourcePath);
                return null;
            }
            byte[] bytes = is.readAllBytes();

            ByteBuffer bb = memAlloc(bytes.length);
            bb.put(bytes).flip();

            try {
                String lower = resourcePath.toLowerCase();
                if (lower.endsWith(".ogg")) {
                    return decodeOGG(bb, resourcePath);
                } else if (lower.endsWith(".wav")) {
                    return decodeWAV(bb, resourcePath);
                } else {
                    System.err.println("[Audio] Định dạng không hỗ trợ: " + resourcePath);
                    return null;
                }
            } finally {
                memFree(bb);
            }
        } catch (IOException e) {
            System.err.println("[Audio] Lỗi đọc: " + resourcePath + " — " + e.getMessage());
            return null;
        }
    }

    // ==================== OGG ====================

    private static SoundBuffer decodeOGG(ByteBuffer data, String name) {
        try (var stack = stackPush()) {
            IntBuffer channels   = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);

            ShortBuffer pcm = stb_vorbis_decode_memory(data, channels, sampleRate);
            if (pcm == null) {
                System.err.println("[Audio] stb_vorbis decode fail: " + name);
                return null;
            }

            int ch   = channels.get(0);
            int rate = sampleRate.get(0);
            int totalSamples = pcm.remaining();

            int format = (ch == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            int alBuf = alGenBuffers();
            alBufferData(alBuf, format, pcm, rate);

            // stb_vorbis trả về buffer do nó malloc — phải free
            // LWJGL tự quản lý qua MemoryUtil, nhưng cần free thủ công:
            // stb_vorbis_decode_memory dùng malloc → dùng nstb_free hoặc để GC
            // LWJGL wrapper đã free cho ta khi buffer bị GC (finalizer).
            // Nếu muốn chủ động free: memFree(pcm);

            float duration = totalSamples / (float)(rate * ch);

            System.out.printf("[Audio] Loaded OGG '%s' — %.2fs %dHz %dch%n",
                    name, duration, rate, ch);

            return new SoundBuffer(alBuf, ch, rate, duration, name);
        }
    }

    // ==================== WAV ====================

    private static SoundBuffer decodeWAV(ByteBuffer data, String name) {
        data.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int startPos = data.position();

        int riff = data.getInt();
        data.getInt();  // fileSize
        int wave = data.getInt();
        if (riff != 0x46464952 || wave != 0x45564157) {
            System.err.println("[Audio] WAV header sai: " + name);
            return null;
        }

        int channels = 0, sampleRate = 0, bitsPerSample = 0, format = 0;
        ByteBuffer pcmData = null;

        while (data.remaining() >= 8) {
            int chunkId   = data.getInt();
            int chunkSize = data.getInt();

            if (chunkId == 0x20746D66) {           // "fmt "
                format        = data.getShort() & 0xFFFF;
                channels      = data.getShort() & 0xFFFF;
                sampleRate    = data.getInt();
                data.getInt();                     // byteRate
                data.getShort();                   // blockAlign
                bitsPerSample = data.getShort() & 0xFFFF;

                // Skip extension nếu có
                int remaining = chunkSize - 16;
                if (remaining > 0) data.position(data.position() + remaining);

            } else if (chunkId == 0x61746164) {    // "data"
                int cur = data.position();
                pcmData = data.slice().order(java.nio.ByteOrder.LITTLE_ENDIAN);
                pcmData.limit(chunkSize);
                data.position(cur + chunkSize);
            } else {
                data.position(data.position() + chunkSize);
            }

            // Word-align padding
            if ((chunkSize & 1) != 0 && data.remaining() > 0) data.get();
        }

        if (pcmData == null || channels == 0) {
            System.err.println("[Audio] WAV thiếu data/fmt: " + name);
            return null;
        }
        if (format != 1 || bitsPerSample != 16) {
            System.err.println("[Audio] Chỉ hỗ trợ WAV 16-bit PCM: " + name);
            return null;
        }

        int alFormat = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        int alBuf = alGenBuffers();
        alBufferData(alBuf, alFormat, pcmData, sampleRate);

        int totalSamples = pcmData.remaining() / 2;
        float duration = totalSamples / (float)(sampleRate * channels);

        System.out.printf("[Audio] Loaded WAV '%s' — %.2fs %dHz %dch%n",
                name, duration, sampleRate, channels);

        return new SoundBuffer(alBuf, channels, sampleRate, duration, name);
    }
}
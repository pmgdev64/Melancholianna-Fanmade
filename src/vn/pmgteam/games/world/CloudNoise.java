/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * 
 * Based on "Melancholianna" (メランコリアンナ) by Omochi Kobo (おもち工房)
 * Original work: https://www.omochikobo.com/
 * 
 * This is an UNOFFICIAL fan game. It is not affiliated with, endorsed,
 * sponsored, or approved by Omochi Kobo or the original creators.
 * 
 * Game Engine & original code elements:
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package vn.pmgteam.games.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

/**
 * Texture mây.
 * - Nếu có src/assets/textures/cloud.png → dùng file đó
 * - Nếu không có → sinh procedural (fallback, không đẹp bằng)
 */
public final class CloudNoise {

    public static final String FILE_PATH = "src/assets/textures/cloud.png";
    public static final int GEN_SIZE = 256;

    private CloudNoise() {}

    public static int create(long seed) {
        int w = GEN_SIZE, h = GEN_SIZE;
        byte[] data = null;

        File f = new File(FILE_PATH);
        if (f.isFile()) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    w = img.getWidth();
                    h = img.getHeight();
                    data = new byte[w * h * 4];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int argb = img.getRGB(x, h - 1 - y);  // flip vertical
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8)  & 0xFF;
                            int b =  argb        & 0xFF;
                            int a = (argb >>> 24) & 0xFF;
                            // Nếu ảnh đã là grayscale (r≈g≈b), dùng R cho tất cả kênh
                            // để shader chỉ cần .r
                            int lum = (r + g + b) / 3;
                            int o = (y * w + x) * 4;
                            data[o]     = (byte) lum;
                            data[o + 1] = (byte) lum;
                            data[o + 2] = (byte) lum;
                            data[o + 3] = (byte) (a > 0 ? 255 : 255); // force opaque
                        }
                    }
                    System.out.println("Cloud texture: " + FILE_PATH + " (" + w + "x" + h + ")");
                }
            } catch (IOException e) {
                System.err.println("Không đọc được " + FILE_PATH + ", dùng procedural");
            }
        }

        if (data == null) {
            w = h = GEN_SIZE;
            data = generateProcedural(seed, GEN_SIZE);
            System.out.println("Cloud texture: procedural " + w + "x" + h);
        }

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);

        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        try {
            buf.put(data).flip();
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 3);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    // ==================== FALLBACK PROCEDURAL ====================

    private static byte[] generateProcedural(long seed, int s) {
        Noise noise = new Noise(seed);
        byte[] data = new byte[s * s * 4];

        float freq1 = 3.0f / s;
        float freq2 = 7.0f / s;
        float freq3 = 16.0f / s;

        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float a = tileableFbm(noise, x, y, s, freq1, 4);
                float b = tileableFbm(noise, x, y, s, freq2, 3);
                float c = tileableFbm(noise, x, y, s, freq3, 3);

                // Combine — base + detail
                float v = a * 0.6f + b * 0.28f + c * 0.12f;

                int lum = clampByte((v + 1f) * 0.5f * 255f);
                int o = (y * s + x) * 4;
                data[o]     = (byte) lum;
                data[o + 1] = (byte) lum;
                data[o + 2] = (byte) lum;
                data[o + 3] = (byte) 255;
            }
        }
        return data;
    }

    private static float tileableFbm(Noise n, int px, int py, int size,
                                     float baseFreq, int octaves) {
        float sum = 0f, amp = 1f, freq = baseFreq, max = 0f;
        float u = px / (float) size;
        float v = py / (float) size;

        for (int i = 0; i < octaves; i++) {
            float fx = px * freq;
            float fy = py * freq;
            float W  = size * freq;

            float a = n.noise(fx,     fy);
            float b = n.noise(fx - W, fy);
            float c = n.noise(fx,     fy - W);
            float d = n.noise(fx - W, fy - W);

            float val = a * (1-u) * (1-v)
                      + b * u * (1-v)
                      + c * (1-u) * v
                      + d * u * v;

            sum += val * amp;
            max += amp;
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / max;
    }

    private static int clampByte(float v) {
        int i = Math.round(v);
        if (i < 0) return 0;
        if (i > 255) return 255;
        return i;
    }
}
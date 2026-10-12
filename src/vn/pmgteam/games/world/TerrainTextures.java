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

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * 4 texture terrain sinh procedural: grass, rock, sand, snow.
 * Mỗi cái 128×128, tile-able, dùng cho splatmap blending.
 */
public final class TerrainTextures {

    public static final int TILE_SIZE = 128;

    public final int grass;
    public final int rock;
    public final int sand;
    public final int snow;

    private enum Mode { GRASS, ROCK, SAND, SNOW }

    public TerrainTextures(long seed) {
        grass = gen(seed + 101L, Mode.GRASS);
        rock  = gen(seed + 202L, Mode.ROCK);
        sand  = gen(seed + 303L, Mode.SAND);
        snow  = gen(seed + 404L, Mode.SNOW);
        System.out.println("Terrain textures: " + TILE_SIZE + "x" + TILE_SIZE + " x4");
    }

    private static int gen(long seed, Mode mode) {
        Noise noise = new Noise(seed);
        int s = TILE_SIZE;
        byte[] data = new byte[s * s * 4];

        float br, bg, bb, variation;
        switch (mode) {
            case GRASS: br = 0.34f; bg = 0.56f; bb = 0.26f; variation = 0.22f; break;
            case ROCK:  br = 0.44f; bg = 0.42f; bb = 0.40f; variation = 0.20f; break;
            case SAND:  br = 0.82f; bg = 0.76f; bb = 0.55f; variation = 0.12f; break;
            case SNOW:  br = 0.92f; bg = 0.94f; bb = 0.98f; variation = 0.06f; break;
            default:    br = 0.5f;  bg = 0.5f;  bb = 0.5f;  variation = 0.10f; break;
        }

        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                // 4 tầng noise chồng nhau — chi tiết từ lớn tới nhỏ
                float n1 = noise.noise(x * 0.04f, y * 0.04f);              // patch lớn
                float n2 = noise.noise(x * 0.11f + 100f, y * 0.11f + 100f); // trung
                float n3 = noise.noise(x * 0.28f + 200f, y * 0.28f + 200f); // nhỏ
                float n4 = noise.noise(x * 0.72f + 300f, y * 0.72f + 300f); // chi tiết

                float n = n1 * 0.40f + n2 * 0.30f + n3 * 0.20f + n4 * 0.10f;

                float r = br, g = bg, b = bb;

                switch (mode) {
                    case GRASS: {
                        // Lá cỏ theo hướng dọc — pattern sợi
                        float blades = noise.noise(x * 0.7f, y * 0.12f);
                        // Đám cỏ khô/vàng
                        float dry = Math.max(0f, noise.noise(x * 0.06f + 500f, y * 0.06f + 500f));
                        float v = variation;
                        r += (n + blades * 0.35f) * v + dry * 0.10f;
                        g += (n + blades * 0.20f) * v * 1.15f;
                        b += n * v * 0.5f - dry * 0.04f;
                        break;
                    }
                    case ROCK: {
                        // Vân nứt
                        float crack = Math.abs(noise.noise(x * 0.15f + 50f, y * 0.15f + 50f));
                        if (crack < 0.10f) { r *= 0.55f; g *= 0.55f; b *= 0.55f; }
                        // Đốm sáng tối ngẫu nhiên (khoáng chất)
                        if (n3 > 0.42f) { r += 0.10f; g += 0.10f; b += 0.10f; }
                        if (n3 < -0.42f) { r -= 0.06f; g -= 0.06f; b -= 0.06f; }
                        r += n * variation;
                        g += n * variation;
                        b += n * variation * 0.85f;
                        break;
                    }
                    case SAND: {
                        // Hạt cát — noise tần số cực cao
                        float grain = noise.noise(x * 1.6f, y * 1.6f);
                        r += n * variation + grain * 0.03f;
                        g += n * variation * 0.9f + grain * 0.02f;
                        b += n * variation * 0.7f - grain * 0.02f;
                        break;
                    }
                    case SNOW: {
                        // Tinh thể băng — chấm sáng nhỏ li ti
                        if (n4 > 0.55f) { r += 0.06f; g += 0.06f; b += 0.07f; }
                        r += n * variation;
                        g += n * variation * 0.9f;
                        b += n * variation * 0.8f + 0.03f;
                        break;
                    }
                }

                int o = (y * s + x) * 4;
                data[o]     = toByte(r);
                data[o + 1] = toByte(g);
                data[o + 2] = toByte(b);
                data[o + 3] = (byte) 255;
            }
        }

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);

        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        try {
            buf.put(data).flip();
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, s, s, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 3);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    public void cleanup() {
        glDeleteTextures(grass);
        glDeleteTextures(rock);
        glDeleteTextures(sand);
        glDeleteTextures(snow);
    }

    private static byte toByte(float v) {
        return (byte) Math.round(clamp01(v) * 255f);
    }
    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
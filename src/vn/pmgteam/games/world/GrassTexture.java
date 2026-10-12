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
import java.util.Random;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

public final class GrassTexture {

    public static final String FILE_PATH = "src/assets/textures/grass.png";
    private static final int GEN_SIZE = 256;

    private GrassTexture() {}

    public static int create() {
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
                            int argb = img.getRGB(x, h - 1 - y);
                            int o = (y * w + x) * 4;
                            data[o]     = (byte) (argb >> 16);
                            data[o + 1] = (byte) (argb >> 8);
                            data[o + 2] = (byte) argb;
                            data[o + 3] = (byte) (argb >>> 24);
                        }
                    }
                    System.out.println("Grass texture: " + FILE_PATH + " (" + w + "x" + h + ")");
                }
            } catch (IOException e) {
                System.err.println("Không đọc được " + FILE_PATH + ", dùng texture tự sinh");
            }
        }

        if (data == null) {
            w = h = GEN_SIZE;
            data = generate(GEN_SIZE);
            System.out.println("Grass texture: tự sinh " + w + "x" + h);
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

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 2);
        glGenerateMipmap(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    private static byte[] generate(int s) {
        byte[] d = new byte[s * s * 4];

        // Pixel trong suốt vẫn mang màu xanh, tránh viền đen khi filter/mipmap
        for (int i = 0; i < s * s; i++) {
            d[i * 4]     = (byte) 70;
            d[i * 4 + 1] = (byte) 140;
            d[i * 4 + 2] = (byte) 45;
            d[i * 4 + 3] = 0;
        }

        Random rnd = new Random(7L);
        int blades = 20;

        for (int b = 0; b < blades; b++) {
            float bx    = 0.07f + rnd.nextFloat() * 0.86f;
            float hf    = 0.50f + rnd.nextFloat() * 0.48f;
            float wf    = 0.040f + rnd.nextFloat() * 0.020f;
            float lean  = (rnd.nextFloat() - 0.5f) * 0.38f;
            float shade = 0.88f + rnd.nextFloat() * 0.22f;

            int rows = (int) (hf * s);
            for (int py = 0; py < rows; py++) {
                float t  = py / (float) rows;
                float cx = (bx + lean * t * t) * s;
                float hw = wf * s * 0.5f * (float) Math.pow(1f - t, 0.85);
                if (hw < 0.45f) hw = 0.45f;

                // Hạ G xuống (0.78 max thay 0.92), nâng R/B tỉ lệ để bớt neon.
                // Tỉ lệ R:G:B ~ 1 : 1.9 : 0.5 → xanh olive tự nhiên, không chóe.
                float r  = lerp(0.16f, 0.38f, t) * shade;
                float g  = lerp(0.36f, 0.78f, t) * shade;
                float bl = lerp(0.10f, 0.20f, t) * shade;

                int x0 = (int) Math.floor(cx - hw - 1f);
                int x1 = (int) Math.ceil(cx + hw + 1f);
                for (int px = x0; px <= x1; px++) {
                    if (px < 0 || px >= s) continue;

                    float dist = Math.abs(px + 0.5f - cx);
                    float a = clamp(hw - dist + 0.5f, 0f, 1f);
                    if (a <= 0f) continue;

                    float vein = (dist < hw * 0.25f) ? 1.08f : 1f;

                    int o = (py * s + px) * 4;
                    int oldA = d[o + 3] & 0xFF;
                    int newA = (int) (a * 255f);
                    if (newA >= oldA) {
                        d[o]     = toByte(r * vein);
                        d[o + 1] = toByte(g * vein);
                        d[o + 2] = toByte(bl * vein);
                        d[o + 3] = (byte) newA;
                    }
                }
            }
        }
        return d;
    }

    private static byte toByte(float v) {
        return (byte) Math.round(clamp(v, 0f, 1f) * 255f);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
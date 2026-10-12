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

import java.util.Random;

public class Noise {

    private final int[] p = new int[512];

    public Noise(long seed) {
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;

        Random rnd = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
        }
        for (int i = 0; i < 512; i++) p[i] = perm[i & 255];
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y) {
        int h = hash & 7;
        float u = (h < 4) ? x : y;
        float v = (h < 4) ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public float noise(float x, float y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        x -= (float) Math.floor(x);
        y -= (float) Math.floor(y);

        float u = fade(x);
        float v = fade(y);

        int A = p[X]     + Y;
        int B = p[X + 1] + Y;

        return lerp(
                lerp(grad(p[A],     x,     y),
                     grad(p[B],     x - 1, y),     u),
                lerp(grad(p[A + 1], x,     y - 1),
                     grad(p[B + 1], x - 1, y - 1), u),
                v);
    }

    /** Fractal Brownian Motion */
    public float fbm(float x, float y, int octaves, float persistence, float lacunarity) {
        float sum = 0, amp = 1, freq = 1, max = 0;
        for (int i = 0; i < octaves; i++) {
            sum  += noise(x * freq, y * freq) * amp;
            max  += amp;
            amp  *= persistence;
            freq *= lacunarity;
        }
        return sum / max;
    }
}
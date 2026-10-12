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
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

import org.lwjgl.system.MemoryUtil;

public class Chunk {

    public static final int SIZE = 64;
    public static final float SPACING = 1f;

    public static final float BOUND_MIN_Y = -20f;
    public static final float BOUND_MAX_Y = 160f;

    private static final float PLATEAU_CELL   = 200f;
    private static final float PLATEAU_RADIUS = 30f;
    private static final float PLATEAU_BLEND  = 100f;
    private static final long  PLATEAU_SEED   = 987654321L;
    private static final int   PLATEAU_CHANCE = 200;

    private static final float SPAWN_FLAT_RADIUS  = 15f;
    private static final float SPAWN_BLEND_RADIUS = 40f;
    private static final float SPAWN_HEIGHT       = 3f;

    private static final float PLAINS_BASE  = 4.5f;
    private static final float PLAINS_AMP   = 9f;
    private static final float PLAINS_FLOOR = 1.5f;

    private static final float MOUNTAIN_HEIGHT    = 120f;
    private static final float MOUNTAIN_MASK_FREQ = 0.0025f;
    private static final float MASK_LO = -0.05f;
    private static final float MASK_HI =  0.30f;
    private static final float RIDGE_FREQ = 0.003f;
    private static final float MOUNTAIN_BASE_FREQ = 0.0015f;

    private static final int   TUFTS_PER_CELL  = 4;
    private static final float TUFT_CHANCE     = 0.65f;
    private static final float GRASS_MIN_Y     = 1.0f;
    private static final float GRASS_MAX_Y     = 22f;
    private static final float GRASS_MAX_SLOPE = 0.9f;
    private static final float GRASS_PATCH_FREQ = 0.055f;
    private static final float[] GRASS_REF = { 0.30f, 0.58f, 0.22f };
    private static final int GRASS_FLOATS = 11;

    private static final int TERRAIN_FLOATS = 15;
    private static final float TERRAIN_UV_SCALE = 0.5f;

    public final int cx, cz;

    private int vao, vbo;
    private int vertexCount;

    private int grassVao, grassVbo;
    private int grassVertexCount;

    private float[] aoGrid;

    public Chunk(int cx, int cz, Noise noise) {
        this.cx = cx;
        this.cz = cz;

        float[] hg = buildHeightGrid(noise);

        // Chỉ smooth khi có vùng cao — tiết kiệm ~5ms cho chunk đồng bằng
        boolean hasHigh = false;
        for (int k = 0; k < hg.length; k++) {
            if (hg[k] > 25f) { hasHigh = true; break; }
        }
        if (hasHigh) smoothHighAreas(hg);

        buildAoGrid(hg);

        float[] mesh = buildMesh(hg, noise);
        if (mesh != null && mesh.length > 0) {
            vertexCount = mesh.length / TERRAIN_FLOATS;
            vao = glGenVertexArrays();
            glBindVertexArray(vao);
            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            FloatBuffer fb = MemoryUtil.memAllocFloat(mesh.length);
            try {
                fb.put(mesh).flip();
                glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            } finally {
                MemoryUtil.memFree(fb);
            }

            setupTerrainAttribs();
            glBindVertexArray(0);
        }

        buildGrass(hg, noise);

        aoGrid = null;
        mesh = null;
    }

    private static void setupTerrainAttribs() {
        int stride = TERRAIN_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 2, GL_FLOAT, false, stride, 9 * Float.BYTES);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(4, 4, GL_FLOAT, false, stride, 11 * Float.BYTES);
        glEnableVertexAttribArray(4);
    }

    private static void setupGrassAttribs() {
        int stride = GRASS_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);
    }

    public float getMinX() { return cx * (SIZE - 1) * SPACING; }
    public float getMaxX() { return getMinX() + (SIZE - 1) * SPACING; }
    public float getMinZ() { return cz * (SIZE - 1) * SPACING; }
    public float getMaxZ() { return getMinZ() + (SIZE - 1) * SPACING; }

    // ==================== HEIGHT GRID ====================

    private float[] buildHeightGrid(Noise noise) {
        float originX = cx * (SIZE - 1) * SPACING;
        float originZ = cz * (SIZE - 1) * SPACING;

        float[] hg = new float[SIZE * SIZE];
        for (int j = 0; j < SIZE; j++) {
            float z = originZ + j * SPACING;
            int row = j * SIZE;
            for (int i = 0; i < SIZE; i++) {
                float x = originX + i * SPACING;
                hg[row + i] = heightAt(noise, x, z);
            }
        }
        return hg;
    }

    /**
     * Laplacian smoothing chỉ ở vùng cao (y > 25).
     * Loại bỏ gai nhọn do noise tần số cao ở đỉnh núi, giữ chi tiết ở đồng bằng.
     */
    private void smoothHighAreas(float[] hg) {
        float[] copy = hg.clone();

        for (int j = 1; j < SIZE - 1; j++) {
            for (int i = 1; i < SIZE - 1; i++) {
                int idx = j * SIZE + i;
                float y = copy[idx];

                float smoothAmount = smoothstep(25f, 70f, y);
                if (smoothAmount < 0.01f) continue;

                float sum = 0f;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        sum += copy[(j + dj) * SIZE + (i + di)];
                    }
                }
                float avg = sum / 9f;

                float blend = smoothAmount * 0.75f;
                hg[idx] = lerp(y, avg, blend);
            }
        }
    }

    private static float gridHeight(float[] hg, float lx, float lz) {
        if (lx < 0f) lx = 0f; else if (lx > SIZE - 1.001f) lx = SIZE - 1.001f;
        if (lz < 0f) lz = 0f; else if (lz > SIZE - 1.001f) lz = SIZE - 1.001f;

        int i = (int) lx;
        int j = (int) lz;
        float fx = lx - i;
        float fz = lz - j;

        float h00 = hg[ j      * SIZE + i    ];
        float h10 = hg[ j      * SIZE + i + 1];
        float h01 = hg[(j + 1) * SIZE + i    ];
        float h11 = hg[(j + 1) * SIZE + i + 1];

        float a = h00 + (h10 - h00) * fx;
        float b = h01 + (h11 - h01) * fx;
        return a + (b - a) * fz;
    }

    private static float groundTint(Noise noise, float x, float z) {
        float big   = noise.noise(x * 0.06f, z * 0.06f);
        float small = noise.noise(x * 0.35f, z * 0.35f);
        float t = 1f + 0.14f * big + 0.06f * small;
        return Math.max(0.78f, Math.min(1.22f, t));
    }

    // ==================== AO GRID ====================

    private void buildAoGrid(float[] hg) {
        aoGrid = new float[SIZE * SIZE];

        final int[] radii = { 3, 7, 14 };
        final float[] weights = { 1.0f, 0.65f, 0.35f };

        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                float y = hg[j * SIZE + i];
                float occl = 0f;
                int count = 0;

                for (int r = 0; r < radii.length; r++) {
                    int rad = radii[r];
                    float w = weights[r];
                    for (int d = 0; d < 8; d++) {
                        double ang = d * Math.PI / 4.0;
                        int ni = i + (int) Math.round(rad * Math.cos(ang));
                        int nj = j + (int) Math.round(rad * Math.sin(ang));
                        if (ni < 0 || nj < 0 || ni >= SIZE || nj >= SIZE) continue;
                        float dh = hg[nj * SIZE + ni] - y;
                        if (dh > 0f) occl += (dh / (rad * SPACING)) * w;
                        count++;
                    }
                }
                float factor = (count == 0) ? 1f
                        : 1f - Math.min(0.45f, (occl / count) * 0.6f);
                aoGrid[j * SIZE + i] = factor;
            }
        }

        float[] blurred = new float[SIZE * SIZE];
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                float sum = 0f;
                int n = 0;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int ni = i + di;
                        int nj = j + dj;
                        if (ni < 0 || nj < 0 || ni >= SIZE || nj >= SIZE) continue;
                        sum += aoGrid[nj * SIZE + ni];
                        n++;
                    }
                }
                blurred[j * SIZE + i] = sum / n;
            }
        }
        aoGrid = blurred;
    }

    private float sampleAO(float lx, float lz) {
        if (lx < 0f) lx = 0f; else if (lx > SIZE - 1.001f) lx = SIZE - 1.001f;
        if (lz < 0f) lz = 0f; else if (lz > SIZE - 1.001f) lz = SIZE - 1.001f;

        int i = (int) lx;
        int j = (int) lz;
        float fx = lx - i;
        float fz = lz - j;

        float a00 = aoGrid[ j      * SIZE + i    ];
        float a10 = aoGrid[ j      * SIZE + i + 1];
        float a01 = aoGrid[(j + 1) * SIZE + i    ];
        float a11 = aoGrid[(j + 1) * SIZE + i + 1];

        float a = a00 + (a10 - a00) * fx;
        float b = a01 + (a11 - a01) * fx;
        return a + (b - a) * fz;
    }

    // ==================== MESH ====================

    private float[] buildMesh(float[] hg, Noise noise) {
        float originX = cx * (SIZE - 1) * SPACING;
        float originZ = cz * (SIZE - 1) * SPACING;

        float[] buf = new float[SIZE * SIZE * 90];
        int bufSize = 0;

        for (int j = 0; j < SIZE - 1; j++) {
            for (int i = 0; i < SIZE - 1; i++) {
                float x0 = originX + i * SPACING;
                float x1 = originX + (i + 1) * SPACING;
                float z0 = originZ + j * SPACING;
                float z1 = originZ + (j + 1) * SPACING;

                float y00 = hg[ j      * SIZE + i    ];
                float y10 = hg[ j      * SIZE + i + 1];
                float y11 = hg[(j + 1) * SIZE + i + 1];
                float y01 = hg[(j + 1) * SIZE + i    ];

                if (bufSize + 6 * TERRAIN_FLOATS > buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }

                boolean alternate = ((i + j) & 1) != 0;

                if (!alternate) {
                    // Diagonal a-d
                    bufSize += emit(buf, bufSize, hg, noise, i,     j,     x0, y00, z0);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j + 1, x1, y11, z1);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j,     x1, y10, z0);

                    bufSize += emit(buf, bufSize, hg, noise, i,     j,     x0, y00, z0);
                    bufSize += emit(buf, bufSize, hg, noise, i,     j + 1, x0, y01, z1);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j + 1, x1, y11, z1);
                } else {
                    // Diagonal b-c
                    bufSize += emit(buf, bufSize, hg, noise, i,     j,     x0, y00, z0);
                    bufSize += emit(buf, bufSize, hg, noise, i,     j + 1, x0, y01, z1);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j,     x1, y10, z0);

                    bufSize += emit(buf, bufSize, hg, noise, i,     j + 1, x0, y01, z1);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j + 1, x1, y11, z1);
                    bufSize += emit(buf, bufSize, hg, noise, i + 1, j,     x1, y10, z0);
                }
            }
        }

        return Arrays.copyOf(buf, bufSize);
    }

    private int emit(float[] buf, int off,
                     float[] hg, Noise noise,
                     int i, int j,
                     float x, float y, float z) {

        float eps = SPACING;
        float hL = (i > 0)        ? hg[ j      * SIZE + (i - 1)] : heightAt(noise, x - eps, z);
        float hR = (i < SIZE - 1) ? hg[ j      * SIZE + (i + 1)] : heightAt(noise, x + eps, z);
        float hD = (j > 0)        ? hg[(j - 1) * SIZE + i]       : heightAt(noise, x, z - eps);
        float hU = (j < SIZE - 1) ? hg[(j + 1) * SIZE + i]       : heightAt(noise, x, z + eps);

        float nx = -2 * eps * (hR - hL);
        float ny =  4 * eps * eps;
        float nz = -2 * eps * (hU - hD);

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) { nx = 0; ny = 1; nz = 0; }
        else { nx /= len; ny /= len; nz /= len; }

        float slope = (float) Math.sqrt(nx * nx + nz * nz) / Math.max(ny, 0.001f);

        float rockW  = smoothstep(0.55f, 0.95f, slope);
        float sandW  = 0f;
        float grassW = 0f;
        float snowW  = 0f;

        if (y < 3f) {
            sandW  = 1f - smoothstep(0.8f, 3f, y);
            grassW = 1f - sandW;
        } else if (y < 18f) {
            grassW = 1f;
        } else if (y < 30f) {
            grassW = 1f - smoothstep(18f, 30f, y);
            rockW  = Math.max(rockW, smoothstep(18f, 30f, y) * 0.7f);
        } else if (y < 60f) {
            rockW = 1f;
        } else if (y < 80f) {
            float t = smoothstep(60f, 80f, y);
            rockW = 1f - t;
            snowW = t;
        } else {
            snowW = 1f;
        }

        grassW *= (1f - rockW);
        sandW  *= (1f - rockW);
        snowW  *= (1f - rockW);

        float sum = grassW + rockW + sandW + snowW;
        if (sum < 1e-4f) { grassW = 1f; sum = 1f; rockW = 0; sandW = 0; snowW = 0; }
        grassW /= sum; rockW /= sum; sandW /= sum; snowW /= sum;

        float ao = sampleAO(i, j);
        float tint = groundTint(noise, x, z) * ao;

        float u = x * TERRAIN_UV_SCALE;
        float v = z * TERRAIN_UV_SCALE;

        buf[off]      = x;
        buf[off + 1]  = y;
        buf[off + 2]  = z;
        buf[off + 3]  = nx;
        buf[off + 4]  = ny;
        buf[off + 5]  = nz;
        buf[off + 6]  = tint;
        buf[off + 7]  = tint;
        buf[off + 8]  = tint;
        buf[off + 9]  = u;
        buf[off + 10] = v;
        buf[off + 11] = grassW;
        buf[off + 12] = rockW;
        buf[off + 13] = sandW;
        buf[off + 14] = snowW;
        return TERRAIN_FLOATS;
    }

    // ==================== GRASS ====================

    private void buildGrass(float[] hg, Noise noise) {
        float originX = cx * (SIZE - 1) * SPACING;
        float originZ = cz * (SIZE - 1) * SPACING;

        Random rnd = new Random((long) cx * 73856093L ^ (long) cz * 19349663L);

        float[] buf = new float[6000 * GRASS_FLOATS];
        int off = 0;
        float[] ground = new float[3];

        for (int j = 0; j < SIZE - 1; j++) {
            for (int i = 0; i < SIZE - 1; i++) {

                float hBase = hg[j * SIZE + i];
                if (hBase < GRASS_MIN_Y || hBase > GRASS_MAX_Y) continue;

                float sx = Math.abs(hg[j * SIZE + i + 1] - hBase);
                float sz = Math.abs(hg[(j + 1) * SIZE + i] - hBase);
                if (Math.max(sx, sz) / SPACING > GRASS_MAX_SLOPE) continue;

                float heightFade = smoothstep(GRASS_MIN_Y, GRASS_MIN_Y + 1.5f, hBase)
                                 * (1f - smoothstep(GRASS_MAX_Y - 3f, GRASS_MAX_Y, hBase));
                if (heightFade <= 0.01f) continue;

                for (int t = 0; t < TUFTS_PER_CELL; t++) {
                    if (rnd.nextFloat() > TUFT_CHANCE * heightFade) continue;

                    float lx = i + rnd.nextFloat();
                    float lz = j + rnd.nextFloat();
                    float wx = originX + lx * SPACING;
                    float wz = originZ + lz * SPACING;
                    float cy = gridHeight(hg, lx, lz);
                    if (cy < GRASS_MIN_Y || cy > GRASS_MAX_Y) continue;

                    float patchN = noise.noise(wx * GRASS_PATCH_FREQ + 91.3f,
                                               wz * GRASS_PATCH_FREQ + 47.7f);
                    float patch  = smoothstep(-0.50f, 0.15f, patchN);
                    if (rnd.nextFloat() > patch) continue;

                    float fade = 1f - smoothstep(GRASS_MAX_Y - 8f, GRASS_MAX_Y, cy);
                    if (rnd.nextFloat() > fade) continue;

                    colorForHeight(cy, ground);
                    float gt = groundTint(noise, wx, wz) * sampleAO(lx, lz);

                    float tr = clamp(ground[0] * gt / GRASS_REF[0], 0.5f, 1.4f);
                    float tg = clamp(ground[1] * gt / GRASS_REF[1], 0.5f, 1.4f);
                    float tb = clamp(ground[2] * gt / GRASS_REF[2], 0.5f, 1.4f);

                    float dry = smoothstep(0.10f, 0.45f,
                            noise.noise(wx * 0.04f + 37.1f, wz * 0.04f + 11.7f)) * 0.65f;
                    tr = lerp(tr, tr * 1.25f, dry);
                    tg = lerp(tg, tg * 1.02f, dry);
                    tb = lerp(tb, tb * 0.55f, dry);

                    if (off + 3 * 6 * GRASS_FLOATS > buf.length) {
                        buf = Arrays.copyOf(buf, buf.length * 2);
                    }

                    off = emitTuft(buf, off, hg, originX, originZ, wx, cy, wz, tr, tg, tb, rnd);
                }
            }
        }

        if (off == 0) return;

        grassVertexCount = off / GRASS_FLOATS;

        grassVao = glGenVertexArrays();
        glBindVertexArray(grassVao);

        grassVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, grassVbo);

        FloatBuffer fb = MemoryUtil.memAllocFloat(off);
        try {
            fb.put(buf, 0, off).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(fb);
        }

        setupGrassAttribs();
        glBindVertexArray(0);
    }

    private int emitTuft(float[] buf, int off,
                         float[] hg, float originX, float originZ,
                         float x, float y, float z,
                         float tr, float tg, float tb, Random rnd) {

        float llx = (x - originX) / SPACING;
        float llz = (z - originZ) / SPACING;
        float ne = 1.0f;
        float hL = gridHeight(hg, llx - ne, llz);
        float hR = gridHeight(hg, llx + ne, llz);
        float hD = gridHeight(hg, llx, llz - ne);
        float hU = gridHeight(hg, llx, llz + ne);
        float nX = -(hR - hL) / (2 * ne);
        float nZ = -(hU - hD) / (2 * ne);
        float nY = 1.0f;
        float nlen = (float) Math.sqrt(nX * nX + nY * nY + nZ * nZ);
        nX /= nlen; nY /= nlen; nZ /= nlen;

        float w = 0.60f + rnd.nextFloat() * 0.45f;
        float h = 0.40f + rnd.nextFloat() * 0.40f;
        float v = 0.85f + rnd.nextFloat() * 0.30f;
        float r = tr * v, g = tg * v, b = tb * v;

        float hw  = w * 0.5f;
        float yaw = rnd.nextFloat() * (float) Math.PI;
        float yTop = y - 0.05f + h;

        for (int q = 0; q < 3; q++) {
            float a    = yaw + q * (float) (Math.PI / 3.0);
            float cosA = (float) Math.cos(a);
            float sinA = (float) Math.sin(a);
            float dx = cosA * hw;
            float dz = sinA * hw;

            float xl = x - dx, zl = z - dz;
            float xr = x + dx, zr = z + dz;
            float yl = gridHeight(hg, (xl - originX) / SPACING, (zl - originZ) / SPACING) - 0.05f;
            float yr = gridHeight(hg, (xr - originX) / SPACING, (zr - originZ) / SPACING) - 0.05f;

            float flare = 1.15f + rnd.nextFloat() * 0.25f;
            float lean  = (rnd.nextFloat() - 0.5f) * 0.40f;
            float lx = -sinA * lean;
            float lz =  cosA * lean;

            float xtl = x - dx * flare + lx, ztl = z - dz * flare + lz;
            float xtr = x + dx * flare + lx, ztr = z + dz * flare + lz;

            boolean flip = rnd.nextBoolean();
            float ul = flip ? 1f : 0f;
            float ur = flip ? 0f : 1f;

            off = emitGrassVertex(buf, off, xl,  yl,   zl,  nX, nY, nZ, ul, 0f, r, g, b);
            off = emitGrassVertex(buf, off, xr,  yr,   zr,  nX, nY, nZ, ur, 0f, r, g, b);
            off = emitGrassVertex(buf, off, xtr, yTop, ztr, nX, nY, nZ, ur, 1f, r, g, b);

            off = emitGrassVertex(buf, off, xl,  yl,   zl,  nX, nY, nZ, ul, 0f, r, g, b);
            off = emitGrassVertex(buf, off, xtr, yTop, ztr, nX, nY, nZ, ur, 1f, r, g, b);
            off = emitGrassVertex(buf, off, xtl, yTop, ztl, nX, nY, nZ, ul, 1f, r, g, b);
        }
        return off;
    }

    private int emitGrassVertex(float[] buf, int off,
                                float x, float y, float z,
                                float nx, float ny, float nz,
                                float u, float v,
                                float r, float g, float b) {
        buf[off++] = x; buf[off++] = y; buf[off++] = z;
        buf[off++] = nx; buf[off++] = ny; buf[off++] = nz;
        buf[off++] = u; buf[off++] = v;
        buf[off++] = r; buf[off++] = g; buf[off++] = b;
        return off;
    }

    public void renderGrass() {
        if (grassVertexCount == 0) return;
        glBindVertexArray(grassVao);
        glDrawArrays(GL_TRIANGLES, 0, grassVertexCount);
        glBindVertexArray(0);
    }

    // ==================== HEIGHT ====================

    public static float heightAt(Noise noise, float x, float z) {
        float plains = plainsHeight(noise, x, z);
        float[] p = plateauInfluence(noise, x, z);
        float h = lerp(plains, p[0], p[1]) + mountainHeight(noise, x, z);

        float d = (float) Math.sqrt(x * x + z * z);
        float spawnFlatness = 1f - smoothstep(SPAWN_FLAT_RADIUS, SPAWN_BLEND_RADIUS, d);
        return lerp(h, SPAWN_HEIGHT, spawnFlatness);
    }

    private static float plainsHeight(Noise noise, float x, float z) {
        float n = noise.fbm(x * 0.018f, z * 0.018f, 5, 0.5f, 2.0f);
        float detail = noise.noise(x * 0.12f, z * 0.12f) * 0.5f;
        float h = PLAINS_BASE + n * PLAINS_AMP + detail;

        if (h < PLAINS_FLOOR) h = PLAINS_FLOOR + (h - PLAINS_FLOOR) * 0.25f;
        return h;
    }

    /**
     * Núi = base + ridged, pow nhẹ (1.3) để giữ độ cao mà không tạo gai,
     * detail rất nhỏ ở đỉnh để phá flat top.
     */
    private static float mountainHeight(Noise noise, float x, float z) {
        float mk = noise.fbm(x * MOUNTAIN_MASK_FREQ + 100f, z * MOUNTAIN_MASK_FREQ + 100f,
                             3, 0.5f, 2.0f);
        float m = smoothstep(MASK_LO, MASK_HI, mk);
        if (m <= 0f) return 0f;

        float base = noise.fbm(x * MOUNTAIN_BASE_FREQ + 200f,
                               z * MOUNTAIN_BASE_FREQ + 200f,
                               3, 0.55f, 2.0f) * 0.5f + 0.5f;

        float r = ridged(noise, x * RIDGE_FREQ, z * RIDGE_FREQ);

        float shape = base * 0.40f + r * 0.60f;

        // pow 1.3 — mềm hơn shape², đỉnh không gai
        shape = (float) Math.pow(Math.max(0f, shape), 1.3);

        // Detail rất nhẹ — tần số thấp, biên độ nhỏ
        float detail = noise.fbm(x * 0.008f + 500f, z * 0.008f + 500f,
                                 2, 0.5f, 2.0f);
        float highMask = smoothstep(0.55f, 0.95f, shape);
        shape += detail * 0.03f * highMask;

        if (shape < 0f) shape = 0f;
        return shape * MOUNTAIN_HEIGHT * m;
    }

    private static float ridged(Noise noise, float x, float z) {
        float sum = 0f, amp = 1f, freq = 1f, norm = 0f;
        for (int i = 0; i < 3; i++) {
            float v = Math.max(0f, 1f - Math.abs(noise.noise(x * freq, z * freq)));
            v *= v;
            sum  += v * amp;
            norm += amp;
            amp  *= 0.55f;
            freq *= 1.75f;
        }
        return sum / norm;
    }

    private static float[] plateauInfluence(Noise noise, float x, float z) {
        int cellX = (int) Math.floor(x / PLATEAU_CELL);
        int cellZ = (int) Math.floor(z / PLATEAU_CELL);

        float bestInfluence = 0f;
        float bestY = 0f;

        for (int oz = -1; oz <= 1; oz++) {
            for (int ox = -1; ox <= 1; ox++) {
                int cx = cellX + ox;
                int cz = cellZ + oz;
                long hash = hash(cx, cz, PLATEAU_SEED);

                int roll = (int)(hash & 0xFF);
                if (roll >= PLATEAU_CHANCE) continue;

                float fx = ((hash >>> 8)  & 0xFFFF) / 65535f;
                float fz = ((hash >>> 24) & 0xFFFF) / 65535f;

                float px = (cx + fx) * PLATEAU_CELL;
                float pz = (cz + fz) * PLATEAU_CELL;

                float dx = x - px;
                float dz = z - pz;
                float d = (float) Math.sqrt(dx * dx + dz * dz);

                float influence = 1f - smoothstep(PLATEAU_RADIUS, PLATEAU_BLEND, d);
                if (influence > bestInfluence) {
                    bestInfluence = influence;
                    bestY = plainsHeight(noise, px, pz);
                }
            }
        }
        return new float[]{ bestY, bestInfluence };
    }

    private static long hash(int x, int z, long seed) {
        long h = seed;
        h = h * 0x9E3779B97F4A7C15L + x;
        h ^= (h >>> 32);
        h = h * 0xD6E8FEB86659FD93L + z;
        h ^= (h >>> 32);
        h = h * 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return h;
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static void colorForHeight(float y, float[] out) {
        float r, g, b;
        if (y < 0.2f) {
            r = 0.84f; g = 0.78f; b = 0.55f;
        } else if (y < 1.2f) {
            float t = (y - 0.2f) / 1.0f;
            r = lerp(0.84f, 0.30f, t);
            g = lerp(0.78f, 0.58f, t);
            b = lerp(0.55f, 0.22f, t);
        } else if (y < 18f) {
            float t = (y - 1.2f) / 16.8f;
            r = lerp(0.30f, 0.26f, t);
            g = lerp(0.58f, 0.52f, t);
            b = lerp(0.22f, 0.20f, t);
        } else if (y < 30f) {
            float t = (y - 18f) / 12f;
            r = lerp(0.26f, 0.46f, t);
            g = lerp(0.52f, 0.43f, t);
            b = lerp(0.20f, 0.40f, t);
        } else if (y < 60f) {
            float t = (y - 30f) / 30f;
            r = lerp(0.46f, 0.52f, t);
            g = lerp(0.43f, 0.49f, t);
            b = lerp(0.40f, 0.47f, t);
        } else if (y < 80f) {
            float t = (y - 60f) / 20f;
            r = lerp(0.52f, 0.95f, t);
            g = lerp(0.49f, 0.96f, t);
            b = lerp(0.47f, 1.00f, t);
        } else {
            r = 0.95f; g = 0.96f; b = 1.00f;
        }
        out[0] = r; out[1] = g; out[2] = b;
    }

    public void render() {
        if (vertexCount == 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (grassVbo != 0) glDeleteBuffers(grassVbo);
        if (grassVao != 0) glDeleteVertexArrays(grassVao);
    }
}
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Terrain {

    public static final int CHUNK_SIZE = Chunk.SIZE;

    /** Số chunk tối đa nạp ngay frame đầu — nhỏ để vào game nhanh */
    public static final int INITIAL_CHUNKS = 3;

    /** Ngân sách thời gian cho chunk gen mỗi frame (ns). 6ms → FPS ~100 */
    public static final long CHUNK_LOAD_BUDGET_NS = 6_000_000L;

    private static final float CHUNK_HALF_DIAG = Chunk.SIZE * 0.72f;

    // ==================== SETTINGS ====================
    /** View radius — đổi runtime qua Settings. */
    private int viewRadius = 6;

    /** Grass draw distance (mét) — đổi runtime qua Settings. */
    private float grassDrawDist = 150f;

    // ====================
    private final Noise noise;
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Frustum frustum = new Frustum();

    private int lastVisibleCount = 0;
    private int lastShadowCount  = 0;
    private boolean firstUpdate = true;

    public Terrain(long seed) {
        this.noise = new Noise(seed);
    }

    // ==================== SETTINGS API ====================

    public void setViewRadius(int r) {
        this.viewRadius = Math.max(2, Math.min(12, r));
    }

    public int getViewRadius() { return viewRadius; }

    public void setGrassDrawDistance(float d) {
        this.grassDrawDist = Math.max(0f, Math.min(400f, d));
    }

    public float getGrassDrawDistance() { return grassDrawDist; }

    // ==================== UPDATE ====================

    public void update(float camX, float camZ) {
        int ccx = worldToChunk(camX);
        int ccz = worldToChunk(camZ);

        int maxInitial = firstUpdate ? INITIAL_CHUNKS : Integer.MAX_VALUE;
        firstUpdate = false;

        List<long[]> missing = null;
        for (int dz = -viewRadius; dz <= viewRadius; dz++) {
            for (int dx = -viewRadius; dx <= viewRadius; dx++) {
                int cx = ccx + dx;
                int cz = ccz + dz;
                long key = key(cx, cz);
                if (!chunks.containsKey(key)) {
                    if (missing == null) missing = new ArrayList<>();
                    long dist2 = (long) dx * dx + (long) dz * dz;
                    missing.add(new long[]{ dist2, cx, cz });
                }
            }
        }

        if (missing != null) {
            missing.sort((a, b) -> Long.compare(a[0], b[0]));

            long budgetEnd = System.nanoTime() + CHUNK_LOAD_BUDGET_NS;
            int loaded = 0;

            for (int i = 0; i < missing.size(); i++) {
                if (loaded > 0 && System.nanoTime() > budgetEnd) break;
                if (loaded >= maxInitial) break;

                long[] m = missing.get(i);
                int cx = (int) m[1];
                int cz = (int) m[2];
                chunks.put(key(cx, cz), new Chunk(cx, cz, noise));
                loaded++;
            }
        }

        List<Long> toRemove = null;
        for (Map.Entry<Long, Chunk> e : chunks.entrySet()) {
            Chunk c = e.getValue();
            if (Math.abs(c.cx - ccx) > viewRadius + 1
             || Math.abs(c.cz - ccz) > viewRadius + 1) {
                c.cleanup();
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(e.getKey());
            }
        }
        if (toRemove != null) {
            for (Long k : toRemove) chunks.remove(k);
        }
    }

    public void render(Camera3D camera) {
        frustum.update(camera);
        int visible = 0;
        for (Chunk c : chunks.values()) {
            if (frustum.isAABBVisible(
                    c.getMinX(), Chunk.BOUND_MIN_Y, c.getMinZ(),
                    c.getMaxX(), Chunk.BOUND_MAX_Y, c.getMaxZ())) {
                c.render();
                visible++;
            }
        }
        lastVisibleCount = visible;
    }

    public void renderShadowPass(float camX, float camZ, float radius) {
        float r2 = radius * radius;
        int count = 0;
        for (Chunk c : chunks.values()) {
            float cx = (c.getMinX() + c.getMaxX()) * 0.5f;
            float cz = (c.getMinZ() + c.getMaxZ()) * 0.5f;
            float dx = cx - camX;
            float dz = cz - camZ;
            if (dx * dx + dz * dz <= r2) {
                c.render();
                count++;
            }
        }
        lastShadowCount = count;
    }

    public void renderGrass(Camera3D camera) {
        frustum.update(camera);
        float maxD = grassDrawDist + CHUNK_HALF_DIAG;
        float maxD2 = maxD * maxD;

        for (Chunk c : chunks.values()) {
            float cx = (c.getMinX() + c.getMaxX()) * 0.5f;
            float cz = (c.getMinZ() + c.getMaxZ()) * 0.5f;
            float dx = cx - camera.position.x;
            float dz = cz - camera.position.z;
            if (dx * dx + dz * dz > maxD2) continue;

            if (frustum.isAABBVisible(
                    c.getMinX(), Chunk.BOUND_MIN_Y, c.getMinZ(),
                    c.getMaxX(), Chunk.BOUND_MAX_Y, c.getMaxZ())) {
                c.renderGrass();
            }
        }
    }

    public void renderGrassShadow(float camX, float camZ, float radius) {
        float r2 = radius * radius;
        for (Chunk c : chunks.values()) {
            float cx = (c.getMinX() + c.getMaxX()) * 0.5f;
            float cz = (c.getMinZ() + c.getMaxZ()) * 0.5f;
            float dx = cx - camX;
            float dz = cz - camZ;
            if (dx * dx + dz * dz <= r2) {
                c.renderGrass();
            }
        }
    }

    public int getLoadedChunks()  { return chunks.size(); }
    public int getVisibleChunks() { return lastVisibleCount; }
    public int getShadowChunks()  { return lastShadowCount; }

    public float heightAt(float x, float z) {
        return Chunk.heightAt(noise, x, z);
    }

    public static int worldToChunk(float world) {
        return (int) Math.floor(world / (float)(CHUNK_SIZE - 1));
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    public void cleanup() {
        for (Chunk c : chunks.values()) c.cleanup();
        chunks.clear();
    }
}
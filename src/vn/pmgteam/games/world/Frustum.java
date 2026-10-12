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

import org.joml.Matrix4f;

/**
 * 6 mặt phẳng của hình nón nhìn (view frustum).
 * Dùng để test AABB nhanh — chunk nào ngoài tầm nhìn thì bỏ qua.
 *
 * Thuật toán Gribb-Hartmann: trích 6 mặt phẳng từ ma trận View*Projection.
 */
public class Frustum {

    // 6 plane × 4 hệ số (nx, ny, nz, d)
    private final float[] planes = new float[24];

    private static final int LEFT   = 0;
    private static final int RIGHT  = 1;
    private static final int BOTTOM = 2;
    private static final int TOP    = 3;
    private static final int NEAR   = 4;
    private static final int FAR    = 5;

    private final float[] tmp = new float[16];

    /** Cập nhật frustum từ camera mỗi frame */
    public void update(Camera3D camera) {
        // viewProj = projection * view
        Matrix4f vp = camera.getProjectionMatrix().mul(camera.getViewMatrix());
        vp.get(tmp);   // column-major: tmp[col*4 + row]

        // Row 0: tmp[0], tmp[4], tmp[8],  tmp[12]
        // Row 1: tmp[1], tmp[5], tmp[9],  tmp[13]
        // Row 2: tmp[2], tmp[6], tmp[10], tmp[14]
        // Row 3: tmp[3], tmp[7], tmp[11], tmp[15]

        float r0x = tmp[0], r0y = tmp[4], r0z = tmp[8],  r0w = tmp[12];
        float r1x = tmp[1], r1y = tmp[5], r1z = tmp[9],  r1w = tmp[13];
        float r2x = tmp[2], r2y = tmp[6], r2z = tmp[10], r2w = tmp[14];
        float r3x = tmp[3], r3y = tmp[7], r3z = tmp[11], r3w = tmp[15];

        setPlane(LEFT,   r3x + r0x, r3y + r0y, r3z + r0z, r3w + r0w);
        setPlane(RIGHT,  r3x - r0x, r3y - r0y, r3z - r0z, r3w - r0w);
        setPlane(BOTTOM, r3x + r1x, r3y + r1y, r3z + r1z, r3w + r1w);
        setPlane(TOP,    r3x - r1x, r3y - r1y, r3z - r1z, r3w - r1w);
        setPlane(NEAR,   r3x + r2x, r3y + r2y, r3z + r2z, r3w + r2w);
        setPlane(FAR,    r3x - r2x, r3y - r2y, r3z - r2z, r3w - r2w);
    }

    private void setPlane(int idx, float a, float b, float c, float d) {
        float len = (float) Math.sqrt(a*a + b*b + c*c);
        if (len < 1e-6f) len = 1f;
        int o = idx * 4;
        planes[o]     = a / len;
        planes[o + 1] = b / len;
        planes[o + 2] = c / len;
        planes[o + 3] = d / len;
    }

    /** Test AABB — trả về false nếu hộp hoàn toàn nằm ngoài frustum */
    public boolean isAABBVisible(float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ) {
        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        float cz = (minZ + maxZ) * 0.5f;
        float hx = (maxX - minX) * 0.5f;
        float hy = (maxY - minY) * 0.5f;
        float hz = (maxZ - minZ) * 0.5f;

        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float nx = planes[o];
            float ny = planes[o + 1];
            float nz = planes[o + 2];
            float d  = planes[o + 3];

            float dist = nx * cx + ny * cy + nz * cz + d;
            float radius = Math.abs(nx) * hx + Math.abs(ny) * hy + Math.abs(nz) * hz;

            if (dist + radius < 0f) return false;   // nằm ngoài plane này → cull
        }
        return true;
    }
}
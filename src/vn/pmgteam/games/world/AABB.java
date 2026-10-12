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

/**
 * Axis-aligned bounding box — dùng cho collision.
 * Immutable sau khi tạo.
 */
public final class AABB {

    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ,
                float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /** AABB có giao với AABB khác không */
    public boolean intersects(float oMinX, float oMinY, float oMinZ,
                              float oMaxX, float oMaxY, float oMaxZ) {
        return oMinX < maxX && oMaxX > minX
            && oMinY < maxY && oMaxY > minY
            && oMinZ < maxZ && oMaxZ > minZ;
    }

    public float getWidth()  { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth()  { return maxZ - minZ; }
    public float getCenterX(){ return (minX + maxX) * 0.5f; }
    public float getCenterY(){ return (minY + maxY) * 0.5f; }
    public float getCenterZ(){ return (minZ + maxZ) * 0.5f; }
}
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

package vn.pmgteam.games.placeable;

import java.util.Collections;
import java.util.List;

import org.joml.Vector3f;

import vn.pmgteam.games.main.Game;
import vn.pmgteam.games.world.AABB;

public abstract class Placeable {

    public final Vector3f position = new Vector3f();
    public float yaw   = 0f;
    public long  ownerId = 0;
    public long  createdAt = System.currentTimeMillis();

    public Placeable(float x, float y, float z) {
        position.set(x, y, z);
    }

    public abstract void render(Game game);
    public abstract void interact(Game game);
    public void update(float dt) { }
    public abstract float getRadius();
    public abstract float getHeight();
    public abstract String getType();
    public String getDisplayName() { return getType(); }

    /** Có va chạm với player không. Mặc định true. */
    public boolean isSolid() { return true; }

    /**
     * Danh sách AABB collision.
     * Mặc định: 1 hộp từ position + radius + height.
     * Override cho building phức tạp (nhiều tường, chừa cửa).
     */
    public List<AABB> getCollisionBoxes() {
        float r = getRadius();
        AABB box = new AABB(
                position.x - r, position.y, position.z - r,
                position.x + r, position.y + getHeight(), position.z + r
        );
        return Collections.singletonList(box);
    }
}
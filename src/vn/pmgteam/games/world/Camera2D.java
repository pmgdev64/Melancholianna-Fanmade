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

public class Camera2D {

    public float x, y;
    public float viewWidth  = 1280;
    public float viewHeight = 720;

    private final Matrix4f projection = new Matrix4f();

    public Matrix4f getProjection() {
        float hw = viewWidth  / 2f;
        float hh = viewHeight / 2f;
        return projection.identity().ortho2D(
                x - hw, x + hw,
                y - hh, y + hh
        );
    }

    public void follow(float tx, float ty, float lerp, float dt) {
        float k = Math.min(1f, lerp * dt);
        x += (tx - x) * k;
        y += (ty - y) * k;
    }
}
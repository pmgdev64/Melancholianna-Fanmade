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

import vn.pmgteam.games.main.Game;

public class Banner extends Placeable {

    public float r = 0.85f, g = 0.20f, b = 0.20f;   // đỏ
    public String name = "Lãnh địa";

    public Banner(float x, float y, float z) {
        super(x, y, z);
    }

    @Override
    public void render(Game game) {
        // Cột gỗ
        game.drawBox(position.x, position.y + 1.75f, position.z,
                0.15f, 3.5f, 0.15f,
                new float[]{ 0.40f, 0.28f, 0.18f, 1f });

        // Cờ
        game.drawBox(position.x + 0.65f, position.y + 3.0f, position.z,
                1.30f, 1.00f, 0.05f,
                new float[]{ r, g, b, 1f });

        // Chóp vàng
        game.drawBox(position.x, position.y + 3.60f, position.z,
                0.20f, 0.20f, 0.20f,
                new float[]{ 0.90f, 0.80f, 0.20f, 1f });
    }

    @Override
    public void interact(Game game) {
        System.out.println("Banner: " + name);
    }

    @Override public float getRadius() { return 0.4f; }
    @Override public float getHeight() { return 3.7f; }
    @Override public String getType()  { return "banner"; }
    @Override public String getDisplayName() { return "Cờ: " + name; }
}
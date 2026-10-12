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

package vn.pmgteam.games.model;

import java.util.ArrayList;
import java.util.List;

public class Model {
    public String name;
    public final List<Mesh> meshes = new ArrayList<>();
    public Skeleton skeleton = null;
    public final List<Animation> animations = new ArrayList<>();

    public void upload() {
        for (Mesh m : meshes) {
            m.upload();
        }
    }

    /**
     * Tìm animation theo tên.
     * 1) Exact case-insensitive
     * 2) Contains cả 2 chiều (xử lý "Idle" ↔ "Armature|Idle")
     */
    public Animation getAnimation(String name) {
        for (Animation a : animations) {
            if (a.name.equalsIgnoreCase(name)) return a;
        }

        String lower = name.toLowerCase();
        for (Animation a : animations) {
            String n = a.name.toLowerCase();
            if (n.contains(lower) || lower.contains(n)) return a;
        }
        return null;
    }

    public Animation getAnyAnimation() {
        return animations.isEmpty() ? null : animations.get(0);
    }
}
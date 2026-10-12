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

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Bone {
    public int index;
    public String name = "";
    public int parentIndex = -1;

    /** Node gốc trong glTF (dùng để map animation channel). */
    public int sourceNode = -1;

    // === Bind pose (từ node TRS trong glTF) ===
    public final Vector3f    bindTranslation = new Vector3f();
    public final Quaternionf bindRotation    = new Quaternionf();
    public final Vector3f    bindScale       = new Vector3f(1, 1, 1);

    // === Current pose (animator cập nhật mỗi frame) ===
    public final Vector3f    currentTranslation = new Vector3f();
    public final Quaternionf currentRotation    = new Quaternionf();
    public final Vector3f    currentScale       = new Vector3f(1, 1, 1);

    // === Ma trận tính toán ===
    public final Matrix4f localTransform    = new Matrix4f();
    public final Matrix4f worldTransform    = new Matrix4f();
    public final Matrix4f inverseBindMatrix = new Matrix4f();
    public final Matrix4f finalMatrix       = new Matrix4f();

    public void resetToBind() {
        currentTranslation.set(bindTranslation);
        currentRotation.set(bindRotation);
        currentScale.set(bindScale);
    }

    public void computeLocal() {
        localTransform.identity()
                .translate(currentTranslation)
                .rotate(currentRotation)
                .scale(currentScale);
    }
}
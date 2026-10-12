/*
 * Melanchoianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

public class Skeleton {
    public final List<Bone> bones = new ArrayList<>();
    public final Map<Integer, Integer> nodeToBone = new HashMap<>();

    public final Matrix4f ancestorMatrix = new Matrix4f();

    public void computeWorldTransforms() {
        for (Bone b : bones) {
            b.computeLocal();
        }
        for (Bone b : bones) {
            if (b.parentIndex < 0) {
                ancestorMatrix.mul(b.localTransform, b.worldTransform);
            } else {
                bones.get(b.parentIndex).worldTransform
                        .mul(b.localTransform, b.worldTransform);
            }
            b.worldTransform.mul(b.inverseBindMatrix, b.finalMatrix);
        }
    }
}
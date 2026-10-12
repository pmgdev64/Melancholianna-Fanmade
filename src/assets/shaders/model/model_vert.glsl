// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec4 aWeights;
layout (location = 3) in vec4 aJoints;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform mat3 uNormalMatrix;
uniform mat4 uLightSpace;
uniform mat4 uBoneMatrices[128];

out vec3 vNormal;
out vec3 vWorldPos;
out vec4 vLightSpacePos;

void main() {
    // Clamp joint index — an toàn nếu model có joint vượt 128 (hiếm)
    // hoặc nếu accessor đọc sai (garbage index)
    int j0 = clamp(int(aJoints.x), 0, 127);
    int j1 = clamp(int(aJoints.y), 0, 127);
    int j2 = clamp(int(aJoints.z), 0, 127);
    int j3 = clamp(int(aJoints.w), 0, 127);

    mat4 skin =
        aWeights.x * uBoneMatrices[j0] +
        aWeights.y * uBoneMatrices[j1] +
        aWeights.z * uBoneMatrices[j2] +
        aWeights.w * uBoneMatrices[j3];

    vec4 localPos    = skin * vec4(aPos, 1.0);
    vec3 localNormal = mat3(skin) * aNormal;

    vec4 world = uModel * localPos;

    gl_Position    = uProjection * uView * world;
    vNormal        = normalize(uNormalMatrix * localNormal);
    vWorldPos      = world.xyz;
    vLightSpacePos = uLightSpace * world;
}
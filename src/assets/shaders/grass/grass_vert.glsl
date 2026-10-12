// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTint;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uLightSpace;
uniform float uTime;

out vec2 vUV;
out vec3 vTint;
out vec3 vNormal;
out vec3 vWorldPos;
out vec4 vLightSpacePos;

void main() {
    // Gió: ngọn lắc, gốc đứng yên. Pha lệch theo vị trí thế giới.
    float sway  = aUV.y * aUV.y;
    float phase = aPos.x * 0.35 + aPos.z * 0.25;

    vec3 p = aPos;
    p.x += (sin(uTime * 1.7 + phase) * 0.10 + sin(uTime * 3.1 + phase * 2.3) * 0.03) * sway;
    p.z += (cos(uTime * 1.3 + phase * 0.8) * 0.07) * sway;

    vec4 world = vec4(p, 1.0);
    gl_Position    = uProjection * uView * world;
    vUV            = aUV;
    vTint          = aTint;
    vNormal        = aNormal;
    vWorldPos      = p;
    vLightSpacePos = uLightSpace * world;
}
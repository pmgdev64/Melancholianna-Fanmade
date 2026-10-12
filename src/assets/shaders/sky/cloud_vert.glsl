// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec3 aDir;

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uCameraPos;
uniform float uDistance;

out vec3 vDir;

void main() {
    vDir = aDir;
    vec3 worldPos = uCameraPos + aDir * uDistance;
    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
}
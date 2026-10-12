// Melancholianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melancholianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in float aSize;
layout (location = 2) in float aPhase;

uniform mat4 uProjection;
uniform mat4 uView;

out float vRotation;
out float vSize;

void main() {
    gl_Position = uProjection * uView * vec4(aPos, 1.0);
    gl_PointSize = aSize;
    vRotation = aPhase;
    vSize = aSize;
}
// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 2) in vec2 aUV;

uniform mat4 uLightSpace;

out vec2 vUV;

void main() {
    vUV = aUV;
    gl_Position = uLightSpace * vec4(aPos, 1.0);
}
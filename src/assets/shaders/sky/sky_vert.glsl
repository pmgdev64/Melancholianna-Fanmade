// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
layout (location = 0) in vec2 aPos;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUV;

void main() {
    vUV = aPos;
    gl_Position = uProjection * uView * uModel * vec4(aPos, 0.0, 1.0);
}
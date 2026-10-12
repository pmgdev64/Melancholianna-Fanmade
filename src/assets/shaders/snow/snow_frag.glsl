// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in float vSize;

uniform float uOpacity;

out vec4 FragColor;

void main() {
    // Bo tròn point — discard ngoài circle
    vec2 c = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(c, c);
    if (r2 > 1.0) discard;

    // Mềm viền
    float a = (1.0 - r2) * uOpacity;

    // Hạt to thì mờ hơn (giả 3D depth)
    if (vSize > 4.0) a *= 0.7;

    FragColor = vec4(1.0, 1.0, 1.0, a);
}
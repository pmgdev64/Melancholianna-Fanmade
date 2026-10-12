// Melancholianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melancholianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in float vRotation;

uniform vec3      uPetalColor;
uniform float     uOpacity;
uniform sampler2D uPetalTex;

out vec4 FragColor;

void main() {
    vec2 p = gl_PointCoord - 0.5;
    float c = cos(vRotation);
    float s = sin(vRotation);
    vec2 rp = vec2(c * p.x - s * p.y,
                   s * p.x + c * p.y);
    vec2 uv = rp + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 ||
        uv.y < 0.0 || uv.y > 1.0) {
        discard;
    }

    vec4 tex = texture(uPetalTex, uv);
    if (tex.a < 0.05) discard;

    vec3 finalRGB = tex.rgb * uPetalColor;
    float finalA  = tex.a * uOpacity;

    FragColor = vec4(finalRGB, finalA);
}
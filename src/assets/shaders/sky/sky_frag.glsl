// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in vec2 vUV;

uniform vec3 uCoreColor;
uniform vec3 uGlowColor;

out vec4 FragColor;

void main() {
    // vUV trong [-1, 1]. Khoảng cách từ tâm (0,0)
    float d = length(vUV);

    // Lõi — đĩa đặc, viền mềm
    float disc = 1.0 - smoothstep(0.55, 0.72, d);

    // Quầng sáng — tan dần ra ngoài
    float glow = exp(-d * d * 3.0) * 0.65;

    // Trộn: lõi sáng ở giữa, quầng màu glow
    vec3 col = mix(uGlowColor, uCoreColor, disc);
    float alpha = max(disc, glow);

    // Bỏ pixel gần như trong suốt (tối ưu)
    if (alpha < 0.02) discard;

    FragColor = vec4(col, alpha);
}
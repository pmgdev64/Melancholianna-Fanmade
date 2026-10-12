// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in vec3 vDir;

uniform float uTime;
uniform vec3 uCloudColor;
uniform vec3 uCloudShadowColor;
uniform float uCloudAmount;
uniform sampler2D uNoiseTex;

out vec4 FragColor;

void main() {
    vec3 dir = normalize(vDir);

    if (dir.y < 0.12) discard;

    vec2 baseUV = dir.xz / dir.y;

    // Chỉ 2 layer scroll — giảm 1 texture fetch
    vec2 uv1 = baseUV * 0.42 + vec2( uTime * 0.0080,  uTime * 0.0040);
    vec2 uv2 = baseUV * 0.85 + vec2(-uTime * 0.0050,  uTime * 0.0065 + 3.7);

    float n1 = texture(uNoiseTex, uv1).r;
    float n2 = texture(uNoiseTex, uv2).r;

    float density = n1 * 0.65 + n2 * 0.35;

    float alpha = smoothstep(0.45, 0.72, density);

    float horizonFade = smoothstep(0.12, 0.35, dir.y);
    alpha *= horizonFade * uCloudAmount;

    if (alpha < 0.02) discard;

    float shade = n2;
    vec3 col = mix(uCloudShadowColor, uCloudColor, smoothstep(0.30, 0.75, shade));

    FragColor = vec4(col, alpha * 0.92);
}
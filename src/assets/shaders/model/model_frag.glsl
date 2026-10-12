// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in vec3 vNormal;
in vec3 vWorldPos;
in vec4 vLightSpacePos;

uniform vec3 uCameraPos;
uniform vec3 uSunDirection;
uniform vec3 uSkyColor;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform vec3 uBaseColor;
uniform sampler2D uShadowMap;

out vec4 FragColor;

const vec2 POISSON[6] = vec2[](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696,  0.457),
    vec2(-0.203,  0.621), vec2( 0.962, -0.195), vec2( 0.473, -0.480)
);

float calculateShadow(vec4 lsp, float ndotl) {
    if (lsp.w <= 0.0) return 0.0;
    vec3 proj = lsp.xyz / lsp.w;
    proj = proj * 0.5 + 0.5;
    if (proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) return 0.0;
    float bias = max(0.0018 * (1.0 - ndotl), 0.0004);
    vec2 texel = 1.0 / textureSize(uShadowMap, 0);
    float s = 0.0;
    for (int k = 0; k < 6; k++) {
        vec2 off = POISSON[k] * texel * 1.6;
        s += (proj.z - bias > texture(uShadowMap, proj.xy + off).r) ? 1.0 : 0.0;
    }
    return s / 6.0;
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uSunDirection);
    float diff = max(dot(N, L), 0.0);

    vec3 ambientColor = vec3(uAmbient) * mix(vec3(1.0), uSkyColor * 1.2, 0.35);

    float shadowStrength = clamp(uAmbient * 2.0, 0.3, 1.0);
    float shadow = calculateShadow(vLightSpacePos, diff);

    vec3 lightContribution = ambientColor
                           + uSunColor * diff * 0.65 * (1.0 - shadow * shadowStrength);

    vec3 litColor = uBaseColor * lightContribution;

    float dist = length(vWorldPos - uCameraPos);
    float fogFactor = clamp((dist - 100.0) / 200.0, 0.0, 1.0);

    vec3 finalColor = mix(litColor, uSkyColor, fogFactor);
    FragColor = vec4(finalColor, 1.0);
}
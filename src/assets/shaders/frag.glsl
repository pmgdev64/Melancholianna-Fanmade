// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in vec3 vNormal;
in vec3 vColor;
in vec2 vUV;
in vec4 vWeights;
in vec3 vWorldPos;
in vec4 vLightSpacePos;

uniform vec3 uCameraPos;
uniform vec3 uSunDirection;
uniform vec3 uSkyColor;
uniform vec3 uSunColor;
uniform float uAmbient;
uniform vec3 uColorOverride;
uniform float uUseOverride;
uniform sampler2D uShadowMap;

uniform sampler2D uTexGrass;
uniform sampler2D uTexRock;
uniform sampler2D uTexSand;
uniform sampler2D uTexSnow;

out vec4 FragColor;

const vec2 POISSON[6] = vec2[](
    vec2(-0.326, -0.406),
    vec2(-0.840, -0.074),
    vec2(-0.696,  0.457),
    vec2(-0.203,  0.621),
    vec2( 0.962, -0.195),
    vec2( 0.473, -0.480)
);

float calculateShadow(vec4 lightSpacePos, float ndotl) {
    if (lightSpacePos.w <= 0.0) return 0.0;
    vec3 proj = lightSpacePos.xyz / lightSpacePos.w;
    proj = proj * 0.5 + 0.5;
    if (proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) return 0.0;

    float bias = max(0.005 * (1.0 - ndotl), 0.0012);

    vec2 texel = 1.0 / textureSize(uShadowMap, 0);
    float s = 0.0;
    for (int k = 0; k < 6; k++) {
        vec2 off = POISSON[k] * texel * 1.6;
        s += (proj.z - bias > texture(uShadowMap, proj.xy + off).r) ? 1.0 : 0.0;
    }
    return s / 6.0;
}

vec3 sampleTerrainSplat(vec2 uv, vec4 w) {
    vec3 c1 = texture(uTexGrass, uv).rgb * w.x
            + texture(uTexRock,  uv).rgb * w.y
            + texture(uTexSand,  uv).rgb * w.z
            + texture(uTexSnow,  uv).rgb * w.w;

    vec2 uv2 = uv * 0.2317;
    vec3 c2 = texture(uTexGrass, uv2).rgb * w.x
            + texture(uTexRock,  uv2).rgb * w.y
            + texture(uTexSand,  uv2).rgb * w.z
            + texture(uTexSnow,  uv2).rgb * w.w;

    vec2 uv3 = uv * 0.0813 + vec2(0.37, 0.61);
    vec3 c3 = texture(uTexGrass, uv3).rgb * w.x
            + texture(uTexRock,  uv3).rgb * w.y
            + texture(uTexSand,  uv3).rgb * w.z
            + texture(uTexSnow,  uv3).rgb * w.w;

    vec3 c = mix(c1, c2, 0.40);
    return mix(c, c3, 0.25);
}

void main() {
    vec3 baseColor;
    if (uUseOverride > 0.5) {
        baseColor = uColorOverride;
    } else {
        baseColor = sampleTerrainSplat(vUV, vWeights) * vColor;
    }

    vec3 N = normalize(vNormal);
    vec3 L = normalize(uSunDirection);
    float diff = max(dot(N, L), 0.0);

    vec3 ambientColor = vec3(uAmbient) * mix(vec3(1.0), uSkyColor * 1.2, 0.35);
    vec3 sunColor = uSunColor;

    float shadowStrength = clamp(uAmbient * 2.0, 0.3, 1.0);
    float shadow = calculateShadow(vLightSpacePos, diff);

    vec3 lightContribution = ambientColor
                           + sunColor * diff * 0.65 * (1.0 - shadow * shadowStrength);

    vec3 litColor = baseColor * lightContribution;

    float dist = length(vWorldPos - uCameraPos);
    float fogStart = 100.0, fogEnd = 300.0;
    float fogFactor = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);

    vec3 finalColor = mix(litColor, uSkyColor, fogFactor);

    FragColor = vec4(finalColor, 1.0);
}
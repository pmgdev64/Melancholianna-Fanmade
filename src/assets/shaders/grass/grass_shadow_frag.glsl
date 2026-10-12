// Melanchoianna 3D - Fan-made Adventure RPG
// Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
// Based on "Melanchoianna" by Omochi Kobo (おもち工房).
// Unofficial fan game, not affiliated with the original creators.
// SPDX-License-Identifier: GPL-3.0-or-later

#version 330 core
in vec2 vUV;

uniform sampler2D uGrassTex;

void main() {
    if (texture(uGrassTex, vUV).a < 0.25) discard;
}
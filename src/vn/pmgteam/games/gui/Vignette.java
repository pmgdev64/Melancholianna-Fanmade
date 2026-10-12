/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.gui;

/**
 * Vignette 4 cạnh — grid N×N với alpha interpolate per vertex.
 * Cache theo kích thước màn hình + tham số alpha/size.
 */
public final class Vignette {

    private static final int GRID = 24;

    private static float[][] alphaGrid = null;
    private static int[] pixelX = null;
    private static int[] pixelY = null;
    private static int cachedW = -1, cachedH = -1;
    private static float cachedAlpha = 0f, cachedSize = 0f;

    private Vignette() {}

    public static void draw(GuiRenderer gui, int w, int h,
                            float alpha, float sizeRatio) {
        if (alphaGrid == null
                || cachedW != w || cachedH != h
                || cachedAlpha != alpha || cachedSize != sizeRatio) {
            build(w, h, alpha, sizeRatio);
        }

        for (int i = 0; i < GRID; i++) {
            int x0 = pixelX[i];
            int x1 = pixelX[i + 1];
            float cw = x1 - x0;

            for (int j = 0; j < GRID; j++) {
                int y0 = pixelY[j];
                int y1 = pixelY[j + 1];
                float ch = y1 - y0;

                float aTL = alphaGrid[i][j];
                float aTR = alphaGrid[i + 1][j];
                float aBL = alphaGrid[i][j + 1];
                float aBR = alphaGrid[i + 1][j + 1];

                if (aTL < 0.003f && aTR < 0.003f
                 && aBL < 0.003f && aBR < 0.003f) continue;

                gui.rectGrad(x0, y0, cw, ch,
                        0f, 0f, 0f,
                        aTL, aTR, aBL, aBR);
            }
        }
    }

    private static void build(int w, int h, float alpha, float sizeRatio) {
        alphaGrid = new float[GRID + 1][GRID + 1];
        pixelX = new int[GRID + 1];
        pixelY = new int[GRID + 1];

        for (int i = 0; i <= GRID; i++) {
            pixelX[i] = Math.round(i * (w / (float) GRID));
            pixelY[i] = Math.round(i * (h / (float) GRID));
        }
        pixelX[GRID] = w;
        pixelY[GRID] = h;

        float band = Math.min(w, h) * sizeRatio;

        for (int i = 0; i <= GRID; i++) {
            float x = pixelX[i];
            float dx = Math.min(x, w - x);

            for (int j = 0; j <= GRID; j++) {
                float y = pixelY[j];
                float dy = Math.min(y, h - y);

                float d = Math.min(dx, dy);
                float t = Math.min(1f, d / band);
                float s = t * t * (3f - 2f * t);
                alphaGrid[i][j] = alpha * (1f - s);
            }
        }

        cachedW = w;
        cachedH = h;
        cachedAlpha = alpha;
        cachedSize = sizeRatio;
    }
}
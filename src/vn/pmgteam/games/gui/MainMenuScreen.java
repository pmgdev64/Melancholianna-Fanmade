/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.gui;

import vn.pmgteam.games.audio.AudioMaster;
import vn.pmgteam.games.audio.SoundCategory;
import vn.pmgteam.games.audio.Sounds;

public class MainMenuScreen extends GuiScreen {

    private static final float[] WHITE     = { 0.96f, 0.96f, 0.96f };
    private static final float[] WHITE_DIM = { 0.72f, 0.72f, 0.72f };
    private static final float[] GREY      = { 0.55f, 0.55f, 0.55f };

    private static final float RIGHT_MARGIN = 80f;
    private static final float MENU_WIDTH   = 340f;
    private static final float ITEM_H       = 50f;
    private static final float ITEM_GAP     = 16f;

    private static final float VIGNETTE_ALPHA = 0.55f;
    private static final float VIGNETTE_SIZE  = 0.32f;

    private float time = 0f;

    @Override
    protected void buildLayout() {
        float menuRight  = screenWidth - RIGHT_MARGIN;
        float menuStartY = screenHeight * 0.40f;
        float x = menuRight - MENU_WIDTH;

        buttons.add(new GuiButton(x, menuStartY,
                MENU_WIDTH, ITEM_H, "New Game", () -> {
            AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
            game.startGame();
        }));

        buttons.add(new GuiButton(x, menuStartY + ITEM_H + ITEM_GAP,
                MENU_WIDTH, ITEM_H, "Settings", () -> {
            AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
            game.openScreen(new SettingsScreen());
        }));

        buttons.add(new GuiButton(x, menuStartY + (ITEM_H + ITEM_GAP) * 2,
                MENU_WIDTH, ITEM_H, "Quit", () -> {
            AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
            game.quit();
        }));
    }

    @Override
    public void update(float dt, double mx, double my) {
        time += dt;
        super.update(dt, mx, my);
    }

    @Override
    public boolean keyPress(int key, int action) { return false; }

    @Override
    public void render(GuiRenderer gui, FontRenderer font) {
        Vignette.draw(gui, screenWidth, screenHeight,
                VIGNETTE_ALPHA, VIGNETTE_SIZE);

        float menuRight = screenWidth - RIGHT_MARGIN;

        float menuTopLine = buttons.get(0).y - 24f;
        drawLineShadowed(gui,
                menuRight - MENU_WIDTH, menuTopLine,
                MENU_WIDTH, 1f, 0.55f);

        for (GuiButton b : buttons) {
            boolean hov = b.hovered;

            String label = b.label.toUpperCase();
            float tScale = 0.85f;
            float tTracking = 10f;
            float tW = trackedWidth(font, label, tScale, tTracking);
            float tX = menuRight - tW;

            float textH = font.getLineHeight() * tScale;
            float textY = b.y + (b.h - textH) * 0.5f - 2f;

            drawTracked(gui, font, label, tX + 2, textY + 2,
                    tScale, tTracking, 0f, 0f, 0f, 0.75f);
            drawTracked(gui, font, label, tX + 1, textY + 1,
                    tScale, tTracking, 0f, 0f, 0f, 0.55f);

            float cr = hov ? WHITE[0] : GREY[0];
            float cg = hov ? WHITE[1] : GREY[1];
            float cb = hov ? WHITE[2] : GREY[2];

            drawTracked(gui, font, label, tX, textY,
                    tScale, tTracking, cr, cg, cb, 1f);

            float lineY = b.y + b.h - 6f;
            float lineAlpha = hov ? 0.90f : 0.35f;
            drawLineShadowed(gui,
                    menuRight - MENU_WIDTH, lineY,
                    MENU_WIDTH, 1f, lineAlpha);

            if (hov) {
                drawButterfly(gui, tX - 30f, b.y + b.h * 0.5f);
            }
        }

        GuiButton last = buttons.get(buttons.size() - 1);
        float menuBottomLine = last.y + last.h + 10f;
        drawLineShadowed(gui,
                menuRight - MENU_WIDTH, menuBottomLine,
                MENU_WIDTH, 1f, 0.55f);

        String footer = "Based on \"Melancholianna\" by Omochi Kobo  —  Fan project, GPL-3.0";
        float footerY = screenHeight - 34f;

        gui.textCentered(footer, screenWidth * 0.5f + 1f, footerY + 1f,
                0.42f, 0f, 0f, 0f, 0.7f);
        gui.textCentered(footer, screenWidth * 0.5f, footerY,
                0.42f, WHITE_DIM[0], WHITE_DIM[1], WHITE_DIM[2], 0.95f);
    }

    // ==================== HELPERS ====================

    private void drawLineShadowed(GuiRenderer gui,
                                   float x, float y, float w, float h,
                                   float alpha) {
        gui.rect(x, y + 1f, w, h, 0f, 0f, 0f, alpha * 0.55f);
        gui.rect(x, y, w, h, WHITE[0], WHITE[1], WHITE[2], alpha);
    }

    private void drawTracked(GuiRenderer gui, FontRenderer font, String text,
                              float x, float y, float scale, float tracking,
                              float r, float g, float b, float a) {
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            gui.text(String.valueOf(c), cx, y, scale, r, g, b, a);
            cx += font.getGlyph(c).advance * scale + tracking;
        }
    }

    private float trackedWidth(FontRenderer font, String text,
                                float scale, float tracking) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += font.getGlyph(text.charAt(i)).advance * scale;
            if (i < text.length() - 1) w += tracking;
        }
        return w;
    }

    private void drawButterfly(GuiRenderer gui, float cx, float cy) {
        final float r = 1.0f, g = 1.0f, b = 1.0f;
        final float A = 0.92f;

        float sx = cx + 1f, sy = cy + 1f;
        final float SA = 0.55f;

        gui.rect(sx - 8, sy - 9, 6, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 9, sy - 6, 7, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 9, sy - 3, 7, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 7, sy - 0, 5, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy - 9, 6, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy - 6, 7, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy - 3, 7, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy - 0, 5, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 7, sy + 3, 5, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 6, sy + 6, 4, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy + 3, 5, 3, 0f, 0f, 0f, SA);
        gui.rect(sx + 2, sy + 6, 4, 3, 0f, 0f, 0f, SA);
        gui.rect(sx - 1, sy - 6, 2, 15, 0f, 0f, 0f, SA);
        gui.rect(sx - 2, sy - 9, 4, 4, 0f, 0f, 0f, SA);

        gui.rect(cx - 8, cy - 9, 6, 3, r, g, b, A);
        gui.rect(cx - 9, cy - 6, 7, 3, r, g, b, A);
        gui.rect(cx - 9, cy - 3, 7, 3, r, g, b, A);
        gui.rect(cx - 7, cy - 0, 5, 3, r, g, b, A * 0.85f);

        gui.rect(cx + 2, cy - 9, 6, 3, r, g, b, A);
        gui.rect(cx + 2, cy - 6, 7, 3, r, g, b, A);
        gui.rect(cx + 2, cy - 3, 7, 3, r, g, b, A);
        gui.rect(cx + 2, cy - 0, 5, 3, r, g, b, A * 0.85f);

        gui.rect(cx - 7, cy + 3, 5, 3, r, g, b, A * 0.85f);
        gui.rect(cx - 6, cy + 6, 4, 3, r, g, b, A * 0.72f);
        gui.rect(cx + 2, cy + 3, 5, 3, r, g, b, A * 0.85f);
        gui.rect(cx + 2, cy + 6, 4, 3, r, g, b, A * 0.72f);

        gui.rect(cx - 1, cy - 6, 2, 15, r, g, b, 1f);
        gui.rect(cx - 2, cy - 9, 4, 4, r, g, b, 1f);

        gui.rect(cx - 4, cy - 12, 1, 4, r, g, b, A * 0.85f);
        gui.rect(cx + 3, cy - 12, 1, 4, r, g, b, A * 0.85f);
    }
}
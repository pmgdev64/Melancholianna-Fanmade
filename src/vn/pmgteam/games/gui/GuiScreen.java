/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.gui;

import java.util.ArrayList;
import java.util.List;

import vn.pmgteam.games.main.Game;

public abstract class GuiScreen {

    protected Game game;
    protected int screenWidth;
    protected int screenHeight;

    /** Vị trí chuột hiện tại (top-left origin). Update mỗi frame trong update(). */
    protected double mouseX = 0;
    protected double mouseY = 0;

    protected final List<GuiButton> buttons = new ArrayList<>();

    public final void init(Game game, int w, int h) {
        this.game = game;
        this.screenWidth = w;
        this.screenHeight = h;
        this.buttons.clear();
        buildLayout();
    }

    protected abstract void buildLayout();
    public abstract void render(GuiRenderer gui, FontRenderer font);

    public void update(float dt, double mx, double my) {
        this.mouseX = mx;
        this.mouseY = my;

        for (GuiButton b : buttons) {
            b.hovered = b.enabled && b.isHovered(mx, my);
        }
    }

    public boolean mouseClick(double mx, double my, int button) {
        if (button != 0) return false;
        for (GuiButton b : buttons) {
            if (b.enabled && b.isHovered(mx, my)) {
                b.onClick();
                return true;
            }
        }
        return false;
    }

    public void mouseRelease(double mx, double my, int button) { }

    /** Subclass override để xử lý cuộn chuột. */
    public void onScroll(double delta) { }

    public boolean keyPress(int key, int action) {
        return false;
    }

    public void onOpen()  { }
    public void onClose() { }

    public void onResize(int w, int h) {
        init(game, w, h);
    }
}
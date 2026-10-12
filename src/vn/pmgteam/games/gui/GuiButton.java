/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.gui;

public class GuiButton {

    public float x, y, w, h;
    public String label;
    public Runnable action;

    public boolean hovered = false;
    public boolean enabled = true;

    public GuiButton(float x, float y, float w, float h,
                     String label, Runnable action) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.label = label;
        this.action = action;
    }

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + w
            && my >= y && my <= y + h;
    }

    public void onClick() {
        if (enabled && action != null) action.run();
    }
}
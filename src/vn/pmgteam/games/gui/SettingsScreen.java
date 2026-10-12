/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.gui;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import vn.pmgteam.games.audio.AudioMaster;
import vn.pmgteam.games.audio.SoundCategory;
import vn.pmgteam.games.audio.Sounds;

public class SettingsScreen extends GuiScreen {

    // ==================== TABS ====================
    private static final int TAB_GRAPHICS = 0;
    private static final int TAB_CONTROLS = 1;
    private static final int TAB_AUDIO    = 2;
    private static final int TAB_GAMEPLAY = 3;

    private static final String[] TAB_NAMES = {
        "GRAPHICS", "CONTROLS", "AUDIO", "GAMEPLAY"
    };

    private int activeTab = TAB_GRAPHICS;

    // ==================== COLORS ====================
    private static final float[] GOLD          = { 1.00f, 0.85f, 0.40f };
    private static final float[] WHITE         = { 0.96f, 0.96f, 0.96f };
    private static final float[] WHITE_DIM     = { 0.72f, 0.72f, 0.72f };
    private static final float[] GREY_DIM      = { 0.40f, 0.40f, 0.40f };
    private static final float[] RESTART_AMBER = { 1.00f, 0.65f, 0.30f };

    // ==================== LAYOUT ====================
    private static final float VIGNETTE_ALPHA = 0.62f;
    private static final float VIGNETTE_SIZE  = 0.32f;

    private static final float CONTENT_W = 680f;
    private static final float ROW_H     = 68f;
    private static final float GROUP_H   = 34f;
    private static final float TRACK_W   = 260f;
    private static final float VALUE_W   = 160f;

    private static final float VIEW_TOP_RATIO    = 0.30f;
    private static final float VIEW_BOTTOM_RATIO = 0.92f;

    // ==================== WIDGETS ====================
    private static class Toggle {
        float x, y, w, h;
        Runnable onClick;
    }

    private static class Slider {
        float x, y, w, h;
        Supplier<Float> getValue;
        Consumer<Float> setValue;
    }

    private static class GroupHeader {
        String title;
        float y;
    }

    private final List<Toggle> toggles = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();
    private final List<GroupHeader> groups = new ArrayList<>();
    private Slider draggingSlider = null;

    private float contentHeight = 0f;
    private float scrollY       = 0f;

    private final float[] tabX = new float[TAB_NAMES.length];
    private final float[] tabW = new float[TAB_NAMES.length];
    private float tabY, tabH;

    private float backX, backY, backW, backH;

    // ==================== DATA ====================
    private static final String[][] BINDINGS = {
        { "WASD",  "Move" },
        { "Space", "Jump / Fly up" },
        { "Ctrl",  "Fly down" },
        { "Shift", "Run" },
        { "F",     "Toggle fly" },
        { "E",     "Interact" },
        { "B",     "Place banner" },
        { "R",     "Remove nearest" },
        { "U",     "Unstick" },
        { "T",     "Fast time" },
        { "N",     "Toggle snow" },
        { "M",     "Toggle petal" },
        { "F3",    "Debug panel" },
    };

    private static final Object[][] AUDIO_ROWS = {
        { SoundCategory.MASTER,  "Master"    },
        { SoundCategory.MUSIC,   "Music"     },
        { SoundCategory.SFX,     "SFX"       },
        { SoundCategory.AMBIENT, "Ambient"   },
        { SoundCategory.UI,      "Interface" },
    };

    // ==================== INIT ====================
    @Override
    protected void buildLayout() {
        buttons.clear();

        float tabW_est = 200f;
        float gap = 24f;
        float totalW = tabW_est * TAB_NAMES.length + gap * (TAB_NAMES.length - 1);
        float startX = (screenWidth - totalW) * 0.5f;

        tabY = screenHeight * 0.20f;
        tabH = 40f;

        for (int i = 0; i < TAB_NAMES.length; i++) {
            tabX[i] = startX + i * (tabW_est + gap);
            tabW[i] = tabW_est;
        }

        backW = 120f;
        backH = 36f;
        backX = screenWidth - backW - 50f;
        backY = 30f;

        scrollY = 0f;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        toggles.clear();
        sliders.clear();
        groups.clear();

        float contentX = (screenWidth - CONTENT_W) * 0.5f;
        float valueX = contentX + CONTENT_W - VALUE_W;
        float trackX = contentX + 240f;

        float y = 0f;

        switch (activeTab) {
            case TAB_GRAPHICS: {
                // ========== Display ==========
                addGroup("Display", y); y += GROUP_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> { game.gfxFullscreen = !game.gfxFullscreen; game.applyFullscreen(); });
                y += ROW_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> { game.gfxVSync = !game.gfxVSync; game.applyVSync(); });
                y += ROW_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> (game.gfxFOV - 45f) / 55f,
                        v -> {
                            float nv = 45f + v * 55f;
                            nv = Math.round(nv);
                            if (nv != game.gfxFOV) {
                                game.gfxFOV = nv;
                                game.applyFOV();
                            }
                        });
                y += ROW_H + 8f;

                // ========== Quality ==========
                addGroup("Quality", y); y += GROUP_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> game.gfxShadows = !game.gfxShadows);
                y += ROW_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f, () -> {
                    int v = game.gfxShadowRes;
                    if (v == 1024) v = 2048;
                    else if (v == 2048) v = 4096;
                    else v = 1024;
                    game.gfxShadowRes = v;
                });
                y += ROW_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f, () -> {
                    int v = game.gfxMSAA;
                    if (v == 0) v = 2;
                    else if (v == 2) v = 4;
                    else if (v == 4) v = 8;
                    else v = 0;
                    game.gfxMSAA = v;
                });
                y += ROW_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> (game.gfxViewDistance - 2f) / 10f,
                        v -> {
                            int nv = 2 + Math.round(v * 10f);
                            if (nv != game.gfxViewDistance) {
                                game.gfxViewDistance = nv;
                                game.applyViewDistance();
                            }
                        });
                y += ROW_H + 8f;

                // ========== Effects ==========
                addGroup("Effects", y); y += GROUP_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> (game.gfxGrassDistance - 40f) / 360f,
                        v -> {
                            float nv = 40f + v * 360f;
                            nv = Math.round(nv / 10f) * 10f;
                            if (nv != game.gfxGrassDistance) {
                                game.gfxGrassDistance = nv;
                                game.applyGrassDistance();
                            }
                        });
                y += ROW_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> game.gfxCloudDensity / 1.5f,
                        v -> {
                            float nv = Math.round(v * 1.5f * 100f) / 100f;
                            game.gfxCloudDensity = nv;
                        });
                y += ROW_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> (game.gfxParticleOpacity - 0.3f) / 1.2f,
                        v -> {
                            float nv = 0.3f + v * 1.2f;
                            nv = Math.round(nv * 100f) / 100f;
                            game.gfxParticleOpacity = nv;
                        });
                y += ROW_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> game.gfxStars = !game.gfxStars);
                y += ROW_H;

                contentHeight = y;
                break;
            }

            case TAB_GAMEPLAY: {
                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> game.gpSnowEnabled = !game.gpSnowEnabled);
                y += ROW_H;

                addToggle(valueX, y, VALUE_W, ROW_H - 12f,
                        () -> game.gpPetalEnabled = !game.gpPetalEnabled);
                y += ROW_H;

                addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                        () -> (game.gpMouseSensitivity - 0.02f) / 0.38f,
                        v -> {
                            float nv = 0.02f + v * 0.38f;
                            nv = Math.round(nv * 100f) / 100f;
                            game.gpMouseSensitivity = nv;
                        });
                y += ROW_H;

                contentHeight = y;
                break;
            }

            case TAB_AUDIO: {
                for (Object[] row : AUDIO_ROWS) {
                    final SoundCategory cat = (SoundCategory) row[0];
                    addSlider(trackX, y, TRACK_W, ROW_H - 12f,
                            () -> AudioMaster.getCategoryVolume(cat),
                            v -> AudioMaster.setCategoryVolume(cat, v));
                    y += ROW_H;
                }
                contentHeight = y;
                break;
            }

            case TAB_CONTROLS: {
                contentHeight = 0f;
                break;
            }
        }

        float viewH = screenHeight * (VIEW_BOTTOM_RATIO - VIEW_TOP_RATIO);
        float maxScroll = Math.max(0f, contentHeight - viewH + 20f);
        scrollY = Math.max(0f, Math.min(maxScroll, scrollY));
    }

    private void addToggle(float x, float y, float w, float h, Runnable r) {
        Toggle t = new Toggle();
        t.x = x; t.y = y; t.w = w; t.h = h;
        t.onClick = r;
        toggles.add(t);
    }

    private void addSlider(float x, float y, float w, float h,
                           Supplier<Float> get, Consumer<Float> set) {
        Slider s = new Slider();
        s.x = x; s.y = y; s.w = w; s.h = h;
        s.getValue = get;
        s.setValue = set;
        sliders.add(s);
    }

    private void addGroup(String title, float y) {
        GroupHeader g = new GroupHeader();
        g.title = title;
        g.y = y;
        groups.add(g);
    }

    // ==================== SCROLL ====================
    @Override
    public void onScroll(double delta) {
        float viewH = screenHeight * (VIEW_BOTTOM_RATIO - VIEW_TOP_RATIO);
        float maxScroll = Math.max(0f, contentHeight - viewH + 20f);
        scrollY -= (float) delta * 40f;
        scrollY = Math.max(0f, Math.min(maxScroll, scrollY));
    }

    private float screenY(float contentY) {
        return screenHeight * VIEW_TOP_RATIO + contentY - scrollY;
    }

    // ==================== INPUT ====================
    @Override
    public boolean keyPress(int key, int action) {
        if (key == GLFW_KEY_ESCAPE) {
            AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
            game.openScreen(new MainMenuScreen());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClick(double mx, double my, int button) {
        if (button != 0) return false;

        if (mx >= backX && mx <= backX + backW
         && my >= backY && my <= backY + backH) {
            AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
            game.openScreen(new MainMenuScreen());
            return true;
        }

        for (int i = 0; i < TAB_NAMES.length; i++) {
            if (mx >= tabX[i] && mx <= tabX[i] + tabW[i]
             && my >= tabY && my <= tabY + tabH) {
                if (activeTab != i) {
                    AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
                    activeTab = i;
                    scrollY = 0f;
                    rebuildWidgets();
                }
                return true;
            }
        }

        for (Toggle t : toggles) {
            float ty = screenY(t.y);
            if (mx >= t.x && mx <= t.x + t.w
             && my >= ty && my <= ty + t.h) {
                AudioMaster.playSound(Sounds.UI_CLICK, SoundCategory.UI);
                t.onClick.run();
                return true;
            }
        }

        for (Slider s : sliders) {
            float ty = screenY(s.y);
            if (mx >= s.x - 20 && mx <= s.x + s.w + 20
             && my >= ty - 20 && my <= ty + s.h + 20) {
                draggingSlider = s;
                setFromMouse(s, mx);
                return true;
            }
        }

        return false;
    }

    @Override
    public void mouseRelease(double mx, double my, int button) {
        if (button == 0) draggingSlider = null;
    }

    @Override
    public void update(float dt, double mx, double my) {
        super.update(dt, mx, my);
        if (draggingSlider != null) setFromMouse(draggingSlider, mx);
    }

    private void setFromMouse(Slider s, double mx) {
        float t = (float)((mx - s.x) / s.w);
        t = Math.max(0f, Math.min(1f, t));
        s.setValue.accept(t);
    }

    // ==================== RENDER ====================
    @Override
    public void render(GuiRenderer gui, FontRenderer font) {
        Vignette.draw(gui, screenWidth, screenHeight,
                VIGNETTE_ALPHA, VIGNETTE_SIZE);

        drawTrackedShadowedCentered(gui, font, "SETTINGS",
                screenWidth * 0.5f, screenHeight * 0.07f,
                1.10f, 10f,
                GOLD[0], GOLD[1], GOLD[2], 1f);

        renderTabs(gui, font);

        float viewTop    = screenHeight * VIEW_TOP_RATIO;
        float viewBottom = screenHeight * VIEW_BOTTOM_RATIO;
        float viewH      = viewBottom - viewTop;
        float clipX      = (screenWidth - CONTENT_W) * 0.5f - 20f;
        float clipW      = CONTENT_W + 40f;

        gui.pushClip(clipX, viewTop, clipW, viewH);

        switch (activeTab) {
            case TAB_GRAPHICS: renderGraphicsContent(gui, font); break;
            case TAB_CONTROLS: renderControlsContent(gui, font); break;
            case TAB_AUDIO:    renderAudioContent(gui, font);    break;
            case TAB_GAMEPLAY: renderGameplayContent(gui, font); break;
        }

        renderScrollbar(gui);

        gui.popClip();

        renderBack(gui, font);
    }

    // ==================== TABS ====================
    private void renderTabs(GuiRenderer gui, FontRenderer font) {
        float fScale = 0.72f;
        float fTracking = 6f;

        for (int i = 0; i < TAB_NAMES.length; i++) {
            boolean active = (i == activeTab);
            String label = TAB_NAMES[i];

            float textW = trackedWidth(font, label, fScale, fTracking);
            float textX = tabX[i] + (tabW[i] - textW) * 0.5f;
            float textH = font.getLineHeight() * fScale;
            float textY = tabY + (tabH - textH) * 0.5f - 1f;

            float cr, cg, cb, ca;
            if (active) {
                cr = GOLD[0]; cg = GOLD[1]; cb = GOLD[2]; ca = 1f;
            } else {
                cr = WHITE_DIM[0]; cg = WHITE_DIM[1]; cb = WHITE_DIM[2]; ca = 0.75f;
            }

            drawTrackedShadowed(gui, font, label, textX, textY,
                    fScale, fTracking, cr, cg, cb, ca);

            if (active) {
                gui.rect(textX, textY + textH + 6f, textW, 2f,
                        GOLD[0], GOLD[1], GOLD[2], 1f);
            }
        }
    }

    // ==================== CONTENT: GRAPHICS ====================
    private void renderGraphicsContent(GuiRenderer gui, FontRenderer font) {
        renderGroups(gui, font);

        int tIdx = 0, sIdx = 0;
        float y;

        y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "Fullscreen",
                "Fullscreen display mode",
                game.gfxFullscreen ? "ON" : "OFF", game.gfxFullscreen);
        tIdx++;

        y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "VSync",
                "Sync framerate with monitor refresh",
                game.gfxVSync ? "ON" : "OFF", game.gfxVSync);
        tIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Field of view",
                "Camera FOV in degrees",
                sliders.get(sIdx), Math.round(game.gfxFOV) + " deg");
        sIdx++;

        y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "Shadows",
                "Real-time shadow mapping",
                game.gfxShadows ? "ON" : "OFF", game.gfxShadows);
        tIdx++;

        y = screenY(toggles.get(tIdx).y);
        String sr = game.gfxShadowRes + " px";
        renderToggleRowRestart(gui, font, y, "Shadow resolution",
                "Shadow map size - restart to apply",
                sr, true);
        tIdx++;

        y = screenY(toggles.get(tIdx).y);
        String aa = game.gfxMSAA == 0 ? "OFF" : (game.gfxMSAA + "x");
        renderToggleRowRestart(gui, font, y, "Anti-aliasing",
                "MSAA samples - restart to apply",
                aa, game.gfxMSAA > 0);
        tIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Render distance",
                "Chunk view radius",
                sliders.get(sIdx), game.gfxViewDistance + " chunks");
        sIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Grass distance",
                "Render distance for grass blades",
                sliders.get(sIdx), ((int) game.gfxGrassDistance) + " m");
        sIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Cloud density",
                "Thickness of cloud layer",
                sliders.get(sIdx), Math.round(game.gfxCloudDensity * 100f) + "%");
        sIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Particle opacity",
                "Snow & petal visibility",
                sliders.get(sIdx), Math.round(game.gfxParticleOpacity * 100f) + "%");
        sIdx++;

        y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "Stars",
                "Show stars in night sky",
                game.gfxStars ? "ON" : "OFF", game.gfxStars);
    }

    // ==================== CONTENT: GAMEPLAY ====================
    private void renderGameplayContent(GuiRenderer gui, FontRenderer font) {
        int tIdx = 0, sIdx = 0;

        float y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "Snow",
                "Show falling snow particles",
                game.gpSnowEnabled ? "ON" : "OFF", game.gpSnowEnabled);
        tIdx++;

        y = screenY(toggles.get(tIdx).y);
        renderToggleRow(gui, font, y, "Petals",
                "Show falling sakura petals",
                game.gpPetalEnabled ? "ON" : "OFF", game.gpPetalEnabled);
        tIdx++;

        y = screenY(sliders.get(sIdx).y);
        renderSliderRow(gui, font, y, "Mouse sensitivity",
                "Camera rotation speed",
                sliders.get(sIdx), String.format("%.2f", game.gpMouseSensitivity));
    }

    // ==================== CONTENT: AUDIO ====================
    private void renderAudioContent(GuiRenderer gui, FontRenderer font) {
        for (int i = 0; i < AUDIO_ROWS.length; i++) {
            SoundCategory cat = (SoundCategory) AUDIO_ROWS[i][0];
            String label = (String) AUDIO_ROWS[i][1];
            float vol = AudioMaster.getCategoryVolume(cat);

            float y = screenY(sliders.get(i).y);
            renderSliderRow(gui, font, y, label, null,
                    sliders.get(i), Math.round(vol * 100f) + "%");
        }
    }

    // ==================== CONTENT: CONTROLS ====================
    private void renderControlsContent(GuiRenderer gui, FontRenderer font) {
        float tableW = 560f;
        float tableX = (screenWidth - tableW) * 0.5f;

        float chipW  = 96f;
        float chipH  = 26f;
        float chipX  = tableX;
        float descX  = tableX + chipW + 40f;

        float headerY = screenHeight * 0.30f;
        float rowY    = headerY + 32f;
        float rowH    = 32f;

        float headerScale = 0.46f;
        float headerTextH = font.getLineHeight() * headerScale;

        drawTracked(gui, font, "KEY", chipX, headerY,
                headerScale, 5f,
                WHITE_DIM[0], WHITE_DIM[1], WHITE_DIM[2], 0.85f);

        drawTracked(gui, font, "ACTION", descX, headerY,
                headerScale, 5f,
                WHITE_DIM[0], WHITE_DIM[1], WHITE_DIM[2], 0.85f);

        gui.rect(tableX, headerY + headerTextH + 6f, tableW, 1f,
                GOLD[0], GOLD[1], GOLD[2], 0.55f);

        float chipTextScale = 0.55f;
        float descTextScale = 0.58f;

        float chipTextH = font.getLineHeight() * chipTextScale;
        float descTextH = font.getLineHeight() * descTextScale;

        for (int i = 0; i < BINDINGS.length; i++) {
            String key  = BINDINGS[i][0];
            String desc = BINDINGS[i][1];

            float chipY = rowY + (rowH - chipH) * 0.5f;

            gui.rect(chipX, chipY, chipW, chipH,
                    0.08f, 0.08f, 0.10f, 0.75f);
            gui.rectOutline(chipX, chipY, chipW, chipH, 1f,
                    GOLD[0], GOLD[1], GOLD[2], 0.65f);

            float chipTextY = chipY + (chipH - chipTextH) * 0.5f - 1f;
            gui.textCentered(key, chipX + chipW * 0.5f, chipTextY,
                    chipTextScale, GOLD[0], GOLD[1], GOLD[2], 1f);

            float descY = rowY + (rowH - descTextH) * 0.5f - 1f;
            drawTrackedShadowed(gui, font, desc, descX, descY,
                    descTextScale, 0f,
                    WHITE[0], WHITE[1], WHITE[2], 0.92f);

            if (i < BINDINGS.length - 1) {
                gui.rect(tableX, rowY + rowH - 1f, tableW, 1f,
                        WHITE[0], WHITE[1], WHITE[2], 0.15f);
            }

            rowY += rowH;
        }

        gui.rect(tableX, rowY, tableW, 1f,
                GOLD[0], GOLD[1], GOLD[2], 0.25f);
    }

    // ==================== GROUPS ====================
    private void renderGroups(GuiRenderer gui, FontRenderer font) {
        float contentX = (screenWidth - CONTENT_W) * 0.5f;
        float fScale = 0.50f;
        float fTracking = 5f;

        for (GroupHeader g : groups) {
            float gy = screenY(g.y);
            float textH = font.getLineHeight() * fScale;
            float textY = gy + (GROUP_H - textH) * 0.5f - 1f;

            drawTrackedShadowed(gui, font, g.title.toUpperCase(),
                    contentX, textY, fScale, fTracking,
                    GOLD[0], GOLD[1], GOLD[2], 0.85f);

            float lineY = gy + GROUP_H - 6f;
            gui.rect(contentX, lineY, CONTENT_W, 1f,
                    GOLD[0], GOLD[1], GOLD[2], 0.35f);
        }
    }

    // ==================== SCROLLBAR ====================
    private void renderScrollbar(GuiRenderer gui) {
        float viewH = screenHeight * (VIEW_BOTTOM_RATIO - VIEW_TOP_RATIO);
        if (contentHeight <= viewH + 20f) return;

        float maxScroll = contentHeight - viewH + 20f;
        float trackX = screenWidth * 0.5f + CONTENT_W * 0.5f + 20f;
        float trackY = screenHeight * VIEW_TOP_RATIO;
        float trackW = 4f;
        float trackH = viewH;

        gui.rect(trackX, trackY, trackW, trackH,
                1.0f, 1.0f, 1.0f, 0.08f);

        float thumbH = Math.max(40f, trackH * (viewH / contentHeight));
        float thumbY = trackY + (trackH - thumbH) * (scrollY / maxScroll);
        gui.rect(trackX, thumbY, trackW, thumbH,
                GOLD[0], GOLD[1], GOLD[2], 0.55f);
    }

    // ==================== ROW RENDERERS ====================
    private void renderToggleRow(GuiRenderer gui, FontRenderer font, float y,
                                  String label, String sub,
                                  String valueText, boolean on) {
        float contentX = (screenWidth - CONTENT_W) * 0.5f;

        float labelTextH = font.getLineHeight() * 0.62f;
        float labelY = y + 8f;

        drawTrackedShadowed(gui, font, label, contentX, labelY,
                0.62f, 0f, WHITE[0], WHITE[1], WHITE[2], 1f);

        if (sub != null && !sub.isEmpty()) {
            gui.text(sub, contentX, labelY + labelTextH + 4f, 0.40f,
                    WHITE_DIM[0], WHITE_DIM[1], WHITE_DIM[2], 0.75f);
        }

        float valueScale = 0.68f;
        float valueTracking = 3f;
        float valueTextW = trackedWidth(font, valueText, valueScale, valueTracking);
        float valueX = contentX + CONTENT_W - valueTextW - 10f;
        float valueTextH = font.getLineHeight() * valueScale;
        float valueY = y + (ROW_H - valueTextH) * 0.5f - 4f;

        float vr = on ? GOLD[0] : GREY_DIM[0];
        float vg = on ? GOLD[1] : GREY_DIM[1];
        float vb = on ? GOLD[2] : GREY_DIM[2];

        drawTrackedShadowed(gui, font, valueText, valueX, valueY,
                valueScale, valueTracking, vr, vg, vb, 1f);

        gui.rect(contentX, y + ROW_H - 1f, CONTENT_W, 1f,
                WHITE[0], WHITE[1], WHITE[2], 0.20f);
    }

    private void renderToggleRowRestart(GuiRenderer gui, FontRenderer font, float y,
                                          String label, String sub,
                                          String valueText, boolean on) {
        float contentX = (screenWidth - CONTENT_W) * 0.5f;

        float labelTextH = font.getLineHeight() * 0.62f;
        float labelY = y + 8f;

        drawTrackedShadowed(gui, font, label, contentX, labelY,
                0.62f, 0f, WHITE[0], WHITE[1], WHITE[2], 1f);

        if (sub != null && !sub.isEmpty()) {
            gui.text(sub, contentX, labelY + labelTextH + 4f, 0.40f,
                    RESTART_AMBER[0], RESTART_AMBER[1], RESTART_AMBER[2], 0.80f);
        }

        float valueScale = 0.68f;
        float valueTracking = 3f;
        float valueTextW = trackedWidth(font, valueText, valueScale, valueTracking);
        float valueX = contentX + CONTENT_W - valueTextW - 10f;
        float valueTextH = font.getLineHeight() * valueScale;
        float valueY = y + (ROW_H - valueTextH) * 0.5f - 4f;

        float vr = on ? GOLD[0] : GREY_DIM[0];
        float vg = on ? GOLD[1] : GREY_DIM[1];
        float vb = on ? GOLD[2] : GREY_DIM[2];

        drawTrackedShadowed(gui, font, valueText, valueX, valueY,
                valueScale, valueTracking, vr, vg, vb, 1f);

        gui.rect(contentX, y + ROW_H - 1f, CONTENT_W, 1f,
                WHITE[0], WHITE[1], WHITE[2], 0.20f);
    }

    private void renderSliderRow(GuiRenderer gui, FontRenderer font, float y,
                                  String label, String sub,
                                  Slider s, String valueText) {
        float contentX = (screenWidth - CONTENT_W) * 0.5f;

        float labelTextH = font.getLineHeight() * 0.62f;
        float labelY = y + 8f;

        drawTrackedShadowed(gui, font, label, contentX, labelY,
                0.62f, 0f, WHITE[0], WHITE[1], WHITE[2], 1f);

        if (sub != null && !sub.isEmpty()) {
            gui.text(sub, contentX, labelY + labelTextH + 4f, 0.40f,
                    WHITE_DIM[0], WHITE_DIM[1], WHITE_DIM[2], 0.75f);
        }

        float trackY = y + (ROW_H - 3f) * 0.5f - 4f;
        float trackH = 3f;

        gui.rect(s.x, trackY, s.w, trackH,
                0.20f, 0.20f, 0.22f, 0.75f);

        float vol = s.getValue.get();
        float fillW = s.w * vol;

        if (fillW > 0.5f) {
            gui.rect(s.x, trackY, fillW, trackH,
                    GOLD[0], GOLD[1], GOLD[2], 0.95f);
        }

        float hs = 10f;
        float hx = s.x + fillW - hs * 0.5f;
        float hy = trackY + trackH * 0.5f - hs * 0.5f;

        boolean dragging = (draggingSlider == s);
        float hr = dragging ? 1.00f : 0.95f;
        float hg = dragging ? 0.95f : 0.85f;
        float hb = dragging ? 0.65f : 0.50f;

        gui.rect(hx, hy, hs, hs, hr, hg, hb, 1f);

        float valueScale = 0.68f;
        float valueTracking = 3f;
        float valueW = trackedWidth(font, valueText, valueScale, valueTracking);
        float valueX = contentX + CONTENT_W - valueW - 10f;
        float valueTextH = font.getLineHeight() * valueScale;
        float valueY = y + (ROW_H - valueTextH) * 0.5f - 4f;

        drawTrackedShadowed(gui, font, valueText, valueX, valueY,
                valueScale, valueTracking,
                WHITE[0], WHITE[1], WHITE[2], 1f);

        gui.rect(contentX, y + ROW_H - 1f, CONTENT_W, 1f,
                WHITE[0], WHITE[1], WHITE[2], 0.20f);
    }

    // ==================== BACK ====================
    private void renderBack(GuiRenderer gui, FontRenderer font) {
        boolean hover = (mouseX >= backX && mouseX <= backX + backW
                      && mouseY >= backY && mouseY <= backY + backH);

        String label = "BACK";
        float fScale = 0.62f;
        float fTracking = 6f;
        float textW = trackedWidth(font, label, fScale, fTracking);
        float textX = backX + (backW - textW) * 0.5f;
        float textH = font.getLineHeight() * fScale;
        float textY = backY + (backH - textH) * 0.5f - 1f;

        float cr = hover ? GOLD[0] : WHITE_DIM[0];
        float cg = hover ? GOLD[1] : WHITE_DIM[1];
        float cb = hover ? GOLD[2] : WHITE_DIM[2];

        drawTrackedShadowed(gui, font, label, textX, textY,
                fScale, fTracking, cr, cg, cb, 1f);

        float lineAlpha = hover ? 0.95f : 0.35f;
        gui.rect(textX, textY + textH + 5f, textW, 1f,
                GOLD[0], GOLD[1], GOLD[2], lineAlpha);
    }

    // ==================== TEXT HELPERS ====================

    private void drawTrackedShadowed(GuiRenderer gui, FontRenderer font,
                                      String text, float x, float y,
                                      float scale, float tracking,
                                      float r, float g, float b, float a) {
        drawTracked(gui, font, text, x + 2, y + 2,
                scale, tracking, 0f, 0f, 0f, 0.75f);
        drawTracked(gui, font, text, x + 1, y + 1,
                scale, tracking, 0f, 0f, 0f, 0.55f);
        drawTracked(gui, font, text, x, y,
                scale, tracking, r, g, b, a);
    }

    private void drawTrackedShadowedCentered(GuiRenderer gui, FontRenderer font,
                                              String text, float cx, float y,
                                              float scale, float tracking,
                                              float r, float g, float b, float a) {
        float w = trackedWidth(font, text, scale, tracking);
        drawTrackedShadowed(gui, font, text, cx - w * 0.5f, y,
                scale, tracking, r, g, b, a);
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
}
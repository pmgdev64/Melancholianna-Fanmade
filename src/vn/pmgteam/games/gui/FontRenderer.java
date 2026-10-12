/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * 
 * Based on "Melancholianna" (メランコリアンナ) by Omochi Kobo (おもち工房)
 * Original work: https://www.omochikobo.com/
 * 
 * This is an UNOFFICIAL fan game. It is not affiliated with, endorsed,
 * sponsored, or approved by Omochi Kobo or the original creators.
 * 
 * Game Engine & original code elements:
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package vn.pmgteam.games.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.system.MemoryUtil;

/**
 * Render text qua atlas glyph bake từ java.awt.Font.
 * 
 * Fix lần này:
 *  - Padding glyph 6px (tránh bleed giữa các glyph lân cận)
 *  - UV inset 1px (tránh sample rìa glyph khi filter)
 *  - GLYPH_PX = 36 (gần với render size hơn → ít artifact downscale)
 */
public class FontRenderer {

    private static final int ATLAS_SIZE = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR  = 126;

    /** Bake size. 36 gần với output (~20-24px) → ít artifact */
    private static final int GLYPH_PX = 36;

    /** Padding giữa các glyph trong atlas */
    private static final int PADDING = 6;

    /** Inset UV — tránh sample ra ngoài glyph khi filter */
    private static final float UV_INSET = 1.0f;

    private static final String[] FONT_CANDIDATES = {
        "Segoe UI",
        "SF Pro Display",
        "Helvetica Neue",
        "DejaVu Sans",
        "Noto Sans",
        "Ubuntu",
        "Cantarell",
        "Liberation Sans",
        "Arial",
        Font.SANS_SERIF,
    };

    private int textureId;
    private int lineHeight;
    private String usedFontName;
    private final Glyph[] glyphs = new Glyph[LAST_CHAR - FIRST_CHAR + 1];

    public static class Glyph {
        public float u0, v0, u1, v1;
        public int advance;
    }

    public FontRenderer() {
        bake();
    }

    private Font pickFont() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()));

        for (String name : FONT_CANDIDATES) {
            if (available.contains(name)) {
                usedFontName = name;
                return new Font(name, Font.PLAIN, GLYPH_PX);
            }
        }
        usedFontName = Font.SANS_SERIF;
        return new Font(Font.SANS_SERIF, Font.PLAIN, GLYPH_PX);
    }

    private void bake() {
        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);   // OFF — ổn định hơn
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);

        Font font = pickFont();
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        lineHeight = fm.getHeight();

        System.out.println("FontRenderer: dùng font \"" + usedFontName
                + "\" size " + GLYPH_PX + " (atlas " + ATLAS_SIZE
                + ", padding " + PADDING + ")");

        int x = PADDING, y = PADDING, rowHeight = 0;

        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            if (c == ' ') {
                Glyph gl = new Glyph();
                gl.advance = fm.charWidth(' ');
                glyphs[c - FIRST_CHAR] = gl;
                continue;
            }

            int w = fm.charWidth(c);
            int h = fm.getAscent() + fm.getDescent();
            if (w <= 0) w = 4;
            if (h <= 0) h = GLYPH_PX;

            if (x + w + PADDING * 2 >= ATLAS_SIZE) {
                x = PADDING;
                y += rowHeight + PADDING;
                rowHeight = 0;
            }

            g.setColor(Color.WHITE);
            g.drawString(String.valueOf((char) c), x, y + fm.getAscent());

            // UV với inset 1px — tránh bleed glyph lân cận khi LINEAR filter
            Glyph gl = new Glyph();
            gl.u0 = (x + UV_INSET) / (float) ATLAS_SIZE;
            gl.v0 = (y + UV_INSET) / (float) ATLAS_SIZE;
            gl.u1 = (x + w - UV_INSET) / (float) ATLAS_SIZE;
            gl.v1 = (y + h - UV_INSET) / (float) ATLAS_SIZE;
            gl.advance = fm.charWidth(c);
            glyphs[c - FIRST_CHAR] = gl;

            x += w + PADDING * 2;
            if (h > rowHeight) rowHeight = h;
        }
        g.dispose();

        int[] pixels = new int[ATLAS_SIZE * ATLAS_SIZE];
        atlas.getRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, pixels, 0, ATLAS_SIZE);

        ByteBuffer bb = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);
        try {
            for (int i = 0; i < pixels.length; i++) {
                int argb = pixels[i];
                bb.put((byte) ((argb >> 16) & 0xFF));
                bb.put((byte) ((argb >> 8)  & 0xFF));
                bb.put((byte) ( argb        & 0xFF));
                bb.put((byte) ((argb >>> 24) & 0xFF));
            }
            bb.flip();

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ATLAS_SIZE, ATLAS_SIZE,
                    0, GL_RGBA, GL_UNSIGNED_BYTE, bb);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D, 0);
        } finally {
            MemoryUtil.memFree(bb);
        }
    }

    public Glyph getGlyph(char c) {
        if (c < FIRST_CHAR || c > LAST_CHAR) c = '?';
        return glyphs[c - FIRST_CHAR];
    }

    public float measureWidth(String text, float scale) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += getGlyph(text.charAt(i)).advance * scale;
        }
        return w;
    }

    public int getLineHeight()  { return lineHeight; }
    public int getTextureId()   { return textureId; }
    public int getAtlasSize()   { return ATLAS_SIZE; }
    public String getFontName() { return usedFontName; }

    public void cleanup() {
        if (textureId != 0) glDeleteTextures(textureId);
    }
}
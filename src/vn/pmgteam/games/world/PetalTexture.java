/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * 
 * Based on "Melancholianna" (メランコリアンナ) by Omochi Kobo (おもち工房)
 * Original work: https://www.omochikobo.com/  (or actual source URL)
 * 
 * This is an UNOFFICIAL fan game. Not affiliated with, endorsed by,
 * or sponsored by Omochi Kobo or the original creators.
 * 
 * Fan game engine & original assets:
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
package vn.pmgteam.games.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

/**
 * Texture cánh hoa đào cho petal system.
 * 
 * - Nếu có src/assets/textures/petal.png → dùng file đó
 * - Nếu không có → sinh procedural (cánh hoa đào hồng)
 * 
 * LƯU Ý: Không flip Y (khác với các texture khác) vì petal dùng gl_PointCoord,
 * trong đó (0,0) = top-left. Load file không flip → pixel (0,0) = top-left.
 */
public final class PetalTexture {

    public static final String FILE_PATH = "src/assets/textures/petal.png";
    private static final int GEN_SIZE = 128;

    private PetalTexture() {}

    public static int create() {
        int w = GEN_SIZE, h = GEN_SIZE;
        byte[] data = null;

        File f = new File(FILE_PATH);
        if (f.isFile()) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    w = img.getWidth();
                    h = img.getHeight();
                    data = new byte[w * h * 4];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            // KHÔNG flip — petal dùng gl_PointCoord
                            int argb = img.getRGB(x, y);
                            int o = (y * w + x) * 4;
                            data[o]     = (byte) (argb >> 16);
                            data[o + 1] = (byte) (argb >> 8);
                            data[o + 2] = (byte) argb;
                            data[o + 3] = (byte) (argb >>> 24);
                        }
                    }
                    System.out.println("Petal texture: " + FILE_PATH + " (" + w + "x" + h + ")");
                }
            } catch (IOException e) {
                System.err.println("Không đọc được " + FILE_PATH + ", dùng procedural");
            }
        }

        if (data == null) {
            w = h = GEN_SIZE;
            data = generate(GEN_SIZE);
            System.out.println("Petal texture: tự sinh " + w + "x" + h);
        }

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);

        ByteBuffer buf = MemoryUtil.memAlloc(data.length);
        try {
            buf.put(data).flip();
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }

    private static byte[] generate(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, s, s);

        // === Cánh hoa đào — oval hơi nhọn đỉnh, có notch nhẹ ===
        Path2D.Float path = new Path2D.Float();

        float cx     = s * 0.5f;
        float topY   = s * 0.08f;
        float botY   = s * 0.95f;
        float leftX  = s * 0.12f;
        float rightX = s * 0.88f;
        float notchX = s * 0.50f;
        float notchY = s * 0.22f;

        path.moveTo(notchX, notchY);
        // Đường cong trái — từ notch xuống đáy
        path.curveTo(leftX + s * 0.10f, topY + s * 0.20f,
                     leftX,              s * 0.55f,
                     cx,                 botY);
        // Đường cong phải — từ đáy lên notch
        path.curveTo(rightX,             s * 0.55f,
                     rightX - s * 0.10f, topY + s * 0.20f,
                     notchX,             notchY);

        // Gradient hồng đào: đỉnh nhạt → giữa → đáy đậm
        float[] fractions = { 0.0f, 0.5f, 1.0f };
        Color[] colors = {
            new Color(0xFF, 0xD0, 0xDD),   // hồng rất nhạt ở đỉnh
            new Color(0xF5, 0x88, 0xA8),   // hồng đào ở giữa
            new Color(0xE0, 0x50, 0x78)    // hồng đậm ở đáy
        };
        LinearGradientPaint grad = new LinearGradientPaint(
                new Point2D.Float(cx, topY),
                new Point2D.Float(cx, botY),
                fractions, colors);

        g.setPaint(grad);
        g.fill(path);

        // === Gân giữa ===
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.012f)));
        g.setColor(new Color(0xD0, 0x50, 0x70, 0x80));
        g.drawLine((int) cx, (int) notchY,
                   (int) cx, (int) (botY - s * 0.06f));

        // === Highlight nhẹ bên trái ===
        Path2D.Float highlight = new Path2D.Float();
        highlight.moveTo(cx - s * 0.03f, notchY + s * 0.10f);
        highlight.curveTo(
                leftX + s * 0.10f, topY + s * 0.30f,
                leftX + s * 0.05f, s * 0.55f,
                cx - s * 0.03f,    botY - s * 0.10f);
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.025f)));
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 0x50));
        g.draw(highlight);

        g.dispose();

        // Convert AWT → byte buffer, KHÔNG flip Y
        byte[] d = new byte[s * s * 4];
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int argb = img.getRGB(x, y);
                int o = (y * s + x) * 4;
                d[o]     = (byte) (argb >> 16);
                d[o + 1] = (byte) (argb >> 8);
                d[o + 2] = (byte) argb;
                d[o + 3] = (byte) (argb >>> 24);
            }
        }
        return d;
    }
}
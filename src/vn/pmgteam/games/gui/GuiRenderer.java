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
 */

package vn.pmgteam.games.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import vn.pmgteam.games.shader.ShaderProgram;

/**
 * Batching 2D renderer cho UI: rect + text.
 * Vertex: x, y (2) + u, v (2) + rgba (4) + useTex (1) = 9 floats
 */
public class GuiRenderer {

    private static final int MAX_VERTICES = 16384;
    private static final int FLOATS_PER_VERTEX = 9;

    private final ShaderProgram shader;
    private final FontRenderer font;
    private final int atlasSize;
    private final int vao, vbo;
    private final FloatBuffer buffer;

    private final Matrix4f projection = new Matrix4f();
    private int vertexCount;

    /** Kích thước screen hiện tại — cần cho glScissor conversion. */
    private int screenW;
    private int screenH;

    public GuiRenderer(FontRenderer font, int atlasSize,
                       String vertSrc, String fragSrc) {
        this.font = font;
        this.atlasSize = atlasSize;

        shader = new ShaderProgram();
        shader.createVertexShader(vertSrc);
        shader.createFragmentShader(fragSrc);
        shader.link();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        long bytes = (long) MAX_VERTICES * FLOATS_PER_VERTEX * Float.BYTES;
        glBufferData(GL_ARRAY_BUFFER, bytes, GL_DYNAMIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);

        buffer = MemoryUtil.memAllocFloat(MAX_VERTICES * FLOATS_PER_VERTEX);
    }

    public void begin(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        projection.identity().ortho(0, screenW, screenH, 0, -1, 1);
        buffer.clear();
        vertexCount = 0;
    }

    private void ensureCapacity(int verts) {
        if (vertexCount + verts > MAX_VERTICES) {
            flush();
        }
    }

    /** Rect solid — 1 màu duy nhất. */
    public void rect(float x, float y, float w, float h,
                     float r, float g, float b, float a) {
        rectGrad(x, y, w, h, r, g, b, a, a, a, a);
    }

    /**
     * Rect với 4 alpha khác nhau ở 4 góc.
     * aTL = top-left, aTR = top-right, aBL = bottom-left, aBR = bottom-right.
     */
    public void rectGrad(float x, float y, float w, float h,
                         float r, float g, float b,
                         float aTL, float aTR, float aBL, float aBR) {
        ensureCapacity(6);

        addVertex(x,     y,     0, 0, r, g, b, aTL, 0);
        addVertex(x,     y + h, 0, 0, r, g, b, aBL, 0);
        addVertex(x + w, y + h, 0, 0, r, g, b, aBR, 0);

        addVertex(x,     y,     0, 0, r, g, b, aTL, 0);
        addVertex(x + w, y + h, 0, 0, r, g, b, aBR, 0);
        addVertex(x + w, y,     0, 0, r, g, b, aTR, 0);
    }

    public void rectOutline(float x, float y, float w, float h,
                            float thick, float r, float g, float b, float a) {
        rect(x, y, w, thick, r, g, b, a);
        rect(x, y + h - thick, w, thick, r, g, b, a);
        rect(x, y, thick, h, r, g, b, a);
        rect(x + w - thick, y, thick, h, r, g, b, a);
    }

    /**
     * Vẽ text.
     * 
     * Floor/ceil thay vì round để đảm bảo glyph hẹp (l, i, 1, .) 
     * không bị width = 0 khi scale nhỏ.
     */
    public void text(String text, float x, float y, float scale,
                     float r, float g, float b, float a) {
        float cx = x;
        float baseY = y;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            FontRenderer.Glyph gl = font.getGlyph(c);

            if (c == ' ') {
                cx += gl.advance * scale;
                continue;
            }

            ensureCapacity(6);

            // Floor cho min, ceil cho max → luôn width >= 1px
            float x0 = (float) Math.floor(cx);
            float y0 = (float) Math.floor(baseY);
            float x1 = (float) Math.ceil(cx + (gl.u1 - gl.u0) * atlasSize * scale);
            float y1 = (float) Math.ceil(baseY + (gl.v1 - gl.v0) * atlasSize * scale);

            addVertex(x0, y0, gl.u0, gl.v0, r, g, b, a, 1);
            addVertex(x0, y1, gl.u0, gl.v1, r, g, b, a, 1);
            addVertex(x1, y1, gl.u1, gl.v1, r, g, b, a, 1);

            addVertex(x0, y0, gl.u0, gl.v0, r, g, b, a, 1);
            addVertex(x1, y1, gl.u1, gl.v1, r, g, b, a, 1);
            addVertex(x1, y0, gl.u1, gl.v0, r, g, b, a, 1);

            cx += gl.advance * scale;
        }
    }

    public void textCentered(String text, float cx, float y, float scale,
                             float r, float g, float b, float a) {
        float w = font.measureWidth(text, scale);
        text(text, cx - w * 0.5f, y, scale, r, g, b, a);
    }

    private void addVertex(float x, float y, float u, float v,
                           float r, float g, float b, float a, float useTex) {
        buffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a).put(useTex);
        vertexCount++;
    }

    public void end() {
        flush();
    }

    // ==================== SCISSOR / CLIP ====================

    /**
     * Bật scissor test — mọi draw sau đó chỉ vẽ trong rect cho trước.
     * Rect dùng top-left origin (giống rest of UI code).
     */
    public void pushClip(float x, float y, float w, float h) {
        flush();
        glEnable(GL_SCISSOR_TEST);
        // glScissor dùng bottom-left origin — convert
        int sx = Math.round(x);
        int sy = Math.round(screenH - y - h);
        int sw = Math.round(w);
        int sh = Math.round(h);
        glScissor(sx, sy, sw, sh);
    }

    /** Tắt scissor. */
    public void popClip() {
        flush();
        glDisable(GL_SCISSOR_TEST);
    }

    // ==================== FLUSH ====================

    private void flush() {
        if (vertexCount == 0) return;

        buffer.flip();

        shader.bind();
        shader.setMat4("uProjection", projection);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTextureId());
        shader.setInt("uTexture", 0);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);

        shader.unbind();

        buffer.clear();
        vertexCount = 0;
    }

    public void cleanup() {
        shader.cleanup();
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        MemoryUtil.memFree(buffer);
    }
}
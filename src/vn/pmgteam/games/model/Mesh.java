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

package vn.pmgteam.games.model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * Mesh — 2 giai đoạn:
 *   1. Parse: lưu data CPU (không cần GL context)
 *   2. upload(): tạo VAO/VBO/EBO (cần GL context)
 */
public class Mesh {

    // === CPU data (parse phase) ===
    public float[] positions;   // 3 floats/vertex
    public float[] normals;     // 3 floats/vertex (có thể null)
    public float[] weights;     // 4 floats/vertex (có thể null)
    public int[]   joints;      // 4 int/vertex (có thể null)
    public int[]   indices;

    // === GPU handles ===
    public int vao = 0;
    public int vbo = 0;
    public int ebo = 0;
    public int vertexCount = 0;
    public int indexCount  = 0;

    private static final int FLOATS_PER_VERTEX = 14;

    /**
     * Upload CPU data lên GPU. Phải gọi SAU khi GL context ready.
     * Sau khi upload xong, CPU arrays được giải phóng.
     */
    public void upload() {
        if (positions == null) return;

        vertexCount = positions.length / 3;
        indexCount  = indices != null ? indices.length : 0;

        // Interleave: pos(3) + normal(3) + weights(4) + joints(4) = 14 floats
        FloatBuffer fb = MemoryUtil.memAllocFloat(vertexCount * FLOATS_PER_VERTEX);
        for (int i = 0; i < vertexCount; i++) {
            fb.put(positions[i*3]);   fb.put(positions[i*3+1]); fb.put(positions[i*3+2]);

            if (normals != null) {
                fb.put(normals[i*3]); fb.put(normals[i*3+1]); fb.put(normals[i*3+2]);
            } else {
                fb.put(0).put(1).put(0);
            }

            if (weights != null) {
                fb.put(weights[i*4]);   fb.put(weights[i*4+1]);
                fb.put(weights[i*4+2]); fb.put(weights[i*4+3]);
            } else {
                fb.put(0).put(0).put(0).put(0);
            }

            if (joints != null) {
                fb.put(joints[i*4]);   fb.put(joints[i*4+1]);
                fb.put(joints[i*4+2]); fb.put(joints[i*4+3]);
            } else {
                fb.put(0).put(0).put(0).put(0);
            }
        }
        fb.flip();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        MemoryUtil.memFree(fb);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 4, GL_FLOAT, false, stride, 10 * Float.BYTES);
        glEnableVertexAttribArray(3);

        if (indices != null) {
            ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
            ib.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
            MemoryUtil.memFree(ib);
        }

        glBindVertexArray(0);

        // Giải phóng CPU data
        positions = null;
        normals = null;
        weights = null;
        joints = null;
        indices = null;
    }

    public void render() {
        if (vao == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (ebo != 0) glDeleteBuffers(ebo);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        vao = vbo = ebo = 0;
    }
}
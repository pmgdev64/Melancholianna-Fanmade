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

package vn.pmgteam.games.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import vn.pmgteam.games.shader.ShaderProgram;

public class Sky {

    private static final float DISTANCE = 400f;

    private final int vao, vbo;

    // Stars
    private int starVao, starVbo;
    private int starCount;

    // Clouds
    private int cloudVao, cloudVbo, cloudEbo;
    private int cloudIndexCount;

    public Sky() {
        float[] verts = {
            -1, -1,   1, -1,   1,  1,
            -1, -1,   1,  1,  -1,  1,
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = MemoryUtil.memAllocFloat(verts.length);
        try {
            fb.put(verts).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(fb);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);

        buildStars();
        buildCloudDome();
    }

    private void buildStars() {
        Random rnd = new Random(987654321L);
        int count = 900;
        float[] data = new float[count * 4];   // x, y, z, size

        for (int i = 0; i < count; i++) {
            // Phân bố đều trên bán cầu trên (cos-weighted)
            float cosPhi = rnd.nextFloat();     // 0..1 (1 = đỉnh)
            float sinPhi = (float) Math.sqrt(1 - cosPhi * cosPhi);
            float theta  = rnd.nextFloat() * (float) (Math.PI * 2);

            float x = sinPhi * (float) Math.cos(theta);
            float y = cosPhi;
            float z = sinPhi * (float) Math.sin(theta);

            float size = 1.5f + rnd.nextFloat() * 2.5f;

            data[i * 4]     = x;
            data[i * 4 + 1] = y;
            data[i * 4 + 2] = z;
            data[i * 4 + 3] = size;
        }
        starCount = count;

        starVao = glGenVertexArrays();
        glBindVertexArray(starVao);

        starVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, starVbo);

        FloatBuffer fb = MemoryUtil.memAllocFloat(data.length);
        try {
            fb.put(data).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(fb);
        }

        int stride = 4 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private void buildCloudDome() {
        int slices = 32;
        int stacks = 16;

        float[] verts = new float[(slices + 1) * (stacks + 1) * 3];
        int idx = 0;

        for (int st = 0; st <= stacks; st++) {
            float phi    = (st / (float) stacks) * (float) Math.PI * 0.5f;
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            for (int sl = 0; sl <= slices; sl++) {
                float theta = (sl / (float) slices) * (float) Math.PI * 2f;
                verts[idx++] = sinPhi * (float) Math.cos(theta);
                verts[idx++] = cosPhi;
                verts[idx++] = sinPhi * (float) Math.sin(theta);
            }
        }

        int[] idxs = new int[slices * stacks * 6];
        int ii = 0;
        for (int st = 0; st < stacks; st++) {
            for (int sl = 0; sl < slices; sl++) {
                int a = st * (slices + 1) + sl;
                int b = a + 1;
                int c = a + (slices + 1);
                int d = c + 1;
                idxs[ii++] = a; idxs[ii++] = c; idxs[ii++] = b;
                idxs[ii++] = b; idxs[ii++] = c; idxs[ii++] = d;
            }
        }
        cloudIndexCount = idxs.length;

        cloudVao = glGenVertexArrays();
        glBindVertexArray(cloudVao);

        cloudVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, cloudVbo);

        FloatBuffer vf = MemoryUtil.memAllocFloat(verts.length);
        try {
            vf.put(verts).flip();
            glBufferData(GL_ARRAY_BUFFER, vf, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(vf);
        }

        cloudEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cloudEbo);
        IntBuffer ib = MemoryUtil.memAllocInt(idxs.length);
        try {
            ib.put(idxs).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(ib);
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    // ==================== RENDER ====================

    public void renderSun(ShaderProgram shader, Camera3D cam, Vector3f sunDir,
                          float cr, float cg, float cb,
                          float gr, float gg, float gb) {
        render(shader, cam, sunDir, 25f, cr, cg, cb, gr, gg, gb);
    }

    public void renderMoon(ShaderProgram shader, Camera3D cam, Vector3f sunDir) {
        Vector3f moonDir = new Vector3f(sunDir).negate();
        render(shader, cam, moonDir, 18f,
                0.98f, 0.98f, 1.00f,
                0.60f, 0.68f, 0.85f);
    }

    private void render(ShaderProgram shader, Camera3D cam, Vector3f dir,
                        float size,
                        float cr, float cg, float cb,
                        float gr, float gg, float gb) {

        Vector3f skyPos = new Vector3f(cam.position).fma(DISTANCE, dir);
        Matrix4f model = billboard(cam.position, skyPos, size);

        shader.setMat4("uModel", model);
        shader.setVec3("uCoreColor", cr, cg, cb);
        shader.setVec3("uGlowColor", gr, gg, gb);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void renderStars() {
        if (starCount == 0) return;
        glBindVertexArray(starVao);
        glDrawArrays(GL_POINTS, 0, starCount);
        glBindVertexArray(0);
    }

    public void renderClouds() {
        if (cloudIndexCount == 0) return;
        glBindVertexArray(cloudVao);
        glDrawElements(GL_TRIANGLES, cloudIndexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    private Matrix4f billboard(Vector3f camPos, Vector3f skyPos, float size) {
        Vector3f forward = new Vector3f(skyPos).sub(camPos).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(forward.dot(up)) > 0.99f) up.set(0, 0, 1);
        Vector3f right  = new Vector3f(forward).cross(up).normalize();
        Vector3f trueUp = new Vector3f(right).cross(forward).normalize();
        right.mul(size);
        trueUp.mul(size);
        return new Matrix4f(
                right.x,   right.y,   right.z,   0f,
                trueUp.x,  trueUp.y,  trueUp.z,  0f,
                forward.x, forward.y, forward.z, 0f,
                skyPos.x,  skyPos.y,  skyPos.z,  1f
        );
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        if (starVbo != 0) glDeleteBuffers(starVbo);
        if (starVao != 0) glDeleteVertexArrays(starVao);
        if (cloudVbo != 0) glDeleteBuffers(cloudVbo);
        if (cloudEbo != 0) glDeleteBuffers(cloudEbo);
        if (cloudVao != 0) glDeleteVertexArrays(cloudVao);
    }
}
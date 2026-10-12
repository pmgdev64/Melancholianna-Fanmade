/*
 * Melanchoianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.system.MemoryUtil;

import vn.pmgteam.games.shader.ShaderProgram;

/**
 * Tuyết rơi xung quanh camera — particle system dùng GL_POINTS.
 * Mỗi hạt được update trên CPU, VBO upload mỗi frame.
 */
public class Snow {

    private static final int   MAX_FLAKES  = 2500;
    private static final float AREA        = 60f;    // vùng 60×60m quanh camera
    private static final float HEIGHT      = 35f;    // độ cao cột tuyết
    private static final float FALL_SPEED  = 2.5f;   // tốc độ rơi (m/s)

    private final int vao, vbo;
    private final FloatBuffer buffer;

    private final float[] x     = new float[MAX_FLAKES];
    private final float[] y     = new float[MAX_FLAKES];
    private final float[] z     = new float[MAX_FLAKES];
    private final float[] vy    = new float[MAX_FLAKES];
    private final float[] size  = new float[MAX_FLAKES];
    private final float[] phase = new float[MAX_FLAKES];

    private final Random rnd = new Random(42L);

    public Snow() {
        // Khởi tạo VBO rỗng, update mỗi frame
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER,
                (long) MAX_FLAKES * 4 * Float.BYTES,
                GL_DYNAMIC_DRAW);

        int stride = 4 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        buffer = MemoryUtil.memAllocFloat(MAX_FLAKES * 4);
    }

    /** Khởi tạo lại hạt ngẫu nhiên trong vùng quanh camera. */
    private void randomizeFlake(int i, float camX, float camY, float camZ) {
        x[i]     = camX + (rnd.nextFloat() - 0.5f) * AREA;
        y[i]     = camY + 5f + rnd.nextFloat() * HEIGHT;
        z[i]     = camZ + (rnd.nextFloat() - 0.5f) * AREA;
        vy[i]    = -(FALL_SPEED * 0.6f + rnd.nextFloat() * FALL_SPEED);
        size[i]  = 2.0f + rnd.nextFloat() * 3.0f;
        phase[i] = rnd.nextFloat() * (float) (Math.PI * 2);
    }

    public void update(float dt, float camX, float camY, float camZ, float time) {
        float halfAreaSq = (AREA * 0.5f) * (AREA * 0.5f);

        for (int i = 0; i < MAX_FLAKES; i++) {
            // Gió — mỗi hạt có phase riêng, lắc lư nhẹ
            float wind = (float) Math.sin(time * 1.5f + phase[i]) * 0.6f;

            x[i] += wind * dt;
            y[i] += vy[i] * dt;
            z[i] += (float) Math.cos(time * 1.2f + phase[i] * 0.7f) * 0.4f * dt;

            // Respawn khi rơi xuống dưới đáy
            if (y[i] < camY - 3f) {
                randomizeFlake(i, camX, camY, camZ);
                continue;
            }

            // Respawn khi ra khỏi vùng ngang
            float dx = x[i] - camX;
            float dz = z[i] - camZ;
            if (dx * dx + dz * dz > halfAreaSq) {
                randomizeFlake(i, camX, camY, camZ);
            }
        }
    }

    public void render(ShaderProgram shader) {
        buffer.clear();
        for (int i = 0; i < MAX_FLAKES; i++) {
            buffer.put(x[i]).put(y[i]).put(z[i]).put(size[i]);
        }
        buffer.flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glDrawArrays(GL_POINTS, 0, MAX_FLAKES);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        MemoryUtil.memFree(buffer);
    }
}
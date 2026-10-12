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
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.system.MemoryUtil;

import vn.pmgteam.games.shader.ShaderProgram;

/**
 * Hoa đào rơi — chậm hơn tuyết, xoay, lắc lư theo gió.
 * Vertex: pos(3) + size(1) + phase(1) = 5 floats
 */
public class Petal {

    private static final int   MAX_PETALS  = 1200;
    private static final float AREA        = 70f;
    private static final float HEIGHT      = 30f;
    private static final float FALL_SPEED  = 1.2f;

    private final int vao, vbo;
    private final FloatBuffer buffer;

    private final float[] x     = new float[MAX_PETALS];
    private final float[] y     = new float[MAX_PETALS];
    private final float[] z     = new float[MAX_PETALS];
    private final float[] vy    = new float[MAX_PETALS];
    private final float[] size  = new float[MAX_PETALS];
    private final float[] phase = new float[MAX_PETALS];

    private final Random rnd = new Random(1337L);

    public Petal() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER,
                (long) MAX_PETALS * 5 * Float.BYTES,
                GL_DYNAMIC_DRAW);

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        buffer = MemoryUtil.memAllocFloat(MAX_PETALS * 5);
    }

    private void randomize(int i, float camX, float camY, float camZ) {
        x[i]     = camX + (rnd.nextFloat() - 0.5f) * AREA;
        y[i]     = camY + 3f + rnd.nextFloat() * HEIGHT;
        z[i]     = camZ + (rnd.nextFloat() - 0.5f) * AREA;
        vy[i]    = -(FALL_SPEED * 0.5f + rnd.nextFloat() * FALL_SPEED);
        size[i]  = 10.0f + rnd.nextFloat() * 8.0f;
        phase[i] = rnd.nextFloat() * (float) (Math.PI * 2);
    }

    public void update(float dt, float camX, float camY, float camZ, float time) {
        float halfAreaSq = (AREA * 0.5f) * (AREA * 0.5f);

        for (int i = 0; i < MAX_PETALS; i++) {
            // Hoa đào bay theo gió mạnh hơn tuyết
            float wind = (float) Math.sin(time * 0.8f + phase[i]) * 1.5f;

            x[i] += wind * dt;
            y[i] += vy[i] * dt;
            z[i] += (float) Math.cos(time * 0.6f + phase[i] * 0.7f) * 1.0f * dt;

            if (y[i] < camY - 3f) {
                randomize(i, camX, camY, camZ);
                continue;
            }

            float dx = x[i] - camX;
            float dz = z[i] - camZ;
            if (dx * dx + dz * dz > halfAreaSq) {
                randomize(i, camX, camY, camZ);
            }
        }
    }

    public void render(ShaderProgram shader, float time) {
        buffer.clear();
        for (int i = 0; i < MAX_PETALS; i++) {
            buffer.put(x[i]).put(y[i]).put(z[i]).put(size[i])
                  .put(phase[i] + time * 2.5f);
        }
        buffer.flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glDrawArrays(GL_POINTS, 0, MAX_PETALS);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        MemoryUtil.memFree(buffer);
    }
}
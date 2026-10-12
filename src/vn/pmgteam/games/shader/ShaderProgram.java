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

package vn.pmgteam.games.shader;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram() {
        programId = glCreateProgram();
        if (programId == 0) throw new RuntimeException("Không tạo được Shader Program");
    }

    public void createVertexShader(String code) {
        int id = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(id, code);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Lỗi Vertex Shader:\n" + glGetShaderInfoLog(id));
        glAttachShader(programId, id);
        glDeleteShader(id);
    }

    public void createFragmentShader(String code) {
        int id = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(id, code);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Lỗi Fragment Shader:\n" + glGetShaderInfoLog(id));
        glAttachShader(programId, id);
        glDeleteShader(id);
    }

    public void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new RuntimeException("Lỗi link:\n" + glGetProgramInfoLog(programId));
    }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setMat4(String name, Matrix4f mat) {
        try (MemoryStack st = stackPush()) {
            FloatBuffer fb = st.mallocFloat(16);
            mat.get(fb);
            glUniformMatrix4fv(loc(name), false, fb);
        }
    }

    /** Set mat3 — chỉ lấy 3x3 upper-left của Matrix4f. */
    public void setMat3(String name, Matrix4f mat) {
        try (MemoryStack st = stackPush()) {
            FloatBuffer fb = st.mallocFloat(9);
            Matrix3f m3 = new Matrix3f();
            mat.get3x3(m3);
            m3.get(fb);
            glUniformMatrix3fv(loc(name), false, fb);
        }
    }
    public void setMat4Array(String name, Matrix4f[] mats, int count) {
        try (MemoryStack st = stackPush()) {
            FloatBuffer fb = st.mallocFloat(count * 16);
            for (int i = 0; i < count; i++) {
                mats[i].get(i * 16, fb);
            }
            // Cache bằng key "name[0]"
            int loc = loc(name + "[0]");
            glUniformMatrix4fv(loc, false, fb);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(loc(name), x, y, z);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(loc(name), v.x, v.y, v.z);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        glUniform4f(loc(name), x, y, z, w);
    }

    public void setInt(String name, int value) {
        glUniform1i(loc(name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(loc(name), value);
    }

    public void bind()   { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }

    public void cleanup() {
        unbind();
        if (programId != 0) glDeleteProgram(programId);
    }
}
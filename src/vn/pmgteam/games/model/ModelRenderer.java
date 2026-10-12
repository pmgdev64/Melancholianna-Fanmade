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
import static org.lwjgl.opengl.GL13.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import vn.pmgteam.games.shader.ShaderProgram;

public class ModelRenderer {

    /** Tăng lên 128 — Mixamo X_Bot có ~65-70 bones */
    public static final int MAX_BONES = 128;

    private final ShaderProgram shader;
    private final Matrix4f[] boneMatrices = new Matrix4f[MAX_BONES];
    private final Matrix4f normalMat = new Matrix4f();

    public ModelRenderer(String vertSrc, String fragSrc) {
        shader = new ShaderProgram();
        shader.createVertexShader(vertSrc);
        shader.createFragmentShader(fragSrc);
        shader.link();

        // Init tất cả = identity để bone chưa update không gây explosion
        for (int i = 0; i < MAX_BONES; i++) {
            boneMatrices[i] = new Matrix4f();
        }
    }

    public void begin(Matrix4f projection, Matrix4f view, Matrix4f lightSpace,
                      Vector3f cameraPos, Vector3f sunDir, float[] skyColor,
                      float[] sunColor, float ambient, int shadowTex) {
        shader.bind();
        shader.setMat4("uProjection", projection);
        shader.setMat4("uView", view);
        shader.setMat4("uLightSpace", lightSpace);
        shader.setVec3("uCameraPos", cameraPos);
        shader.setVec3("uSunDirection", sunDir);
        shader.setVec3("uSkyColor", skyColor[0], skyColor[1], skyColor[2]);
        shader.setVec3("uSunColor", sunColor[0], sunColor[1], sunColor[2]);
        shader.setFloat("uAmbient", ambient);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowTex);
        shader.setInt("uShadowMap", 0);
    }

    public void render(Model model, Matrix4f modelMatrix, float[] color) {
        shader.setMat4("uModel", modelMatrix);
        shader.setVec3("uBaseColor", color[0], color[1], color[2]);

        // Normal matrix = transpose(inverse(mat3(model)))
        modelMatrix.normal(normalMat);
        shader.setMat3("uNormalMatrix", normalMat);

        if (model.skeleton != null) {
            int boneCount = model.skeleton.bones.size();

            // Log 1 lần nếu vượt MAX_BONES
            if (boneCount > MAX_BONES) {
                System.err.println("[ModelRenderer] WARNING: model has " + boneCount
                        + " bones but MAX_BONES=" + MAX_BONES
                        + " — extra bones will use identity");
            }

            int n = Math.min(boneCount, MAX_BONES);
            for (int i = 0; i < n; i++) {
                boneMatrices[i].set(model.skeleton.bones.get(i).finalMatrix);
            }
            // Bone > MAX_BONES giữ nguyên identity (đã init ở constructor)
            // → vertices có joint index vượt range sẽ dùng bind pose, không explode

            shader.setMat4Array("uBoneMatrices", boneMatrices, MAX_BONES);
        }

        for (Mesh m : model.meshes) {
            m.render();
        }
    }

    public void end() {
        shader.unbind();
    }

    public void cleanup() {
        shader.cleanup();
    }
}
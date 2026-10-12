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
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShadowMap {

    // 2048 texel trải trên 200m => ~10cm/texel
    public static final int SIZE = 2048;
    public static final float ORTHO_SIZE = 100f;

    // Mặt trời đặt cách tâm 200m; near/far quanh đó chỉ vừa đủ (±180m).
    // Depth range hẹp => bias tính theo mét nhỏ => vật thấp như cỏ mới đổ bóng được.
    private static final float SUN_DISTANCE = 200f;
    private static final float LIGHT_NEAR   = 20f;
    private static final float LIGHT_FAR    = 380f;

    private final int fbo;
    private final int depthTex;
    private final Matrix4f lightSpace = new Matrix4f();

    /** Sun direction động — cập nhật mỗi frame cho chu kỳ ngày/đêm */
    private final Vector3f sunDir = new Vector3f(0.4f, 1.0f, 0.3f).normalize();

    public ShadowMap() {
        fbo = glGenFramebuffers();

        depthTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24,
                SIZE, SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] border = { 1f, 1f, 1f, 1f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, border);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_2D, depthTex, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("ShadowMap FBO không hoàn chỉnh");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void setSunDirection(Vector3f dir) {
        this.sunDir.set(dir).normalize();
    }

    public Vector3f getSunDirection() { return sunDir; }

    public void update(Vector3f center) {
        Vector3f sunPos = new Vector3f(center).fma(SUN_DISTANCE, sunDir);
        Vector3f target = new Vector3f(center);

        Matrix4f lightView = new Matrix4f().lookAt(sunPos, target, new Vector3f(0, 1, 0));
        Matrix4f lightProj = new Matrix4f().ortho(
                -ORTHO_SIZE, ORTHO_SIZE,
                -ORTHO_SIZE, ORTHO_SIZE,
                LIGHT_NEAR, LIGHT_FAR);

        lightProj.mul(lightView, lightSpace);
    }

    public void begin() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, SIZE, SIZE);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void end(int screenW, int screenH) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenW, screenH);
    }

    public Matrix4f getLightSpaceMatrix() { return lightSpace; }
    public int getDepthTexture()          { return depthTex; }

    public void cleanup() {
        glDeleteTextures(depthTex);
        glDeleteFramebuffers(fbo);
    }
}
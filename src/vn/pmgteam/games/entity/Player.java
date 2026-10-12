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

package vn.pmgteam.games.entity;

import static org.lwjgl.glfw.GLFW.*;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import vn.pmgteam.games.main.Game;
import vn.pmgteam.games.model.Animator;
import vn.pmgteam.games.model.Model;
import vn.pmgteam.games.model.ModelRenderer;
import vn.pmgteam.games.placeable.Placeable;
import vn.pmgteam.games.world.AABB;
import vn.pmgteam.games.world.Camera3D;
import vn.pmgteam.games.world.Terrain;

public class Player {

    public final Vector3f position = new Vector3f();
    public final Vector3f velocity = new Vector3f();

    public float width  = 0.6f;
    public float depth  = 0.6f;
    public float height = 1.8f;
    public float eyeHeight = 1.7f;

    public float walkSpeed = 2.5f;
    public float runSpeed  = 5.0f;
    public float flySpeed  = 40f;
    public float jumpSpeed = 9f;
    public float gravity   = 28f;

    public float stepHeight = 0.3f;
    public float maxSlope   = 1.8f;

    public boolean onGround = false;
    public boolean flying = false;
    public boolean running = false;

    public float bodyYaw = 0f;

    private float walkTime = 0f;

    private List<Placeable> placeables;

    private Model    model;
    private Animator animator;
    private String   currentAnim = "";
    private final Matrix4f playerMatrix = new Matrix4f();
    private final float[]  bodyColor = { 0.85f, 0.55f, 0.45f, 1f };

    private static final float TURN_SPEED = 12f;

    private static final float MIXAMO_WALK_REF = 1.5f;
    private static final float MIXAMO_RUN_REF  = 3.5f;
    private static final float MAX_ANIM_SPEED  = 1.5f;

    private static final float MODEL_Y_OFFSET = 0.2f;

    private static final float TERRAIN_SAMPLE_RADIUS = 0.1f;

    private boolean prevP = false;
    private boolean prevO = false;

    public Player(float x, float z, Terrain terrain) {
        position.set(x, terrain.heightAt(x, z) + 3f, z);
    }

    public void setPlaceables(List<Placeable> list) {
        this.placeables = list;
    }

    public void setModel(Model model) {
        this.model = model;
        if (model != null) {
            this.animator = new Animator(model);
            // Thử Idle trước; nếu không có, Animator.play sẽ tự fallback
            playAnim("Idle");
        }
    }

    public String getCurrentAnim() { return currentAnim; }

    private void playAnim(String name) {
        if (animator == null) return;
        if (name.equals(currentAnim)) return;
        animator.play(name);
        animator.setLooping(true);
        currentAnim = name;
    }

    // ==================== UPDATE ====================

    public void update(float dt, long window, Terrain terrain, Camera3D camera) {
        boolean pNow = glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS;
        boolean oNow = glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS;

        if (pNow && !prevP && animator != null) {
            animator.setEnabled(false);
            System.out.println("=== T-POSE MODE ===");
        }
        if (oNow && !prevO && animator != null) {
            animator.setEnabled(true);
            System.out.println("=== ANIMATION ON ===");
        }
        prevP = pNow;
        prevO = oNow;

        float yawRad = (float) Math.toRadians(camera.yaw);
        Vector3f forward = new Vector3f(
                (float)  Math.sin(yawRad), 0,
                (float) -Math.cos(yawRad));
        Vector3f right = new Vector3f(forward).cross(0, 1, 0).normalize();

        Vector3f wish = new Vector3f();
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) wish.add(forward);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) wish.sub(forward);
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) wish.add(right);
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) wish.sub(right);

        running = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                  || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;

        boolean moving = wish.lengthSquared() > 1e-4f;
        if (moving) wish.normalize();

        if (moving) {
            float targetYaw = (float) Math.toDegrees(
                    Math.atan2(-wish.x, -wish.z));
            bodyYaw = lerpAngle(bodyYaw, targetYaw, dt * TURN_SPEED);
        }

        if (moving && !flying && onGround) {
            walkTime += dt * 1.0f;
            walkTime %= (float)(Math.PI * 2.0);
        } else {
            walkTime *= 0.85f;
        }

        if (flying) updateFly(dt, window, wish);
        else        updateWalk(dt, window, wish, terrain);

        if (animator != null) {
            String desired;
            float animSpeed = 1.0f;

            if (!moving) {
                desired = "Idle";
            } else if (running) {
                desired = "Run";
                animSpeed = runSpeed / MIXAMO_RUN_REF;
            } else {
                desired = "Walk";
                animSpeed = walkSpeed / MIXAMO_WALK_REF;
            }

            animSpeed = Math.max(0.5f, Math.min(animSpeed, MAX_ANIM_SPEED));

            playAnim(desired);
            animator.setSpeed(animSpeed);
            animator.update(dt);
        }
    }

    private static float lerpAngle(float current, float target, float t) {
        t = Math.min(1f, Math.max(0f, t));
        float diff = target - current;
        while (diff >  180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return current + diff * t;
    }

    public void toggleFly() {
        flying = !flying;
        if (flying) { velocity.y = 0; onGround = false; }
    }

    public Vector3f getEyePosition(Vector3f out) {
        return out.set(position.x, position.y + eyeHeight, position.z);
    }

    // ==================== RENDER ====================

    public void render(Game game) {
        if (model != null) return;
        game.drawBoxRotated(
                position.x,
                position.y + height * 0.5f,
                position.z,
                width, height, depth,
                bodyYaw,
                new float[]{ 0.9f, 0.2f, 0.2f, 1f });
    }

    public void renderModel(ModelRenderer renderer) {
        if (model == null || renderer == null) return;

        playerMatrix.identity()
                .translate(position.x, position.y + MODEL_Y_OFFSET, position.z)
                .rotateY((float) Math.toRadians(bodyYaw + 180f));

        renderer.render(model, playerMatrix, bodyColor);
    }

    // ==================== INTERNAL ====================

    private void updateFly(float dt, long window, Vector3f wish) {
        velocity.x = wish.x * flySpeed;
        velocity.z = wish.z * flySpeed;
        velocity.y = 0;

        if (glfwGetKey(window, GLFW_KEY_SPACE)        == GLFW_PRESS) velocity.y =  flySpeed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) velocity.y = -flySpeed;

        position.x += velocity.x * dt;
        position.y += velocity.y * dt;
        position.z += velocity.z * dt;
    }

    private void updateWalk(float dt, long window, Vector3f wish, Terrain terrain) {
        float speed = running ? runSpeed : walkSpeed;
        float vx = wish.x * speed;
        float vz = wish.z * speed;

        moveAxis(vx * dt, 0, terrain);
        moveAxis(0, vz * dt, terrain);

        velocity.y -= gravity * dt;

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround) {
            velocity.y = jumpSpeed;
            onGround = false;
        }

        float newPy = position.y + velocity.y * dt;

        if (collidesWithPlaceable(position.x, newPy, position.z)) {
            if (velocity.y < 0) {
                float topY = topOfPlaceableUnder(position.x, position.z, position.y);
                if (topY > -Float.MAX_VALUE) {
                    position.y = topY;
                    velocity.y = 0;
                    onGround = true;
                    return;
                }
                position.y = newPy;
            } else if (velocity.y > 0) {
                velocity.y = 0;
            }
        } else {
            position.y = newPy;
        }

        float groundH = maxHeightInAABB(position.x, position.z, terrain);
        if (position.y <= groundH) {
            position.y = groundH;
            velocity.y = 0;
            onGround = true;
        } else {
            onGround = false;
        }
    }

    private void moveAxis(float dx, float dz, Terrain terrain) {
        if (dx == 0f && dz == 0f) return;

        float newX = position.x + dx;
        float newZ = position.z + dz;

        if (!canStepTo(position.x, position.z, newX, newZ, terrain)) return;

        if (!collidesWithPlaceable(newX, position.y, newZ)) {
            position.x = newX;
            position.z = newZ;
            return;
        }

        float stepY = position.y + stepHeight;
        if (!collidesWithPlaceable(newX, stepY, newZ)) {
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.001f && stepHeight / dist > maxSlope) return;

            position.x = newX;
            position.z = newZ;
            position.y = stepY;
            onGround = false;
            return;
        }
    }

    private boolean collidesWithPlaceable(float px, float py, float pz) {
        if (placeables == null || placeables.isEmpty()) return false;

        float pl = px - width * 0.5f, pr = px + width * 0.5f;
        float pb = py, pt = py + height;
        float pd = pz - depth * 0.5f, pu = pz + depth * 0.5f;

        for (int i = 0; i < placeables.size(); i++) {
            Placeable q = placeables.get(i);
            if (!q.isSolid()) continue;

            List<AABB> boxes = q.getCollisionBoxes();
            for (int b = 0; b < boxes.size(); b++) {
                if (boxes.get(b).intersects(pl, pb, pd, pr, pt, pu)) {
                    return true;
                }
            }
        }
        return false;
    }

    private float topOfPlaceableUnder(float px, float pz, float feetY) {
        if (placeables == null) return -Float.MAX_VALUE;

        float pl = px - width * 0.5f, pr = px + width * 0.5f;
        float pd = pz - depth * 0.5f, pu = pz + depth * 0.5f;

        float best = -Float.MAX_VALUE;

        for (int i = 0; i < placeables.size(); i++) {
            Placeable q = placeables.get(i);
            if (!q.isSolid()) continue;

            List<AABB> boxes = q.getCollisionBoxes();
            for (int b = 0; b < boxes.size(); b++) {
                AABB box = boxes.get(b);
                if (pl < box.maxX && pr > box.minX
                 && pd < box.maxZ && pu > box.minZ) {
                    float top = box.maxY;
                    if (top <= feetY + 0.01f && top > best) best = top;
                }
            }
        }
        return best;
    }

    private boolean canStepTo(float oldX, float oldZ,
                              float newX, float newZ, Terrain terrain) {
        float currentY = position.y;
        float newH = maxHeightInAABB(newX, newZ, terrain);

        if (newH <= currentY) return true;

        float delta = newH - currentY;
        if (delta < stepHeight) return true;

        float dx = newX - oldX, dz = newZ - oldZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001f) return true;

        return (delta / dist) <= maxSlope;
    }

    private float maxHeightInAABB(float cx, float cz, Terrain terrain) {
        float r = TERRAIN_SAMPLE_RADIUS;

        float h0 = terrain.heightAt(cx, cz);
        float h1 = terrain.heightAt(cx - r, cz - r);
        float h2 = terrain.heightAt(cx + r, cz - r);
        float h3 = terrain.heightAt(cx - r, cz + r);
        float h4 = terrain.heightAt(cx + r, cz + r);

        return Math.max(h0, Math.max(Math.max(h1, h2), Math.max(h3, h4)));
    }
}
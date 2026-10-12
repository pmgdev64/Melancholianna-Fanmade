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

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import vn.pmgteam.games.placeable.Placeable;

public class Camera3D {

    public Vector3f position = new Vector3f(0, 45, 30);
    public float yaw   = 0f;
    public float pitch = -15f;
    public float speed = 40f;

    /** FOV thực tế đang dùng — tự cập nhật theo distance */
    public float fov    = 65f;

    /** FOV cơ sở khi distance = baseDistance */
    public float baseFov = 65f;

    /** Khoảng cách cơ sở — FOV lệch quanh giá trị này */
    public float baseDistance = 5f;

    /** Bao nhiêu độ FOV thay đổi cho mỗi mét thay đổi distance.
     *  0.2 = mỗi notch scroll (0.5m) → 0.1 độ */
    private static final float FOV_PER_METER = 0.2f;

    public float aspect = 1280f / 720f;
    public float near   = 0.1f;
    public float far    = 800f;

    /** Khoảng cách mong muốn (input từ scroll) */
    public float distance = 5f;

    /** Khoảng cách thực tế sau collision + smooth */
    private float currentDistance = 5f;

    /** Tốc độ zoom out khi hết vật cản (m/s) */
    private static final float ZOOM_OUT_SPEED = 12f;

    /** Tốc độ zoom in khi gặp vật cản (m/s) — nhanh để không bị clip */
    private static final float ZOOM_IN_SPEED  = 60f;

    public Vector3f getForward() {
        float cy = (float) Math.cos(Math.toRadians(yaw));
        float sy = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));
        return new Vector3f(cp * sy, sp, -cp * cy).normalize();
    }

    public Vector3f getRight() {
        return new Vector3f(getForward()).cross(0, 1, 0).normalize();
    }

    public Matrix4f getViewMatrix() {
        Vector3f fwd = getForward();
        Vector3f target = new Vector3f(position).add(fwd);
        return new Matrix4f().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fov), aspect, near, far);
    }

    public Matrix4f getViewProjectionMatrix() {
        return getProjectionMatrix().mul(getViewMatrix());
    }

    /**
     * Ray-march từ target ra sau lưng — trả khoảng cách AN TOÀN.
     */
    private float computeSafeDistance(Vector3f target, Vector3f fwd,
                                       Terrain terrain,
                                       List<Placeable> placeables) {
        float start = 1.5f;
        float end   = distance;

        for (float d = start; d < end; d += 0.2f) {
            Vector3f p = new Vector3f(target).fma(-d, fwd);

            // Check terrain
            float h = terrain.heightAt(p.x, p.z) + 0.3f;
            if (p.y < h) return Math.max(1.2f, d - 0.2f);

            // Check placeables
            if (placeables != null) {
                boolean blocked = false;
                for (Placeable q : placeables) {
                    if (!q.isSolid()) continue;
                    List<AABB> boxes = q.getCollisionBoxes();
                    for (AABB box : boxes) {
                        if (p.x >= box.minX && p.x <= box.maxX
                         && p.y >= box.minY && p.y <= box.maxY
                         && p.z >= box.minZ && p.z <= box.maxZ) {
                            blocked = true;
                            break;
                        }
                    }
                    if (blocked) break;
                }
                if (blocked) return Math.max(1.2f, d - 0.2f);
            }
        }
        return end;
    }

    /**
     * Cập nhật camera 3rd person với collision + smooth transition.
     * FOV tự động thay đổi theo distance: zoom in → FOV tăng, zoom out → FOV giảm.
     */
    public void updateThirdPerson(Vector3f target, Terrain terrain,
                                  List<Placeable> placeables, float dt) {
        Vector3f fwd = getForward();

        float safeTarget = computeSafeDistance(target, fwd, terrain, placeables);

        // Lerp currentDistance → safeTarget
        float diff = safeTarget - currentDistance;
        float maxDelta = (diff < 0) ? (ZOOM_IN_SPEED * dt) : (ZOOM_OUT_SPEED * dt);

        if (Math.abs(diff) <= maxDelta) {
            currentDistance = safeTarget;
        } else {
            currentDistance += Math.signum(diff) * maxDelta;
        }

        position.set(target).fma(-currentDistance, fwd);

        // === Cập nhật FOV theo distance (không phải currentDistance) ===
        // Chỉ scroll wheel ảnh hưởng FOV, không phải collision zoom.
        float newFov = baseFov + (baseDistance - distance) * FOV_PER_METER;

        // Clamp FOV trong khoảng hợp lý
        newFov = Math.max(45f, Math.min(95f, newFov));

        this.fov = newFov;
    }

    public void updateThirdPerson(Vector3f target, Terrain terrain,
                                  List<Placeable> placeables) {
        updateThirdPerson(target, terrain, placeables, 1f / 60f);
    }

    public void updateThirdPerson(Vector3f target, Terrain terrain) {
        updateThirdPerson(target, terrain, null, 1f / 60f);
    }

    public void setDistance(float d) {
        this.distance = Math.max(2.5f, Math.min(20f, d));
    }
}
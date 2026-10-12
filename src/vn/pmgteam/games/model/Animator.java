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

import java.util.stream.Collectors;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Animator {

    private final Skeleton skeleton;
    private final Model model;

    private Animation current;
    private float time = 0f;
    private boolean looping = true;
    private float speed = 1f;

    // === Crossfade ===
    private Animation prev;
    private float prevTime = 0f;
    private float prevDuration = 0f;
    private float blendTime = 0f;
    private float blendDuration = 0f;
    private boolean blending = false;

    public static final float DEFAULT_BLEND = 0.2f;

    private int rootBoneIndex = -1;

    /** Debug — khi false, model giữ bind pose (T-pose). */
    private boolean enabled = true;

    public Animator(Model model) {
        this.model = model;
        this.skeleton = model.skeleton;

        if (skeleton != null) {
            for (int i = 0; i < skeleton.bones.size(); i++) {
                if (skeleton.bones.get(i).parentIndex == -1) {
                    rootBoneIndex = i;
                    break;
                }
            }
            System.out.println("Animator: " + skeleton.bones.size()
                    + " bones, rootIndex=" + rootBoneIndex);
        }
    }

    public void play(String animName) {
        play(animName, DEFAULT_BLEND);
    }

    public void play(String animName, float blendDur) {
        Animation a = model.getAnimation(animName);

        // Fallback: model chỉ có 1 anim → dùng luôn
        if (a == null && model.animations.size() == 1) {
            a = model.getAnyAnimation();
            System.out.println("Animator fallback: '" + animName
                    + "' → '" + a.name + "' (only anim)");
        }

        if (a == null) {
            String available = model.animations.stream()
                    .map(x -> x.name)
                    .collect(Collectors.joining(", "));
            System.err.println("Không tìm thấy animation: '" + animName
                    + "'. Có sẵn: [" + available + "]");
            return;
        }
        if (current == a) return;

        if (current != null && blendDur > 0f) {
            prev = current;
            prevTime = time;
            prevDuration = current.duration;
            blendTime = 0f;
            blendDuration = blendDur;
            blending = true;
        } else {
            blending = false;
            prev = null;
        }

        this.current = a;
        this.time = 0f;
    }

    public void setLooping(boolean loop) { this.looping = loop; }
    public void setSpeed(float s)        { this.speed = s; }
    public Animation getCurrent()        { return current; }
    public float getTime()               { return time; }
    public boolean isBlending()          { return blending; }
    public float getBlendProgress() {
        return blending ? (blendTime / blendDuration) : 1f;
    }

    public void setEnabled(boolean e)    { this.enabled = e; }
    public boolean isEnabled()           { return enabled; }

    public void update(float dt) {
        if (current == null) return;

        time += dt * speed;
        if (time >= current.duration) {
            if (looping) time = time % current.duration;
            else time = current.duration;
        }

        if (blending) {
            blendTime += dt;
            prevTime += dt;
            if (prevDuration > 0f && prevTime >= prevDuration) {
                prevTime = prevTime % prevDuration;
            }
            if (blendTime >= blendDuration) {
                blending = false;
                prev = null;
            }
        }

        applyPose();
    }

    private void applyPose() {
        // 1. Reset về bind pose
        for (Bone b : skeleton.bones) {
            b.resetToBind();
        }

        // Debug — T-pose nếu disabled
        if (!enabled) {
            skeleton.computeWorldTransforms();
            return;
        }

        if (blending && prev != null) {
            float t = Math.min(1f, blendTime / blendDuration);
            float alpha = t * t * (3f - 2f * t);

            // Bước 1: pose = prev
            applyAnimationToPose(prev, prevTime, 1f);

            // Bước 2: lerp/slerp về current
            applyAnimationToPose(current, time, alpha);
        } else {
            applyAnimationToPose(current, time, 1f);
        }

        skeleton.computeWorldTransforms();
    }

    private void applyAnimationToPose(Animation anim, float t, float weight) {
        for (Animation.Channel ch : anim.channels) {
            Integer boneIdx = skeleton.nodeToBone.get(ch.targetNode);
            if (boneIdx == null) continue;

            Bone bone = skeleton.bones.get(boneIdx);
            boolean isRoot = (boneIdx == rootBoneIndex);

            if ("translation".equals(ch.path)) {
                if (isRoot) continue;   // bỏ root motion

                Vector3f v = sampleVec3(ch.times, ch.values, t);
                if (weight >= 1f) {
                    bone.currentTranslation.set(v);
                } else {
                    bone.currentTranslation.lerp(v, weight);
                }

            } else if ("rotation".equals(ch.path)) {
                Quaternionf q = sampleQuat(ch.times, ch.values, t);
                if (weight >= 1f) {
                    bone.currentRotation.set(q);
                } else {
                    bone.currentRotation.slerp(q, weight);
                }

            } else if ("scale".equals(ch.path)) {
                Vector3f v = sampleVec3(ch.times, ch.values, t);
                if (weight >= 1f) {
                    bone.currentScale.set(v);
                } else {
                    bone.currentScale.lerp(v, weight);
                }
            }
        }
    }

    private Vector3f sampleVec3(float[] times, float[] values, float t) {
        int idx = findKeyframe(times, t);
        if (idx < 0) return new Vector3f(values[0], values[1], values[2]);

        int i0 = idx, i1 = idx + 1;
        if (i1 >= times.length) i1 = i0;

        float t0 = times[i0];
        float t1 = times[i1];
        float a = (t1 > t0) ? (t - t0) / (t1 - t0) : 0f;

        return new Vector3f(
                values[i0*3]   + a * (values[i1*3]   - values[i0*3]),
                values[i0*3+1] + a * (values[i1*3+1] - values[i0*3+1]),
                values[i0*3+2] + a * (values[i1*3+2] - values[i0*3+2])
        );
    }

    private Quaternionf sampleQuat(float[] times, float[] values, float t) {
        int idx = findKeyframe(times, t);
        if (idx < 0) {
            Quaternionf q = new Quaternionf(
                    values[0], values[1], values[2], values[3]);
            q.normalize();
            return q;
        }

        int i0 = idx, i1 = idx + 1;
        if (i1 >= times.length) i1 = i0;

        float t0 = times[i0];
        float t1 = times[i1];
        float a = (t1 > t0) ? (t - t0) / (t1 - t0) : 0f;

        Quaternionf q0 = new Quaternionf(
                values[i0*4], values[i0*4+1], values[i0*4+2], values[i0*4+3]);
        Quaternionf q1 = new Quaternionf(
                values[i1*4], values[i1*4+1], values[i1*4+2], values[i1*4+3]);

        q0.normalize();
        q1.normalize();

        if (q0.dot(q1) < 0f) {
            q1.x = -q1.x;
            q1.y = -q1.y;
            q1.z = -q1.z;
            q1.w = -q1.w;
        }

        Quaternionf result = q0.slerp(q1, a, new Quaternionf());
        result.normalize();
        return result;
    }

    private int findKeyframe(float[] times, float t) {
        if (times.length == 0) return -1;
        if (t <= times[0]) return 0;
        if (t >= times[times.length - 1]) return times.length - 1;

        int lo = 0, hi = times.length - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) / 2;
            if (times[mid] <= t) lo = mid;
            else hi = mid;
        }
        return lo;
    }
}
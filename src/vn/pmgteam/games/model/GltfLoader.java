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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Minimal glTF 2.0 binary (.glb) loader.
 */
public final class GltfLoader {

    private GltfLoader() {}

    // ==================== PUBLIC API ====================

    public static Model load(String filepath) {
        try {
            byte[] raw = Files.readAllBytes(Paths.get(filepath));
            return parse(raw, filepath);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file: " + filepath, e);
        }
    }

    public static Model loadResource(String resourcePath) {
        try (InputStream is = GltfLoader.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Không tìm thấy resource: " + resourcePath);
            }
            byte[] raw = is.readAllBytes();
            return parse(raw, resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được resource: " + resourcePath, e);
        }
    }

    // ==================== NUMBER HELPERS ====================

    private static float numF(Object o) {
        if (o == null) return 0f;
        if (o instanceof Number) return ((Number) o).floatValue();
        throw new RuntimeException("Not a number: " + o.getClass());
    }

    private static int numI(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        throw new RuntimeException("Not a number: " + o.getClass());
    }

    // ==================== PARSING ====================

    private static Model parse(byte[] raw, String debugName) {
        ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);

        int magic = buf.getInt();
        if (magic != 0x46546C67) {
            throw new RuntimeException("Không phải file GLB (magic sai): " + debugName);
        }
        int version = buf.getInt();
        int totalLength = buf.getInt();
        System.out.println("GLB loaded: " + debugName + " v" + version
                + " size=" + totalLength);

        int jsonChunkLen  = buf.getInt();
        int jsonChunkType = buf.getInt();
        if (jsonChunkType != 0x4E4F534A) {
            throw new RuntimeException("Chunk đầu không phải JSON");
        }
        byte[] jsonBytes = new byte[jsonChunkLen];
        buf.get(jsonBytes);
        String json = new String(jsonBytes, StandardCharsets.UTF_8);

        byte[] binBytes = null;
        if (buf.remaining() >= 8) {
            int binChunkLen  = buf.getInt();
            int binChunkType = buf.getInt();
            if (binChunkType == 0x004E4942) {
                binBytes = new byte[binChunkLen];
                buf.get(binBytes);
            }
        }

        Object jsonRoot = new JsonParser(json).parse();
        if (!(jsonRoot instanceof Map)) {
            throw new RuntimeException("JSON root không phải object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) jsonRoot;

        return buildModel(root, binBytes, debugName);
    }

    @SuppressWarnings("unchecked")
    private static Model buildModel(Map<String, Object> root,
                                     byte[] bin,
                                     String debugName) {
        Model model = new Model();
        model.name = debugName;

        List<Object> accessors = (List<Object>) root.get("accessors");
        List<Object> bufferViews = (List<Object>) root.get("bufferViews");

        List<Object> meshes = (List<Object>) root.get("meshes");
        if (meshes != null) {
            for (Object meshObj : meshes) {
                Map<String, Object> mesh = (Map<String, Object>) meshObj;
                List<Object> primitives = (List<Object>) mesh.get("primitives");
                if (primitives == null) continue;

                for (Object primObj : primitives) {
                    Map<String, Object> prim = (Map<String, Object>) primObj;
                    Mesh m = buildMesh(prim, accessors, bufferViews, bin);
                    if (m != null) model.meshes.add(m);
                }
            }
        }

        List<Object> skins = (List<Object>) root.get("skins");
        if (skins != null && !skins.isEmpty()) {
            Map<String, Object> skin = (Map<String, Object>) skins.get(0);
            model.skeleton = buildSkeleton(skin, root, accessors, bufferViews, bin);
        }

        List<Object> anims = (List<Object>) root.get("animations");
        if (anims != null) {
            for (Object animObj : anims) {
                Map<String, Object> animMap = (Map<String, Object>) animObj;
                Animation a = buildAnimation(animMap, accessors, bufferViews, bin);
                if (a != null) model.animations.add(a);
            }
        }

        System.out.println("  Meshes: " + model.meshes.size()
                + " | Bones: " + (model.skeleton != null
                        ? model.skeleton.bones.size() : 0)
                + " | Animations: " + model.animations.size());
        for (Animation a : model.animations) {
            System.out.println("    - '" + a.name + "' (" + a.duration + "s, "
                             + a.channels.size() + " channels)");
        }

        return model;
    }

    @SuppressWarnings("unchecked")
    private static Mesh buildMesh(Map<String, Object> prim,
                                   List<Object> accessors,
                                   List<Object> bufferViews,
                                   byte[] bin) {
        Map<String, Object> attrs = (Map<String, Object>) prim.get("attributes");
        if (attrs == null) return null;

        int posAcc = numI(attrs.get("POSITION"));
        int normAcc = attrs.containsKey("NORMAL")
                ? numI(attrs.get("NORMAL")) : -1;
        int jointAcc = attrs.containsKey("JOINTS_0")
                ? numI(attrs.get("JOINTS_0")) : -1;
        int weightAcc = attrs.containsKey("WEIGHTS_0")
                ? numI(attrs.get("WEIGHTS_0")) : -1;

        Mesh mesh = new Mesh();
        mesh.positions = readFloat3(posAcc, accessors, bufferViews, bin);
        mesh.normals   = normAcc >= 0
                ? readFloat3(normAcc, accessors, bufferViews, bin) : null;
        mesh.joints    = jointAcc >= 0
                ? readInt4(jointAcc, accessors, bufferViews, bin) : null;
        mesh.weights   = weightAcc >= 0
                ? readFloat4(weightAcc, accessors, bufferViews, bin) : null;

        int idxAcc = numI(prim.get("indices"));
        mesh.indices = readIndices(idxAcc, accessors, bufferViews, bin);

        mesh.vertexCount = mesh.positions.length / 3;
        mesh.indexCount  = mesh.indices.length;

        return mesh;
    }

    @SuppressWarnings("unchecked")
    private static Skeleton buildSkeleton(Map<String, Object> skin,
                                           Map<String, Object> root,
                                           List<Object> accessors,
                                           List<Object> bufferViews,
                                           byte[] bin) {
        Skeleton skeleton = new Skeleton();

        List<Object> joints = (List<Object>) skin.get("joints");
        int ibmAcc = numI(skin.get("inverseBindMatrices"));

        float[] ibmData = readMat4(ibmAcc, accessors, bufferViews, bin);

        List<Object> nodes = (List<Object>) root.get("nodes");

        // Pass 1: điền nodeToBone map
        for (int i = 0; i < joints.size(); i++) {
            int nodeIdx = numI(joints.get(i));
            skeleton.nodeToBone.put(nodeIdx, i);
        }

        // Pass 2: build bones
        for (int i = 0; i < joints.size(); i++) {
            Bone b = new Bone();
            b.index = i;

            int nodeIdx = numI(joints.get(i));
            b.sourceNode = nodeIdx;

            Map<String, Object> node = (Map<String, Object>) nodes.get(nodeIdx);

            if (node.containsKey("name")) {
                b.name = (String) node.get("name");
            }

            if (node.containsKey("translation")) {
                List<Object> t = (List<Object>) node.get("translation");
                b.bindTranslation.set(numF(t.get(0)), numF(t.get(1)), numF(t.get(2)));
            }
            if (node.containsKey("rotation")) {
                List<Object> r = (List<Object>) node.get("rotation");
                b.bindRotation.set(numF(r.get(0)), numF(r.get(1)),
                                   numF(r.get(2)), numF(r.get(3)));
            }
            if (node.containsKey("scale")) {
                List<Object> s = (List<Object>) node.get("scale");
                b.bindScale.set(numF(s.get(0)), numF(s.get(1)), numF(s.get(2)));
            }

            b.resetToBind();

            int parentNode = findParent(nodes, nodeIdx);
            b.parentIndex = parentNode >= 0
                    ? skeleton.nodeToBone.getOrDefault(parentNode, -1)
                    : -1;

            // === IBM: JOML set() nhận tham số COLUMN-MAJOR (mXY = cột X, hàng Y)
            //     glTF IBM cũng là column-major → truyền trực tiếp, KHÔNG transpose
            b.inverseBindMatrix.set(
                ibmData[i*16],    ibmData[i*16+1],  ibmData[i*16+2],  ibmData[i*16+3],
                ibmData[i*16+4],  ibmData[i*16+5],  ibmData[i*16+6],  ibmData[i*16+7],
                ibmData[i*16+8],  ibmData[i*16+9],  ibmData[i*16+10], ibmData[i*16+11],
                ibmData[i*16+12], ibmData[i*16+13], ibmData[i*16+14], ibmData[i*16+15]
            );

            skeleton.bones.add(b);
        }

        // Compute ancestor matrix — KHÔNG modify bind pose
        if (!skeleton.bones.isEmpty()) {
            int rootNodeIdx = skeleton.bones.get(0).sourceNode;
            Matrix4f ancestor = computeAncestorMatrix(nodes, rootNodeIdx);
            skeleton.ancestorMatrix.set(ancestor);
        }

        skeleton.computeWorldTransforms();
        return skeleton;
    }

    @SuppressWarnings("unchecked")
    private static Matrix4f computeAncestorMatrix(List<Object> nodes, int rootBoneNodeIdx) {
        List<Integer> chain = new ArrayList<>();
        int p = findParent(nodes, rootBoneNodeIdx);
        while (p >= 0) {
            chain.add(p);
            p = findParent(nodes, p);
        }

        Matrix4f ancestorMatrix = new Matrix4f();
        for (int i = chain.size() - 1; i >= 0; i--) {
            Map<String, Object> node = (Map<String, Object>) nodes.get(chain.get(i));
            Matrix4f local = nodeToMatrix(node);
            ancestorMatrix.mul(local, ancestorMatrix);
        }

        Vector3f scale = ancestorMatrix.getScale(new Vector3f());
        System.out.println("Ancestor chain length: " + chain.size());
        System.out.printf("Ancestor scale: (%.4f, %.4f, %.4f)%n",
                scale.x, scale.y, scale.z);

        return ancestorMatrix;
    }

    @SuppressWarnings("unchecked")
    private static Matrix4f nodeToMatrix(Map<String, Object> node) {
        Matrix4f m = new Matrix4f();
        if (node.containsKey("translation")) {
            List<Object> t = (List<Object>) node.get("translation");
            m.translate(numF(t.get(0)), numF(t.get(1)), numF(t.get(2)));
        }
        if (node.containsKey("rotation")) {
            List<Object> r = (List<Object>) node.get("rotation");
            Quaternionf q = new Quaternionf(
                    numF(r.get(0)), numF(r.get(1)),
                    numF(r.get(2)), numF(r.get(3)));
            m.rotate(q);
        }
        if (node.containsKey("scale")) {
            List<Object> s = (List<Object>) node.get("scale");
            m.scale(numF(s.get(0)), numF(s.get(1)), numF(s.get(2)));
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static int findParent(List<Object> nodes, int childIdx) {
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = (Map<String, Object>) nodes.get(i);
            List<Object> children = (List<Object>) node.get("children");
            if (children == null) continue;
            for (Object c : children) {
                if (numI(c) == childIdx) return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static Animation buildAnimation(Map<String, Object> animMap,
                                             List<Object> accessors,
                                             List<Object> bufferViews,
                                             byte[] bin) {
        Animation anim = new Animation();
        anim.name = animMap.containsKey("name")
                ? (String) animMap.get("name") : "unnamed";

        List<Object> channels = (List<Object>) animMap.get("channels");
        List<Object> samplers = (List<Object>) animMap.get("samplers");
        if (channels == null || samplers == null) return null;

        float maxTime = 0f;

        for (Object chObj : channels) {
            Map<String, Object> ch = (Map<String, Object>) chObj;
            Map<String, Object> target = (Map<String, Object>) ch.get("target");
            int nodeIdx = numI(target.get("node"));
            String path = (String) target.get("path");
            int samplerIdx = numI(ch.get("sampler"));

            Map<String, Object> sampler = (Map<String, Object>) samplers.get(samplerIdx);
            int inputAcc  = numI(sampler.get("input"));
            int outputAcc = numI(sampler.get("output"));

            float[] times = readFloat(inputAcc, accessors, bufferViews, bin);

            Animation.Channel chData = new Animation.Channel();
            chData.targetNode = nodeIdx;
            chData.path = path;
            chData.times = times;

            if ("translation".equals(path) || "scale".equals(path)) {
                chData.values = readFloat3(outputAcc, accessors, bufferViews, bin);
            } else if ("rotation".equals(path)) {
                chData.values = readFloat4(outputAcc, accessors, bufferViews, bin);
            }

            anim.channels.add(chData);

            if (times.length > 0) {
                maxTime = Math.max(maxTime, times[times.length - 1]);
            }
        }

        anim.duration = maxTime;
        return anim;
    }

    // ==================== ACCESSOR READERS ====================

    @SuppressWarnings("unchecked")
    private static ByteBuffer getBufferView(int bvIdx,
                                             List<Object> bufferViews,
                                             byte[] bin) {
        Map<String, Object> bv = (Map<String, Object>) bufferViews.get(bvIdx);
        int offset = bv.containsKey("byteOffset")
                ? numI(bv.get("byteOffset")) : 0;
        int length = numI(bv.get("byteLength"));

        return ByteBuffer.wrap(bin, offset, length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @SuppressWarnings("unchecked")
    private static int getAccessorComponentType(int accIdx, List<Object> accessors) {
        Map<String, Object> acc = (Map<String, Object>) accessors.get(accIdx);
        return numI(acc.get("componentType"));
    }

    @SuppressWarnings("unchecked")
    private static int getAccessorCount(int accIdx, List<Object> accessors) {
        Map<String, Object> acc = (Map<String, Object>) accessors.get(accIdx);
        return numI(acc.get("count"));
    }

    @SuppressWarnings("unchecked")
    private static int getAccessorBV(int accIdx, List<Object> accessors) {
        Map<String, Object> acc = (Map<String, Object>) accessors.get(accIdx);
        return numI(acc.get("bufferView"));
    }

    @SuppressWarnings("unchecked")
    private static int getAccessorByteOffset(int accIdx, List<Object> accessors) {
        Map<String, Object> acc = (Map<String, Object>) accessors.get(accIdx);
        if (!acc.containsKey("byteOffset")) return 0;
        return numI(acc.get("byteOffset"));
    }

    private static float[] readFloat(int accIdx, List<Object> accessors,
                                      List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        float[] out = new float[count];
        for (int i = 0; i < count; i++) out[i] = buf.getFloat();
        return out;
    }

    private static float[] readFloat3(int accIdx, List<Object> accessors,
                                       List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        float[] out = new float[count * 3];
        for (int i = 0; i < count * 3; i++) out[i] = buf.getFloat();
        return out;
    }

    private static float[] readFloat4(int accIdx, List<Object> accessors,
                                       List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        float[] out = new float[count * 4];
        for (int i = 0; i < count * 4; i++) out[i] = buf.getFloat();
        return out;
    }

    private static int[] readInt4(int accIdx, List<Object> accessors,
                                   List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int compType = getAccessorComponentType(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        int[] out = new int[count * 4];
        if (compType == 5121) {
            for (int i = 0; i < count * 4; i++) out[i] = buf.get() & 0xFF;
        } else if (compType == 5123) {
            for (int i = 0; i < count * 4; i++) out[i] = buf.getShort() & 0xFFFF;
        } else if (compType == 5125) {
            for (int i = 0; i < count * 4; i++) out[i] = buf.getInt();
        }
        return out;
    }

    private static int[] readIndices(int accIdx, List<Object> accessors,
                                      List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int compType = getAccessorComponentType(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        int[] out = new int[count];
        if (compType == 5121) {
            for (int i = 0; i < count; i++) out[i] = buf.get() & 0xFF;
        } else if (compType == 5123) {
            for (int i = 0; i < count; i++) out[i] = buf.getShort() & 0xFFFF;
        } else if (compType == 5125) {
            for (int i = 0; i < count; i++) out[i] = buf.getInt();
        }
        return out;
    }

    private static float[] readMat4(int accIdx, List<Object> accessors,
                                     List<Object> bufferViews, byte[] bin) {
        int count = getAccessorCount(accIdx, accessors);
        int bv = getAccessorBV(accIdx, accessors);
        int accOff = getAccessorByteOffset(accIdx, accessors);
        ByteBuffer buf = getBufferView(bv, bufferViews, bin);
        buf.position(buf.position() + accOff);

        float[] out = new float[count * 16];
        for (int i = 0; i < count * 16; i++) out[i] = buf.getFloat();
        return out;
    }
}
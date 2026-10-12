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

package vn.pmgteam.games.placeable;

import java.util.ArrayList;
import java.util.List;

import vn.pmgteam.games.main.Game;
import vn.pmgteam.games.world.AABB;

/**
 * Nhà nhỏ ở spawn — multi-AABB collision.
 * 4 tường chừa cửa + mái + ống khói. Cửa sổ/cửa ra vào đều đi qua được.
 */
public class HobbyPlace extends Placeable {

    private static final float[] WOOD       = { 0.45f, 0.30f, 0.20f, 1f };
    private static final float[] WOOD_DARK  = { 0.30f, 0.20f, 0.12f, 1f };
    private static final float[] ROOF_RED   = { 0.55f, 0.20f, 0.15f, 1f };
    private static final float[] STONE      = { 0.45f, 0.42f, 0.40f, 1f };
    private static final float[] WINDOW     = { 0.75f, 0.85f, 0.95f, 1f };
    private static final float[] SIGN_POST  = { 0.35f, 0.24f, 0.15f, 1f };
    private static final float[] SIGN_BOARD = { 0.85f, 0.70f, 0.40f, 1f };

    public HobbyPlace(float x, float y, float z) {
        super(x, y, z);
    }

    @Override
    public void render(Game game) {
        float px = position.x;
        float py = position.y;
        float pz = position.z;

        // Sàn
        game.drawBox(px, py + 0.10f, pz, 5.0f, 0.20f, 5.0f, WOOD_DARK);

        // 4 tường
        game.drawBox(px, py + 1.50f, pz - 2.40f, 5.0f, 2.80f, 0.20f, WOOD);       // sau
        game.drawBox(px - 2.40f, py + 1.50f, pz, 0.20f, 2.80f, 5.0f, WOOD);       // trái
        game.drawBox(px + 2.40f, py + 1.50f, pz, 0.20f, 2.80f, 5.0f, WOOD);       // phải
        game.drawBox(px - 1.75f, py + 1.50f, pz + 2.40f, 1.50f, 2.80f, 0.20f, WOOD); // trước trái
        game.drawBox(px + 1.75f, py + 1.50f, pz + 2.40f, 1.50f, 2.80f, 0.20f, WOOD); // trước phải

        // Cửa (visual, không có collision)
        game.drawBox(px, py + 1.00f, pz + 2.42f, 1.00f, 2.00f, 0.15f, WOOD_DARK);
        game.drawBox(px + 0.35f, py + 1.00f, pz + 2.55f, 0.08f, 0.08f, 0.08f,
                new float[]{ 0.85f, 0.75f, 0.30f, 1f });

        // Cửa sổ (visual)
        game.drawBox(px - 1.75f, py + 1.80f, pz + 2.51f, 0.80f, 0.80f, 0.05f, WINDOW);
        game.drawBox(px + 1.75f, py + 1.80f, pz + 2.51f, 0.80f, 0.80f, 0.05f, WINDOW);
        game.drawBox(px + 2.51f, py + 1.80f, pz, 0.05f, 0.80f, 0.80f, WINDOW);
        game.drawBox(px - 2.51f, py + 1.80f, pz, 0.05f, 0.80f, 0.80f, WINDOW);

        // Mái
        game.drawBox(px, py + 3.10f, pz, 5.8f, 0.30f, 5.8f, ROOF_RED);

        // Ống khói
        game.drawBox(px + 1.60f, py + 3.55f, pz - 1.60f, 0.50f, 1.00f, 0.50f, STONE);
        game.drawBox(px + 1.60f, py + 4.10f, pz - 1.60f, 0.60f, 0.15f, 0.60f, STONE);

        // Bảng gỗ
        float sx = px + 3.20f;
        float sz = pz + 1.00f;
        game.drawBox(sx, py + 0.80f, sz, 0.10f, 1.60f, 0.10f, SIGN_POST);
        game.drawBox(sx, py + 1.50f, sz + 0.15f, 1.20f, 0.50f, 0.06f, SIGN_BOARD);
        game.drawBox(sx - 0.35f, py + 1.55f, sz + 0.20f, 0.15f, 0.10f, 0.02f, WOOD_DARK);
        game.drawBox(sx - 0.10f, py + 1.55f, sz + 0.20f, 0.15f, 0.10f, 0.02f, WOOD_DARK);
        game.drawBox(sx + 0.15f, py + 1.55f, sz + 0.20f, 0.15f, 0.10f, 0.02f, WOOD_DARK);
    }

    @Override
    public void interact(Game game) {
        game.showToast("Rest house - press E again to rest");
    }

    @Override public float getRadius()  { return 2.85f; }
    @Override public float getHeight()  { return 3.25f; }
    @Override public String getType()   { return "hobby_place"; }
    @Override public String getDisplayName() { return "Rest House"; }

    /**
     * Multi-AABB: 4 tường chừa cửa + mái + ống khói.
     * Cửa ra vào nằm ở khoảng X ∈ [-1.0, +1.0] trên mặt trước (+Z) → đi qua được.
     */
    @Override
    public List<AABB> getCollisionBoxes() {
        float px = position.x;
        float py = position.y;
        float pz = position.z;

        List<AABB> list = new ArrayList<>(8);

        // === Sàn — đỉnh sàn ở py + 0.20 ===
        list.add(new AABB(px - 2.5f, py, pz - 2.5f,
                          px + 2.5f, py + 0.20f, pz + 2.5f));

        // Tường sau (−Z)
        list.add(new AABB(px - 2.5f, py + 0.10f, pz - 2.5f,
                          px + 2.5f, py + 2.9f,  pz - 2.3f));

        // Tường trái (−X)
        list.add(new AABB(px - 2.5f, py + 0.10f, pz - 2.5f,
                          px - 2.3f, py + 2.9f,  pz + 2.5f));

        // Tường phải (+X)
        list.add(new AABB(px + 2.3f, py + 0.10f, pz - 2.5f,
                          px + 2.5f, py + 2.9f,  pz + 2.5f));

        // Tường trước — 2 đoạn chừa cửa giữa
        list.add(new AABB(px - 2.5f, py + 0.10f, pz + 2.3f,
                          px - 1.0f, py + 2.9f,  pz + 2.5f));
        list.add(new AABB(px + 1.0f, py + 0.10f, pz + 2.3f,
                          px + 2.5f, py + 2.9f,  pz + 2.5f));

        // Mái
        list.add(new AABB(px - 2.9f, py + 2.95f, pz - 2.9f,
                          px + 2.9f, py + 3.25f, pz + 2.9f));

        // Ống khói
        list.add(new AABB(px + 1.30f, py + 3.05f, pz - 1.90f,
                          px + 1.90f, py + 4.175f, pz - 1.30f));

        return list;
    }
}
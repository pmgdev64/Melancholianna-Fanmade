/*
 * Melancholianna 3D - Fan-made Adventure RPG
 * 
 * Based on "Melancholianna" (メランコリアンナ) by Omochi Kobo (おもち工房)
 * Original work: https://www.omochikobo.com/
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
 */
package vn.pmgteam.games.main;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import vn.pmgteam.games.audio.AudioMaster;
import vn.pmgteam.games.audio.Sounds;
import vn.pmgteam.games.entity.Player;
import vn.pmgteam.games.gui.FontRenderer;
import vn.pmgteam.games.gui.GuiRenderer;
import vn.pmgteam.games.gui.GuiScreen;
import vn.pmgteam.games.gui.MainMenuScreen;
import vn.pmgteam.games.model.Animation;
import vn.pmgteam.games.model.GltfLoader;
import vn.pmgteam.games.model.Model;
import vn.pmgteam.games.model.ModelRenderer;
import vn.pmgteam.games.placeable.Banner;
import vn.pmgteam.games.placeable.HobbyPlace;
import vn.pmgteam.games.placeable.Placeable;
import vn.pmgteam.games.shader.ShaderProgram;
import vn.pmgteam.games.world.Camera3D;
import vn.pmgteam.games.world.CloudNoise;
import vn.pmgteam.games.world.GrassTexture;
import vn.pmgteam.games.world.Petal;
import vn.pmgteam.games.world.PetalTexture;
import vn.pmgteam.games.world.ShadowMap;
import vn.pmgteam.games.world.Sky;
import vn.pmgteam.games.world.Snow;
import vn.pmgteam.games.world.Terrain;
import vn.pmgteam.games.world.TerrainTextures;

public class Game {

    private long window;
    private int width  = 1280;
    private int height = 720;
    private boolean running = false;

    private ShaderProgram shader;
    private ShaderProgram shadowShader;
    private ShaderProgram skyShader;
    private ShaderProgram grassShader;
    private ShaderProgram grassShadowShader;
    private ShaderProgram starShader;
    private ShaderProgram cloudShader;
    private ShaderProgram snowShader;
    private ShaderProgram petalShader;

    private FontRenderer font;
    private GuiRenderer  gui;

    private int grassTex;
    private int cloudNoiseTex;
    private int petalTex;
    private TerrainTextures terrainTextures;

    private ShadowMap shadowMap;
    private Sky sky;
    private Snow snow;
    private Petal petal;
    private Camera3D camera;
    private Terrain terrain;
    private Player player;

    private final List<Placeable> placeables = new ArrayList<>();
    private HobbyPlace hobbyPlace;

    private Model         playerModel;
    private ModelRenderer modelRenderer;

    private int cubeVao, cubeVbo;

    private final Matrix4f modelMatrix = new Matrix4f();
    private final Vector3f eyePos = new Vector3f();

    private boolean firstMouse = true;
    private double lastMouseX, lastMouseY;
    private double mouseX, mouseY;

    private float timeOfDay = 0.28f;
    private float dayLengthSeconds = 120f;
    private boolean fastTime = false;

    private final Vector3f sunDirection = new Vector3f();
    private final float[] skyColor = new float[3];
    private final float[] cloudColor = new float[3];
    private final float[] cloudShadowColor = new float[3];
    private float ambient = 0.35f;
    private float nightAmount = 0f;

    private final float[] sunCore = { 1.0f, 0.95f, 0.70f };
    private final float[] sunGlow = { 1.0f, 0.80f, 0.35f };

    private String toastText = null;
    private float  toastTimer = 0f;

    private boolean showDebug = true;

    // ==================== SETTINGS — GRAPHICS ====================
    public boolean gfxFullscreen    = false;
    public boolean gfxVSync         = true;
    public int     gfxViewDistance  = 6;
    public float   gfxGrassDistance = 150f;
    public boolean gfxShadows       = true;

    // Advanced (runtime apply được)
    public float   gfxFOV             = 65f;    // 45..100
    public float   gfxCloudDensity    = 1.0f;   // 0.0..1.5
    public float   gfxParticleOpacity = 1.0f;   // 0.3..1.5
    public boolean gfxStars           = true;

    // Restart-required (chỉ hiển thị, không apply runtime)
    public int     gfxMSAA            = 4;      // 0, 2, 4, 8
    public int     gfxShadowRes       = 2048;   // 1024, 2048, 4096

    // ==================== SETTINGS — GAMEPLAY ====================
    public boolean gpSnowEnabled      = true;
    public boolean gpPetalEnabled     = true;
    public float   gpMouseSensitivity = 0.12f;

    // === Screen system ===
    private GuiScreen currentScreen = null;

    // === Menu camera ===
    private static final float MENU_CAM_HEIGHT = 4.0f;
    private static final float MENU_CAM_PITCH  = 8.0f;
    private static final float MENU_CAM_YAW    = -90f;

    // === Menu time ===
    private static final float MENU_DAY_LENGTH = 120f;
    private static final float MENU_TIME_START = 0.62f;
    private float menuTime = MENU_TIME_START;

    private static final float SHADOW_CULL_RADIUS =
            ShadowMap.ORTHO_SIZE + Terrain.CHUNK_SIZE * 0.75f;

    private static final boolean GRASS_CASTS_SHADOW = false;
    private static final float GRASS_SHADOW_RADIUS = 80f;

    private static final float STAR_DISTANCE  = 700f;
    private static final float CLOUD_DISTANCE = 500f;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Không init được GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, gfxMSAA);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, "Melancholianna 3D", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Không tạo được window");

        // ==================== KEY ====================
        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (action != GLFW_PRESS) return;

            if (currentScreen != null) {
                currentScreen.keyPress(key, action);
                return;
            }

            if (key == GLFW_KEY_ESCAPE) {
                openScreen(new MainMenuScreen());
                return;
            }
            if (key == GLFW_KEY_F && player != null) player.toggleFly();
            if (key == GLFW_KEY_T) fastTime = !fastTime;
            if (key == GLFW_KEY_B) placeBanner();
            if (key == GLFW_KEY_E) interact();
            if (key == GLFW_KEY_R) removeNearestPlaceable();
            if (key == GLFW_KEY_U) unstickFromPlaceable();
            if (key == GLFW_KEY_F3) showDebug = !showDebug;
            if (key == GLFW_KEY_N) gpSnowEnabled  = !gpSnowEnabled;
            if (key == GLFW_KEY_M) gpPetalEnabled = !gpPetalEnabled;
        });

        // ==================== MOUSE BUTTON ====================
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (currentScreen == null) return;

            if (action == GLFW_PRESS) {
                currentScreen.mouseClick(mouseX, mouseY, button);
            } else if (action == GLFW_RELEASE) {
                currentScreen.mouseRelease(mouseX, mouseY, button);
            }
        });

        // ==================== FRAMEBUFFER SIZE ====================
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                width  = w;
                height = h;
                glViewport(0, 0, w, h);
                if (camera != null) camera.aspect = (float) w / h;
                if (currentScreen != null) currentScreen.onResize(w, h);
            }
        });

        // ==================== SCROLL ====================
        glfwSetScrollCallback(window, (win, dx, dy) -> {
            if (currentScreen != null) {
                currentScreen.onScroll(dy);
                return;
            }
            camera.distance -= (float) dy * 0.5f;
            camera.distance = Math.max(2.5f, Math.min(20f, camera.distance));
        });

        // ==================== CURSOR POS ====================
        glfwSetCursorPosCallback(window, (win, mx, my) -> {
            mouseX = mx;
            mouseY = my;

            if (currentScreen != null) return;

            if (firstMouse) {
                lastMouseX = mx; lastMouseY = my;
                firstMouse = false;
                return;
            }
            float dx = (float) (mx - lastMouseX);
            float dy = (float) (my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;

            camera.yaw   += dx * gpMouseSensitivity;
            camera.pitch -= dy * gpMouseSensitivity;
            if (camera.pitch >  89) camera.pitch =  89;
            if (camera.pitch < -89) camera.pitch = -89;
        });

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null)
            glfwSetWindowPos(window, (vid.width() - width) / 2, (vid.height() - height) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(gfxVSync ? 1 : 0);
        GL.createCapabilities();

        glEnable(GL_MULTISAMPLE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        glEnable(GL_PROGRAM_POINT_SIZE);

        setupShader();
        setupShadow();
        setupSky();
        setupGrass();
        setupCube();
        setupCamera();
        applyFOV();
        setupTerrain();
        applyViewDistance();
        applyGrassDistance();
        setupPlayer();
        setupGui();
        setupStructures();
        setupModel();
        setupSnow();
        setupPetals();

        // ==================== AUDIO ====================
        AudioMaster.init();
        Sounds.loadAll();

        updateTime(0);

        glfwShowWindow(window);
        running = true;

        openScreen(new MainMenuScreen());
    }

    // ==================== SCREEN API ====================

    public void openScreen(GuiScreen screen) {
        if (currentScreen != null) currentScreen.onClose();

        currentScreen = screen;

        if (screen != null) {
            screen.init(this, width, height);
            screen.onOpen();

            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            if (glfwRawMouseMotionSupported())
                glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
            firstMouse = true;
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            if (glfwRawMouseMotionSupported())
                glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            firstMouse = true;
        }

        if (AudioMaster.isInitialized()) {
            if (screen != null) {
                AudioMaster.getMusic().play(Sounds.MUSIC_MENU, 1.5f, 1.0f);
            } else {
                AudioMaster.getMusic().play(Sounds.MUSIC_GAME, 2.0f, 1.5f);
            }
        }
    }

    public void startGame() {
        openScreen(null);
    }

    public void quit() {
        running = false;
    }

    // ==================== SETTINGS APPLY ====================

    public void applyVSync() {
        glfwSwapInterval(gfxVSync ? 1 : 0);
    }

    public void applyFullscreen() {
        long monitor = glfwGetPrimaryMonitor();
        if (monitor == NULL) return;
        GLFWVidMode mode = glfwGetVideoMode(monitor);
        if (mode == null) return;

        if (gfxFullscreen) {
            glfwSetWindowMonitor(window, monitor,
                    0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else {
            int ww = 1280, wh = 720;
            int px = (mode.width()  - ww) / 2;
            int py = (mode.height() - wh) / 2;
            glfwSetWindowMonitor(window, NULL, px, py, ww, wh, 0);
        }
    }

    public void applyViewDistance() {
        if (terrain != null) terrain.setViewRadius(gfxViewDistance);
    }

    public void applyGrassDistance() {
        if (terrain != null) terrain.setGrassDrawDistance(gfxGrassDistance);
    }

    public void applyFOV() {
        if (camera != null) {
            camera.baseFov = Math.max(45f, Math.min(100f, gfxFOV));
        }
    }

    // ==================== SETUP ====================

    private void setupShader() {
        shader = new ShaderProgram();
        shader.createVertexShader(loadShaderFile("vert.glsl"));
        shader.createFragmentShader(loadShaderFile("frag.glsl"));
        shader.link();
    }

    private void setupShadow() {
        shadowShader = new ShaderProgram();
        shadowShader.createVertexShader(loadShaderFile("shadow/shadow_vert.glsl"));
        shadowShader.createFragmentShader(loadShaderFile("shadow/shadow_frag.glsl"));
        shadowShader.link();
        shadowMap = new ShadowMap();
    }

    private void setupSky() {
        skyShader = new ShaderProgram();
        skyShader.createVertexShader(loadShaderFile("sky/sky_vert.glsl"));
        skyShader.createFragmentShader(loadShaderFile("sky/sky_frag.glsl"));
        skyShader.link();

        starShader = new ShaderProgram();
        starShader.createVertexShader(loadShaderFile("sky/star_vert.glsl"));
        starShader.createFragmentShader(loadShaderFile("sky/star_frag.glsl"));
        starShader.link();

        cloudShader = new ShaderProgram();
        cloudShader.createVertexShader(loadShaderFile("sky/cloud_vert.glsl"));
        cloudShader.createFragmentShader(loadShaderFile("sky/cloud_frag.glsl"));
        cloudShader.link();

        sky = new Sky();
        cloudNoiseTex = CloudNoise.create(314159L);
    }

    private void setupGrass() {
        grassShader = new ShaderProgram();
        grassShader.createVertexShader(loadShaderFile("grass/grass_vert.glsl"));
        grassShader.createFragmentShader(loadShaderFile("grass/grass_frag.glsl"));
        grassShader.link();

        grassShadowShader = new ShaderProgram();
        grassShadowShader.createVertexShader(loadShaderFile("grass/grass_shadow_vert.glsl"));
        grassShadowShader.createFragmentShader(loadShaderFile("grass/grass_shadow_frag.glsl"));
        grassShadowShader.link();

        grassTex = GrassTexture.create();
    }

    private void setupSnow() {
        snowShader = new ShaderProgram();
        snowShader.createVertexShader(loadShaderFile("snow/snow_vert.glsl"));
        snowShader.createFragmentShader(loadShaderFile("snow/snow_frag.glsl"));
        snowShader.link();
        snow = new Snow();
        System.out.println("Snow system ready: 2500 flakes");
    }

    private void setupPetals() {
        petalShader = new ShaderProgram();
        petalShader.createVertexShader(loadShaderFile("petal/petal_vert.glsl"));
        petalShader.createFragmentShader(loadShaderFile("petal/petal_frag.glsl"));
        petalShader.link();
        petalTex = PetalTexture.create();
        petal = new Petal();
        System.out.println("Petal system ready: 1200 petals");
    }

    private void setupGui() {
        font = new FontRenderer();
        gui = new GuiRenderer(font, font.getAtlasSize(),
                loadShaderFile("gui/gui_vert.glsl"),
                loadShaderFile("gui/gui_frag.glsl"));
    }

    private void setupStructures() {
        float houseX = 7f;
        float houseZ = -3f;
        float houseY = terrain.heightAt(houseX, houseZ);
        hobbyPlace = new HobbyPlace(houseX, houseY, houseZ);
        placeables.add(hobbyPlace);
        System.out.printf("Placed rest house at (%.1f, %.1f, %.1f)%n",
                houseX, houseY, houseZ);
    }

    private void setupModel() {
        String path = "assets/models/X_Bot.glb";
        System.out.println("Loading model from classpath: " + path);

        try {
            playerModel = GltfLoader.loadResource(path);
            playerModel.upload();

            modelRenderer = new ModelRenderer(
                    loadShaderFile("model/model_vert.glsl"),
                    loadShaderFile("model/model_frag.glsl"));

            System.out.println("Model ready: " + playerModel.meshes.size() + " meshes");
            System.out.println("Animations: " + playerModel.animations.size());
            for (Animation a : playerModel.animations) {
                System.out.println("  anim: '" + a.name + "' " + a.duration + "s");
            }

            if (player != null) {
                player.setModel(playerModel);
                System.out.println("Player model set, currentAnim="
                                 + player.getCurrentAnim());
            }
        } catch (Exception e) {
            System.err.println("Không load được model: " + e.getMessage());
            e.printStackTrace();
            playerModel = null;
            modelRenderer = null;
        }
    }

    private void setupCube() {
        float[] v = {
            // +X
             0.5f,-0.5f,-0.5f,  1,0,0,  1,1,1,
             0.5f, 0.5f,-0.5f,  1,0,0,  1,1,1,
             0.5f, 0.5f, 0.5f,  1,0,0,  1,1,1,
             0.5f,-0.5f,-0.5f,  1,0,0,  1,1,1,
             0.5f, 0.5f, 0.5f,  1,0,0,  1,1,1,
             0.5f,-0.5f, 0.5f,  1,0,0,  1,1,1,
            // -X
            -0.5f,-0.5f, 0.5f, -1,0,0,  1,1,1,
            -0.5f, 0.5f, 0.5f, -1,0,0,  1,1,1,
            -0.5f, 0.5f,-0.5f, -1,0,0,  1,1,1,
            -0.5f,-0.5f, 0.5f, -1,0,0,  1,1,1,
            -0.5f, 0.5f,-0.5f, -1,0,0,  1,1,1,
            -0.5f,-0.5f,-0.5f, -1,0,0,  1,1,1,
            // +Y
            -0.5f, 0.5f,-0.5f,  0,1,0,  1,1,1,
            -0.5f, 0.5f, 0.5f,  0,1,0,  1,1,1,
             0.5f, 0.5f, 0.5f,  0,1,0,  1,1,1,
            -0.5f, 0.5f,-0.5f,  0,1,0,  1,1,1,
             0.5f, 0.5f, 0.5f,  0,1,0,  1,1,1,
             0.5f, 0.5f,-0.5f,  0,1,0,  1,1,1,
            // -Y
            -0.5f,-0.5f, 0.5f,  0,-1,0, 1,1,1,
            -0.5f,-0.5f,-0.5f,  0,-1,0, 1,1,1,
             0.5f,-0.5f,-0.5f,  0,-1,0, 1,1,1,
            -0.5f,-0.5f, 0.5f,  0,-1,0, 1,1,1,
             0.5f,-0.5f,-0.5f,  0,-1,0, 1,1,1,
             0.5f,-0.5f, 0.5f,  0,-1,0, 1,1,1,
            // +Z
            -0.5f,-0.5f, 0.5f,  0,0,1,  1,1,1,
             0.5f,-0.5f, 0.5f,  0,0,1,  1,1,1,
             0.5f, 0.5f, 0.5f,  0,0,1,  1,1,1,
            -0.5f,-0.5f, 0.5f,  0,0,1,  1,1,1,
             0.5f, 0.5f, 0.5f,  0,0,1,  1,1,1,
            -0.5f, 0.5f, 0.5f,  0,0,1,  1,1,1,
            // -Z
             0.5f,-0.5f,-0.5f,  0,0,-1, 1,1,1,
            -0.5f,-0.5f,-0.5f,  0,0,-1, 1,1,1,
            -0.5f, 0.5f,-0.5f,  0,0,-1, 1,1,1,
             0.5f,-0.5f,-0.5f,  0,0,-1, 1,1,1,
            -0.5f, 0.5f,-0.5f,  0,0,-1, 1,1,1,
             0.5f, 0.5f,-0.5f,  0,0,-1, 1,1,1,
        };

        cubeVao = glGenVertexArrays();
        glBindVertexArray(cubeVao);
        cubeVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);

        FloatBuffer fb = MemoryUtil.memAllocFloat(v.length);
        try {
            fb.put(v).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(fb);
        }

        int stride = 9 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
        glVertexAttrib2f(3, 0f, 0f);
        glVertexAttrib4f(4, 1f, 0f, 0f, 0f);

        glBindVertexArray(0);
    }

    private void setupCamera() {
        camera = new Camera3D();
        camera.aspect = (float) width / height;
        camera.yaw   = 0f;
        camera.pitch = -15f;
        camera.distance = 5f;
    }

    private void setupTerrain() {
        terrain = new Terrain(12345L);
        terrain.update(0, 0);
        terrainTextures = new TerrainTextures(12345L);
        System.out.println("Load ban đầu: " + terrain.getLoadedChunks() + " chunk");
    }

    private void setupPlayer() {
        player = new Player(0, 0, terrain);
        player.setPlaceables(placeables);
    }

    // ==================== ACTIONS ====================

    public void showToast(String text) {
        toastText  = text;
        toastTimer = 3.0f;
    }

    private void placeBanner() {
        Vector3f fwd = camera.getForward();
        Vector3f horiz = new Vector3f(fwd.x, 0f, fwd.z);
        if (horiz.lengthSquared() < 1e-4f) horiz.set(0, 0, -1);
        horiz.normalize();

        float offset = 2.5f;
        float px = player.position.x + horiz.x * offset;
        float pz = player.position.z + horiz.z * offset;
        float py = terrain.heightAt(px, pz);

        Banner b = new Banner(px, py, pz);
        if (overlapsPlayer(b)) {
            showToast("Too close to player");
            return;
        }
        placeables.add(b);
        System.out.printf("Placed banner at (%.1f, %.1f, %.1f)%n", px, py, pz);
    }

    private boolean overlapsPlayer(Placeable q) {
        float pl = player.position.x - player.width * 0.5f;
        float pr = player.position.x + player.width * 0.5f;
        float pb = player.position.y;
        float pt = player.position.y + player.height;
        float pd = player.position.z - player.depth * 0.5f;
        float pu = player.position.z + player.depth * 0.5f;

        float r  = q.getRadius();
        float ql = q.position.x - r, qr = q.position.x + r;
        float qb = q.position.y, qt = q.position.y + q.getHeight();
        float qd = q.position.z - r, qu = q.position.z + r;

        return pl < qr && pr > ql && pb < qt && pt > qb && pd < qu && pu > qd;
    }

    private void interact() {
        Placeable target = findNearest(player.position, 4f);
        if (target != null) target.interact(this);
        else showToast("Nothing to interact with");
    }

    private void removeNearestPlaceable() {
        Placeable target = findNearest(player.position, 4f);
        if (target != null) {
            placeables.remove(target);
            showToast("Removed: " + target.getDisplayName());
        }
    }

    private void unstickFromPlaceable() {
        Placeable target = findNearest(player.position, 3f);
        if (target == null) return;
        float top = target.position.y + target.getHeight();
        player.position.y = top + 0.05f;
        player.velocity.y = 0;
    }

    private Placeable findNearest(Vector3f pos, float maxDist) {
        Placeable best = null;
        float bestD2 = maxDist * maxDist;
        for (Placeable p : placeables) {
            float d2 = p.position.distanceSquared(pos);
            if (d2 < bestD2) { bestD2 = d2; best = p; }
        }
        return best;
    }

    // ==================== LOOP ====================

    private void loop() {
        double last = glfwGetTime();
        int frames = 0;
        double fpsT = 0;

        while (running && !glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            if (dt > 0.1f) dt = 0.1f;
            last = now;

            update(dt);
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++; fpsT += dt;
            if (fpsT >= 1.0) {
                String state = currentScreen != null ? "MENU" :
                        (player.flying ? "FLY" : (player.onGround ? "GROUND" : "AIR"));
                System.out.printf("FPS: %d | Pos: %.1f, %.1f, %.1f | %s | Dist: %.1f%n",
                        frames,
                        player.position.x, player.position.y, player.position.z,
                        state, camera.distance);
                frames = 0; fpsT = 0;
            }
        }
    }

    private void update(float dt) {
        // ==================== TIME OF DAY ====================
        if (currentScreen != null) {
            menuTime += dt / MENU_DAY_LENGTH;
            menuTime = menuTime - (float) Math.floor(menuTime);
            timeOfDay = menuTime;
        } else {
            float speed = fastTime ? 10f : 1f;
            timeOfDay += (dt / dayLengthSeconds) * speed;
            timeOfDay = timeOfDay - (float) Math.floor(timeOfDay);
        }
        updateTime(dt);

        // ==================== AUDIO ====================
        AudioMaster.setListener(camera);
        AudioMaster.update(dt);

        // ==================== CAMERA / LOGIC ====================
        if (currentScreen != null) {
            updateMenuCamera(dt);
            currentScreen.update(dt, mouseX, mouseY);
        } else {
            player.update(dt, window, terrain, camera);
            terrain.update(player.position.x, player.position.z);

            player.getEyePosition(eyePos);
            camera.updateThirdPerson(eyePos, terrain, placeables, dt);
        }

        for (Placeable p : placeables) p.update(dt);

        float t = (float) glfwGetTime();
        if (snow != null && gpSnowEnabled) {
            snow.update(dt, camera.position.x, camera.position.y, camera.position.z, t);
        }
        if (petal != null && gpPetalEnabled && currentScreen == null) {
            petal.update(dt, camera.position.x, camera.position.y, camera.position.z, t);
        }

        if (toastTimer > 0) {
            toastTimer -= dt;
            if (toastTimer <= 0) toastText = null;
        }
    }

    private void updateMenuCamera(float dt) {
        float groundY = terrain.heightAt(0f, 0f);
        camera.position.set(0f, groundY + MENU_CAM_HEIGHT, 0f);

        camera.yaw      = MENU_CAM_YAW;
        camera.pitch    = MENU_CAM_PITCH;
        camera.distance = 5f;
        camera.fov      = camera.baseFov;

        terrain.update(camera.position.x, camera.position.z);
    }

    private void updateTime(float dt) {
        float angle = (timeOfDay - 0.25f) * (float)(Math.PI * 2.0);
        float sx = (float)  Math.cos(angle);
        float sy = (float)  Math.sin(angle);
        float sz = 0.25f;

        sunDirection.set(sx, sy, sz).normalize();
        if (shadowMap != null) shadowMap.setSunDirection(sunDirection);

        ambient = 0.35f + Math.max(sunDirection.y, 0f) * 0.35f;
        ambient = Math.max(ambient, 0.38f);

        float sunY = sunDirection.y;
        float dayFactor    = clamp((sunY + 0.05f) / 0.35f, 0f, 1f);
        float sunsetFactor = (float) Math.exp(-sunY * sunY * 15f);

        nightAmount = clamp((-0.05f - sunY) / 0.20f, 0f, 1f);

        float nr = 0.03f, ng = 0.04f, nb = 0.10f;
        float dr = 0.55f, dg = 0.72f, db = 0.92f;
        float sr = 1.00f, sg = 0.45f, sb = 0.25f;

        float r = lerp(nr, dr, dayFactor);
        float g = lerp(ng, dg, dayFactor);
        float b = lerp(nb, db, dayFactor);

        float sf = sunsetFactor * 0.85f;
        r = lerp(r, sr, sf);
        g = lerp(g, sg, sf);
        b = lerp(b, sb, sf);

        skyColor[0] = r;
        skyColor[1] = g;
        skyColor[2] = b;
        glClearColor(r, g, b, 1f);

        sunCore[0] = 1.00f;
        sunCore[1] = lerp(0.95f, 0.45f, sunsetFactor);
        sunCore[2] = lerp(0.70f, 0.20f, sunsetFactor);
        sunGlow[0] = 1.00f;
        sunGlow[1] = lerp(0.80f, 0.40f, sunsetFactor);
        sunGlow[2] = lerp(0.35f, 0.15f, sunsetFactor);

        float cloudBright = 0.30f + 0.65f * dayFactor;
        float cr = cloudBright;
        float cg = cloudBright;
        float cb = cloudBright * 1.02f;

        float sf2 = sunsetFactor * 0.7f;
        cr = lerp(cr, 1.00f, sf2);
        cg = lerp(cg, 0.50f, sf2);
        cb = lerp(cb, 0.35f, sf2);

        cloudColor[0] = cr;
        cloudColor[1] = cg;
        cloudColor[2] = cb;

        cloudShadowColor[0] = cr * 0.55f;
        cloudShadowColor[1] = cg * 0.55f;
        cloudShadowColor[2] = cb * 0.62f;
    }

    // ==================== RENDER ====================

    private void render() {
        boolean inMenu = (currentScreen != null);

        renderShadowPass();
        renderSkyPass();
        renderWorldPass(inMenu);
        renderParticlesPass();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (inMenu) {
            gui.begin(width, height);
            currentScreen.render(gui, font);
            gui.end();
        } else {
            renderHud();
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderShadowPass() {
        shadowMap.update(camera.position);
        shadowMap.begin();

        if (gfxShadows) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(2.0f, 4.0f);

            shadowShader.bind();
            shadowShader.setMat4("uLightSpace", shadowMap.getLightSpaceMatrix());

            float cullX = currentScreen != null ? camera.position.x : player.position.x;
            float cullZ = currentScreen != null ? camera.position.z : player.position.z;
            terrain.renderShadowPass(cullX, cullZ, SHADOW_CULL_RADIUS);
            shadowShader.unbind();

            if (GRASS_CASTS_SHADOW) {
                glActiveTexture(GL_TEXTURE1);
                glBindTexture(GL_TEXTURE_2D, grassTex);
                glActiveTexture(GL_TEXTURE0);

                grassShadowShader.bind();
                grassShadowShader.setMat4("uLightSpace", shadowMap.getLightSpaceMatrix());
                grassShadowShader.setInt("uGrassTex", 1);

                glDisable(GL_CULL_FACE);
                terrain.renderGrassShadow(cullX, cullZ, GRASS_SHADOW_RADIUS);
                glEnable(GL_CULL_FACE);

                grassShadowShader.unbind();
            }

            glDisable(GL_POLYGON_OFFSET_FILL);
        }

        shadowMap.end(width, height);
    }

    private void renderSkyPass() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        skyShader.bind();
        skyShader.setMat4("uProjection", camera.getProjectionMatrix());
        skyShader.setMat4("uView",       camera.getViewMatrix());
        sky.renderSun(skyShader, camera, sunDirection,
                sunCore[0], sunCore[1], sunCore[2],
                sunGlow[0], sunGlow[1], sunGlow[2]);
        sky.renderMoon(skyShader, camera, sunDirection);
        skyShader.unbind();

        if (nightAmount > 0.01f && gfxStars) {
            starShader.bind();
            starShader.setMat4("uProjection",  camera.getProjectionMatrix());
            starShader.setMat4("uView",        camera.getViewMatrix());
            starShader.setVec3("uCameraPos",   camera.position);
            starShader.setFloat("uDistance",   STAR_DISTANCE);
            starShader.setFloat("uNightAmount", nightAmount);
            starShader.setVec3("uStarColor",   0.95f, 0.95f, 1.05f);
            sky.renderStars();
            starShader.unbind();
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, cloudNoiseTex);

        cloudShader.bind();
        cloudShader.setMat4("uProjection",  camera.getProjectionMatrix());
        cloudShader.setMat4("uView",        camera.getViewMatrix());
        cloudShader.setVec3("uCameraPos",   camera.position);
        cloudShader.setFloat("uDistance",   CLOUD_DISTANCE);
        cloudShader.setFloat("uTime",       (float) glfwGetTime());
        cloudShader.setVec3("uCloudColor",       cloudColor[0], cloudColor[1], cloudColor[2]);
        cloudShader.setVec3("uCloudShadowColor", cloudShadowColor[0], cloudShadowColor[1], cloudShadowColor[2]);
        cloudShader.setFloat("uCloudAmount", gfxCloudDensity);
        cloudShader.setInt("uNoiseTex", 0);
        sky.renderClouds();
        cloudShader.unbind();

        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderWorldPass(boolean inMenu) {
        shader.bind();
        shader.setMat4("uProjection",   camera.getProjectionMatrix());
        shader.setMat4("uView",         camera.getViewMatrix());
        shader.setMat4("uLightSpace",   shadowMap.getLightSpaceMatrix());
        shader.setVec3("uCameraPos",    camera.position);
        shader.setVec3("uSunDirection", sunDirection);
        shader.setVec3("uSkyColor",     skyColor[0], skyColor[1], skyColor[2]);
        shader.setVec3("uSunColor",     sunCore[0], sunCore[1], sunCore[2]);
        shader.setFloat("uAmbient",     ambient);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthTexture());
        shader.setInt("uShadowMap", 0);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, terrainTextures.grass);
        shader.setInt("uTexGrass", 2);

        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, terrainTextures.rock);
        shader.setInt("uTexRock", 3);

        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, terrainTextures.sand);
        shader.setInt("uTexSand", 4);

        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, terrainTextures.snow);
        shader.setInt("uTexSnow", 5);

        glActiveTexture(GL_TEXTURE0);

        modelMatrix.identity();
        shader.setMat4("uModel", modelMatrix);
        shader.setFloat("uUseOverride", 0f);
        shader.setVec3("uColorOverride", -1f, -1f, -1f);
        terrain.render(camera);

        for (Placeable p : placeables) p.render(this);

        if (!inMenu) player.render(this);

        shader.unbind();

        // ===== Grass =====
        grassShader.bind();
        grassShader.setMat4("uProjection",   camera.getProjectionMatrix());
        grassShader.setMat4("uView",         camera.getViewMatrix());
        grassShader.setMat4("uLightSpace",   shadowMap.getLightSpaceMatrix());
        grassShader.setVec3("uCameraPos",    camera.position);
        grassShader.setVec3("uSunDirection", sunDirection);
        grassShader.setVec3("uSkyColor",     skyColor[0], skyColor[1], skyColor[2]);
        grassShader.setVec3("uSunColor",     sunCore[0], sunCore[1], sunCore[2]);
        grassShader.setFloat("uAmbient",     ambient);
        grassShader.setFloat("uTime",        (float) glfwGetTime());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthTexture());
        grassShader.setInt("uShadowMap", 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, grassTex);
        grassShader.setInt("uGrassTex", 1);
        glActiveTexture(GL_TEXTURE0);

        glDisable(GL_CULL_FACE);
        terrain.renderGrass(camera);
        glEnable(GL_CULL_FACE);

        grassShader.unbind();

        if (!inMenu) {
            renderModels();
        }
    }

    private void renderParticlesPass() {
        renderSnow();
        renderPetals();
    }

    private void renderModels() {
        if (modelRenderer == null) return;

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        modelRenderer.begin(
                camera.getProjectionMatrix(),
                camera.getViewMatrix(),
                shadowMap.getLightSpaceMatrix(),
                camera.position,
                sunDirection,
                skyColor,
                sunCore,
                ambient,
                shadowMap.getDepthTexture());

        if (player != null) {
            player.renderModel(modelRenderer);
        }

        modelRenderer.end();
        glEnable(GL_CULL_FACE);
    }

    private void renderSnow() {
        if (snow == null || snowShader == null || !gpSnowEnabled) return;

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        snowShader.bind();
        snowShader.setMat4("uProjection", camera.getProjectionMatrix());
        snowShader.setMat4("uView",       camera.getViewMatrix());
        snowShader.setFloat("uOpacity",   0.85f * gfxParticleOpacity);

        snow.render(snowShader);
        snowShader.unbind();

        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
    }

    private void renderPetals() {
        if (petal == null || petalShader == null || !gpPetalEnabled) return;
        if (currentScreen != null) return;

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, petalTex);

        float dim = 0.4f + ambient * 0.6f * (1f - nightAmount * 0.5f);

        petalShader.bind();
        petalShader.setMat4("uProjection", camera.getProjectionMatrix());
        petalShader.setMat4("uView",       camera.getViewMatrix());
        petalShader.setVec3("uPetalColor", dim, dim * 0.90f, dim * 0.95f);
        petalShader.setFloat("uOpacity",
                (0.5f + ambient * 0.5f) * gfxParticleOpacity);
        petalShader.setInt("uPetalTex",    0);

        petal.render(petalShader, (float) glfwGetTime());
        petalShader.unbind();

        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
    }

    private void renderHud() {
        gui.begin(width, height);

        float hpX = 10, hpY = 10, hpW = 240, hpH = 210;
        gui.rect(hpX, hpY, hpW, hpH, 0.06f, 0.06f, 0.09f, 0.72f);
        gui.rectOutline(hpX, hpY, hpW, hpH, 1.5f, 0.85f, 0.75f, 0.35f, 0.85f);

        float fs = 0.55f;
        float cText = 0.92f;
        float goldR = 1.0f, goldG = 0.85f, goldB = 0.40f;

        gui.text("WASD",    22,  24, fs, goldR, goldG, goldB, 1);
        gui.text("Move",   100,  24, fs, cText, cText, cText, 1);

        gui.text("Space",   22,  50, fs, goldR, goldG, goldB, 1);
        gui.text("Jump",   100,  50, fs, cText, cText, cText, 1);

        gui.text("F",       22,  76, fs, goldR, goldG, goldB, 1);
        gui.text("Fly",    100,  76, fs, cText, cText, cText, 1);

        gui.text("E",       22, 102, fs, goldR, goldG, goldB, 1);
        gui.text("Interact", 100, 102, fs, cText, cText, cText, 1);

        gui.text("T",       22, 128, fs, goldR, goldG, goldB, 1);
        gui.text("Time",   100, 128, fs, cText, cText, cText, 1);

        gui.text("N",       22, 154, fs, goldR, goldG, goldB, 1);
        gui.text("Snow",   100, 154, fs, cText, cText, cText, 1);

        gui.text("M",       22, 180, fs, goldR, goldG, goldB, 1);
        gui.text("Petal",  100, 180, fs, cText, cText, cText, 1);

        if (showDebug) {
            renderDebugPanel(fs);
        }

        renderWaypoint();

        if (toastText != null) {
            float ts = 0.65f;
            float textW = font.measureWidth(toastText, ts);
            float padX = 60;
            float tw = textW + padX * 2;
            float th = 52;
            float tx = (width - tw) * 0.5f;
            float ty = height - 120;

            gui.rect(tx, ty, tw, th, 0.06f, 0.06f, 0.09f, 0.85f);
            gui.rectOutline(tx, ty, tw, th, 2.0f, 0.95f, 0.80f, 0.40f, 0.95f);

            float textY = ty + (th - font.getLineHeight() * ts) * 0.5f - 2;
            gui.textCentered(toastText, width * 0.5f, textY, ts,
                    0.95f, 0.90f, 0.70f, 1);
        }

        gui.end();
    }

    private void renderDebugPanel(float fs) {
        float posX = width - 260;
        float posY = 10;
        float posW = 250;
        float posH = 200;

        gui.rect(posX, posY, posW, posH, 0.06f, 0.06f, 0.09f, 0.72f);
        gui.rectOutline(posX, posY, posW, posH, 1.5f, 0.55f, 0.75f, 0.85f, 0.85f);

        gui.text("DEBUG  [F3]", posX + 12, posY + 8, fs * 0.75f,
                0.55f, 0.75f, 0.85f, 0.85f);

        gui.text("Player", posX + 12, posY + 32, fs, 0.85f, 0.90f, 0.95f, 1);
        gui.text(String.format("X %.1f", player.position.x),
                posX + 105, posY + 32, fs, 0.75f, 0.82f, 0.90f, 1);
        gui.text(String.format("Y %.1f", player.position.y),
                posX + 105, posY + 54, fs, 0.75f, 0.82f, 0.90f, 1);
        gui.text(String.format("Z %.1f", player.position.z),
                posX + 105, posY + 76, fs, 0.75f, 0.82f, 0.90f, 1);

        if (hobbyPlace != null) {
            gui.text("House", posX + 12, posY + 108, fs, 0.85f, 0.90f, 0.95f, 1);
            gui.text(String.format("X %.1f", hobbyPlace.position.x),
                    posX + 105, posY + 108, fs, 0.95f, 0.85f, 0.55f, 1);
            gui.text(String.format("Y %.1f", hobbyPlace.position.y),
                    posX + 105, posY + 130, fs, 0.95f, 0.85f, 0.55f, 1);
            gui.text(String.format("Z %.1f", hobbyPlace.position.z),
                    posX + 105, posY + 152, fs, 0.95f, 0.85f, 0.55f, 1);

            float dx = hobbyPlace.position.x - player.position.x;
            float dz = hobbyPlace.position.z - player.position.z;
            float dist = (float) Math.sqrt(dx*dx + dz*dz);
            gui.text(String.format("Dist %.1f", dist),
                    posX + 12, posY + 176, fs, 0.95f, 0.75f, 0.55f, 1);
        }
    }

    private void renderWaypoint() {
        if (hobbyPlace == null) return;

        float dx = hobbyPlace.position.x - player.position.x;
        float dz = hobbyPlace.position.z - player.position.z;
        float dist = (float) Math.sqrt(dx*dx + dz*dz);

        if (dist < 4f) return;

        Vector3f fwd = camera.getForward();
        Vector3f horizFwd = new Vector3f(fwd.x, 0, fwd.z).normalize();

        float toHouseX = dx / dist;
        float toHouseZ = dz / dist;

        float cross = horizFwd.x * toHouseZ - horizFwd.z * toHouseX;
        float dot   = horizFwd.x * toHouseX + horizFwd.z * toHouseZ;

        float angle = (float) Math.atan2(cross, dot);

        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float radius = 150f;

        float wx = cx + (float) Math.sin(angle) * radius;
        float wy = cy - (float) Math.cos(angle) * radius;

        float sz = 10f;
        gui.rect(wx - sz, wy - sz * 0.4f, sz * 2, sz * 0.8f,
                1.0f, 0.85f, 0.40f, 0.9f);
        gui.rect(wx - sz * 0.4f, wy - sz, sz * 0.8f, sz * 2,
                1.0f, 0.85f, 0.40f, 0.9f);

        String label = String.format("House %.0fm", dist);
        float lw = font.measureWidth(label, 0.5f);
        gui.rect(wx - lw * 0.5f - 8, wy + 16, lw + 16, 26,
                0.06f, 0.06f, 0.09f, 0.78f);
        gui.rectOutline(wx - lw * 0.5f - 8, wy + 16, lw + 16, 26, 1.2f,
                1.0f, 0.85f, 0.40f, 0.85f);
        gui.textCentered(label, wx, wy + 22, 0.5f, 1.0f, 0.85f, 0.40f, 1);
    }

    // ==================== PRIMITIVES ====================

    public void drawBox(float cx, float cy, float cz,
                        float w, float h, float d, float[] color) {
        modelMatrix.identity()
                   .translate(cx, cy, cz)
                   .scale(w, h, d);
        drawBoxTransformed(modelMatrix, color);
    }

    public void drawBoxRotated(float cx, float cy, float cz,
                               float w, float h, float d, float yaw, float[] color) {
        modelMatrix.identity()
                   .translate(cx, cy, cz)
                   .rotateY((float) Math.toRadians(yaw))
                   .scale(w, h, d);
        drawBoxTransformed(modelMatrix, color);
    }

    public void drawBoxTransformed(Matrix4f model, float[] color) {
        shader.setMat4("uModel", model);
        shader.setVec3("uColorOverride", color[0], color[1], color[2]);
        shader.setFloat("uUseOverride", 1f);

        glBindVertexArray(cubeVao);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private String loadShaderFile(String fileName) {
        String resource = "assets/shaders/" + fileName;
        try (java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream(resource)) {
            if (is == null) {
                java.nio.file.Path diskPath = Paths.get("src", "assets", "shaders", fileName);
                if (Files.exists(diskPath)) {
                    return Files.readString(diskPath);
                }
                throw new RuntimeException("Không tìm thấy shader: " + resource);
            }
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được shader: " + resource, e);
        }
    }

    private void cleanup() {
        Sounds.cleanup();
        AudioMaster.cleanup();

        if (shader != null) shader.cleanup();
        if (shadowShader != null) shadowShader.cleanup();
        if (skyShader != null) skyShader.cleanup();
        if (grassShader != null) grassShader.cleanup();
        if (grassShadowShader != null) grassShadowShader.cleanup();
        if (starShader != null) starShader.cleanup();
        if (cloudShader != null) cloudShader.cleanup();
        if (snowShader != null) snowShader.cleanup();
        if (petalShader != null) petalShader.cleanup();
        if (snow != null) snow.cleanup();
        if (petal != null) petal.cleanup();
        if (gui != null) gui.cleanup();
        if (font != null) font.cleanup();
        if (modelRenderer != null) modelRenderer.cleanup();
        if (playerModel != null) {
            for (var m : playerModel.meshes) m.cleanup();
        }
        if (grassTex != 0) glDeleteTextures(grassTex);
        if (cloudNoiseTex != 0) glDeleteTextures(cloudNoiseTex);
        if (petalTex != 0) glDeleteTextures(petalTex);
        if (terrainTextures != null) terrainTextures.cleanup();
        if (shadowMap != null) shadowMap.cleanup();
        if (sky != null) sky.cleanup();
        if (terrain != null) terrain.cleanup();

        glDeleteBuffers(cubeVbo);
        glDeleteVertexArrays(cubeVao);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
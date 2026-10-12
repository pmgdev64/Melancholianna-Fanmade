package vn.pmgteam.games.audio;

/**
 * Central registry cho tất cả sound effect + music.
 * File path tương đối trong classpath (src/assets/sounds/...).
 */
public final class Sounds {

    // === UI ===
    public static SoundBuffer UI_CLICK;
    public static SoundBuffer UI_HOVER;

    // === Music ===
    public static SoundBuffer MUSIC_MENU;
    public static SoundBuffer MUSIC_GAME;

    // === Ambient 3D ===
    public static SoundBuffer AMBIENT_WIND;
    public static SoundBuffer AMBIENT_BIRDS;

    // === SFX ===
    public static SoundBuffer STEP_GRASS;
    public static SoundBuffer PLACE_OBJECT;

    private Sounds() {}

    public static void loadAll() {
        UI_CLICK      = SoundLoader.loadResource("assets/sounds/ui/click.ogg");
        UI_HOVER      = SoundLoader.loadResource("assets/sounds/ui/hover.ogg");

        MUSIC_MENU    = SoundLoader.loadResource("assets/sounds/music/menu.ogg");
        MUSIC_GAME    = SoundLoader.loadResource("assets/sounds/music/game.ogg");

        AMBIENT_WIND  = SoundLoader.loadResource("assets/sounds/ambient/wind.ogg");
        AMBIENT_BIRDS = SoundLoader.loadResource("assets/sounds/ambient/birds.ogg");

        STEP_GRASS    = SoundLoader.loadResource("assets/sounds/sfx/step_grass.ogg");
        PLACE_OBJECT  = SoundLoader.loadResource("assets/sounds/sfx/place.ogg");
    }

    public static void cleanup() {
        free(UI_CLICK); free(UI_HOVER);
        free(MUSIC_MENU); free(MUSIC_GAME);
        free(AMBIENT_WIND); free(AMBIENT_BIRDS);
        free(STEP_GRASS); free(PLACE_OBJECT);
    }

    private static void free(SoundBuffer b) {
        if (b != null) b.cleanup();
    }
}
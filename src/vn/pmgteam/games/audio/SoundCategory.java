package vn.pmgteam.games.audio;

/**
 * Nhóm âm thanh — mỗi category có volume riêng, nhân với MASTER.
 */
public enum SoundCategory {
    MASTER  (1.00f),
    MUSIC   (0.55f),
    SFX     (1.00f),
    AMBIENT (0.45f),
    UI      (0.85f);

    private float volume;

    SoundCategory(float v) { this.volume = v; }

    public float getVolume() { return volume; }
    public void  setVolume(float v) {
        this.volume = Math.max(0f, Math.min(1f, v));
    }
}
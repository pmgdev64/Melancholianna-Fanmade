package vn.pmgteam.games.model;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    public String name;
    public float duration = 0f;
    public final List<Channel> channels = new ArrayList<>();

    public static class Channel {
        public int targetNode;
        public String path;   // "translation", "rotation", "scale"
        public float[] times;
        public float[] values;
    }
}
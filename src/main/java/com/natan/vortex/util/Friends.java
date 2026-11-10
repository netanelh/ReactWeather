package com.natan.vortex.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** Players combat modules never target. Saved to config/vortex-friends.json. */
public final class Friends {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vortex-friends.json");
    private static Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        try {
            if (Files.exists(FILE)) {
                Set<String> s = new Gson().fromJson(Files.readString(FILE), new TypeToken<Set<String>>() {}.getType());
                if (s != null) names.addAll(s);
            }
        } catch (Exception ignored) {}
    }

    private Friends() {}

    public static boolean is(String name) { return names.contains(name); }
    public static Set<String> all() { return names; }
    public static boolean add(String n) { boolean r = names.add(n); save(); return r; }
    public static boolean remove(String n) { boolean r = names.remove(n); save(); return r; }

    private static void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, new Gson().toJson(names)); } catch (Exception ignored) {}
    }
}

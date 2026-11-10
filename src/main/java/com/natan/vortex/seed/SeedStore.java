package com.natan.vortex.seed;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Known world seeds per server (cracked or typed in). config/vortex-seeds.json */
public final class SeedStore {
    private static Map<String, Long> seeds;
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-seeds.json"); }

    private static Map<String, Long> map() {
        if (seeds == null) {
            seeds = new HashMap<>();
            try { if (Files.exists(file())) seeds.putAll(new Gson().fromJson(Files.readString(file()), new TypeToken<Map<String, Long>>() {}.getType())); }
            catch (Exception ignored) {}
        }
        return seeds;
    }

    public static String server() {
        var s = Minecraft.getInstance().getCurrentServer();
        return s == null ? null : s.ip;
    }

    public static Long current() { String s = server(); return s == null ? null : map().get(s); }

    public static void set(Long seed) {
        String s = server();
        if (s == null) return;
        if (seed == null) map().remove(s); else map().put(s, seed);
        try { Files.writeString(file(), new Gson().toJson(map())); } catch (Exception ignored) {}
    }
}

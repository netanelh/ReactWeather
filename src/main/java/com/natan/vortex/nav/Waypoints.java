package com.natan.vortex.nav;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.natan.vortex.VortexClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named positions per server/world and dimension, saved to config/vortex-waypoints.json. */
public final class Waypoints {
    // GSON must be initialized before INSTANCE: the constructor calls load(), which uses it.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Waypoints INSTANCE = new Waypoints();
    public record Point(int x, int y, int z, String dim) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
    }

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("vortex-waypoints.json");
    private Map<String, Map<String, Point>> data = new LinkedHashMap<>();

    private Waypoints() { load(); }

    private static String world() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip;
        if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        return "unknown";
    }

    private static String dim() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "?" : mc.level.dimension().identifier().getPath();
    }

    /** Waypoints of the current world AND dimension (so "home" in the Nether doesn't replace the Overworld one). */
    public Map<String, Point> here() { return data.computeIfAbsent(world() + DIM_SEP + dim(), k -> new LinkedHashMap<>()); }

    private static final String DIM_SEP = "#dim=";

    public void set(String name, BlockPos p) {
        here().put(name.toLowerCase(), new Point(p.getX(), p.getY(), p.getZ(), dim()));
        save();
    }

    public Point get(String name) {
        Point p = here().get(name.toLowerCase());
        return p != null && p.dim().equals(dim()) ? p : null;
    }

    public boolean remove(String name) {
        boolean r = here().remove(name.toLowerCase()) != null;
        if (r) save();
        return r;
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                Map<String, Map<String, Point>> m = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, Map<String, Point>>>() {}.getType());
                if (m != null) {
                    // migrate old files (keyed per world only) to per world+dimension keys
                    Map<String, Map<String, Point>> mig = new LinkedHashMap<>();
                    m.forEach((w, pts) -> {
                        if (pts == null) return;
                        if (w.contains(DIM_SEP)) mig.computeIfAbsent(w, k -> new LinkedHashMap<>()).putAll(pts);
                        else pts.forEach((n, p) -> { if (p != null) mig.computeIfAbsent(w + DIM_SEP + (p.dim() == null ? "overworld" : p.dim()), k -> new LinkedHashMap<>()).putIfAbsent(n, p); });
                    });
                    data = mig;
                }
            }
        } catch (Exception e) {
            VortexClient.LOG.error("Failed to load waypoints", e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data));
        } catch (Exception e) {
            VortexClient.LOG.error("Failed to save waypoints", e);
        }
    }
}

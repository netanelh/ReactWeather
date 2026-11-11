package com.natan.vortex.memory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.natan.vortex.VortexClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remembers where interesting blocks are in every chunk you have ever loaded (ores, storage,
 * spawners, portals...). Lets ".find" work thousands of blocks away, in chunks that are no
 * longer loaded. Saved per server/world + dimension in config/vortex-memory/.
 */
public final class WorldMemory {
    public static final WorldMemory INSTANCE = new WorldMemory();
    private static final int MAX_PER_TYPE = 4000;
    private static final Gson GSON = new Gson();

    /** Block ids always remembered. Anything used with ".find" gets added too. */
    private final Set<String> tracked = new LinkedHashSet<>(List.of(
        "diamond_ore", "deepslate_diamond_ore", "ancient_debris", "emerald_ore", "deepslate_emerald_ore",
        "gold_ore", "deepslate_gold_ore", "iron_ore", "deepslate_iron_ore", "lapis_ore", "deepslate_lapis_ore",
        "redstone_ore", "deepslate_redstone_ore", "copper_ore", "deepslate_copper_ore", "coal_ore", "nether_gold_ore", "nether_quartz_ore",
        "chest", "trapped_chest", "barrel", "ender_chest", "shulker_box", "spawner", "trial_spawner", "vault",
        "nether_portal", "end_portal_frame", "end_portal", "beacon", "enchanting_table", "anvil", "bell", "crafting_table",
        "furnace", "blast_furnace", "smoker", "brewing_stand", "obsidian", "crying_obsidian", "budding_amethyst", "suspicious_sand", "suspicious_gravel", "blue_terracotta"));

    private String key = "";
    private Map<String, Set<Long>> data = new HashMap<>();   // block id -> packed positions
    private final Set<Long> scannedChunks = new HashSet<>();
    private final ArrayDeque<long[]> queue = new ArrayDeque<>();
    private boolean dirty;
    private int saveTimer, discoverTimer;

    private WorldMemory() {}

    public static long pack(int x, int y, int z) { return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL); }
    public static BlockPos unpack(long p) {
        int x = (int) (p >> 38), z = (int) (p << 26 >> 38), y = (int) (p << 52 >> 52);
        return new BlockPos(x, y, z);
    }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        String w = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : (mc.getSingleplayerServer() != null ? "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName() : "singleplayer");
        String d = mc.level == null ? "none" : mc.level.dimension().identifier().getPath();
        return (w + "_" + d).replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path file() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-memory").resolve(key + ".json"); }

    public void track(Block b) { if (tracked.add(id(b))) scannedChunks.clear(); } // rescan with the new type

    public static String id(Block b) { return BuiltInRegistries.BLOCK.getKey(b).getPath(); }

    /** Called every client tick. Scans a couple of chunks per tick so it never lags. */
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        String k = worldKey();
        if (!k.equals(key)) { save(); key = k; load(); scannedChunks.clear(); queue.clear(); }

        if (discoverTimer-- <= 0) {
            discoverTimer = 40;
            int view = mc.options.renderDistance().get() + 1;
            int cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
            for (int x = cx - view; x <= cx + view; x++) for (int z = cz - view; z <= cz + view; z++) {
                long ck = ((long) x << 32) | (z & 0xFFFFFFFFL);
                if (scannedChunks.contains(ck) || !mc.level.getChunkSource().hasChunk(x, z)) continue;
                scannedChunks.add(ck);
                queue.add(new long[]{x, z});
            }
        }
        for (int i = 0; i < 2 && !queue.isEmpty(); i++) {
            long[] c = queue.poll();
            if (mc.level.getChunkSource().hasChunk((int) c[0], (int) c[1])) scanChunk(mc.level.getChunk((int) c[0], (int) c[1]));
        }
        if (dirty && saveTimer-- <= 0) { saveTimer = 600; save(); }
    }

    private void scanChunk(LevelChunk chunk) {
        Minecraft mc = Minecraft.getInstance();
        int baseX = chunk.getPos().x() << 4, baseZ = chunk.getPos().z() << 4;
        int minY = mc.level.getMinY(), maxY = minY + mc.level.getHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        // forget what we knew about this chunk (blocks may have been mined)
        for (Set<Long> s : data.values()) s.removeIf(p -> { BlockPos b = unpack(p); return b.getX() >> 4 == baseX >> 4 && b.getZ() >> 4 == baseZ >> 4; });
        LevelChunkSection[] sections = chunk.getSections();
        for (int y = minY; y < maxY; y++) {
            int si = chunk.getSectionIndex(y);
            if (si < 0 || si >= sections.length || sections[si] == null || sections[si].hasOnlyAir()) { y = ((y >> 4) << 4) + 15; continue; }
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                m.set(baseX + x, y, baseZ + z);
                BlockState st = chunk.getBlockState(m);
                if (st.isAir()) continue;
                String id = id(st.getBlock());
                if (id.endsWith("shulker_box")) id = "shulker_box";
                if (!tracked.contains(id)) continue;
                Set<Long> set = data.computeIfAbsent(id, kk -> new HashSet<>());
                if (set.size() < MAX_PER_TYPE) { set.add(pack(m.getX(), m.getY(), m.getZ())); dirty = true; }
            }
        }
    }

    /** Remembered positions of these blocks, nearest first. */
    public List<BlockPos> nearest(Set<Block> blocks, BlockPos from, int max) {
        List<BlockPos> out = new ArrayList<>();
        for (Block b : blocks) {
            String id = id(b);
            if (id.endsWith("shulker_box")) id = "shulker_box";
            Set<Long> s = data.get(id);
            if (s != null) for (long p : s) out.add(unpack(p));
        }
        out.sort(Comparator.comparingDouble(p -> p.distSqr(from)));
        return out.size() > max ? out.subList(0, max) : out;
    }

    public int count() { int c = 0; for (Set<Long> s : data.values()) c += s.size(); return c; }
    public int chunksScanned() { return scannedChunks.size(); }

    public void forget(BlockPos p) {
        long k = pack(p.getX(), p.getY(), p.getZ());
        for (Set<Long> s : data.values()) if (s.remove(k)) dirty = true;
    }

    private void load() {
        data = new HashMap<>();
        try {
            if (Files.exists(file())) {
                Map<String, Set<Long>> m = GSON.fromJson(Files.readString(file()), new TypeToken<Map<String, Set<Long>>>() {}.getType());
                if (m != null) data.putAll(m);
            }
        } catch (Exception e) { VortexClient.LOG.warn("Could not load world memory", e); }
    }

    public void save() {
        if (!dirty || key.isEmpty()) return;
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(data));
            dirty = false;
        } catch (Exception e) { VortexClient.LOG.warn("Could not save world memory", e); }
    }
}

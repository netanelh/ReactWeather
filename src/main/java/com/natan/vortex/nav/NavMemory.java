package com.natan.vortex.nav;

import com.natan.vortex.VortexClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A compact "walk map" of every chunk you have ever loaded: for each column, the Y you'd stand on
 * and whether that spot is dangerous (lava/hazard). Lets the pathfinder plan long routes across
 * chunks that aren't loaded right now; when the real chunk loads, the navigator re-plans against it.
 * Thread-safe reads from the pathfinding thread. Saved per server+dimension in config/vortex-nav/.
 */
public final class NavMemory {
    public static final NavMemory INSTANCE = new NavMemory();
    private static final int MAGIC = 0x564E4D31; // "VNM1"

    /** One chunk: 256 surface heights (short) + 256 danger flags (bit-packed). */
    private static final class Chunk {
        final short[] height = new short[256];
        final long[] danger = new long[4]; // 256 bits
        boolean dirty;
        void setHeight(int lx, int lz, int y) { height[(lz << 4) | lx] = (short) y; }
        int height(int lx, int lz) { return height[(lz << 4) | lx]; }
        void setDanger(int lx, int lz, boolean d) { int i = (lz << 4) | lx; if (d) danger[i >> 6] |= 1L << i; else danger[i >> 6] &= ~(1L << i); }
        boolean danger(int lx, int lz) { int i = (lz << 4) | lx; return (danger[i >> 6] & (1L << i)) != 0; }
    }

    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private String key = "";
    private int saveTimer;
    private volatile boolean dirty;

    private NavMemory() {}

    private static long ck(int cx, int cz) { return ((long) cx & 0xFFFFFFFFL) << 32 | (cz & 0xFFFFFFFFL); }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        String w = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        String d = mc.level == null ? "none" : mc.level.dimension().identifier().getPath();
        return (w + "_" + d).replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path file() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-nav").resolve(key + ".bin"); }

    /** Called each client tick: records the chunks around you. */
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        String k = worldKey();
        if (!k.equals(key)) { save(); key = k; chunks.clear(); load(); }
        int view = Math.min(8, mc.options.renderDistance().get());
        int cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
        for (int x = cx - view; x <= cx + view; x++) for (int z = cz - view; z <= cz + view; z++)
            if (mc.level.getChunkSource().hasChunk(x, z)) record(x, z);
        if (dirty && saveTimer-- <= 0) { saveTimer = 1200; save(); }
    }

    private void record(int cx, int cz) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.level.getChunkSource().hasChunk(cx, cz)) return;
        LevelChunk chunk = mc.level.getChunk(cx, cz);
        Chunk c = new Chunk();
        int minY = mc.level.getMinY(), maxY = minY + mc.level.getHeight();
        int baseX = cx << 4, baseZ = cz << 4;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int surf = Short.MIN_VALUE;
            boolean danger = false;
            // highest spot with 2 air above standable ground
            for (int y = maxY - 2; y > minY; y--) {
                m.set(baseX + lx, y - 1, baseZ + lz);
                BlockState floor = chunk.getBlockState(m);
                if (!World.standable2(floor, m)) continue;
                m.set(baseX + lx, y, baseZ + lz);
                if (!World.air2(chunk.getBlockState(m))) continue;
                m.set(baseX + lx, y + 1, baseZ + lz);
                if (!World.air2(chunk.getBlockState(m))) continue;
                surf = y;
                m.set(baseX + lx, y - 1, baseZ + lz);
                danger = World.isLava(floor);
                break;
            }
            if (surf != Short.MIN_VALUE) { c.setHeight(lx, lz, surf); c.setDanger(lx, lz, danger); any = true; }
            else c.setHeight(lx, lz, Short.MIN_VALUE);
        }
        if (any) { chunks.put(ck(cx, cz), c); dirty = true; }
    }

    public boolean has(int cx, int cz) { return chunks.containsKey(ck(cx, cz)); }

    /** Remembered surface Y at this column, or Integer.MIN_VALUE if unknown / no floor. */
    public int surfaceY(int x, int z) {
        Chunk c = chunks.get(ck(x >> 4, z >> 4));
        if (c == null) return Integer.MIN_VALUE;
        int h = c.height(x & 15, z & 15);
        return h == Short.MIN_VALUE ? Integer.MIN_VALUE : h;
    }

    public boolean dangerAt(int x, int z) {
        Chunk c = chunks.get(ck(x >> 4, z >> 4));
        return c != null && c.danger(x & 15, z & 15);
    }

    private void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(f)))) {
                if (in.readInt() != MAGIC) return;
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    long k = in.readLong();
                    Chunk c = new Chunk();
                    for (int j = 0; j < 256; j++) c.height[j] = in.readShort();
                    for (int j = 0; j < 4; j++) c.danger[j] = in.readLong();
                    chunks.put(k, c);
                }
            }
        } catch (Exception e) { VortexClient.LOG.warn("Could not load nav memory", e); }
    }

    public void save() {
        if (!dirty || key.isEmpty()) return;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(f)))) {
                out.writeInt(MAGIC);
                out.writeInt(chunks.size());
                for (var e : chunks.entrySet()) {
                    out.writeLong(e.getKey());
                    for (short s : e.getValue().height) out.writeShort(s);
                    for (long l : e.getValue().danger) out.writeLong(l);
                }
            }
            dirty = false;
        } catch (Exception e) { VortexClient.LOG.warn("Could not save nav memory", e); }
    }
}

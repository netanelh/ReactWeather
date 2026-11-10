package com.natan.vortex.seed;

import com.natan.vortex.VortexClient;
import com.natan.vortex.memory.WorldMemory;
import com.natan.vortex.mixin.StructurePlacementAccessor;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Recovers the world seed from things you can see, no OP:
 *  1. Desert pyramids sit exactly on a chunk corner, so each one you pass gives its exact chunk.
 *  2. Their chunk offsets inside each 32x32 region come from java.util.Random seeded with the world seed:
 *     the low bits are solved first (cheap filter), then the remaining bits are brute-forced on all cores
 *     -> the 48-bit structure seed.
 *  3. The top 16 bits come from the hashed seed every server sends on join (SHA-256 check, 65,536 tries).
 * Needs ~4-5 pyramids. Runs passively whenever you're in the Overworld.
 */
public final class SeedCracker {
    public static volatile long hashedSeed;
    public static volatile String status = "idle";
    private static volatile boolean running;
    private static int lastCount, timer;

    private static final long MULT = 0x5DEECE66DL, ADD = 0xBL, M48 = (1L << 48) - 1, RA = 341873128712L, RB = 132897987541L;
    private static int spacing, n, salt;

    public static boolean running() { return running; }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (running || mc.level == null || mc.player == null || SeedStore.server() == null || SeedStore.current() != null) return;
        if (timer-- > 0) return;
        timer = 100;
        if (!mc.level.dimension().equals(Level.OVERWORLD)) return;
        List<int[]> p = pyramids();
        if (p.size() >= 4 && p.size() != lastCount) { lastCount = p.size(); crack(false); }
    }

    /** Chunk coords of every desert pyramid seen (centre blue terracotta sits at chunk corner +10,+10). */
    public static List<int[]> pyramids() {
        Minecraft mc = Minecraft.getInstance();
        List<int[]> out = new ArrayList<>();
        if (mc.player == null) return out;
        Set<Long> seen = new HashSet<>();
        for (BlockPos b : WorldMemory.INSTANCE.nearest(Set.of(Blocks.BLUE_TERRACOTTA), mc.player.blockPosition(), 4000)) {
            if (Math.floorMod(b.getX() - 10, 16) != 0 || Math.floorMod(b.getZ() - 10, 16) != 0) continue;
            int cx = (b.getX() - 10) >> 4, cz = (b.getZ() - 10) >> 4;
            if (seen.add(ChunkPos.pack(cx, cz))) out.add(new int[]{cx, cz});
        }
        return out;
    }

    public static void crack(boolean loud) {
        Minecraft mc = Minecraft.getInstance();
        if (running) { Chat.info("Seed cracker already running: " + status); return; }
        List<int[]> p = pyramids();
        if (p.size() < 4) {
            if (loud) Chat.error("Need at least 4 desert pyramids, seen " + p.size() + ". Walk/fly through deserts (try .seed hunt), it cracks automatically.");
            return;
        }
        long hashed = hashedSeed;
        running = true;
        Thread t = new Thread(() -> {
            try {
                Long seed = solveAll(p, hashed);
                mc.execute(() -> {
                    if (seed != null) {
                        SeedStore.set(seed);
                        Chat.info("§aSeed cracked: §f" + seed + "§a. Now .locate works for any structure or biome on this server.");
                    } else if (loud) Chat.error("No seed matched (" + status + "). Find another pyramid or two and try again.");
                });
            } catch (Throwable e) {
                VortexClient.LOG.warn("Seed cracker failed", e);
                status = "error: " + e.getMessage();
            } finally { running = false; }
        }, "Vortex-SeedCracker");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static Long solveAll(List<int[]> p, long hashed) {
        var set = Worldgen.lookup().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.DESERT_PYRAMIDS).value();
        RandomSpreadStructurePlacement pl = (RandomSpreadStructurePlacement) set.placement();
        if (pl.spreadType() != RandomSpreadType.LINEAR) { status = "pyramid placement isn't linear in this version"; return null; }
        spacing = pl.spacing();
        n = pl.spacing() - pl.separation();
        salt = ((StructurePlacementAccessor) pl).vortex$salt();
        // self-test against the game's own placement code
        for (long test : new long[]{12345L, -987654321987L, 42L}) for (int r = -3; r <= 3; r += 3) {
            ChunkPos real = pl.getPotentialStructureChunk(test, r * spacing, -r * spacing);
            int[] mine = offsets(test, r, -r);
            if (real.x() != r * spacing + mine[0] || real.z() != -r * spacing + mine[1]) { status = "placement formula changed in this version"; return null; }
        }
        Long s = solve(p, hashed);
        if (s != null || p.size() < 5) return s;
        for (int skip = 0; skip < p.size(); skip++) { // one of them might be a fake (player-built) pyramid
            List<int[]> sub = new ArrayList<>(p);
            sub.remove(skip);
            s = solve(sub, hashed);
            if (s != null) return s;
        }
        return null;
    }

    private static Long solve(List<int[]> pyr, long hashed) {
        int k = Integer.numberOfTrailingZeros(n);
        if (k == 0) { status = "unsupported spacing"; return null; }
        int bits = 17 + k;
        long maskB = (1L << bits) - 1;
        int km = (1 << k) - 1;
        int m = pyr.size();
        int[] rx = new int[m], rz = new int[m], ox = new int[m], oz = new int[m];
        for (int i = 0; i < m; i++) {
            rx[i] = Math.floorDiv(pyr.get(i)[0], spacing); rz[i] = Math.floorDiv(pyr.get(i)[1], spacing);
            ox[i] = pyr.get(i)[0] - rx[i] * spacing; oz[i] = pyr.get(i)[1] - rz[i] * spacing;
            if (ox[i] >= n || oz[i] >= n) { status = "a pyramid doesn't fit the placement grid"; return null; }
        }
        // phase 1: low bits only
        status = "phase 1/3 (low bits)";
        List<Long> lows = new ArrayList<>();
        outer:
        for (long low = 0; low <= maskB; low++) {
            for (int i = 0; i < m; i++) {
                long s = ((rx[i] * RA + rz[i] * RB + low + salt) ^ MULT) & maskB;
                s = (s * MULT + ADD) & maskB;
                if (((s >>> 17) & km) != (ox[i] & km)) continue outer;
                s = (s * MULT + ADD) & maskB;
                if (((s >>> 17) & km) != (oz[i] & km)) continue outer;
            }
            lows.add(low);
        }
        // phase 2: brute force the rest of the 48-bit structure seed on all cores
        ConcurrentLinkedQueue<Long> structure = new ConcurrentLinkedQueue<>();
        AtomicInteger hits = new AtomicInteger();
        long highCount = 1L << (48 - bits);
        int li = 0;
        for (long low : lows) {
            li++;
            status = "phase 2/3 (" + li + "/" + lows.size() + ")";
            LongStream.range(0, highCount).parallel().forEach(h -> {
                long seed = (h << bits) | low;
                for (int i = 0; i < m; i++) {
                    int[] o = offsets(seed, rx[i], rz[i]);
                    if (o[0] != ox[i] || o[1] != oz[i]) return;
                }
                if (hits.incrementAndGet() <= 20000) structure.add(seed);
            });
            if (hits.get() > 20000) { status = "too many candidates, need more pyramids"; return null; }
        }
        if (structure.isEmpty()) { status = "no structure seed fits"; return null; }
        // phase 3: top 16 bits from the hashed seed the server sent
        status = "phase 3/3 (" + structure.size() + " candidates)";
        if (hashed == 0) { status = "server didn't send the hashed seed"; return null; }
        ConcurrentLinkedQueue<Long> full = new ConcurrentLinkedQueue<>();
        for (long ss : structure) IntStream.range(0, 1 << 16).parallel().forEach(u -> {
            long seed = ((long) u << 48) | ss;
            if (BiomeManager.obfuscateSeed(seed) == hashed) full.add(seed);
        });
        if (full.isEmpty()) { status = "hashed seed didn't match (world may use custom generation)"; return null; }
        status = "done";
        return full.peek();
    }

    /** Chunk offset inside the region exactly like RandomSpreadStructurePlacement (LINEAR) with java.util.Random. */
    private static int[] offsets(long worldSeed, int rx, int rz) {
        long s = ((rx * RA + rz * RB + worldSeed + salt) ^ MULT) & M48;
        int[] out = new int[2];
        for (int j = 0; j < 2; j++) {
            int bitsv, val;
            do {
                s = (s * MULT + ADD) & M48;
                bitsv = (int) (s >>> 17);
                val = bitsv % n;
            } while (bitsv - val + (n - 1) < 0);
            out[j] = val;
        }
        return out;
    }
}

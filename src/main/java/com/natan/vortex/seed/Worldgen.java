package com.natan.vortex.seed;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.List;

/** Runs the vanilla world generator locally with a known seed, so we can answer "where is X" anywhere. */
public final class Worldgen {
    private static HolderLookup.Provider lookup;
    private static Ctx cached;

    public record Ctx(long seed, ResourceKey<Level> dim, BiomeSource source, RandomState random, ChunkGeneratorStructureState structures) {}

    public static synchronized HolderLookup.Provider lookup() {
        if (lookup == null) lookup = VanillaRegistries.createLookup();
        return lookup;
    }

    public static synchronized Ctx ctx(long seed, ResourceKey<Level> dim) {
        if (cached != null && cached.seed == seed && cached.dim.equals(dim)) return cached;
        HolderLookup.Provider l = lookup();
        BiomeSource src;
        ResourceKey<NoiseGeneratorSettings> settings;
        if (dim.equals(Level.NETHER)) {
            src = MultiNoiseBiomeSource.createFromPreset(l.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));
            settings = NoiseGeneratorSettings.NETHER;
        } else if (dim.equals(Level.END)) {
            src = TheEndBiomeSource.create(l.lookupOrThrow(Registries.BIOME));
            settings = NoiseGeneratorSettings.END;
        } else {
            src = MultiNoiseBiomeSource.createFromPreset(l.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
            settings = NoiseGeneratorSettings.OVERWORLD;
        }
        RandomState rs = RandomState.create(l, settings, seed);
        var st = ChunkGeneratorStructureState.createForNormal(rs, seed, src, l.lookupOrThrow(Registries.STRUCTURE_SET));
        cached = new Ctx(seed, dim, src, rs, st);
        return cached;
    }

    public static BlockPos findBiome(Ctx c, String name, BlockPos origin, LevelReader level) {
        var key = ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(name));
        Pair<BlockPos, Holder<Biome>> r = c.source.findClosestBiome3d(origin, 6400, 32, 64, h -> h.is(key), c.random.sampler(), level);
        return r == null ? null : r.getFirst();
    }

    private static boolean matches(String path, String name) { return path.equals(name) || path.startsWith(name + "_"); }

    /** Nearest generated position of the structure, or null. */
    public static BlockPos findStructure(Ctx c, String name, BlockPos origin) {
        int ocx = origin.getX() >> 4, ocz = origin.getZ() >> 4;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (var ref : lookup().lookupOrThrow(Registries.STRUCTURE_SET).listElements().toList()) {
            StructureSet set = ref.value();
            List<Holder<Structure>> wanted = new ArrayList<>();
            boolean setMatch = matches(ref.key().identifier().getPath(), name) || ref.key().identifier().getPath().equals(name + "s");
            for (var e : set.structures()) {
                String p = e.structure().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
                if (setMatch || matches(p, name)) wanted.add(e.structure());
            }
            if (wanted.isEmpty()) continue;

            if (set.placement() instanceof ConcentricRingsStructurePlacement ring) {
                for (ChunkPos cp : c.structures.getRingPositionsFor(ring)) {
                    double d = sq(cp.x() - ocx, cp.z() - ocz);
                    if (d < bestD) { bestD = d; best = new BlockPos((cp.x() << 4) + 8, 40, (cp.z() << 4) + 8); }
                }
            } else if (set.placement() instanceof RandomSpreadStructurePlacement p) {
                int sp = p.spacing();
                int orx = Math.floorDiv(ocx, sp), orz = Math.floorDiv(ocz, sp);
                int foundRing = -1;
                for (int r = 0; r <= 120 && (foundRing < 0 || r <= foundRing + 1); r++) {
                    for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        ChunkPos cp = p.getPotentialStructureChunk(c.seed, (orx + dx) * sp, (orz + dz) * sp);
                        if (!p.isStructureChunk(c.structures, cp.x(), cp.z())) continue;
                        if (!biomeFits(c, wanted, cp)) continue;
                        double d = sq(cp.x() - ocx, cp.z() - ocz);
                        if (d < bestD) { bestD = d; best = new BlockPos((cp.x() << 4) + 8, 64, (cp.z() << 4) + 8); }
                        if (foundRing < 0) foundRing = r;
                    }
                }
            }
        }
        return best;
    }

    private static boolean biomeFits(Ctx c, List<Holder<Structure>> wanted, ChunkPos cp) {
        int qx = ((cp.x() << 4) + 8) >> 2, qz = ((cp.z() << 4) + 8) >> 2;
        int[] ys = c.dim.equals(Level.OVERWORLD) ? new int[]{64, -27} : new int[]{64, 32};
        for (int y : ys) {
            Holder<Biome> b = c.source.getNoiseBiome(qx, y >> 2, qz, c.random.sampler());
            for (Holder<Structure> s : wanted) if (s.value().biomes().contains(b)) return true;
        }
        return false;
    }

    private static double sq(int a, int b) { return (double) a * a + (double) b * b; }
}

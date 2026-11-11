package com.natan.vortex.builder;

import com.mojang.brigadier.StringReader;
import com.natan.vortex.VortexClient;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Loads a Litematica .litematic file (all regions) into a Schematic. */
public final class Litematic {

    public static Schematic load(String fileName, HolderLookup.Provider registries) throws Exception {
        Path f = Schematic.dir().resolve(fileName.endsWith(".litematic") ? fileName : fileName + ".litematic");
        if (!Files.exists(f)) throw new IllegalArgumentException("No such file: " + f.getFileName());
        CompoundTag root;
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(f))))) {
            root = NbtIo.read(in, NbtAccounter.create(256L * 1024 * 1024));
        }
        HolderLookup<net.minecraft.world.level.block.Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        CompoundTag regions = root.getCompoundOrEmpty("Regions");
        Map<BlockPos, BlockState> world = new HashMap<>();
        for (String regionName : regions.keySet()) {
            CompoundTag r = regions.getCompoundOrEmpty(regionName);
            CompoundTag size = r.getCompoundOrEmpty("Size");
            CompoundTag pos = r.getCompoundOrEmpty("Position");
            int sx = size.getInt("x").orElse(0), sy = size.getInt("y").orElse(0), sz = size.getInt("z").orElse(0);
            int px = pos.getInt("x").orElse(0), py = pos.getInt("y").orElse(0), pz = pos.getInt("z").orElse(0);
            int w = Math.abs(sx), h = Math.abs(sy), l = Math.abs(sz);
            int ox = Math.min(px, px + sx + (sx < 0 ? 1 : -1)); // min corner per axis sign
            int oy = Math.min(py, py + sy + (sy < 0 ? 1 : -1));
            int oz = Math.min(pz, pz + sz + (sz < 0 ? 1 : -1));
            // palette
            ListTag pal = r.getList("BlockStatePalette").orElse(new ListTag());
            BlockState[] states = new BlockState[pal.size()];
            for (int i = 0; i < pal.size(); i++) {
                CompoundTag e = pal.getCompound(i).orElse(new CompoundTag());
                String name = e.getString("Name").orElse("minecraft:air");
                StringBuilder sb = new StringBuilder(name);
                CompoundTag props = e.getCompoundOrEmpty("Properties");
                if (!props.isEmpty()) {
                    sb.append('[');
                    boolean first = true;
                    for (String k : props.keySet()) { if (!first) sb.append(','); sb.append(k).append('=').append(props.getString(k).orElse("")); first = false; }
                    sb.append(']');
                }
                try { states[i] = BlockStateParser.parseForBlock(blocks, new StringReader(sb.toString()), false).blockState(); }
                catch (Exception ex) { states[i] = null; }
            }
            long[] data = r.getLongArray("BlockStates").orElse(new long[0]);
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, pal.size() - 1)));
            long mask = (1L << bits) - 1;
            long volume = (long) w * h * l;
            for (long index = 0; index < volume; index++) {
                int id = bitAt(data, index, bits, mask);
                if (id < 0 || id >= states.length) continue;
                BlockState st = states[id];
                if (st == null || st.isAir()) continue;
                int x = (int) (index % w);
                int y = (int) (index / ((long) w * l));
                int z = (int) ((index / w) % l);
                world.put(new BlockPos(ox + x, oy + y, oz + z), st);
            }
        }
        VortexClient.LOG.info("Loaded litematic {} ({} blocks)", f.getFileName(), world.size());
        return Schematic.fromSparse(f.getFileName().toString(), world);
    }

    /** Litematica's straddling bit-array read. */
    private static int bitAt(long[] a, long index, int bits, long mask) {
        long start = index * bits;
        int startIdx = (int) (start >> 6);
        int endIdx = (int) (((index + 1) * bits - 1) >> 6);
        int startBit = (int) (start & 63);
        if (startIdx >= a.length) return -1;
        if (startIdx == endIdx) return (int) ((a[startIdx] >>> startBit) & mask);
        if (endIdx >= a.length) return -1;
        int endBits = 64 - startBit;
        return (int) (((a[startIdx] >>> startBit) | (a[endIdx] << endBits)) & mask);
    }
}

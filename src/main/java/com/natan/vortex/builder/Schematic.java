package com.natan.vortex.builder;

import com.natan.vortex.VortexClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.brigadier.StringReader;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A loaded Sponge .schem (v2 or v3). Holds the block states in a flat array indexed by
 * (y*length + z)*width + x. Air entries are kept as null to skip them while building.
 */
public final class Schematic {
    public final int width, height, length;
    public final BlockState[] blocks; // null = air / skip
    public final String name;

    private Schematic(String name, int w, int h, int l, BlockState[] b) {
        this.name = name; this.width = w; this.height = h; this.length = l; this.blocks = b;
    }

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-schematics"); }

    static Schematic of(String name, int w, int h, int l, BlockState[] b) { return new Schematic(name, w, h, l, b); }

    /** Build from a sparse map of absolute positions; normalizes to the min corner. */
    public static Schematic fromSparse(String name, java.util.Map<net.minecraft.core.BlockPos, BlockState> map) {
        if (map.isEmpty()) return new Schematic(name, 0, 0, 0, new BlockState[0]);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var p : map.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        int w = maxX - minX + 1, h = maxY - minY + 1, l = maxZ - minZ + 1;
        BlockState[] out = new BlockState[w * h * l];
        for (var e : map.entrySet()) {
            int x = e.getKey().getX() - minX, y = e.getKey().getY() - minY, z = e.getKey().getZ() - minZ;
            out[(y * l + z) * w + x] = e.getValue();
        }
        return new Schematic(name, w, h, l, out);
    }

    public BlockState at(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) return null;
        return blocks[(y * length + z) * width + x];
    }

    public static Schematic load(String fileName, HolderLookup.Provider registries) throws Exception {
        Path f = dir().resolve(fileName.endsWith(".schem") ? fileName : fileName + ".schem");
        if (!Files.exists(f)) throw new IllegalArgumentException("No such schematic: " + f.getFileName());
        CompoundTag root;
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(f))))) {
            root = NbtIo.read(in, NbtAccounter.create(64L * 1024 * 1024));
        }
        // v3 nests everything under "Schematic"; v2 is flat
        CompoundTag s = root.contains("Schematic") ? root.getCompoundOrEmpty("Schematic") : root;
        int w = s.getShortOr("Width", (short) 0) & 0xFFFF, h = s.getShortOr("Height", (short) 0) & 0xFFFF, l = s.getShortOr("Length", (short) 0) & 0xFFFF;
        CompoundTag blockContainer = s.contains("Blocks") ? s.getCompoundOrEmpty("Blocks") : s; // v3: Blocks{Palette,Data}; v2: Palette,BlockData
        CompoundTag paletteTag = blockContainer.getCompoundOrEmpty("Palette");
        byte[] data = blockContainer.getByteArray("Data").orElseGet(() -> blockContainer.getByteArray("BlockData").orElse(new byte[0]));

        // palette index -> BlockState
        Map<Integer, BlockState> palette = new HashMap<>();
        HolderLookup<net.minecraft.world.level.block.Block> blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        for (String key : paletteTag.keySet()) {
            int id = paletteTag.getInt(key).orElse(0);
            BlockState state;
            try {
                state = BlockStateParser.parseForBlock(blockLookup, new StringReader(key), false).blockState();
            } catch (Exception e) { state = null; }
            palette.put(id, state);
        }

        BlockState[] out = new BlockState[w * h * l];
        int i = 0, idx = 0;
        while (i < data.length && idx < out.length) {
            int val = 0, shift = 0; byte b;
            do { b = data[i++]; val |= (b & 0x7F) << shift; shift += 7; } while ((b & 0x80) != 0 && i < data.length);
            BlockState st = palette.get(val);
            if (st != null && !st.isAir()) out[idx] = st;
            idx++;
        }
        VortexClient.LOG.info("Loaded schematic {} ({}x{}x{})", f.getFileName(), w, h, l);
        return new Schematic(f.getFileName().toString(), w, h, l, out);
    }
}

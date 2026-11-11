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
        CompoundTag s = root.contains("Schematic") ? root.getCompound("Schematic") : root;
        int w = s.getShort("Width") & 0xFFFF, h = s.getShort("Height") & 0xFFFF, l = s.getShort("Length") & 0xFFFF;
        CompoundTag blockContainer = s.contains("Blocks") ? s.getCompound("Blocks") : s; // v3: Blocks{Palette,Data}; v2: Palette,BlockData
        CompoundTag paletteTag = blockContainer.getCompound("Palette");
        byte[] data = blockContainer.contains("Data") ? blockContainer.getByteArray("Data") : blockContainer.getByteArray("BlockData");

        // palette index -> BlockState
        Map<Integer, BlockState> palette = new HashMap<>();
        HolderLookup<net.minecraft.world.level.block.Block> blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        for (String key : paletteTag.getAllKeys()) {
            int id = paletteTag.getInt(key);
            BlockState state;
            try {
                state = BlockStateParser.parseForBlock(blockLookup, new java.io.StringReader(key), false).blockState();
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

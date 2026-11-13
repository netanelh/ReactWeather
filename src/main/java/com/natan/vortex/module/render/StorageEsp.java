package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Colors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Wireframe boxes around chests, barrels, shulkers and other storage through walls (drawn by HudRenderer, Sodium-safe). */
public class StorageEsp extends Module {
    private final BoolSetting chests = add(new BoolSetting("Chests", "Chests and trapped chests", true).group("Types"));
    private final ModeSetting chestColor = add(new ModeSetting("Chest Color", "Color for chests", "Orange", Colors.NAMES).visibleWhen(chests::on).group("Types"));
    private final BoolSetting ender = add(new BoolSetting("Ender Chests", "Ender chests", true).group("Types"));
    private final ModeSetting enderColor = add(new ModeSetting("Ender Color", "Color for ender chests", "Purple", Colors.NAMES).visibleWhen(ender::on).group("Types"));
    private final BoolSetting barrels = add(new BoolSetting("Barrels", "Barrels", true).group("Types"));
    private final ModeSetting barrelColor = add(new ModeSetting("Barrel Color", "Color for barrels", "Yellow", Colors.NAMES).visibleWhen(barrels::on).group("Types"));
    private final BoolSetting shulkers = add(new BoolSetting("Shulkers", "Shulker boxes", true).group("Types"));
    private final ModeSetting shulkerColor = add(new ModeSetting("Shulker Color", "Color for shulker boxes", "Pink", Colors.NAMES).visibleWhen(shulkers::on).group("Types"));
    private final BoolSetting hoppers = add(new BoolSetting("Hoppers", "Hoppers, droppers, dispensers", false).group("Types"));
    private final ModeSetting hopperColor = add(new ModeSetting("Hopper Color", "Color for hoppers/droppers/dispensers", "White", Colors.NAMES).visibleWhen(hoppers::on).group("Types"));
    private final BoolSetting furnaces = add(new BoolSetting("Furnaces", "Furnaces, smokers, blast furnaces", false).group("Types"));
    private final ModeSetting furnaceColor = add(new ModeSetting("Furnace Color", "Color for furnaces", "Red", Colors.NAMES).visibleWhen(furnaces::on).group("Types"));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", "Line from the bottom of the screen to each container", false));
    private final NumberSetting maxShown = add(new NumberSetting("Max Shown", "Nearest containers drawn", 200, 20, 500, 20));

    /** Found containers, nearest first: positions and colors (replaced as a whole, read on the render thread). */
    public volatile List<BlockPos> found = List.of();
    public volatile int[] colors = new int[0];
    private int timer;

    public StorageEsp() { super("StorageESP", "Boxes around chests, barrels and shulkers through walls", Category.RENDER); }

    @Override public String info() { return Integer.toString(found.size()); }

    @Override public void onDisable() { found = List.of(); colors = new int[0]; }
    @Override public void onWorldChange() { found = List.of(); colors = new int[0]; timer = 0; }

    /** Color for a block, or null if it isn't a shown storage type. */
    private Integer colorOf(Block b, int seed) {
        String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (id.equals("chest") || id.equals("trapped_chest") || id.endsWith("copper_chest")) return chests.on() ? Colors.named(chestColor.get(), seed) : null;
        if (id.equals("ender_chest")) return ender.on() ? Colors.named(enderColor.get(), seed) : null;
        if (id.equals("barrel")) return barrels.on() ? Colors.named(barrelColor.get(), seed) : null;
        if (id.endsWith("shulker_box")) return shulkers.on() ? Colors.named(shulkerColor.get(), seed) : null;
        if (id.equals("hopper") || id.equals("dropper") || id.equals("dispenser") || id.equals("crafter")) return hoppers.on() ? Colors.named(hopperColor.get(), seed) : null;
        if (id.equals("furnace") || id.equals("smoker") || id.equals("blast_furnace")) return furnaces.on() ? Colors.named(furnaceColor.get(), seed) : null;
        return null;
    }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        timer = 20;
        int view = mc.options.renderDistance().get();
        int ccx = mc.player.getBlockX() >> 4, ccz = mc.player.getBlockZ() >> 4;
        List<BlockPos> all = new ArrayList<>();
        for (int cx = ccx - view; cx <= ccx + view; cx++) for (int cz = ccz - view; cz <= ccz + view; cz++) {
            if (!mc.level.getChunkSource().hasChunk(cx, cz)) continue;
            for (BlockPos p : mc.level.getChunk(cx, cz).getBlockEntities().keySet())
                if (colorOf(mc.level.getBlockState(p).getBlock(), 0) != null) all.add(p.immutable());
        }
        BlockPos me = mc.player.blockPosition();
        all.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(me)));
        if (all.size() > maxShown.i()) all = new ArrayList<>(all.subList(0, maxShown.i()));
        int[] cols = new int[all.size()];
        for (int i = 0; i < cols.length; i++) {
            Integer c = colorOf(mc.level.getBlockState(all.get(i)).getBlock(), i);
            cols[i] = c == null ? 0xFFFFFFFF : c;
        }
        colors = cols;
        found = all;
    }
}

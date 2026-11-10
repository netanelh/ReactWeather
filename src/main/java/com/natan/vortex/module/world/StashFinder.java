package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Waypoints;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Chat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/** Scans loaded chunks for clusters of storage blocks and saves them as waypoints (works great with Explore). */
public class StashFinder extends Module {
    private final NumberSetting minChests = add(new NumberSetting("Min Storage", "Storage blocks in one chunk to count as a stash", 6, 2, 50, 1));
    private final Set<Long> reported = new HashSet<>();
    private int timer, found;

    public StashFinder() { super("StashFinder", "Finds bases/stashes (chest clusters) and saves waypoints", Category.WORLD); }

    @Override public String info() { return Integer.toString(found); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        timer = 60;
        int view = mc.options.renderDistance().get();
        int ccx = mc.player.getBlockX() >> 4, ccz = mc.player.getBlockZ() >> 4;
        int minY = mc.level.getMinY(), maxY = minY + mc.level.getHeight();
        for (int cx = ccx - view; cx <= ccx + view; cx++) for (int cz = ccz - view; cz <= ccz + view; cz++) {
            long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
            if (reported.contains(key) || !mc.level.getChunkSource().hasChunk(cx, cz)) continue;
            var chunk = mc.level.getChunk(cx, cz);
            int count = 0; BlockPos any = null;
            for (BlockPos p : chunk.getBlockEntities().keySet()) {
                var b = mc.level.getBlockState(p).getBlock();
                if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL || b == Blocks.ENDER_CHEST
                    || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().endsWith("shulker_box")) { count++; any = p; }
            }
            if (count >= minChests.i() && any != null && any.getY() >= minY && any.getY() < maxY) {
                reported.add(key);
                found++;
                String name = "stash" + found;
                Waypoints.INSTANCE.set(name, any);
                Chat.info("§6Stash found§r: " + count + " storage blocks at " + any.toShortString() + " (waypoint " + name + ")");
            }
        }
    }
}

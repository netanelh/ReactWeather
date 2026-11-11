package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Actions;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class TreeAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Reach for logs", 4.5, 2, 5, 0.5));
    private BlockPos current;

    public TreeAura() { super("TreeAura", "Chops every log in reach automatically", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (current == null || !isLog(current) || Vec3.atCenterOf(current).distanceTo(mc.player.getEyePosition()) > range.d()) {
            BlockPos me = mc.player.blockPosition();
            int r = (int) Math.ceil(range.d());
            current = BlockPos.betweenClosedStream(me.offset(-r, -1, -r), me.offset(r, 5, r))
                .filter(this::isLog)
                .filter(p -> Vec3.atCenterOf(p).distanceTo(mc.player.getEyePosition()) <= range.d())
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(p -> p.distSqr(me))).orElse(null);
        }
        if (current != null && Actions.mine(current)) current = null;
    }

    @Override public void onDisable() { current = null; Actions.stopMining(); }

    private boolean isLog(BlockPos p) {
        String id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(p).getBlock()).getPath();
        return (id.endsWith("_log") || id.endsWith("_stem")) && !id.startsWith("stripped_");
    }
}

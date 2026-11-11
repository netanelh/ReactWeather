package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoTrap extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max target distance", 4.5, 2, 6, 0.5));
    private final NumberSetting perTick = add(new NumberSetting("Per Tick", "Blocks per tick", 3, 1, 8, 1));
    private final BoolSetting top = add(new BoolSetting("Top", "Also seal the block above their head", true));

    private Player target;

    private final com.natan.vortex.setting.BoolSetting anyBlock = add(new com.natan.vortex.setting.BoolSetting("Any Block", "If no obsidian, seal with any full block", true));

    public AutoTrap() { super("AutoTrap", "Traps the nearest player in obsidian", Category.COMBAT); }

    @Override public String info() { return target != null ? target.getName().getString() : null; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        target = null; double best = range.d();
        for (Player p : mc.level.players()) {
            if (p == mc.player || p.isCreative() || p.isSpectator()) continue;
            double d = p.distanceTo(mc.player);
            if (d < best) { best = d; target = p; }
        }
        if (target == null) return;
        int slot = Place.buildBlockSlot(anyBlock.on());
        if (slot == -1) return;
        BlockPos feet = target.blockPosition();
        List<BlockPos> plan = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) { plan.add(feet.relative(d)); plan.add(feet.relative(d).above()); }
        if (top.on()) plan.add(feet.above(2));
        int placed = 0, prev = com.natan.vortex.util.Inv.selected();
        for (BlockPos p : plan) {
            if (placed >= perTick.i()) break;
            if (Place.canPlaceAt(p) && Place.at(p, slot, true)) placed++;
        }
        com.natan.vortex.util.Inv.select(prev);
    }
}

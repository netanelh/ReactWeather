package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

public class Surround extends Module {
    private final NumberSetting perTick = add(new NumberSetting("Per Tick", "Blocks placed per tick", 4, 1, 8, 1));
    private final BoolSetting center = add(new BoolSetting("Center", "Snap to the middle of the block first", true));
    private final BoolSetting disableOnJump = add(new BoolSetting("Off On Jump", "Turn off when you jump away", true));

    private int startY = Integer.MIN_VALUE;

    private final com.natan.vortex.setting.BoolSetting anyBlock = add(new com.natan.vortex.setting.BoolSetting("Any Block", "If no obsidian, seal with any full block", true));

    public Surround() { super("Surround", "Boxes your feet in obsidian against crystals", Category.COMBAT); }

    @Override
    public void onEnable() {
        startY = Integer.MIN_VALUE;
        if (!inGame()) return;
        startY = mc.player.getBlockY();
        if (center.on()) {
            var p = mc.player;
            p.setPos(Math.floor(p.getX()) + 0.5, p.getY(), Math.floor(p.getZ()) + 0.5);
            p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
        }
    }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (startY == Integer.MIN_VALUE) startY = mc.player.getBlockY();
        if (disableOnJump.on() && mc.player.getBlockY() > startY) { toggle(); return; }
        int slot = Place.buildBlockSlot(anyBlock.on());
        if (slot == -1) return;
        BlockPos feet = mc.player.blockPosition();
        int placed = 0, prev = com.natan.vortex.util.Inv.selected();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (placed >= perTick.i()) break;
            BlockPos p = feet.relative(d);
            if (!mc.level.getBlockState(p).canBeReplaced()) continue;
            if (!Place.canPlaceAt(p)) { // support block underneath first
                if (Place.at(p.below(), slot, true)) placed++;
                continue;
            }
            if (Place.at(p, slot, true)) placed++;
        }
        com.natan.vortex.util.Inv.select(prev);
    }
}

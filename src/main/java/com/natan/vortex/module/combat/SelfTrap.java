package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

public class SelfTrap extends Module {
    private final BoolSetting top = add(new BoolSetting("Top", "Cover the block above your head", true));

    public SelfTrap() { super("SelfTrap", "Encases you in obsidian (sides at head height + top)", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int slot = Place.findSlot(s -> s.is(Items.OBSIDIAN) || s.is(Items.CRYING_OBSIDIAN));
        if (slot == -1) return;
        int prev = Inv.selected();
        BlockPos head = mc.player.blockPosition().above();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = head.relative(d);
            if (Place.canPlaceAt(p) && Place.at(p, slot, true)) { Inv.select(prev); return; }
        }
        if (top.on() && Place.canPlaceAt(head.above()) && Place.at(head.above(), slot, true)) { Inv.select(prev); return; }
        Inv.select(prev);
    }
}

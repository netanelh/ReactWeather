package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

public class LiquidFiller extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Fill range", 4.5, 2, 5, 0.5));
    private final BoolSetting lava = add(new BoolSetting("Lava", "Fill lava sources", true));
    private final BoolSetting water = add(new BoolSetting("Water", "Fill water sources", false));

    public LiquidFiller() { super("LiquidFiller", "Plugs lava/water source blocks around you", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int slot = Place.findSlot(Place::isThrowaway);
        if (slot == -1) return;
        int r = (int) Math.ceil(range.d());
        BlockPos me = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -r, -r), me.offset(r, r, r))) {
            if (Vec3.atCenterOf(p).distanceTo(mc.player.getEyePosition()) > range.d()) continue;
            var fs = mc.level.getFluidState(p);
            if (fs.isEmpty() || !fs.isSource()) continue;
            if (!(lava.on() && fs.is(FluidTags.LAVA) || water.on() && fs.is(FluidTags.WATER))) continue;
            int prev = Inv.selected();
            if (Place.at(p.immutable(), slot, true)) { Inv.select(prev); return; }
        }
    }
}

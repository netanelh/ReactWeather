package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Enemies;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Traps the nearest enemy in cobwebs (feet, and head if you want). */
public class AutoWeb extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Enemy range", 4.5, 2, 6, 0.5));
    private final BoolSetting head = add(new BoolSetting("Head Too", "Also web their head block", false));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between placements", 2, 0, 10, 1));
    private int timer;

    public AutoWeb() { super("AutoWeb", "Places cobwebs on the nearest enemy", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        Player t = Enemies.nearest(range.d());
        if (t == null) return;
        int slot = Inv.findHotbar(s -> s.is(Items.COBWEB));
        if (slot == -1) return;
        int prev = Inv.selected();
        BlockPos feet = t.blockPosition();
        boolean done = tryAt(feet, slot) || (head.on() && tryAt(feet.above(), slot));
        if (done) { timer = delay.i(); Inv.select(prev); }
    }

    private boolean tryAt(BlockPos p, int slot) {
        return mc.level.getBlockState(p).canBeReplaced() && !mc.level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.COBWEB) && Place.at(p, slot, true);
    }
}

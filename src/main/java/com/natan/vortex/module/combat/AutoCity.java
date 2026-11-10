package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Actions;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Enemies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Breaks the obsidian around the nearest enemy's feet so crystals can reach them. */
public class AutoCity extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Enemy range", 5, 2, 6, 0.5));
    private BlockPos mining;

    public AutoCity() { super("AutoCity", "Mines the enemy's surround block closest to you", Category.COMBAT); }

    @Override public void onDisable() { if (mining != null && inGame()) mc.gameMode.stopDestroyBlock(); mining = null; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (mining != null && mc.level.getBlockState(mining).isAir()) { Chat.info("City broken."); mining = null; setEnabled(false); return; }
        if (mining == null) {
            Player t = Enemies.nearest(range.d());
            if (t == null) return;
            BlockPos feet = t.blockPosition();
            double best = 4.5 * 4.5;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = feet.relative(d);
                var st = mc.level.getBlockState(p);
                if (st.isAir() || st.is(Blocks.BEDROCK) || st.getDestroySpeed(mc.level, p) < 0) continue;
                double dist = mc.player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p));
                if (dist < best) { best = dist; mining = p; }
            }
            if (mining == null) return;
            Actions.selectBestTool(mc.level.getBlockState(mining));
            mc.gameMode.startDestroyBlock(mining, Direction.UP);
        } else {
            Actions.selectBestTool(mc.level.getBlockState(mining));
            mc.gameMode.continueDestroyBlock(mining, Direction.UP);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}

package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Enemies;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Overworld/End respawn-anchor bombing: place, charge with glowstone, detonate next to the enemy. */
public class AnchorAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Enemy range", 6, 3, 8, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between steps", 2, 0, 10, 1));
    private int timer;

    public AnchorAura() { super("AnchorAura", "Explodes respawn anchors on enemies (not in the Nether)", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.level.dimension().equals(Level.NETHER) || timer-- > 0) return;
        Player t = Enemies.nearest(range.d());
        if (t == null) return;
        int anchor = Inv.findHotbar(s -> s.is(Items.RESPAWN_ANCHOR)), glow = Inv.findHotbar(s -> s.is(Items.GLOWSTONE));
        if (glow == -1) return;
        BlockPos head = t.blockPosition().above();
        BlockPos[] spots = {head.above(), head.north(), head.south(), head.east(), head.west()};
        for (BlockPos p : spots) {
            var st = mc.level.getBlockState(p);
            boolean isAnchor = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().equals("respawn_anchor");
            if (isAnchor) {
                int charge = st.getValue(RespawnAnchorBlock.CHARGE);
                if (charge == 0) use(p, glow);
                else {
                    int other = Inv.findHotbar(s -> !s.is(Items.GLOWSTONE));
                    if (other == -1) return;
                    if (mc.player.distanceToSqr(Vec3.atCenterOf(p)) < t.distanceToSqr(Vec3.atCenterOf(p))) return; // anti-suicide
                    use(p, other);
                }
                timer = delay.i();
                return;
            }
        }
        if (anchor == -1) return;
        for (BlockPos p : spots) {
            if (mc.player.distanceToSqr(Vec3.atCenterOf(p)) < t.distanceToSqr(Vec3.atCenterOf(p))) continue;
            if (Place.canPlaceAt(p) && Place.at(p, anchor, true)) { timer = delay.i(); return; }
        }
    }

    private void use(BlockPos p, int slot) {
        Inv.select(slot);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}

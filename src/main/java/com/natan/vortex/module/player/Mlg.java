package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Water-bucket clutch: places water right before you hit the ground, then picks it back up. */
public class Mlg extends Module {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall", "Only clutch falls higher than this", 5, 3, 40, 1));
    private int pickupIn = -1, prevSlot = -1;

    public Mlg() { super("MLG", "Auto water-bucket clutch on big falls", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (pickupIn >= 0) {
            if (--pickupIn == 0) {
                int empty = Inv.findHotbar(s -> s.is(Items.BUCKET));
                if (empty != -1) { Inv.select(empty); Rotations.sendSilent(p.getYRot(), 90); mc.gameMode.useItem(p, InteractionHand.MAIN_HAND); }
                if (prevSlot != -1) Inv.select(prevSlot);
                pickupIn = -1;
            }
            return;
        }
        if (p.fallDistance < minFall.d() || p.onGround() || p.isFallFlying() || p.isInWater()) return;
        Vec3 from = p.position(), to = from.add(0, Math.min(-3, p.getDeltaMovement().y * 3), 0);
        BlockHitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        int slot = Inv.findHotbar(s -> s.is(Items.WATER_BUCKET));
        if (slot == -1) return;
        prevSlot = Inv.selected();
        Inv.select(slot);
        p.setXRot(90);
        Rotations.sendSilent(p.getYRot(), 90);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        pickupIn = 6;
    }
}

package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import com.natan.vortex.util.Targets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Places end crystals next to enemies and blows them up. Skips spots that would hurt you more than them. */
public class CrystalAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Place/break range", 4.5, 2, 6, 0.5));
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", "Enemy search range", 10, 4, 16, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between placements", 2, 0, 10, 1));
    private final NumberSetting breakDelay = add(new NumberSetting("Break Delay", "Ticks between breaking crystals", 1, 0, 10, 1));
    private final NumberSetting maxSelf = add(new NumberSetting("Max Self", "Anti Suicide: max estimated damage to you (HP)", 8, 1, 36, 1));
    private final BoolSetting place = add(new BoolSetting("Place", "Place crystals", true));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to crystals in the hotbar", true));
    private final BoolSetting antiSuicide = add(new BoolSetting("Anti Suicide", "Skip crystals that would hurt you more than the target (or too much)", true));

    private Player target;
    private int timer, breakTimer;

    /** Rough crystal damage (power 6, full exposure, no armor) at distance d. Used to rank and filter spots. */
    private static double crystalDamage(double d) {
        if (d >= 12) return 0;
        double imp = 1 - d / 12;
        return (imp * imp + imp) / 2 * 7 * 12 + 1;
    }

    private boolean safeFor(Vec3 explosion) {
        if (!antiSuicide.on()) return true;
        double self = crystalDamage(explosion.distanceTo(mc.player.position()));
        double tgt = crystalDamage(explosion.distanceTo(target.position()));
        return self <= maxSelf.d() && self < mc.player.getHealth() + mc.player.getAbsorptionAmount() && self < tgt;
    }

    public CrystalAura() { super("CrystalAura", "Auto places and explodes end crystals at enemies", Category.COMBAT); }

    @Override public String info() { return target != null ? target.getName().getString() : null; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        target = null; double best = targetRange.d();
        for (Player p : mc.level.players()) {
            if (!Targets.valid(p, true, false, false)) continue;
            double d = p.distanceTo(mc.player);
            if (d < best) { best = d; target = p; }
        }
        if (timer > 0) timer--;
        if (breakTimer > 0) breakTimer--;
        if (target == null) return;

        // 1) break the crystal that hurts the target most (own break timer, so place can follow in the same tick)
        Entity bestCrystal = null; double bestDmg = 0;
        if (breakTimer == 0) for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal) || e.distanceTo(mc.player) > range.d()) continue;
            if (e.distanceTo(target) > 6 || !safeFor(e.position())) continue;
            double dmg = crystalDamage(e.position().distanceTo(target.position()));
            if (dmg > bestDmg) { bestDmg = dmg; bestCrystal = e; }
        }
        if (bestCrystal != null) {
            Entity e = bestCrystal;
            Vec3 c = e.getBoundingBox().getCenter();
            float[] a = Rotations.anglesTo(c);
            Rotations.sendSilent(a[0], a[1]);
            mc.gameMode.attack(mc.player, e);
            mc.player.swing(InteractionHand.MAIN_HAND);
            breakTimer = breakDelay.i();
        }
        if (!place.on() || timer > 0) return;

        // 2) place a crystal on obsidian/bedrock next to the target
        int slot = Inv.findHotbar(s -> s.is(Items.END_CRYSTAL));
        boolean offhand = mc.player.getOffhandItem().is(Items.END_CRYSTAL);
        if (!offhand && (slot == -1 || !autoSwitch.on() && !mc.player.getMainHandItem().is(Items.END_CRYSTAL))) return;

        BlockPos bestPos = null; double bestScore = -Double.MAX_VALUE;
        BlockPos tp = target.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(tp.offset(-2, -2, -2), tp.offset(2, 1, 2))) {
            var b = mc.level.getBlockState(p).getBlock();
            if (b != Blocks.OBSIDIAN && b != Blocks.BEDROCK) continue;
            if (!mc.level.getBlockState(p.above()).isAir() || !mc.level.getBlockState(p.above(2)).isAir()) continue;
            Vec3 top = Vec3.atCenterOf(p).add(0, 0.5, 0);
            if (top.distanceTo(mc.player.getEyePosition()) > range.d()) continue;
            if (!mc.level.getEntities((Entity) null, new AABB(p.above()).expandTowards(0, 1, 0), e -> true).isEmpty()) continue;
            Vec3 boom = Vec3.atBottomCenterOf(p.above()); // crystal sits on top of the block
            if (!safeFor(boom)) continue;
            double score = crystalDamage(boom.distanceTo(target.position())) - crystalDamage(boom.distanceTo(mc.player.position())) * 0.5;
            if (score > bestScore) { bestScore = score; bestPos = p.immutable(); }
        }
        if (bestPos == null) return;
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!offhand) Inv.select(slot);
        Vec3 hit = Vec3.atCenterOf(bestPos).add(0, 0.5, 0);
        float[] a = Rotations.anglesTo(hit);
        Rotations.sendSilent(a[0], a[1]);
        mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(hit, Direction.UP, bestPos, false));
        mc.player.swing(hand);
        timer = delay.i();
    }
}

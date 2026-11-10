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
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between actions", 2, 0, 10, 1));
    private final BoolSetting place = add(new BoolSetting("Place", "Place crystals", true));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to crystals in the hotbar", true));
    private final BoolSetting antiSuicide = add(new BoolSetting("Anti Suicide", "Never place closer to you than to the target", true));

    private Player target;
    private int timer;

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
        if (target == null || timer-- > 0) return;

        // 1) break the nearest crystal close to the target
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal) || e.distanceTo(mc.player) > range.d()) continue;
            if (e.distanceTo(target) > 6) continue;
            Vec3 c = e.getBoundingBox().getCenter();
            float[] a = Rotations.anglesTo(c);
            Rotations.sendSilent(a[0], a[1]);
            mc.gameMode.attack(mc.player, e);
            mc.player.swing(InteractionHand.MAIN_HAND);
            timer = delay.i();
            return;
        }
        if (!place.on()) return;

        // 2) place a crystal on obsidian/bedrock next to the target
        int slot = Inv.findHotbar(s -> s.is(Items.END_CRYSTAL));
        boolean offhand = mc.player.getOffhandItem().is(Items.END_CRYSTAL);
        if (!offhand && (slot == -1 || !autoSwitch.on() && !mc.player.getMainHandItem().is(Items.END_CRYSTAL))) return;

        BlockPos bestPos = null; double bestScore = Double.MAX_VALUE;
        BlockPos tp = target.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(tp.offset(-2, -2, -2), tp.offset(2, 1, 2))) {
            var b = mc.level.getBlockState(p).getBlock();
            if (b != Blocks.OBSIDIAN && b != Blocks.BEDROCK) continue;
            if (!mc.level.getBlockState(p.above()).isAir() || !mc.level.getBlockState(p.above(2)).isAir()) continue;
            Vec3 top = Vec3.atCenterOf(p).add(0, 0.5, 0);
            if (top.distanceTo(mc.player.getEyePosition()) > range.d()) continue;
            if (!mc.level.getEntities((Entity) null, new AABB(p.above()).expandTowards(0, 1, 0), e -> true).isEmpty()) continue;
            double toTarget = top.distanceTo(target.position()), toMe = top.distanceTo(mc.player.position());
            if (antiSuicide.on() && toMe < toTarget + 1) continue;
            if (toTarget < bestScore) { bestScore = toTarget; bestPos = p.immutable(); }
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

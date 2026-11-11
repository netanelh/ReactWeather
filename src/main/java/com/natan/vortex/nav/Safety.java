package com.natan.vortex.nav;

import com.natan.vortex.module.pathing.NavSettings;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import com.natan.vortex.util.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/** Things the navigator does before any task step: eat, fight back, wait out low health. */
final class Safety {
    private static boolean eating;
    private static int prevSlot = -1;
    private static boolean lowHpPaused;
    private static boolean mlgPlaced;
    private static int mlgTicks;
    private static int mlgSlotPrev = -1;
    private static net.minecraft.core.BlockPos mlgWater;

    private Safety() {}

    /** @return true if a safety action used this tick (task movement should wait). */
    static boolean tick(NavSettings s) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;

        // gliding: never take over the elytra controls (eating / MLG / fighting mid-flight would crash us)
        if (p.isFallFlying()) { if (eating) stopEating(); return false; }

        // 0) MLG water bucket to survive a long fall
        if (s.mlg.on() && mlg(mc, p)) return true;

        // 1) low health: stand still and let regen work (eat if we can)
        float hearts = (p.getHealth() + p.getAbsorptionAmount()) / 2f;
        if (s.lowHealth.d() > 0) {
            if (!lowHpPaused && hearts <= s.lowHealth.d()) { lowHpPaused = true; com.natan.vortex.util.Chat.info("Low health, pausing task until healed."); }
            if (lowHpPaused && hearts >= Math.min(10, s.lowHealth.d() + 4)) { lowHpPaused = false; com.natan.vortex.util.Chat.info("Healed, resuming."); }
        } else lowHpPaused = false;

        // 2) eating
        int food = p.getFoodData().getFoodLevel();
        boolean wantEat = s.autoEat.on() && !p.isCreative() && (food <= 13 || lowHpPaused && food < 20);
        if (eating) {
            if (!wantEat || !p.getMainHandItem().has(DataComponents.FOOD)) { stopEating(); }
            else { mc.options.keyUse.setDown(true); return true; }
        } else if (wantEat) {
            int slot = Inv.findHotbar(st -> st.has(DataComponents.FOOD) && !com.natan.vortex.util.Items2.id(st).equals("rotten_flesh")
                && !com.natan.vortex.util.Items2.id(st).equals("spider_eye"));
            if (slot != -1) {
                Actions.releaseKeys();
                prevSlot = Inv.selected();
                Inv.select(slot);
                eating = true;
                mc.options.keyUse.setDown(true);
                return true;
            }
        }

        // 3) self defense
        if (s.selfDefense.on()) {
            Entity threat = null; double best = 3.6;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!Targets.isHostile(e) || !e.isAlive() || !Targets.valid(e, false, true, false)) continue;
                double d = e.distanceTo(p);
                if (d < best && p.hasLineOfSight(e)) { best = d; threat = e; }
            }
            if (threat != null) {
                Actions.releaseKeys();
                Actions.lookAt(Rotations.aimPoint(threat), 70);
                com.natan.vortex.module.combat.AutoWeapon.switchToBest(threat);
                if (p.getAttackStrengthScale(0.5f) >= 1f) {
                    mc.gameMode.attack(p, threat);
                    p.swing(InteractionHand.MAIN_HAND);
                }
                return true;
            }
        }

        if (lowHpPaused) { Actions.releaseKeys(); return true; }
        return false;
    }

    private static boolean mlg(Minecraft mc, net.minecraft.client.player.LocalPlayer p) {
        // already placed water: pick it back up once we have landed in it
        if (mlgPlaced) {
            mlgTicks++;
            boolean wet = p.isInWater() || com.natan.vortex.nav.World.inWater(p.getBlockX(), p.getBlockY(), p.getBlockZ());
            if (mlgWater != null && p.onGround() && wet) {
                int empty = Inv.findHotbar(st -> st.is(net.minecraft.world.item.Items.BUCKET));
                if (empty != -1) { Inv.select(empty); p.setXRot(90f); mc.gameMode.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND); }
            } else if (mlgTicks < 60 && !(p.onGround() && mlgTicks > 5)) return false;
            // landed (or gave up): reset so MLG works again next fall
            mlgPlaced = false; mlgWater = null; mlgTicks = 0;
            if (mlgSlotPrev != -1) Inv.select(mlgSlotPrev);
            mlgSlotPrev = -1;
            return false;
        }
        if (p.onGround() || p.getDeltaMovement().y > -0.6 || p.isInWater() || p.getAbilities().flying) return false;
        if (p.fallDistance < 5) return false;
        int slot = Inv.findHotbar(st -> st.is(net.minecraft.world.item.Items.WATER_BUCKET));
        if (slot == -1) return false;
        // find the solid block we are about to hit, within the next few blocks down
        int bx = p.getBlockX(), bz = p.getBlockZ();
        int fy = p.getBlockY();
        net.minecraft.core.BlockPos ground = null;
        for (int k = 1; k <= 3; k++) {
            if (com.natan.vortex.nav.World.standable(bx, fy - k, bz)) { ground = new net.minecraft.core.BlockPos(bx, fy - k + 1, bz); break; }
        }
        if (ground == null) return false;
        Actions.releaseKeys();
        if (mlgSlotPrev == -1) mlgSlotPrev = Inv.selected();
        Inv.select(slot);
        p.setXRot(90f); // buckets only act through use(): look straight down and use
        if (mc.gameMode.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()) { mlgPlaced = true; mlgWater = ground; mlgTicks = 0; }
        return true;
    }

    /** Called when a task stops or pauses: release right-click and clear state. */
    static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (eating) {
            eating = false;
            mc.options.keyUse.setDown(false);
            if (prevSlot != -1 && mc.player != null) Inv.select(prevSlot);
        }
        prevSlot = -1;
        lowHpPaused = false;
        mlgPlaced = false; mlgWater = null; mlgSlotPrev = -1; mlgTicks = 0;
    }

    private static void stopEating() {
        eating = false;
        Minecraft.getInstance().options.keyUse.setDown(false);
        if (prevSlot != -1) Inv.select(prevSlot);
        prevSlot = -1;
    }
}

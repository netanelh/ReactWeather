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

    private Safety() {}

    /** @return true if a safety action used this tick (task movement should wait). */
    static boolean tick(NavSettings s) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;

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

    private static void stopEating() {
        eating = false;
        Minecraft.getInstance().options.keyUse.setDown(false);
        if (prevSlot != -1) Inv.select(prevSlot);
        prevSlot = -1;
    }
}

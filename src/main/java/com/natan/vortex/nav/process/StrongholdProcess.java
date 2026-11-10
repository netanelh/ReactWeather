package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the stronghold with 2 eyes of ender and triangulation, no OP needed:
 * throw one, record its direction, walk ~80 blocks sideways, throw another,
 * intersect the two lines, walk there. Uses your eyes from the hotbar.
 */
public class StrongholdProcess implements Process {
    private enum Phase { THROW1, WATCH1, MOVE, THROW2, WATCH2, GO }

    private Phase phase = Phase.THROW1;
    private Vec3 origin1, dir1, origin2, dir2, eyeStart;
    private EyeOfEnder eye;
    private int ticks;
    private Goal goal, sideGoal;
    private boolean done;

    @Override public String name() { return "Stronghold"; }
    @Override public String status() { return switch (phase) {
        case THROW1, WATCH1 -> "reading the 1st eye";
        case MOVE -> "moving sideways";
        case THROW2, WATCH2 -> "reading the 2nd eye";
        case GO -> "stronghold";
    }; }
    @Override public boolean isDone() { return done; }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        switch (phase) {
            case THROW1, THROW2 -> {
                int slot = Inv.findHotbar(s -> s.is(Items.ENDER_EYE));
                if (slot == -1) { Chat.error("Put eyes of ender in your hotbar (need 2)."); done = true; return; }
                Actions.releaseKeys();
                Inv.select(slot);
                mc.player.setXRot(-10);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                if (phase == Phase.THROW1) origin1 = mc.player.position(); else origin2 = mc.player.position();
                phase = phase == Phase.THROW1 ? Phase.WATCH1 : Phase.WATCH2;
                eye = null; ticks = 0;
            }
            case WATCH1, WATCH2 -> {
                if (eye == null) {
                    for (Entity e : mc.level.entitiesForRendering())
                        if (e instanceof EyeOfEnder ee && ee.distanceTo(mc.player) < 4) { eye = ee; eyeStart = ee.position(); }
                    if (eye == null && ticks > 40) { Chat.error("Didn't see the eye fly. Are you in the Overworld?"); done = true; }
                    return;
                }
                if (ticks < 25 && eye.isAlive()) return;
                Vec3 now = eye.position();
                Vec3 d = new Vec3(now.x - eyeStart.x, 0, now.z - eyeStart.z);
                if (d.lengthSqr() < 0.5) { Chat.error("The eye didn't move far enough, try again from open ground."); done = true; return; }
                d = d.normalize();
                if (phase == Phase.WATCH1) {
                    dir1 = d;
                    // walk 80 blocks perpendicular to the eye's direction
                    Vec3 side = new Vec3(-d.z, 0, d.x).scale(80);
                    sideGoal = new Goal.XZ((int) (origin1.x + side.x), (int) (origin1.z + side.z));
                    phase = Phase.MOVE;
                    Chat.info("First eye points " + Math.round(Math.toDegrees(Math.atan2(-d.x, d.z))) + "°. Moving sideways for the 2nd reading...");
                } else {
                    dir2 = d;
                    Vec3 hit = intersect(origin1, dir1, origin2, dir2);
                    if (hit == null) { Chat.error("Readings were parallel, try again."); done = true; return; }
                    goal = new Goal.XZ((int) hit.x, (int) hit.z);
                    phase = Phase.GO;
                    Chat.info("Stronghold is around " + (int) hit.x + ", " + (int) hit.z + " (" + Math.round(Math.hypot(hit.x - mc.player.getX(), hit.z - mc.player.getZ())) + " blocks). Going there; dig down near the spot.");
                }
            }
            default -> {}
        }
    }

    private static Vec3 intersect(Vec3 p1, Vec3 d1, Vec3 p2, Vec3 d2) {
        double det = d1.x * d2.z - d1.z * d2.x;
        if (Math.abs(det) < 1e-4) return null;
        double t = ((p2.x - p1.x) * d2.z - (p2.z - p1.z) * d2.x) / det;
        return new Vec3(p1.x + d1.x * t, 0, p1.z + d1.z * t);
    }

    @Override
    public Goal goal() {
        return switch (phase) { case MOVE -> sideGoal; case GO -> goal; default -> null; };
    }

    @Override
    public void atGoal() {
        if (phase == Phase.MOVE) phase = Phase.THROW2;
        else if (phase == Phase.GO) done = true;
    }
}

package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Targets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;

/** Aims bows/crossbows with gravity compensation and leads moving targets. */
public class BowAimbot extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max target distance", 60, 10, 120, 5));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", true));
    private final BoolSetting predict = add(new BoolSetting("Predict", "Lead moving targets", true));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", "Allow targets you can't see", false));

    private Entity target;

    public BowAimbot() { super("BowAimbot", "Perfect bow aim with drop and movement prediction", Category.COMBAT); }

    @Override public String info() { return target != null ? target.getName().getString() : null; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var item = mc.player.getMainHandItem().getItem();
        boolean bow = item instanceof BowItem, crossbow = item instanceof CrossbowItem;
        if (!(bow && mc.player.isUsingItem()) && !crossbow) { target = null; return; }

        target = null; double best = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!Targets.valid(e, players.on(), mobs.on(), false)) continue;
            double d = e.distanceTo(mc.player);
            if (d > range.d() || (!throughWalls.on() && !mc.player.hasLineOfSight(e))) continue;
            double score = d + com.natan.vortex.util.Rotations.angleTo(e) * 0.5;
            if (score < best) { best = score; target = e; }
        }
        if (target == null) return;

        float power = bow ? BowItem.getPowerForTime(mc.player.getTicksUsingItem()) : 1f;
        if (power < 0.1f) return;
        double v = power * (crossbow ? 3.15 : 3.0);
        Vec3 eye = mc.player.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter();
        double dist = Math.hypot(aim.x - eye.x, aim.z - eye.z);
        if (predict.on() && target instanceof LivingEntity) {
            double flight = dist / v; // ticks, roughly
            Vec3 vel = target.getDeltaMovement();
            aim = aim.add(vel.x * flight, Math.max(0, vel.y) * flight, vel.z * flight);
            dist = Math.hypot(aim.x - eye.x, aim.z - eye.z);
        }
        double dy = aim.y - eye.y;
        double g = 0.05;
        double v2 = v * v;
        double root = v2 * v2 - g * (g * dist * dist + 2 * dy * v2);
        if (root < 0) return; // out of range
        float pitch = (float) -Math.toDegrees(Math.atan((v2 - Math.sqrt(root)) / (g * dist)));
        float yaw = (float) Math.toDegrees(Math.atan2(aim.z - eye.z, aim.x - eye.x)) - 90f;
        mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot()));
        mc.player.setXRot(Mth.clamp(pitch, -90, 90));
    }
}

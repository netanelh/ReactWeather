package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.util.Rotations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Avoid: never accidentally look an enderman in the eyes. Stare: aggro the nearest one (XP farming). */
public class EndermanLook extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Avoid or Stare", "Avoid", "Avoid", "Stare"));

    public EndermanLook() { super("EndermanLook", "Avoid or provoke endermen with your gaze", Category.WORLD); }
    @Override public String info() { return mode.get(); }

    private static boolean enderman(Entity e) { return e != null && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("enderman"); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (mode.is("Avoid")) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!enderman(e) || e.distanceToSqr(mc.player) > 64 * 64) continue;
                double a = Rotations.angleTo(e);
                if (a < 12 && mc.player.getXRot() < 60) mc.player.setXRot(Math.min(90, mc.player.getXRot() + 25));
            }
        } else {
            Entity best = null; double bd = 48 * 48;
            for (Entity e : mc.level.entitiesForRendering()) if (enderman(e) && e.distanceToSqr(mc.player) < bd) { bd = e.distanceToSqr(mc.player); best = e; }
            if (best != null) {
                float[] a = Rotations.anglesTo(best.getEyePosition());
                mc.player.setYRot(a[0]); mc.player.setXRot(a[1]);
            }
        }
    }
}

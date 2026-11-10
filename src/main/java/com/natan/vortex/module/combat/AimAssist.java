package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Rotations;
import com.natan.vortex.util.Targets;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public class AimAssist extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance", 5, 2, 8, 0.5));
    private final NumberSetting fov = add(new NumberSetting("FOV", "Only pull toward targets inside this cone", 70, 10, 180, 5));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Degrees per tick", 6, 1, 30, 0.5));
    private final BoolSetting onlyClicking = add(new BoolSetting("Only Clicking", "Only while holding attack", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Aim at players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Aim at hostile mobs", true));
    private final BoolSetting humanize = add(new BoolSetting("Humanize", "Add slight random jitter", true));

    public AimAssist() { super("AimAssist", "Gently pulls your aim toward the nearest target", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.gui.screen() != null) return;
        if (onlyClicking.on() && !mc.options.keyAttack.isDown()) return;
        Entity best = null; double bestAngle = fov.d() / 2;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!Targets.valid(e, players.on(), mobs.on(), false) || e.distanceTo(mc.player) > range.d()) continue;
            double a = Rotations.angleTo(e);
            if (a < bestAngle && mc.player.hasLineOfSight(e)) { bestAngle = a; best = e; }
        }
        if (best == null) return;
        float[] t = Rotations.anglesTo(Rotations.aimPoint(best));
        float s = speed.f();
        if (humanize.on()) { s *= 0.7f + ThreadLocalRandom.current().nextFloat() * 0.6f; t[1] += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 2; }
        Rotations.stepClient(t, s);
    }
}

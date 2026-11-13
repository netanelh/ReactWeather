package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/** Finds the nearest matching mob, walks to it and kills it, then the next one. ".hunt zombie" / ".hunt hostiles". */
public class HuntProcess implements Process {
    private final Predicate<Entity> filter;
    private final String label;
    private final int limit;
    private LivingEntity target;
    private int kills, searchCooldown;
    private Goal lastGoal;
    private net.minecraft.core.BlockPos lastPos;
    private LivingEntity lastTarget;
    private final java.util.Set<Integer> ignored = new java.util.HashSet<>(); // unreachable mobs

    public HuntProcess(Predicate<Entity> filter, String label, int limit) { this.filter = filter; this.label = label; this.limit = limit; }

    public static Predicate<Entity> byName(String what) {
        String w = what.toLowerCase();
        return switch (w) {
            case "hostiles", "monsters", "mobs" -> com.natan.vortex.util.Targets::isHostile;
            case "animals", "passives" -> e -> e instanceof LivingEntity && com.natan.vortex.util.Targets.isPassive(e);
            default -> e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals(w);
        };
    }

    @Override public String name() { return "Hunt"; }
    @Override public String status() { return label + " · " + kills + (limit > 0 ? "/" + limit : "") + " killed" + (target == null ? " · searching" : ""); }
    @Override public boolean isDone() { return limit > 0 && kills >= limit; }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (target != null && (!target.isAlive() || target.isRemoved())) {
            if (target.isDeadOrDying() || target.getHealth() <= 0) kills++;
            target = null;
        }
        if (target == null && searchCooldown-- <= 0) {
            searchCooldown = 10;
            double best = 64;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity le) || e == mc.player || e instanceof Player || !le.isAlive() || !filter.test(e)) continue;
                if (e.hasCustomName()) continue; // never hunt named pets
                if (ignored.contains(e.getId())) continue;
                double d = e.distanceTo(mc.player);
                if (d < best) { best = d; target = le; }
            }
        }
        // attack whenever in reach (also while walking)
        if (target != null && target.distanceTo(mc.player) < 3.2 && mc.player.hasLineOfSight(target)) {
            Actions.lookAt(Rotations.aimPoint(target), 60);
            if (mc.player.getAttackStrengthScale(0.5f) >= 1f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @Override
    public boolean onPathFailed() {
        if (target == null) return false;
        ignored.add(target.getId()); target = null; lastGoal = null; lastTarget = null;
        return true;
    }

    @Override
    public Goal goal() {
        if (target == null) { lastGoal = null; lastTarget = null; return null; }
        var p = target.blockPosition();
        // only re-plan when the target really moved, so the walk doesn't stutter every tick
        if (lastGoal == null || target != lastTarget || lastPos.distSqr(p) > 4) { lastTarget = target; lastPos = p; lastGoal = new Goal.Near(p, 2.0); }
        return lastGoal;
    }
}

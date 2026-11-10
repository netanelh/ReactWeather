package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Rotations;
import com.natan.vortex.util.Targets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Attack reach in blocks", 3.6, 1, 6, 0.1));
    private final NumberSetting wallRange = add(new NumberSetting("Wall Range", "Reach when target is behind blocks", 0, 0, 6, 0.1));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Target hostile mobs", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", "Target animals and other passive mobs", false));
    private final ModeSetting priority = add(new ModeSetting("Priority", "Which target goes first", "Angle", "Angle", "Distance", "Health", "Armor"));
    private final NumberSetting fov = add(new NumberSetting("FOV", "Only attack targets inside this view cone", 360, 30, 360, 5));
    private final ModeSetting rotate = add(new ModeSetting("Rotate", "Silent = server only, Client = moves your camera", "Silent", "Silent", "Client", "None"));
    private final NumberSetting turnSpeed = add(new NumberSetting("Turn Speed", "Max degrees per tick for Client rotation", 90, 10, 180, 5).visibleWhen(() -> rotate.is("Client")));
    private final BoolSetting cooldown = add(new BoolSetting("Cooldown", "Wait for a full-strength swing (max damage)", true));
    private final NumberSetting cps = add(new NumberSetting("CPS", "Clicks per second when Cooldown is off", 12, 1, 20, 1).visibleWhen(() -> !cooldown.on()));
    private final BoolSetting randomize = add(new BoolSetting("Randomize", "Jitters click timing", true).visibleWhen(() -> !cooldown.on()));
    private final NumberSetting maxTargets = add(new NumberSetting("Targets", "How many entities to hit per swing (1 = single)", 1, 1, 6, 1));
    private final BoolSetting onlyWeapon = add(new BoolSetting("Only Weapon", "Only attack while holding a sword, axe, mace or trident", false));
    private final BoolSetting pauseOnUse = add(new BoolSetting("Pause On Use", "Stop while eating, blocking or mining", true));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Weapon", "Switch to the best hotbar weapon before hitting", false));
    private final BoolSetting predict = add(new BoolSetting("Predict", "Aim where moving targets will be (beats strafing)", true));
    private final BoolSetting ignoreNamed = add(new BoolSetting("Ignore Named", "Don't hit mobs with a name tag (pets)", true));

    private Entity target;

    public Entity target() { return isEnabled() ? target : null; }
    private double clickTimer;

    public KillAura() { super("KillAura", "Attacks entities around you automatically", Category.COMBAT); }

    @Override public void onDisable() { target = null; }

    @Override
    public String info() { return target != null ? target.getName().getString() : priority.get(); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.isDeadOrDying()) { target = null; return; }
        if (pauseOnUse.on() && (mc.player.isUsingItem() || mc.gameMode.isDestroying())) return;

        List<Entity> targets = findTargets();
        target = targets.isEmpty() ? null : targets.get(0);
        if (target == null) return;

        float[] angles = Rotations.anglesTo(aim(target));
        if (rotate.is("Client")) Rotations.stepClient(angles, turnSpeed.f());

        if (autoSwitch.on()) AutoWeapon.switchToBest(target);
        if (onlyWeapon.on() && !isWeapon(mc.player.getMainHandItem())) return;
        if (!readyToHit()) return;

        int hits = 0;
        for (Entity e : targets) {
            if (hits >= maxTargets.i()) break;
            if (rotate.is("Silent")) {
                float[] a = Rotations.anglesTo(aim(e));
                Rotations.sendSilent(a[0], a[1]);
            } else if (rotate.is("Client") && e == target && Rotations.angleTo(e) > 25) {
                continue; // camera hasn't reached the target yet
            }
            mc.gameMode.attack(mc.player, e);
            hits++;
        }
        if (hits > 0) mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private boolean readyToHit() {
        if (cooldown.on()) return mc.player.getAttackStrengthScale(0.5f) >= 1f;
        clickTimer -= 1;
        if (clickTimer > 0) return false;
        double interval = 20.0 / cps.d();
        if (randomize.on()) interval *= 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        clickTimer = interval;
        return true;
    }

    private List<Entity> findTargets() {
        List<Entity> list = new ArrayList<>();
        Vec3 eye = mc.player.getEyePosition();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!Targets.valid(e, players.on(), hostiles.on(), passives.on())) continue;
            if (ignoreNamed.on() && !(e instanceof net.minecraft.world.entity.player.Player) && e.hasCustomName()) continue;
            Vec3 p = Rotations.aimPoint(e);
            double dist = eye.distanceTo(p);
            boolean visible = mc.player.hasLineOfSight(e);
            if (dist > (visible ? range.d() : wallRange.d())) continue;
            if (fov.d() < 360 && Rotations.angleTo(e) > fov.d() / 2) continue;
            list.add(e);
        }
        Comparator<Entity> cmp = switch (priority.get()) {
            case "Distance" -> Comparator.comparingDouble(e -> e.distanceToSqr(mc.player));
            case "Health" -> Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth() + ((LivingEntity) e).getAbsorptionAmount());
            case "Armor" -> Comparator.comparingDouble(e -> ((LivingEntity) e).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
            default -> Comparator.comparingDouble(Rotations::angleTo);
        };
        list.sort(cmp);
        return list;
    }

    private Vec3 aim(Entity e) {
        Vec3 p = Rotations.aimPoint(e);
        if (!predict.on()) return p;
        Vec3 v = e.getDeltaMovement();
        return p.add(v.x * 2, 0, v.z * 2); // ~2 ticks of latency + swing
    }

    static boolean isWeapon(ItemStack s) {
        String id = com.natan.vortex.util.Items2.id(s);
        return id.contains("sword") || id.contains("_axe") || id.contains("mace") || id.contains("trident") || id.contains("spear");
    }
}

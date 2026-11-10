package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Rotations;
import com.natan.vortex.util.Targets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Spear charge damage = how fast you move toward the target (the server measures it from your
 * movement packets). While you charge a spear (hold right click) and a target is in reach,
 * this sends a back-and-forward "dash" so the server sees a huge forward speed -> one-shot.
 */
public class SpearKill extends Module {
    private final NumberSetting dash = add(new NumberSetting("Dash", "Fake speed: blocks per packet (9 = ~180 blocks/s)", 9, 1, 9.5, 0.5));
    private final NumberSetting reach = add(new NumberSetting("Reach", "Target distance", 4.5, 2, 6, 0.5));
    private final BoolSetting aim = add(new BoolSetting("Auto Aim", "Turn toward the target while charging", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Target hostile mobs", true));
    private final BoolSetting passives = add(new BoolSetting("Animals", "Target animals", false));

    public SpearKill() { super("SpearKill", "Spear charges hit with huge speed (one-shot)", Category.COMBAT); }

    private boolean holdingSpear() {
        return BuiltInRegistries.ITEM.getKey(mc.player.getUseItem().getItem()).getPath().endsWith("_spear");
    }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.isUsingItem() || !holdingSpear()) return;
        Entity target = null; double best = reach.d() * reach.d();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!Targets.valid(e, players.on(), hostiles.on(), passives.on())) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < best && (aim.on() || Rotations.angleTo(e) < 25)) { best = d; target = e; }
        }
        if (target == null) return;
        if (aim.on()) {
            float[] a = Rotations.anglesTo(Rotations.aimPoint(target));
            mc.player.setYRot(a[0]); mc.player.setXRot(a[1]);
        }
        Vec3 look = mc.player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 back = flat.normalize().scale(-dash.d());
        if (!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(back))) {
            back = back.scale(0.5);
            if (!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(back))) return;
        }
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        boolean ground = mc.player.onGround();
        var c = mc.getConnection();
        c.send(new ServerboundMovePlayerPacket.Pos(x + back.x, y, z + back.z, ground, false));
        c.send(new ServerboundMovePlayerPacket.Pos(x, y, z, ground, false));
    }
}

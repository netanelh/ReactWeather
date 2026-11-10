package com.natan.vortex.module.render;

import com.mojang.authlib.GameProfile;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Fly the camera around freely while your body stays put (on the server too). */
public class Freecam extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Camera speed (blocks/tick)", 1.0, 0.1, 5, 0.1));
    private RemotePlayer cam;

    public Freecam() { super("Freecam", "Detached flying camera; your player doesn't move", Category.RENDER); }

    public boolean active() { return isEnabled() && cam != null; }
    public RemotePlayer camera() { return cam; }

    @Override
    public void onEnable() {
        if (!inGame()) { setEnabledSilently(false); return; }
        cam = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Freecam"));
        cam.snapTo(mc.player.getX(), mc.player.getEyeY() - cam.getEyeHeight(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot());
        cam.setOldPosAndRot();
        cam.noPhysics = true;
        mc.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
    }

    @Override
    public void onTick() {
        if (cam == null) return;
        if (!inGame()) { setEnabled(false); return; }
        cam.setOldPosAndRot();
        var o = mc.options;
        double f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double s = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        double sp = speed.d() * (o.keySprint.isDown() ? 2 : 1);
        double yaw = Math.toRadians(cam.getYRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 move = fwd.scale(f).add(left.scale(s)).add(0, up, 0);
        if (move.lengthSqr() > 0) move = move.normalize().scale(sp);
        cam.setPos(cam.position().add(move));
    }

    /** Mouse movement turns the camera instead of the player. */
    public boolean turn(double dx, double dy) {
        if (!active()) return false;
        cam.turn(dx, dy);
        return true;
    }
}

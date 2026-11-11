package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps movement under Paper's / vanilla's "moved too quickly" and "moved wrongly" thresholds, so
 * teleport-style and speed cheats don't rubberband on a Paper server without editing server configs.
 * Caps per-tick horizontal speed and the teleport hop size. Doesn't touch legit movement (sprinting
 * is far below the cap) or elytra/vehicles.
 */
public class PaperSafe extends Module {
    private final NumberSetting maxHop = add(new NumberSetting("TP Hop", "Max blocks per teleport packet (vanilla rejects > 10)", 9.0, 4, 9.8, 0.2));
    private final NumberSetting maxTick = add(new NumberSetting("Max Speed", "Cap on blocks moved per tick (0.28 = sprint)", 1.3, 0.3, 4, 0.1));

    public PaperSafe() { super("PaperSafe", "Stops rubberbanding on Paper by keeping movement under the server's limits", Category.CLIENT); }

    public double hop() { return maxHop.d(); }

    /** Called each tick after modules run: scale down any oversized horizontal velocity. */
    public void clampVelocity() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || p.isFallFlying() || p.isPassenger() || p.getAbilities().flying && p.isCreative()) return;
        Vec3 v = p.getDeltaMovement();
        double h = Math.hypot(v.x, v.z);
        double cap = maxTick.d();
        if (h > cap) {
            double s = cap / h;
            p.setDeltaMovement(v.x * s, v.y, v.z * s);
        }
    }
}

package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Every hit counts as a sprint hit (extra knockback). */
public class SuperKnockback extends Module {
    public SuperKnockback() { super("SuperKnockback", "Every hit gets sprint knockback", Category.COMBAT); }

    private boolean restore;

    public void beforeAttack(Entity target, boolean crit) {
        restore = false;
        if (!isEnabled() || crit || !(target instanceof LivingEntity)) return;
        var c = mc.getConnection();
        restore = !mc.player.isSprinting(); // client won't send STOP itself if it never thought we sprinted
        if (mc.player.isSprinting()) c.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        com.natan.vortex.module.player.AntiHunger.bypass = true;
        try { c.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING)); }
        finally { com.natan.vortex.module.player.AntiHunger.bypass = false; }
    }

    public void afterAttack() {
        if (!restore || mc.getConnection() == null || mc.player == null) return;
        restore = false;
        if (!mc.player.isSprinting()) mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
    }
}

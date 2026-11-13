package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Called from MultiPlayerGameModeMixin right before every attack. */
public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet = instant fake hop, Jump = hop so the next hit lands while falling", "Packet", "Packet", "Jump"));
    private final BoolSetting onlyFullCharge = add(new BoolSetting("Only Charged", "Skip crits on weak (uncharged) hits", true));

    public Criticals() { super("Criticals", "Turns your hits into critical hits (wins over SuperKnockback on crit hits)", Category.COMBAT); }

    private boolean restoreSprint;

    @Override public String info() { return mode.get(); }

    /** @return true if this hit was turned into a crit attempt (SuperKnockback then skips it). */
    public boolean beforeAttack(Entity target) {
        if (!isEnabled() || !inGame() || !(target instanceof LivingEntity)) return false;
        var p = mc.player;
        if (!p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger()) return false;
        if (onlyFullCharge.on() && p.getAttackStrengthScale(0.5f) < 0.9f) return false;
        // Jump: hop now so the NEXT hit lands while falling; this hit goes through normally (SuperKnockback still applies)
        if (mode.is("Jump")) { p.jumpFromGround(); return false; }
        // the server never crits a sprinting player: stop sprinting for this hit, restore afterwards
        if (p.isSprinting()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            restoreSprint = true;
        }

        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hc = p.horizontalCollision;
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, hc));
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.000011, z, false, hc));
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, hc));
        return true;
    }

    /** Called after the attack packet went out. */
    public void afterAttack() {
        if (!restoreSprint) return;
        restoreSprint = false;
        if (mc.player != null && mc.getConnection() != null && mc.player.isSprinting())
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }
}

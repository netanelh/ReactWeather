package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Called from MultiPlayerGameModeMixin right before every attack. */
public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet = instant fake hop, Jump = real mini jump", "Packet", "Packet", "Jump"));
    private final BoolSetting onlyFullCharge = add(new BoolSetting("Only Charged", "Skip crits on weak (uncharged) hits", true));

    public Criticals() { super("Criticals", "Turns your hits into critical hits", Category.COMBAT); }

    @Override public String info() { return mode.get(); }

    public void beforeAttack(Entity target) {
        if (!isEnabled() || !inGame() || !(target instanceof LivingEntity)) return;
        var p = mc.player;
        if (!p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger()) return;
        if (onlyFullCharge.on() && p.getAttackStrengthScale(0.5f) < 0.9f) return;

        if (mode.is("Jump")) {
            p.jumpFromGround();
            return;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hc = p.horizontalCollision;
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, hc));
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.000011, z, false, hc));
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, hc));
    }
}

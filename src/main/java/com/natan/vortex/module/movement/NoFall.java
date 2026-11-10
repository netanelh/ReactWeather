package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall", "Fall distance (blocks) before it kicks in", 2.5, 2, 10, 0.5));

    public NoFall() { super("NoFall", "Prevents fall damage", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (p.getAbilities().instabuild || p.isFallFlying()) return;
        if (p.fallDistance > minFall.d() && p.getDeltaMovement().y < 0) {
            // Telling the server we touched the ground resets its fall-distance counter
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(p.getX(), p.getY(), p.getZ(), true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }
}

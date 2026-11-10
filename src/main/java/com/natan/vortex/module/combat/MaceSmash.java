package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;

/** Mace hits act like you fell from high up (smash damage) without leaving the ground. */
public class MaceSmash extends Module {
    private final NumberSetting height = add(new NumberSetting("Height", "Fake fall height in blocks (more = more damage)", 10, 2, 60, 1));

    public MaceSmash() { super("MaceSmash", "Mace hits get full fall-smash damage from the ground", Category.COMBAT); }

    public void beforeAttack(Entity target) {
        if (!isEnabled() || !mc.player.getMainHandItem().is(Items.MACE)) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0, height.d(), 0))) return; // need room above
        var c = mc.getConnection();
        int hops = (int) Math.ceil(height.d() / 9.0);
        for (int i = 1; i <= hops; i++) c.send(new ServerboundMovePlayerPacket.Pos(x, y + height.d() * i / hops, z, false, false));
        for (int i = hops - 1; i >= 0; i--) c.send(new ServerboundMovePlayerPacket.Pos(x, y + height.d() * i / hops + (i == 0 ? 0.01 : 0), z, false, false));
    }
}

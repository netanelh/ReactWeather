package com.natan.vortex.module.player;

import com.mojang.authlib.GameProfile;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Spawns a client-side copy of you to test KillAura, CrystalAura, ESP... without another player. */
public class FakePlayer extends Module {
    public static final UUID ID = UUID.fromString("7a1c0000-0000-4000-8000-00000000d0d0");
    private RemotePlayer dummy;

    public FakePlayer() { super("FakePlayer", "Spawns a dummy player to test combat modules", Category.PLAYER); }

    @Override
    public void onEnable() {
        if (!inGame()) { setEnabledSilently(false); return; }
        dummy = new RemotePlayer(mc.level, new GameProfile(ID, "ZephexDummy"));
        dummy.setId(-424242);
        dummy.snapTo(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot());
        dummy.setOldPosAndRot();
        dummy.setHealth(20);
        mc.level.addEntity(dummy);
    }

    @Override
    public void onDisable() {
        if (dummy != null && mc.level != null) mc.level.removeEntity(dummy.getId(), Entity.RemovalReason.DISCARDED);
        dummy = null;
    }
}

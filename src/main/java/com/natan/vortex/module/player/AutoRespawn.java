package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends Module {
    public AutoRespawn() { super("AutoRespawn", "Respawns instantly when you die", Category.PLAYER); }

    @Override
    public void onTick() {
        if (inGame() && mc.gui.screen() instanceof DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
    }
}

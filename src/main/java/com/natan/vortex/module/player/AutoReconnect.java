package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class AutoReconnect extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Seconds before reconnecting", 5, 1, 60, 1));
    public static ServerData lastServer; // captured on join by VortexClient

    private int timer = -1;

    public AutoReconnect() { super("AutoReconnect", "Reconnects automatically when you get kicked", Category.PLAYER); }

    @Override
    public void onTick() {
        if (mc.player != null && mc.getCurrentServer() != null) lastServer = mc.getCurrentServer();
    }

    /** Called every client tick (also while disconnected). */
    public void screenTick() {
        if (!isEnabled() || lastServer == null) { timer = -1; return; }
        if (!(mc.gui.screen() instanceof DisconnectedScreen)) { timer = -1; return; }
        if (timer < 0) timer = delay.i() * 20;
        if (--timer > 0) return;
        timer = -1;
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(lastServer.ip), lastServer, false, null);
    }

    public int secondsLeft() { return timer < 0 ? -1 : timer / 20 + 1; }
}

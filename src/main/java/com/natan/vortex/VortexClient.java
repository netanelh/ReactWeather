package com.natan.vortex;

import com.natan.vortex.config.ConfigManager;
import com.natan.vortex.gui.ClickGuiScreen;
import com.natan.vortex.hud.HudRenderer;
import com.natan.vortex.module.ModuleManager;
import com.natan.vortex.util.Keys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VortexClient implements ClientModInitializer {
    public static final String MOD_ID = "vortex";
    public static final String NAME = "Zephex";
    public static final String VERSION = "1.2.2";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    public static Minecraft mc;
    public static ModuleManager modules;
    public static ConfigManager config;

    @Override
    public void onInitializeClient() {
        mc = Minecraft.getInstance();
        modules = new ModuleManager();
        config = new ConfigManager();
        config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { config.save(); com.natan.vortex.memory.WorldMemory.INSTANCE.save(); com.natan.vortex.nav.NavMemory.INSTANCE.save(); });
        com.natan.vortex.command.Commands.register();

        LOG.info("{} (vortex) {} loaded with {} modules", NAME, VERSION, modules.all().size());
    }

    private static Object lastLevel;

    private void onTick() {
        if (mc.level != lastLevel) { lastLevel = mc.level; com.natan.vortex.module.render.ServerLag.reset(); }
        java.util.List<Integer> keys = new java.util.ArrayList<>();
        for (var m : modules.all()) keys.add(m.key);
        java.util.Set<Integer> fresh = Keys.update(keys);

        // Keybinds only fire with no screen open, so typing in chat doesn't toggle modules
        if (mc.gui.screen() == null && mc.player != null && !fresh.isEmpty()) {
            for (var m : modules.all()) {
                if (!fresh.contains(m.key)) continue;
                if (m == modules.clickGui()) mc.gui.setScreen(new ClickGuiScreen());
                else m.toggle();
            }
        }
        modules.onTick();
        com.natan.vortex.memory.WorldMemory.INSTANCE.tick();
        com.natan.vortex.memory.ChestMemory.INSTANCE.tick();
        com.natan.vortex.nav.NavMemory.INSTANCE.tick();
        modules.get(com.natan.vortex.module.client.PaperSafe.class).clampVelocity();
        com.natan.vortex.seed.SeedCracker.tick();
        modules.get(com.natan.vortex.module.player.AutoReconnect.class).screenTick();
        if (modules.get(com.natan.vortex.module.pathing.NavSettings.class).isEnabled()) com.natan.vortex.nav.Navigator.INSTANCE.tick();
    }
}

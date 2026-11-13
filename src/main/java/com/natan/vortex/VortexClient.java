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
    public static final String VERSION = "1.7.1";
    /** Sodium replaces vanilla chunk meshing, so terrain-hiding hooks never run with it. */
    public static final boolean SODIUM = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium");
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
        com.natan.vortex.command.ClientCommands.register();

        LOG.info("{} (vortex) {} loaded with {} modules", NAME, VERSION, modules.all().size());
    }

    private static Object lastLevel;
    private static final java.util.Set<com.natan.vortex.module.Module> holdLatched = new java.util.HashSet<>();

    private void onTick() {
        if (mc.level != lastLevel) { lastLevel = mc.level; com.natan.vortex.module.render.ServerLag.reset();
            holdLatched.clear(); com.natan.vortex.util.RotationManager.reset();
            if (modules != null) modules.onWorldChange(); }
        java.util.List<Integer> keys = new java.util.ArrayList<>();
        for (var m : modules.all()) keys.add(m.key);
        java.util.Set<Integer> fresh = Keys.update(keys);

        // Keybinds only fire with no screen open, so typing in chat doesn't toggle modules
        if (mc.gui.screen() == null && mc.player != null && !fresh.isEmpty()) {
            for (var m : modules.all()) {
                if (!fresh.contains(m.key) || (m.holdBind && m != modules.clickGui())) continue;
                if (m == modules.clickGui()) mc.gui.setScreen(new ClickGuiScreen());
                else m.toggle();
            }
        }
        // hold-mode binds: on while the key is held (and no screen is open), off when released
        if (mc.player != null) for (var m : modules.all()) {
            if (!m.holdBind || m.key == Keys.NONE || m == modules.clickGui()) continue;
            boolean down = mc.gui.screen() == null && Keys.isDown(m.key);
            // latch: enable once per press (a module that turns itself off isn't restarted every tick),
            // disable only on release of a press that enabled it (GUI/.toggle enables are left alone)
            if (down && fresh.contains(m.key) && !m.isEnabled()) { m.setEnabled(true); holdLatched.add(m); }
            else if (!down && holdLatched.remove(m) && m.isEnabled()) m.setEnabled(false);
        }
        modules.onTick();
        com.natan.vortex.util.RotationManager.endTick();
        com.natan.vortex.memory.WorldMemory.INSTANCE.tick();
        com.natan.vortex.memory.ChestMemory.INSTANCE.tick();
        com.natan.vortex.nav.NavMemory.INSTANCE.tick();
        modules.get(com.natan.vortex.module.client.PaperSafe.class).clampVelocity();
        com.natan.vortex.seed.SeedCracker.tick();
        modules.get(com.natan.vortex.module.player.AutoReconnect.class).screenTick();
        if (modules.get(com.natan.vortex.module.pathing.NavSettings.class).isEnabled()) com.natan.vortex.nav.Navigator.INSTANCE.tick();
    }
}

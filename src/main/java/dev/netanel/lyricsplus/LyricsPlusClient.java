package dev.netanel.lyricsplus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.gui.LyricsConfigScreen;
import dev.netanel.lyricsplus.render.HudRenderer;
import dev.netanel.lyricsplus.render.WorldLyrics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class LyricsPlusClient implements ClientModInitializer {

    public static final String MOD_ID = "lyricsplus";

    private static final LyricsEngine ENGINE = new LyricsEngine();

    private static boolean openPanelNextTick;
    private static KeyMapping openPanel;
    private static KeyMapping toggleLyrics;
    private static KeyMapping offsetDown;
    private static KeyMapping offsetUp;

    public static LyricsEngine engine() {
        return ENGINE;
    }

    @Override
    public void onInitializeClient() {
        LyricsConfig.get();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openPanel = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyricsplus.panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, category));
        toggleLyrics = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyricsplus.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        offsetDown = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyricsplus.earlier", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, category));
        offsetUp = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyricsplus.later", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, category));

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"),
                HudRenderer::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("lyrics").executes(context -> {
                    openPanelNextTick = true; // the chat screen closes right after the command runs
                    return 1;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        ENGINE.tick();
        LyricsConfig cfg = LyricsConfig.get();

        if (openPanelNextTick) {
            openPanelNextTick = false;
            mc.gui.setScreen(new LyricsConfigScreen(null));
        }
        while (openPanel.consumeClick()) {
            mc.gui.setScreen(new LyricsConfigScreen(mc.gui.screen()));
        }
        while (toggleLyrics.consumeClick()) {
            cfg.enabled = !cfg.enabled;
            cfg.save();
            notify(mc, Component.translatable(cfg.enabled ? "lyricsplus.msg.shown" : "lyricsplus.msg.hidden"));
        }
        while (offsetDown.consumeClick()) {
            changeOffset(mc, cfg, -100);
        }
        while (offsetUp.consumeClick()) {
            changeOffset(mc, cfg, 100);
        }

        WorldLyrics.tick(ENGINE.snapshot());
    }

    private static void changeOffset(Minecraft mc, LyricsConfig cfg, int delta) {
        cfg.offsetMs = Math.max(-5000, Math.min(5000, cfg.offsetMs + delta));
        cfg.save();
        notify(mc, Component.translatable("lyricsplus.msg.offset",
                String.format(Locale.ROOT, "%+.1fs", cfg.offsetMs / 1000.0)));
    }

    private static void notify(Minecraft mc, Component message) {
        if (mc.player != null) {
            mc.player.sendOverlayMessage(message);
        }
    }
}

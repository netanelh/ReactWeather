package com.natan.vortex.module;

import com.natan.vortex.setting.Setting;
import com.natan.vortex.util.Keys;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();
    public int key = Keys.NONE;
    public boolean showInHud = true;
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) { settings.add(setting); return setting; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        try {
            if (value) onEnable(); else onDisable();
            var mods = com.natan.vortex.VortexClient.modules;
            if (mods != null && enabled == value && mods.get(com.natan.vortex.module.player.Notifier.class).moduleToggles() && showInHud)
                com.natan.vortex.util.Chat.info(name + (value ? " \u00a7aON" : " \u00a7cOFF"));
        } catch (Throwable t) {
            com.natan.vortex.VortexClient.LOG.error("Error toggling {}", name, t);
        }
    }

    /** Flip the state without running onEnable/onDisable (used when something else already stopped the work). */
    public void setEnabledSilently(boolean value) { enabled = value; }

    protected boolean inGame() { return mc.player != null && mc.level != null && mc.gameMode != null; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    /** Short suffix shown in the HUD module list, e.g. the current mode. */
    public String info() { return null; }
}

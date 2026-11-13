package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;

/** Hooks in HudMixin. */
public class NoOverlay extends Module {
    public final BoolSetting pumpkin = add(new BoolSetting("Pumpkin", "No pumpkin head overlay", true).group("Overlays"));
    public final BoolSetting portal = add(new BoolSetting("Portal", "No nether portal swirl", true).group("Overlays"));
    public final BoolSetting vignette = add(new BoolSetting("Vignette", "No dark screen edges", true).group("Overlays"));
    public final BoolSetting spyglass = add(new BoolSetting("Spyglass", "No spyglass mask", false).group("Overlays"));
    public final BoolSetting effects = add(new BoolSetting("Effect Icons", "Hide the vanilla potion icons", false).group("Overlays"));
    public final BoolSetting scoreboard = add(new BoolSetting("Scoreboard", "Hide the sidebar scoreboard", false).group("Overlays"));
    public final BoolSetting titles = add(new BoolSetting("Titles", "Hide big title text", false).group("Overlays"));

    public NoOverlay() { super("NoOverlay", "Removes pumpkin, portal, vignette and other overlays", Category.RENDER); }

    public boolean on(BoolSetting s) { return isEnabled() && s.on(); }
}

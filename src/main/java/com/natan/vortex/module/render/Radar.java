package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;

/** Drawn by HudRenderer. */
public class Radar extends Module {
    public final NumberSetting size = add(new NumberSetting("Size", "Radar size in pixels", 90, 50, 180, 5));
    public final NumberSetting range = add(new NumberSetting("Range", "Blocks shown from center to edge", 48, 16, 128, 8));
    public final ModeSetting position = add(new ModeSetting("Position", "Screen corner", "Top Left", "Top Left", "Bottom Right", "Bottom Left"));
    public final BoolSetting players = add(new BoolSetting("Players", "Show players", true).group("Show"));
    public final BoolSetting names = add(new BoolSetting("Names", "Label players with names", true).group("Show"));
    public final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Show hostile mobs", true).group("Show"));
    public final BoolSetting passives = add(new BoolSetting("Passives", "Show animals", false).group("Show"));
    public final BoolSetting items = add(new BoolSetting("Items", "Show dropped items", false).group("Show"));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", "Radar turns with you (up = where you look)", true));

    public Radar() { super("Radar", "Mini radar of nearby players and mobs", Category.RENDER); showInHud = false; }
}

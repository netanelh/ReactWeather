package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

/** HUD list of the nearest remembered valuables with distance and a direction arrow (drawn by HudElements). */
public class BlockRadar extends Module {
    public final BoolSetting diamonds = add(new BoolSetting("Diamonds", "Diamond ore", true));
    public final BoolSetting debris = add(new BoolSetting("Ancient Debris", "Netherite", true));
    public final BoolSetting emeralds = add(new BoolSetting("Emeralds", "Emerald ore", false));
    public final BoolSetting gold = add(new BoolSetting("Gold", "Gold ore", false));
    public final BoolSetting spawners = add(new BoolSetting("Spawners", "Mob and trial spawners", true));
    public final BoolSetting storage = add(new BoolSetting("Storage", "Chests, barrels, shulkers", false));
    public final BoolSetting portals = add(new BoolSetting("Portals", "Nether portals, end portal frames", true));
    public final NumberSetting lines = add(new NumberSetting("Lines", "How many to show", 5, 1, 12, 1));

    public BlockRadar() { super("BlockRadar", "Shows the nearest diamonds/spawners/portals with distance + arrow", Category.RENDER); showInHud = false; }
}

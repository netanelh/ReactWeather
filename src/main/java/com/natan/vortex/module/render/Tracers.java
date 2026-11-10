package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

/** Arrows around the crosshair pointing at nearby players (drawn by HudMore). */
public class Tracers extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", "Max distance", 128, 16, 512, 16));
    public final BoolSetting names = add(new BoolSetting("Names", "Show name and distance", true));
    public final BoolSetting friends = add(new BoolSetting("Friends", "Include friends (shown in green)", true));
    public final NumberSetting radius = add(new NumberSetting("Radius", "Distance from the crosshair", 45, 20, 120, 5));

    public Tracers() { super("Tracers", "On-screen arrows pointing at players", Category.RENDER); showInHud = false; }
}

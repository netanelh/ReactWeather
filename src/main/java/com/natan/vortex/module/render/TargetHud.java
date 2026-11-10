package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Drawn by HudRenderer: health, armor and distance of your current combat target. */
public class TargetHud extends Module {
    public TargetHud() { super("TargetHUD", "Shows your target's health, armor and distance", Category.RENDER); showInHud = false; }
}

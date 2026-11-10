package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class ClockHud extends Module {
    public ClockHud() { super("Clock", "Real-world time and in-game day", Category.RENDER); showInHud = false; }
}

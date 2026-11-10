package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class Compass extends Module {
    public Compass() { super("Compass", "Facing direction and heading in degrees", Category.RENDER); showInHud = false; }
}

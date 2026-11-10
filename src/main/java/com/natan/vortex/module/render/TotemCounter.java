package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class TotemCounter extends Module {
    public TotemCounter() { super("TotemCounter", "How many totems you have left", Category.RENDER); showInHud = false; }
}

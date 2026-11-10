package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class PingHud extends Module {
    public PingHud() { super("Ping", "Your ping to the server", Category.RENDER); showInHud = false; }
}

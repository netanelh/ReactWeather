package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class Keystrokes extends Module {
    public Keystrokes() { super("Keystrokes", "WASD, space and mouse buttons with CPS", Category.RENDER); showInHud = false; }
}

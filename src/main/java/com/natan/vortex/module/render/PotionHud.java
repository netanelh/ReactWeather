package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class PotionHud extends Module {
    public PotionHud() { super("PotionHUD", "Lists your active effects with time left", Category.RENDER); showInHud = false; }
}

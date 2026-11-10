package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class InventoryHud extends Module {
    public InventoryHud() { super("InventoryHUD", "Shows your inventory contents on screen", Category.RENDER); showInHud = false; }
}

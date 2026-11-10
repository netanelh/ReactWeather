package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class NearbyPlayers extends Module {
    public NearbyPlayers() { super("NearbyPlayers", "List of nearby players with health and distance", Category.RENDER); showInHud = false; }
}

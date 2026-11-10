package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class ServerInfo extends Module {
    public ServerInfo() { super("ServerInfo", "Server address and players online", Category.RENDER); showInHud = false; }
}

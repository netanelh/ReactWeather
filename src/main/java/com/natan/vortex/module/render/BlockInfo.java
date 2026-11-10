package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** HUD element, drawn by HudElements. */
public class BlockInfo extends Module {
    public BlockInfo() { super("BlockInfo", "Name and position of the block you look at", Category.RENDER); showInHud = false; }
}

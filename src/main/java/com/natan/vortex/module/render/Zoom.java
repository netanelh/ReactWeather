package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** Hold the module's key to zoom (CameraMixin). */
public class Zoom extends Module {
    public final NumberSetting factor = add(new NumberSetting("Factor", "How much to zoom", 4, 1.5, 15, 0.5));

    public Zoom() { super("Zoom", "OptiFine-style zoom (works as toggle or bind)", Category.RENDER); showInHud = false; }

    public float apply(float fov) { return isEnabled() ? fov / factor.f() : fov; }
}

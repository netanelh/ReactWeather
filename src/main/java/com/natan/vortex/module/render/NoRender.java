package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;

/** Hides annoying effects. */
public class NoRender extends Module {
    public final BoolSetting hurtCam = add(new BoolSetting("Hurt Cam", "No screen shake when hit", true));
    public final BoolSetting totem = add(new BoolSetting("Totem Animation", "No big totem on screen when you pop", true));
    public final BoolSetting particles = add(new BoolSetting("All Particles", "No particles at all (big FPS boost)", false));

    public NoRender() { super("NoRender", "Hides hurt cam, totem animation, particles", Category.RENDER); }
    public boolean on(BoolSetting s) { return isEnabled() && s.on(); }
}

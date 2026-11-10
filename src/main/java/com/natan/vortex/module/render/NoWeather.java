package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

public class NoWeather extends Module {
    public NoWeather() { super("NoWeather", "Hides rain, snow and thunder on your screen", Category.RENDER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        mc.level.setRainLevel(0);
        mc.level.setThunderLevel(0);
    }
}

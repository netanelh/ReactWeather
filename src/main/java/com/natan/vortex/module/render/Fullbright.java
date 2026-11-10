package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.ModeSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Gamma = lightmap override, Potion = client-side night vision", "Gamma", "Gamma", "Potion"));

    public Fullbright() { super("Fullbright", "See everything as if it were daytime", Category.RENDER); }

    @Override public String info() { return mode.get(); }

    /** Read by LightmapMixin. */
    public boolean gamma() { return isEnabled() && mode.is("Gamma"); }

    private boolean ours;

    @Override
    public void onTick() {
        if (!inGame()) return;
        var nv = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value());
        if (mode.is("Potion")) {
            var cur = mc.player.getEffect(nv);
            if (cur == null || ours && cur.getDuration() < 220) {
                mc.player.addEffect(new MobEffectInstance(nv, 420, 0, false, false));
                ours = true;
            }
        } else if (ours) {
            mc.player.removeEffect(nv);
            ours = false;
        }
    }

    @Override
    public void onDisable() {
        if (inGame() && ours) mc.player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()));
        ours = false;
    }
}

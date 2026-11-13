package com.natan.vortex.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** With Xray on, force every face of the blocks that still render (the targets) to draw, so buried ores show. */
@Mixin(AbstractBlockRenderContext.class)
public class SodiumBlockContextMixin {
    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"), require = 0)
    private boolean vortex$alwaysDraw(boolean original) {
        if (VortexClient.modules == null) return original;
        Xray xray = VortexClient.modules.get(Xray.class);
        return (xray.isEnabled() && xray.realXray()) || original;
    }
}

package com.natan.vortex.mixin.sodium;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Real Xray with Sodium: don't mesh water/lava, so lakes and oceans don't cover ores. */
@Mixin(DefaultFluidRenderer.class)
public class SodiumFluidRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void vortex$xray(CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        Xray xray = VortexClient.modules.get(Xray.class);
        if (xray.isEnabled() && xray.realXray()) ci.cancel();
    }
}

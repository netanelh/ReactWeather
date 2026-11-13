package com.natan.vortex.mixin.sodium;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Real Xray with Sodium: turn off section occlusion culling so buried ores aren't culled away. */
@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean vortex$noOcclusion(boolean useOcclusionCulling) {
        if (VortexClient.modules == null) return useOcclusionCulling;
        Xray xray = VortexClient.modules.get(Xray.class);
        return useOcclusionCulling && !(xray.isEnabled() && xray.realXray());
    }
}

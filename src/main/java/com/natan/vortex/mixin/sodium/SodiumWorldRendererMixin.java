package com.natan.vortex.mixin.sodium;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Real Xray with Sodium: turn off section occlusion culling so buried ores aren't culled away. */
@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean vortex$noOcclusion(boolean useOcclusionCulling) {
        if (VortexClient.modules == null) return useOcclusionCulling;
        Xray xray = VortexClient.modules.get(Xray.class);
        return useOcclusionCulling && !(xray.isEnabled() && xray.realXray());
    }

    /** ESP: Sodium culls entities in occluded sections; keep highlighted ones rendering so their glow shows through walls. */
    @Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true, require = 0)
    private void vortex$espVisible(net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || VortexClient.modules == null) return;
        if (VortexClient.modules.get(com.natan.vortex.module.render.Esp.class).glowColor(entity) != null) cir.setReturnValue(true);
    }
}

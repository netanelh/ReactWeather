package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Xray (vanilla renderer): never mark blocks opaque, so chunk sections behind solid stone aren't culled. */
@Mixin(VisGraph.class)
public class VisGraphMixin {
    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true, require = 0)
    private void vortex$xray(BlockPos pos, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        Xray xray = VortexClient.modules.get(Xray.class);
        if (xray.isEnabled() && xray.hideTerrain()) ci.cancel();
    }
}

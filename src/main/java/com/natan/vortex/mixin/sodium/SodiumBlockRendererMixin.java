package com.natan.vortex.mixin.sodium;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Real Xray with Sodium: skip meshing any block that isn't an Xray target, so terrain turns invisible. */
@Mixin(BlockRenderer.class)
public class SodiumBlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, require = 0)
    private void vortex$xray(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        Xray xray = VortexClient.modules.get(Xray.class);
        if (xray.isEnabled() && xray.realXray() && !xray.shows(state.getBlock())) ci.cancel();
    }
}

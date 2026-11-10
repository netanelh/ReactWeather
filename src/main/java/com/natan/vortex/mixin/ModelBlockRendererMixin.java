package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    /** Xray: skip building geometry for every block that isn't on the list. */
    @Inject(method = {"tesselateFlat", "tesselateAmbientOcclusion"}, at = @At("HEAD"), cancellable = true)
    private void vortex$xrayHide(BlockQuadOutput output, float x, float y, float z, List<BlockStateModelPart> parts,
                                 BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        Xray xray = VortexClient.modules.get(Xray.class);
        if (xray.isEnabled() && !xray.shows(state.getBlock())) ci.cancel();
    }
}

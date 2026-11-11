package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
    /** Xray: always draw every face of the blocks we want to see (their neighbours are hidden). */
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void vortex$xrayFaces(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules == null) return;
        Xray xray = VortexClient.modules.get(Xray.class);
        if (xray.isEnabled() && xray.hideTerrain()) cir.setReturnValue(xray.shows(state.getBlock()));
    }
}

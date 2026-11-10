package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void vortex$berry(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise, CallbackInfo ci) {
        if (VortexClient.modules == null || entity != Minecraft.getInstance().player) return;
        NoSlow n = VortexClient.modules.get(NoSlow.class);
        if (n.on(n.berryBush)) ci.cancel();
    }
}

package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.movement.EntityControl;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerJumpMixin {
    @ModifyReturnValue(method = "getJumpRidingScale", at = @At("RETURN"))
    private float vortex$maxJump(float original) {
        return VortexClient.modules != null && VortexClient.modules.get(EntityControl.class).maxJump() && original > 0 ? 1.0f : original;
    }
}

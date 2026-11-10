package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.movement.NoSlow;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    /** NoSlow: the input slowdown checks isUsingItem(); pretend we aren't. */
    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean vortex$noSlowItems(boolean original) {
        if (VortexClient.modules == null) return original;
        NoSlow n = VortexClient.modules.get(NoSlow.class);
        return n.on(n.items) ? false : original;
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void vortex$noSlowSneak(CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules == null) return;
        NoSlow n = VortexClient.modules.get(NoSlow.class);
        if (n.on(n.sneaking)) cir.setReturnValue(false);
    }
}

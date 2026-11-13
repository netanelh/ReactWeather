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

    /** Sprint Omni: vanilla cancels sprint every tick when there is no forward input. */
    @ModifyExpressionValue(method = "shouldStopRunSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean vortex$omniSprint(boolean original) {
        if (original || VortexClient.modules == null) return original;
        return VortexClient.modules.get(com.natan.vortex.module.movement.Sprint.class).omniActive();
    }

    /** Rotations module: movement packets carry the latest silent aura/scaffold look instead of snapping back. */
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void vortex$rotHead(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.natan.vortex.util.RotationManager.beforeSend((LocalPlayer) (Object) this);
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void vortex$rotReturn(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.natan.vortex.util.RotationManager.afterSend((LocalPlayer) (Object) this);
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void vortex$noSlowSneak(CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules == null) return;
        NoSlow n = VortexClient.modules.get(NoSlow.class);
        if (n.on(n.sneaking)) cir.setReturnValue(false);
    }
}

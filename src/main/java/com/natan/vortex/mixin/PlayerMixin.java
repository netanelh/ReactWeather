package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.combat.Reach;
import com.natan.vortex.module.movement.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    /** SafeWalk: vanilla sneak-edge logic runs when this returns true. */
    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void vortex$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return;
        if (VortexClient.modules.isActive(SafeWalk.class)) cir.setReturnValue(true);
    }

    /** NoPush Water: Player overrides isPushedByFluid, so the hook must live here. */
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void vortex$noWaterPush(CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return;
        var n = VortexClient.modules.get(com.natan.vortex.module.movement.NoPush.class);
        if (n.on(n.water)) cir.setReturnValue(false);
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double vortex$entityReach(double original) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return original;
        Reach r = VortexClient.modules.get(Reach.class);
        return r.isEnabled() ? original + r.entity.d() : original;
    }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    private double vortex$blockReach(double original) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return original;
        Reach r = VortexClient.modules.get(Reach.class);
        return r.isEnabled() ? original + r.block.d() : original;
    }
}

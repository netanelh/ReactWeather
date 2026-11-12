package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.combat.AutoWeapon;
import com.natan.vortex.module.combat.Criticals;
import com.natan.vortex.module.player.FastBreak;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow private int destroyDelay;

    @Inject(method = "attack", at = @At("HEAD"))
    private void vortex$beforeAttack(Player player, Entity target, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        if (VortexClient.modules.isActive(AutoWeapon.class)) AutoWeapon.switchToBest(target);
        boolean crit = VortexClient.modules.get(Criticals.class).beforeAttack(target);
        VortexClient.modules.get(com.natan.vortex.module.combat.SuperKnockback.class).beforeAttack(target, crit);
        VortexClient.modules.get(com.natan.vortex.module.combat.MaceSmash.class).beforeAttack(target);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void vortex$afterAttack(Player player, Entity target, CallbackInfo ci) {
        if (VortexClient.modules != null) VortexClient.modules.get(Criticals.class).afterAttack();
    }

    /** Remember which block we right-clicked (ChestMemory uses it to know which chest a screen belongs to). */
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void vortex$useItemOn(net.minecraft.client.player.LocalPlayer player, net.minecraft.world.InteractionHand hand,
                                  net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        com.natan.vortex.memory.ChestMemory.lastInteracted = hit.getBlockPos().immutable();
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void vortex$fastBreak(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (VortexClient.modules != null && VortexClient.modules.isActive(FastBreak.class)) destroyDelay = 0;
    }
}

package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.natan.vortex.module.combat.Hitboxes;
import com.natan.vortex.module.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Hitboxes: the client's crosshair picking uses this margin around the bounding box. */
    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void vortex$hitboxes(CallbackInfoReturnable<Float> cir) {
        if (VortexClient.modules == null) return;
        float extra = VortexClient.modules.get(Hitboxes.class).extra((Entity) (Object) this);
        if (extra > 0) cir.setReturnValue(extra);
    }

    /** NoSlow: soul sand / honey speed factor -> treat as stone. */
    @ModifyExpressionValue(method = "getBlockSpeedFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block vortex$speedFactor(Block original) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return original;
        NoSlow n = VortexClient.modules.get(NoSlow.class);
        if (original == Blocks.SOUL_SAND && n.on(n.soulSand)) return Blocks.STONE;
        if (original == Blocks.HONEY_BLOCK && n.on(n.honey)) return Blocks.STONE;
        return original;
    }

    /** Freecam / FreeLook: mouse turns the camera instead of the player. */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void vortex$turn(double dx, double dy, CallbackInfo ci) {
        if (VortexClient.modules == null || (Object) this != Minecraft.getInstance().player) return;
        if (VortexClient.modules.get(com.natan.vortex.module.render.Freecam.class).turn(dx, dy)
            || VortexClient.modules.get(com.natan.vortex.module.render.FreeLook.class).turn(dx, dy)) ci.cancel();
    }

    /** NoPush: only entity-collision pushes on the local player are dropped (riptide etc. still work). */
    @WrapOperation(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void vortex$noPush(Entity instance, double x, double y, double z, Operation<Void> original) {
        if (VortexClient.modules != null && instance == Minecraft.getInstance().player) {
            var n = VortexClient.modules.get(com.natan.vortex.module.movement.NoPush.class);
            if (n.on(n.entities)) return;
        }
        original.call(instance, x, y, z);
    }
}

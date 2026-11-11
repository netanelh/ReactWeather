package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.NoRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** Captures the world matrices for the on-screen path overlay. */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void vortex$matrices(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projectionMatrix, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix) {
        com.natan.vortex.hud.PathOverlay.capture(projectionMatrix, modelViewMatrix);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void vortex$hurtCam(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        NoRender nr = VortexClient.modules.get(NoRender.class);
        if (nr.on(nr.hurtCam)) ci.cancel();
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void vortex$totem(ItemStack stack, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        NoRender nr = VortexClient.modules.get(NoRender.class);
        if (nr.on(nr.totem)) ci.cancel();
    }
}

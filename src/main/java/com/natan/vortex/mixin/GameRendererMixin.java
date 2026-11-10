package com.natan.vortex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.NoRender;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
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

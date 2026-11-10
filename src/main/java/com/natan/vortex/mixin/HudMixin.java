package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.NoOverlay;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Unique
    private static NoOverlay vortex$no() { return VortexClient.modules == null ? null : VortexClient.modules.get(NoOverlay.class); }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void vortex$effects(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.effects)) ci.cancel(); }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void vortex$portal(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.portal)) ci.cancel(); }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void vortex$vignette(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.vignette)) ci.cancel(); }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void vortex$spyglass(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.spyglass)) ci.cancel(); }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void vortex$scoreboard(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.scoreboard)) ci.cancel(); }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void vortex$title(CallbackInfo ci) { var n = vortex$no(); if (n != null && n.on(n.titles)) ci.cancel(); }

    @ModifyArgs(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", ordinal = 0))
    private void vortex$pumpkin(Args args) { var n = vortex$no(); if (n != null && n.on(n.pumpkin)) args.set(2, 0f); }
}

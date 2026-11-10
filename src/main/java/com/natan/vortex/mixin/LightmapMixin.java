package com.natan.vortex.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Fullbright;
import com.natan.vortex.module.render.Xray;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Shadow @Final private GpuTexture texture;

    /** Fullbright (Gamma) / Xray: paint the whole lightmap white = max brightness everywhere. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vortex$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        if (VortexClient.modules.get(Fullbright.class).gamma() || VortexClient.modules.isActive(Xray.class)) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(1));
            ci.cancel();
        }
    }
}

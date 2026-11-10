package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float vortex$zoom(float original) {
        return VortexClient.modules == null ? original : VortexClient.modules.get(Zoom.class).apply(original);
    }
}

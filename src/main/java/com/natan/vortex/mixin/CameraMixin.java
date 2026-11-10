package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float vortex$zoom(float original) {
        return VortexClient.modules == null ? original : VortexClient.modules.get(Zoom.class).apply(original);
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
    private float vortex$freeLookYaw(float yaw) {
        return VortexClient.modules == null ? yaw : VortexClient.modules.get(com.natan.vortex.module.render.FreeLook.class).yaw(yaw);
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
    private float vortex$freeLookPitch(float pitch) {
        return VortexClient.modules == null ? pitch : VortexClient.modules.get(com.natan.vortex.module.render.FreeLook.class).pitch(pitch);
    }
}

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

    // FreeLook: only the minecart (ordinal 0) and normal (ordinal 1) setRotation calls. The mirrored front view
    // (ordinal 2) builds on the already-modified rotation and sleeping (ordinal 3) must stay vanilla.
    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 0)
    private float vortex$freeLookYaw0(float yaw) { return freeLook() == null ? yaw : freeLook().yaw(yaw); }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 1)
    private float vortex$freeLookPitch0(float pitch) { return freeLook() == null ? pitch : freeLook().pitch(pitch); }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 0)
    private float vortex$freeLookYaw1(float yaw) { return freeLook() == null ? yaw : freeLook().yaw(yaw); }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 1)
    private float vortex$freeLookPitch1(float pitch) { return freeLook() == null ? pitch : freeLook().pitch(pitch); }

    private static com.natan.vortex.module.render.FreeLook freeLook() {
        return VortexClient.modules == null ? null : VortexClient.modules.get(com.natan.vortex.module.render.FreeLook.class);
    }
}

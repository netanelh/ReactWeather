package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.Performance;
import com.natan.vortex.module.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void vortex$particles(Particle p, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        NoRender nr = VortexClient.modules.get(NoRender.class);
        if (nr.on(nr.particles) || !VortexClient.modules.get(Performance.class).allowParticle()) ci.cancel();
    }
}

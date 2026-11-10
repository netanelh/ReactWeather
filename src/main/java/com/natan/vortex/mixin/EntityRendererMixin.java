package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Esp;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    /** ESP: recolor the outline after vanilla filled in the team color. */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vortex$espColor(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        Integer color = VortexClient.modules.get(Esp.class).colorFor(entity);
        if (color != null) state.outlineColor = color;
    }
}

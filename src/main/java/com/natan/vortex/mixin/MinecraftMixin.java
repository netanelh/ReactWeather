package com.natan.vortex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Esp;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** ESP: force the vanilla glow outline on matching entities. */
    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean vortex$espGlow(boolean original, Entity entity) {
        if (original || VortexClient.modules == null) return original;
        return VortexClient.modules.get(Esp.class).glowColor(entity) != null;
    }
}

package com.natan.vortex.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float vortex$getDestroyProgress();

    @Accessor("destroyProgress")
    void vortex$setDestroyProgress(float value);
}

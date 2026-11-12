package com.natan.vortex.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key vortex$getKey();

    /** Sets the pressed state directly, skipping ToggleKeyMapping's toggle logic. */
    @Accessor("isDown")
    void vortex$setDownRaw(boolean down);
}

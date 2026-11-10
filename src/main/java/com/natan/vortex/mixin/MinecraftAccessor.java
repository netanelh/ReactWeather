package com.natan.vortex.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("fps")
    static int vortex$getFps() { throw new AssertionError(); }

    @Accessor("rightClickDelay")
    int vortex$getRightClickDelay();

    @Accessor("rightClickDelay")
    void vortex$setRightClickDelay(int value);
}

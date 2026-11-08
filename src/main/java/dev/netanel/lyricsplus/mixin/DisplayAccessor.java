package dev.netanel.lyricsplus.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Invoker("setTransformation")
    void lyricsplus$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void lyricsplus$setTransformationInterpolationDuration(int ticks);

    @Invoker("setTransformationInterpolationDelay")
    void lyricsplus$setTransformationInterpolationDelay(int ticks);

    @Invoker("setBillboardConstraints")
    void lyricsplus$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setBrightnessOverride")
    void lyricsplus$setBrightnessOverride(Brightness brightness);

    @Invoker("setViewRange")
    void lyricsplus$setViewRange(float range);

    /** Restarts transformation interpolation, so animations are smooth at any frame rate. */
    @Accessor("updateStartTick")
    void lyricsplus$setUpdateStartTick(boolean value);
}

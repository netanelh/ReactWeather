package dev.netanel.lyricsplus.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {

    @Invoker("setText")
    void lyricsplus$setText(Component text);

    @Invoker("setLineWidth")
    void lyricsplus$setLineWidth(int width);

    @Invoker("setTextOpacity")
    void lyricsplus$setTextOpacity(byte opacity);

    @Invoker("setBackgroundColor")
    void lyricsplus$setBackgroundColor(int argb);

    @Invoker("setFlags")
    void lyricsplus$setFlags(byte flags);
}

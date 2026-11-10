package com.natan.vortex.mixin;

import com.natan.vortex.seed.SeedCracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Grabs the hashed world seed the server sends on join (vanilla sends it to every player for biome blending). */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vortex$seed(ClientPacketListener conn, ClientLevel.ClientLevelData data, ResourceKey<Level> dim, Holder<DimensionType> type,
                             int a, int b, LevelExtractor ex, boolean debug, long hashedSeed, int c, CallbackInfo ci) {
        SeedCracker.hashedSeed = hashedSeed;
    }
}

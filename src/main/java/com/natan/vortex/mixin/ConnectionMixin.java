package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.movement.Blink;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void vortex$send(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (VortexClient.modules == null) return;
        if (VortexClient.modules.get(Blink.class).intercept(packet)) { ci.cancel(); return; }
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket cmd
            && cmd.getAction() == net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING
            && VortexClient.modules.isActive(com.natan.vortex.module.player.AntiHunger.class)) ci.cancel();
    }
}

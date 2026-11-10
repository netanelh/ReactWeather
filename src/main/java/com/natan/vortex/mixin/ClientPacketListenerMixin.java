package com.natan.vortex.mixin;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.combat.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Velocity: remember our motion before a knockback packet is applied, then blend
 * old and new motion by the configured percentage afterwards.
 * (The HEAD hook also fires on the network thread before the packet is re-queued;
 * TAIL is only reached on the main thread, so the scaling happens exactly once.)
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private Vec3 vortex$before;

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void vortex$motionHead(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        vortex$before = p != null && packet.id() == p.getId() ? p.getDeltaMovement() : null;
    }

    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void vortex$motionTail(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) { vortex$scale(); }

    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void vortex$explosionHead(ClientboundExplodePacket packet, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        vortex$before = p != null ? p.getDeltaMovement() : null;
    }

    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void vortex$explosionTail(ClientboundExplodePacket packet, CallbackInfo ci) { vortex$scale(); }

    @Unique
    private void vortex$scale() {
        var p = Minecraft.getInstance().player;
        if (p == null || vortex$before == null || VortexClient.modules == null) return;
        Velocity v = VortexClient.modules.get(Velocity.class);
        Vec3 before = vortex$before;
        vortex$before = null;
        if (!v.isEnabled()) return;
        Vec3 now = p.getDeltaMovement();
        double h = v.horizontal.d() / 100.0, y = v.vertical.d() / 100.0;
        p.setDeltaMovement(before.x + (now.x - before.x) * h, before.y + (now.y - before.y) * y, before.z + (now.z - before.z) * h);
    }
}

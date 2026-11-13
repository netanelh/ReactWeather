package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayList;
import java.util.List;

/** Holds back movement packets; to the server you freeze, then teleport when you turn it off. Hooked in ConnectionMixin. */
public class Blink extends Module {
    private final NumberSetting maxPackets = add(new NumberSetting("Auto Release", "Release automatically after this many packets (0 = never)", 0, 0, 400, 10));
    private final List<Packet<?>> queue = new ArrayList<>();
    private boolean flushing, releasing;

    public Blink() { super("Blink", "Freeze for the server, then teleport where you walked", Category.MOVEMENT); }

    @Override public String info() { return Integer.toString(queue.size()); }

    /** @return true if the packet should be held back. */
    public boolean intercept(Packet<?> packet) {
        if (!isEnabled() || flushing || !(packet instanceof ServerboundMovePlayerPacket)) return false;
        queue.add(packet);
        int cap = maxPackets.i() > 0 ? maxPackets.i() : 1000; // hard cap so the queue can't grow forever
        if (queue.size() >= cap && !releasing) { releasing = true; mc.execute(() -> { if (isEnabled()) toggle(); }); }
        return true;
    }

    /** Drops held packets without sending them (world/server changed: they belong to the old connection). */
    public void clearQueue() { queue.clear(); }

    /** New world/server: drop old packets (never flush them to the new connection) and stop blinking. */
    @Override public void onWorldChange() { queue.clear(); releasing = false; setEnabledSilently(false); }

    @Override
    public void onDisable() {
        releasing = false;
        if (mc.getConnection() != null) {
            flushing = true;
            for (Packet<?> p : queue) mc.getConnection().send(p);
            flushing = false;
        }
        queue.clear();
    }
}

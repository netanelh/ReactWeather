package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Chat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Remembers where players were when they disappeared and tells you (combat-loggers!). */
public class LogoutSpots extends Module {
    private Map<String, BlockPos> lastSeen = new HashMap<>();
    public final Map<String, BlockPos> spots = new HashMap<>();

    public LogoutSpots() { super("LogoutSpots", "Reports where players logged out", Category.RENDER); }

    @Override
    public void onTick() {
        if (!inGame() || mc.getConnection() == null) return;
        Map<String, BlockPos> now = new HashMap<>();
        for (Player p : mc.level.players()) if (p != mc.player) now.put(p.getName().getString(), p.blockPosition());
        for (var e : lastSeen.entrySet()) {
            if (now.containsKey(e.getKey())) continue;
            boolean online = mc.getConnection().getOnlinePlayers().stream().anyMatch(i -> i.getProfile().name().equals(e.getKey()));
            if (!online) {
                spots.put(e.getKey(), e.getValue());
                Chat.info("§e" + e.getKey() + "§r logged out at " + e.getValue().toShortString());
            }
        }
        for (String n : now.keySet()) spots.remove(n);
        lastSeen = now;
    }

    @Override public void onDisable() { lastSeen.clear(); spots.clear(); }
}

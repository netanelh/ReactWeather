package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Project;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Remembers where players were when they disappeared, tells you, and marks the spot on screen. */
public class LogoutSpots extends Module {
    private final BoolSetting markers = add(new BoolSetting("Markers", "Show logout spots on screen (works with Sodium)", true));

    private Map<String, BlockPos> lastSeen = new HashMap<>();
    public final Map<String, BlockPos> spots = new HashMap<>();
    /** Players that vanished but are still in the tab list: re-checked for a short while (packet order varies). */
    private final Map<String, BlockPos> pendingPos = new HashMap<>();
    private final Map<String, Integer> pendingTicks = new HashMap<>();
    private Object level;

    public LogoutSpots() { super("LogoutSpots", "Reports where players logged out", Category.RENDER); }

    @Override
    public void onTick() {
        if (!inGame() || mc.getConnection() == null) return;
        if (mc.level != level) { level = mc.level; lastSeen.clear(); spots.clear(); pendingPos.clear(); pendingTicks.clear(); }
        Map<String, BlockPos> now = new HashMap<>();
        for (Player p : mc.level.players()) if (p != mc.player) now.put(p.getName().getString(), p.blockPosition());
        for (var e : lastSeen.entrySet()) {
            if (now.containsKey(e.getKey())) continue;
            if (!online(e.getKey())) record(e.getKey(), e.getValue());
            else { pendingPos.put(e.getKey(), e.getValue()); pendingTicks.put(e.getKey(), 40); }
        }
        for (Iterator<Map.Entry<String, Integer>> it = pendingTicks.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            String n = e.getKey();
            if (now.containsKey(n)) { it.remove(); pendingPos.remove(n); continue; }
            if (!online(n)) { record(n, pendingPos.remove(n)); it.remove(); continue; }
            if (e.getValue() <= 1) { it.remove(); pendingPos.remove(n); } else e.setValue(e.getValue() - 1);
        }
        for (String n : now.keySet()) spots.remove(n);
        lastSeen = now;
    }

    private boolean online(String name) {
        return mc.getConnection().getOnlinePlayers().stream().anyMatch(i -> i.getProfile().name().equals(name));
    }

    private void record(String name, BlockPos pos) {
        if (pos == null) return;
        spots.put(name, pos);
        Chat.info("§e" + name + "§r logged out at " + pos.toShortString());
    }

    /** Called from HudRenderer: name + distance tag over each spot. */
    public void renderMarkers(GuiGraphicsExtractor g) {
        if (!isEnabled() || !markers.on() || mc.player == null || spots.isEmpty()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        for (var e : spots.entrySet()) {
            BlockPos p = e.getValue();
            float[] s = Project.toScreen(new Vec3(p.getX() + 0.5, p.getY() + 2.1, p.getZ() + 0.5), w, h);
            if (s == null || s[0] < 0 || s[0] > w || s[1] < 0 || s[1] > h) continue;
            int x = (int) s[0], y = (int) s[1];
            String label = e.getKey() + " " + (int) Math.sqrt(mc.player.blockPosition().distSqr(p)) + "m";
            int tw = mc.font.width(label);
            g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFFD000);
            g.fill(x - tw / 2 - 2, y - 14, x + tw / 2 + 2, y - 4, 0x90101014);
            g.text(mc.font, label, x - tw / 2, y - 13, 0xFFFFD000, true);
        }
    }

    @Override public void onDisable() { lastSeen.clear(); spots.clear(); pendingPos.clear(); pendingTicks.clear(); }
}

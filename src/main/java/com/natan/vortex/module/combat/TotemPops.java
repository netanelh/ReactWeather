package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Chat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Announces in chat whenever a player pops a totem (and how many so far). */
public class TotemPops extends Module {
    private final Map<String, Integer> pops = new HashMap<>();

    public TotemPops() { super("TotemPops", "Counts players' totem pops in chat", Category.COMBAT); }

    public int pops(String name) { return pops.getOrDefault(name, 0); }

    public void onEntityEvent(Entity e, byte id) {
        if (!isEnabled() || !(e instanceof Player p)) return;
        String n = p.getName().getString();
        if (id == 35) {
            int c = pops.merge(n, 1, Integer::sum);
            Chat.info((p == mc.player ? "§eYou" : "§c" + n) + "§7 popped §f" + c + "§7 totem" + (c > 1 ? "s" : ""));
        } else if (id == 3 && pops.containsKey(n)) {
            Chat.info("§c" + n + "§7 died after §f" + pops.remove(n) + "§7 pops");
        }
    }
}

package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.util.Colors;
import com.natan.vortex.util.Targets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** Outline ESP through walls, using the vanilla glowing effect (works with shaders off and on). */
public class Esp extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", "Outline players", true));
    private final ModeSetting playerColor = add(new ModeSetting("Player Color", "Outline color for players", "Red", Colors.NAMES).visibleWhen(players::on));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Outline hostile mobs", true));
    private final ModeSetting hostileColor = add(new ModeSetting("Hostile Color", "Outline color for hostiles", "Orange", Colors.NAMES).visibleWhen(hostiles::on));
    private final BoolSetting passives = add(new BoolSetting("Passives", "Outline animals", false));
    private final ModeSetting passiveColor = add(new ModeSetting("Passive Color", "Outline color for animals", "Green", Colors.NAMES).visibleWhen(passives::on));
    private final BoolSetting items = add(new BoolSetting("Items", "Outline dropped items", false));
    private final ModeSetting itemColor = add(new ModeSetting("Item Color", "Outline color for items", "Cyan", Colors.NAMES).visibleWhen(items::on));
    private final BoolSetting healthColor = add(new BoolSetting("Health Color", "Players: green to red by health instead of a fixed color", false).visibleWhen(players::on));

    public Esp() { super("ESP", "Outlines entities through walls", Category.RENDER); }

    /** Returns ARGB outline color, or null if the entity should not be highlighted. */
    public Integer colorFor(Entity e) {
        if (!isEnabled() || e == mc.player) return null;
        if (e instanceof Player p) {
            if (!players.on()) return null;
            if (healthColor.on()) {
                float t = Math.max(0, Math.min(1, p.getHealth() / p.getMaxHealth()));
                return Colors.lerp(0xFFEF4444, 0xFF22C55E, t);
            }
            return Colors.named(playerColor.get(), e.getId());
        }
        if (e instanceof ItemEntity) return items.on() ? Colors.named(itemColor.get(), e.getId()) : null;
        if (Targets.isHostile(e)) return hostiles.on() ? Colors.named(hostileColor.get(), e.getId()) : null;
        if (e instanceof net.minecraft.world.entity.LivingEntity && Targets.isPassive(e)) return passives.on() ? Colors.named(passiveColor.get(), e.getId()) : null;
        return null;
    }
}

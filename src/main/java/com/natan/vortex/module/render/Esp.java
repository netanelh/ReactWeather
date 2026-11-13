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
    private final BoolSetting players = add(new BoolSetting("Players", "Outline players", true).group("Targets"));
    private final ModeSetting playerColor = add(new ModeSetting("Player Color", "Outline color for players", "Red", Colors.NAMES).visibleWhen(players::on).group("Targets"));
    private final BoolSetting healthColor = add(new BoolSetting("Health Color", "Players: green to red by health instead of a fixed color", false).visibleWhen(players::on).group("Targets"));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Outline hostile mobs", true).group("Targets"));
    private final ModeSetting hostileColor = add(new ModeSetting("Hostile Color", "Outline color for hostiles", "Orange", Colors.NAMES).visibleWhen(hostiles::on).group("Targets"));
    private final BoolSetting passives = add(new BoolSetting("Passives", "Outline animals", false).group("Targets"));
    private final ModeSetting passiveColor = add(new ModeSetting("Passive Color", "Outline color for animals", "Green", Colors.NAMES).visibleWhen(passives::on).group("Targets"));
    private final BoolSetting items = add(new BoolSetting("Items", "Outline dropped items", false).group("Targets"));
    private final ModeSetting itemColor = add(new ModeSetting("Item Color", "Outline color for items", "Cyan", Colors.NAMES).visibleWhen(items::on).group("Targets"));
    public final BoolSetting box = add(new BoolSetting("Box", "Draw a box around entities", true).group("Visuals"));
    public final ModeSetting boxMode = add(new ModeSetting("Box Mode", "2D = flat screen rectangle, 3D = wireframe box in the world", "3D", "2D", "3D").visibleWhen(box::on).group("Visuals"));
    public final BoolSetting tracer = add(new BoolSetting("Tracers", "Line from the bottom of the screen to each entity", false).group("Visuals"));
    public final BoolSetting nametag = add(new BoolSetting("Name Tags", "Name, health and distance above the entity", true).group("Visuals"));
    public final BoolSetting tagItems = add(new BoolSetting("Tag Items", "Players: show armor and held item above the name tag", true).visibleWhen(nametag::on).group("Visuals"));
    public final BoolSetting glow = add(new BoolSetting("Glow", "Also outline through walls with the glow effect", true).group("Visuals"));
    private final BoolSetting showInvisible = add(new BoolSetting("Show Invisible", "Render invisible highlighted entities (translucent)", true).group("Visuals"));

    public Esp() { super("ESP", "Outlines entities through walls", Category.RENDER); }

    /** Color for the glow outline (null = no glow), separate so boxes can be shown without glow. */
    public Integer glowColor(Entity e) { return glow.on() ? colorFor(e) : null; }

    public boolean showsInvisible(Entity e) { return showInvisible.on() && colorFor(e) != null; }

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

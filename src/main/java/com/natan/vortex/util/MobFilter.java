package com.natan.vortex.util;

import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Per-mob-type targeting for combat modules: choose exactly what gets attacked.
 * Neutral mobs (endermen, piglins, golems, villagers, pets) are OFF by default.
 */
public final class MobFilter {
    private record Group(BoolSetting setting, Set<String> ids) {}

    public final BoolSetting players;
    private final BoolSetting otherHostiles, otherAnimals;
    private final List<Group> groups = new ArrayList<>();

    public MobFilter(Consumer<BoolSetting> adder, boolean playersDefault) {
        players = add(adder, "Players", "Attack players (friends are always skipped)", playersDefault);
        Map<String, Object[]> g = new LinkedHashMap<>();
        g.put("Zombies", new Object[]{true, Set.of("zombie", "husk", "drowned", "zombie_villager")});
        g.put("Skeletons", new Object[]{true, Set.of("skeleton", "stray", "wither_skeleton", "bogged")});
        g.put("Creepers", new Object[]{true, Set.of("creeper")});
        g.put("Spiders", new Object[]{true, Set.of("spider", "cave_spider")});
        g.put("Witches", new Object[]{true, Set.of("witch")});
        g.put("Slimes", new Object[]{true, Set.of("slime", "magma_cube")});
        g.put("Phantoms", new Object[]{true, Set.of("phantom")});
        g.put("Illagers", new Object[]{true, Set.of("pillager", "vindicator", "evoker", "ravager", "vex", "illusioner")});
        g.put("Nether Mobs", new Object[]{true, Set.of("blaze", "ghast", "wither", "hoglin", "zoglin", "piglin_brute")});
        g.put("Endermen", new Object[]{false, Set.of("enderman", "endermite")});
        g.put("Piglins", new Object[]{false, Set.of("piglin", "zombified_piglin")});
        g.put("Golems", new Object[]{false, Set.of("iron_golem", "snow_golem")});
        g.put("Villagers", new Object[]{false, Set.of("villager", "wandering_trader")});
        g.put("Pets", new Object[]{false, Set.of("wolf", "cat", "parrot", "horse", "donkey", "mule", "llama", "camel", "fox", "axolotl", "allay")});
        g.put("Cows", new Object[]{false, Set.of("cow", "mooshroom")});
        g.put("Pigs", new Object[]{false, Set.of("pig")});
        g.put("Sheep", new Object[]{false, Set.of("sheep")});
        g.put("Chickens", new Object[]{false, Set.of("chicken")});
        g.forEach((name, v) -> {
            @SuppressWarnings("unchecked") Set<String> ids = (Set<String>) v[1];
            groups.add(new Group(add(adder, name, "Attack " + name.toLowerCase(), (Boolean) v[0]), ids));
        });
        otherHostiles = add(adder, "Other Hostiles", "Hostile mobs not listed above", true);
        otherAnimals = add(adder, "Other Animals", "Passive mobs not listed above", false);
    }

    private static BoolSetting add(Consumer<BoolSetting> adder, String n, String d, boolean def) {
        BoolSetting s = new BoolSetting(n, d, def);
        adder.accept(s);
        return s;
    }

    /** Full check: alive, not you, not a friend, and allowed by the toggles. */
    public boolean test(Entity e) {
        if (!(e instanceof LivingEntity l) || !l.isAlive() || l.getHealth() <= 0) return false;
        if (e instanceof Player) return Targets.valid(e, players.on(), false, false);
        if (e.isSpectator() || e == net.minecraft.client.Minecraft.getInstance().player) return false;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        for (Group g : groups) if (g.ids.contains(id)) return g.setting.on();
        if (Targets.isHostile(e)) return otherHostiles.on();
        return Targets.isPassive(e) && otherAnimals.on();
    }
}

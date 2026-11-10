package com.natan.vortex.nav.process;

import com.natan.vortex.memory.WorldMemory;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.BlockSets;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multiplayer structure finding without OP: explores outward in a big spiral while the world memory
 * scans every chunk that loads for the structure's signature blocks (and villagers for villages).
 * Stops and walks in as soon as it spots one. Also checks memory first (places you've already seen).
 */
public class ScoutProcess implements Process {
    public static final Map<String, List<String>> SIGNATURES = Map.ofEntries(
        Map.entry("village", List.of("bell")),
        Map.entry("stronghold", List.of("end_portal_frame")),
        Map.entry("ancient_city", List.of("reinforced_deepslate", "sculk_shrieker")),
        Map.entry("trial_chambers", List.of("trial_spawner", "vault")),
        Map.entry("monument", List.of("dark_prismarine", "prismarine_bricks")),
        Map.entry("fortress", List.of("nether_brick_fence")),
        Map.entry("bastion", List.of("gilded_blackstone")),
        Map.entry("bastion_remnant", List.of("gilded_blackstone")),
        Map.entry("ruined_portal", List.of("crying_obsidian")),
        Map.entry("desert_pyramid", List.of("suspicious_sand")),
        Map.entry("trail_ruins", List.of("suspicious_gravel")),
        Map.entry("end_city", List.of("purpur_block", "end_rod")),
        Map.entry("dungeon", List.of("spawner")),
        Map.entry("mineshaft", List.of("rail")));

    private final String label;
    private final Set<Block> signature = new HashSet<>();
    private final ExploreProcess explore;
    private BlockPos found;
    private int checkTimer;
    private boolean done;

    public ScoutProcess(String what) {
        this.label = what;
        for (String id : SIGNATURES.getOrDefault(what, List.of())) {
            var set = BlockSets.parse(id);
            signature.addAll(set);
            for (Block b : set) WorldMemory.INSTANCE.track(b);
        }
        explore = new ExploreProcess(160);
    }

    public static boolean known(String what) { return SIGNATURES.containsKey(what); }

    @Override public String name() { return "Scout"; }
    @Override public String status() { return found != null ? label + " found" : "searching for " + label + " · " + explore.status(); }
    @Override public boolean isDone() { return done; }

    @Override
    public void tick() {
        if (found != null || checkTimer-- > 0) return;
        checkTimer = 20;
        Minecraft mc = Minecraft.getInstance();
        BlockPos me = mc.player.blockPosition();
        var hits = WorldMemory.INSTANCE.nearest(signature, me, 1);
        if (!hits.isEmpty()) found = hits.get(0);
        if (found == null && label.equals("village")) {
            for (Entity e : mc.level.entitiesForRendering())
                if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("villager")) { found = e.blockPosition(); break; }
        }
        if (found != null) Chat.info("Spotted a " + label + " at " + found.getX() + ", " + found.getZ() + " (" + Math.round(Math.sqrt(found.distSqr(me))) + " blocks). Going there.");
    }

    @Override public Goal goal() { return found != null ? new Goal.Near(found.getX(), found.getY(), found.getZ(), 6) : explore.goal(); }

    @Override
    public void atGoal() {
        if (found != null) done = true;
        else explore.atGoal();
    }
}

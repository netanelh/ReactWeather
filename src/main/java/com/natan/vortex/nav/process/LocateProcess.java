package com.natan.vortex.nav.process;

import com.mojang.datafixers.util.Pair;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;

/**
 * Singleplayer-only exact locate: reads the world straight from your own integrated server
 * (no commands, no OP, no cheats). On multiplayer, ".locate" uses ScoutProcess / StrongholdProcess instead.
 */
public class LocateProcess implements Process {
    private final String label;
    private volatile Goal goal;
    private volatile boolean failed;
    private boolean done;

    public LocateProcess(String what, boolean biome) {
        this.label = what;
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        var dim = mc.level.dimension();
        BlockPos origin = mc.player.blockPosition();
        String name = what.contains(":") ? what.substring(what.indexOf(':') + 1) : what;
        server.execute(() -> {
            BlockPos found = null;
            try {
                ServerLevel lvl = server.getLevel(dim);
                if (biome) {
                    var key = ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(name));
                    Pair<BlockPos, ?> r = lvl.findClosestBiome3d(h -> h.is(key), origin, 6400, 32, 64);
                    if (r != null) found = r.getFirst();
                } else {
                    var reg = lvl.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                    HolderSet<net.minecraft.world.level.levelgen.structure.Structure> set = null;
                    var tag = reg.get(TagKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(name)));
                    if (tag.isPresent()) set = tag.get();
                    else {
                        var holder = reg.get(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(name)));
                        if (holder.isPresent()) set = HolderSet.direct(holder.get());
                    }
                    if (set != null) {
                        var r = lvl.getChunkSource().getGenerator().findNearestMapStructure(lvl, set, origin, 100, false);
                        if (r != null) found = r.getFirst();
                    }
                }
            } catch (Throwable t) {
                com.natan.vortex.VortexClient.LOG.warn("Locate failed", t);
            }
            BlockPos f = found;
            mc.execute(() -> {
                if (f == null) { failed = true; return; }
                goal = biome ? new Goal.Near(f.getX(), f.getY(), f.getZ(), 4) : new Goal.XZ(f.getX(), f.getZ());
                Chat.info("Found " + label + " at " + f.getX() + ", " + f.getZ() + " (" + Math.round(Math.sqrt(f.distSqr(origin))) + " blocks). Walking there.");
            });
        });
    }

    @Override public String name() { return "Locate"; }
    @Override public String status() { return goal == null ? "searching the world for " + label + "..." : label; }

    @Override
    public void tick() {
        if (failed) { Chat.error("Couldn't find '" + label + "'. Names like: village, stronghold, ancient_city, mansion, monument, trial_chambers, fortress, bastion_remnant, or biome <name>."); done = true; }
    }

    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { done = true; }
    @Override public boolean isDone() { return done; }
}

package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.seed.Worldgen;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Exact locate on a server using the cracked (or typed) seed: runs vanilla worldgen math locally. */
public class SeedLocateProcess implements Process {
    private final String label;
    private volatile Goal goal;
    private volatile boolean failed, done;

    public SeedLocateProcess(String what, boolean biome, long seed) {
        this.label = what;
        Minecraft mc = Minecraft.getInstance();
        var dim = mc.level.dimension();
        var level = mc.level;
        BlockPos origin = mc.player.blockPosition();
        Thread t = new Thread(() -> {
            BlockPos f = null;
            try {
                var c = Worldgen.ctx(seed, dim);
                f = biome ? Worldgen.findBiome(c, what, origin, level) : Worldgen.findStructure(c, what, origin);
            } catch (Throwable e) { com.natan.vortex.VortexClient.LOG.warn("Seed locate failed", e); }
            BlockPos r = f;
            mc.execute(() -> {
                if (r == null) { failed = true; return; }
                goal = biome ? new Goal.Near(r.getX(), r.getY(), r.getZ(), 4) : new Goal.XZ(r.getX(), r.getZ());
                Chat.info("Found " + label + " at " + r.getX() + ", " + r.getZ() + " (" + Math.round(Math.sqrt(r.distSqr(origin))) + " blocks, from the seed). Walking there.");
            });
        }, "Vortex-Locate");
        t.setDaemon(true);
        t.start();
    }

    @Override public String name() { return "Locate"; }
    @Override public String status() { return goal == null ? "calculating " + label + " from the seed..." : label; }
    @Override public void tick() {
        if (failed && !done) { Chat.error("No '" + label + "' found nearby in this dimension (check the name)."); done = true; }
    }
    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { done = true; }
    @Override public boolean isDone() { return done; }
}

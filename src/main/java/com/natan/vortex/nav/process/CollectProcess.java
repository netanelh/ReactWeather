package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Items2;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Walks to dropped items (optionally only one kind) and picks them up. ".collect" / ".collect diamond". */
public class CollectProcess implements Process {
    private final String itemFilter; // null = everything
    private final int radius;
    private ItemEntity target;
    private int picked, idleTicks;

    public CollectProcess(String itemFilter, int radius) { this.itemFilter = itemFilter == null ? null : itemFilter.toLowerCase(); this.radius = radius; }

    @Override public String name() { return "Collect"; }
    @Override public String status() { return (itemFilter == null ? "all items" : itemFilter) + " · " + picked + " picked"; }
    @Override public boolean isDone() { return idleTicks > 100; } // nothing left for 5 seconds

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (target != null && !target.isAlive()) { picked++; target = null; }
        if (target == null) {
            double best = radius;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof ItemEntity it) || !it.isAlive()) continue;
                if (itemFilter != null && !Items2.id(it.getItem()).contains(itemFilter)) continue;
                double d = it.distanceTo(mc.player);
                if (d < best) { best = d; target = it; }
            }
        }
        idleTicks = target == null ? idleTicks + 1 : 0;
    }

    @Override public Goal goal() { return target == null ? null : new Goal.Block(target.blockPosition()); }
    @Override public void atGoal() { /* item gets picked up by walking over it */ }
}

package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.World;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Place;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Digs a straight tunnel (1x2, 2x2 or 3x3) in the direction you face, placing floor over gaps and stopping at lava. */
public class TunnelProcess implements Process {
    private final Direction dir;
    private final int length, width, height;
    private final BlockPos start;
    private int progress;
    private boolean done;

    public TunnelProcess(int length, int width, int height) {
        var p = Minecraft.getInstance().player;
        this.dir = p.getDirection();
        this.length = length; this.width = width; this.height = height;
        this.start = BlockPos.containing(p.getX(), p.getY() + 0.2, p.getZ());
    }

    @Override public String name() { return "Tunnel"; }
    @Override public String status() { return progress + "/" + length + " " + dir.getName() + " " + width + "x" + height; }
    @Override public boolean isDone() { return done; }
    @Override public Goal goal() { return null; } // acts in place

    private BlockPos slice(int i) { return start.relative(dir, i); }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        var o = mc.options;
        if (progress >= length) { done = true; Actions.releaseKeys(); return; }
        BlockPos next = slice(progress + 1);

        // safety: lava anywhere in the next two slices -> stop
        for (int s = 1; s <= 2; s++) for (BlockPos b : cells(slice(progress + s), true))
            if (World.isLavaAt(b.getX(), b.getY(), b.getZ())) {
                Chat.error("Lava ahead at " + b.toShortString() + ", tunnel stopped.");
                done = true; Actions.releaseKeys(); return;
            }

        for (BlockPos b : cells(next, false)) {
            if (World.passable(b.getX(), b.getY(), b.getZ())) continue;
            o.keyUp.setDown(false);
            Actions.mine(b);
            return;
        }
        // floor under the next slice
        BlockPos floor = next.below();
        if (World.replaceable(floor.getX(), floor.getY(), floor.getZ())) {
            int slot = Place.findSlot(Place::isThrowaway);
            if (slot == -1) { Chat.error("Need building blocks for the tunnel floor."); done = true; return; }
            Place.at(floor, slot, true);
            return;
        }
        // walk into the next slice
        Vec3 goal = Vec3.atBottomCenterOf(next);
        Actions.faceYaw(goal, 45);
        double d = Math.hypot(goal.x - mc.player.getX(), goal.z - mc.player.getZ());
        o.keyUp.setDown(d > 0.2);
        if (d < 0.3) progress++;
    }

    private List<BlockPos> cells(BlockPos center, boolean withBorder) {
        List<BlockPos> out = new ArrayList<>();
        Direction side = dir.getClockWise();
        int half = width / 2;
        int lo = -half - (withBorder ? 1 : 0), hi = (width - 1 - half) + (withBorder ? 1 : 0);
        for (int w = lo; w <= hi; w++)
            for (int h = withBorder ? -1 : 0; h < height + (withBorder ? 1 : 0); h++)
                out.add(center.relative(side, w).above(h));
        return out;
    }

    @Override public void onStop() { Actions.releaseKeys(); }
}

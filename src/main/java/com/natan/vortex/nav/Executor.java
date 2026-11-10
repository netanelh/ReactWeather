package com.natan.vortex.nav;

import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Walks a computed path: mines, bridges, pillars and jumps as each step requires. */
public final class Executor {
    public enum Status { RUNNING, DONE, FAILED }

    private final List<Step> steps;
    private final boolean sprint;
    private int index = 1;
    private int ticksOnStep, stuckTicks;
    private double bestDist = Double.MAX_VALUE;
    private double pillarStartY = Double.NaN;

    public Executor(List<Step> steps, boolean sprint) { this.steps = steps; this.sprint = sprint; }

    public List<Step> steps() { return steps; }
    public int index() { return index; }
    public int remaining() { return Math.max(0, steps.size() - index); }

    public Status tick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        var o = mc.options;
        if (index >= steps.size()) { Actions.releaseKeys(); return Status.DONE; }
        Step step = steps.get(index);
        BlockPos target = step.pos();
        ticksOnStep++;

        // 1) mine whatever blocks this step needs cleared
        for (BlockPos b : step.toBreak()) {
            if (World.passable(b.getX(), b.getY(), b.getZ())) continue;
            o.keyUp.setDown(false); o.keySprint.setDown(false); o.keyJump.setDown(false);
            Actions.mine(b);
            stuckTicks = 0;
            return Status.RUNNING;
        }

        // 2) place a block if needed (bridge under the next cell, or pillar under ourselves)
        if (step.toPlace() != null && World.replaceable(step.toPlace().getX(), step.toPlace().getY(), step.toPlace().getZ())) {
            int slot = Place.findSlot(Place::isThrowaway);
            if (slot == -1) {
                // bring building blocks from the inventory into the hotbar
                int inv = Inv.findAny(Place::isThrowaway);
                if (inv == -1) return Status.FAILED;
                Inv.click(Inv.toMenuSlot(inv), Inv.selected(), net.minecraft.world.inventory.ContainerInput.SWAP);
                return Status.RUNNING;
            }
            if (step.move() == Step.Move.PILLAR) {
                o.keyUp.setDown(false);
                if (Double.isNaN(pillarStartY)) pillarStartY = p.getY();
                Actions.lookAt(Vec3.atCenterOf(step.toPlace()).add(0, -0.5, 0), 90);
                if (p.onGround()) o.keyJump.setDown(true);
                if (p.getY() > pillarStartY + 1.0) { Place.at(step.toPlace(), slot, true); o.keyJump.setDown(false); }
                return Status.RUNNING;
            }
            // bridging: crouch at the edge so we can't fall, then place against our own floor
            o.keyShift.setDown(true);
            Actions.faceYaw(Vec3.atBottomCenterOf(target), 40);
            Vec3 c = Vec3.atBottomCenterOf(steps.get(index - 1).pos());
            double fromCenter = Math.hypot(p.getX() - c.x, p.getZ() - c.z);
            o.keyUp.setDown(fromCenter < 0.35);
            Place.at(step.toPlace(), slot, true);
            return Status.RUNNING;
        }
        pillarStartY = Double.NaN;
        o.keyShift.setDown(false);

        // 3) move toward the step
        Vec3 goal = Vec3.atBottomCenterOf(target);
        double dx = goal.x - p.getX(), dz = goal.z - p.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY() - p.getY();

        boolean arrived = horiz < (index == steps.size() - 1 ? 0.25 : 0.4) && Math.abs(dy) < 0.6 && (p.onGround() || p.isInWater());
        if (step.move() == Step.Move.PARKOUR) arrived = horiz < 0.5 && Math.abs(dy) < 0.6 && p.onGround();
        if (step.move() == Step.Move.DESCEND || step.move() == Step.Move.DIG_DOWN)
            arrived = horiz < 0.45 && dy > -0.6 && dy < 0.6 && (p.onGround() || p.isInWater());
        if (arrived) {
            index++; ticksOnStep = 0; stuckTicks = 0; bestDist = Double.MAX_VALUE;
            return index >= steps.size() ? done() : Status.RUNNING;
        }

        // off the path or stuck -> let the navigator re-plan from here
        if (horiz > (step.move() == Step.Move.PARKOUR ? 4.5 : 3.5) || dy < -4 || dy > 2.5) return Status.FAILED;
        double d = horiz + Math.abs(dy);
        if (d < bestDist - 0.05) { bestDist = d; stuckTicks = 0; } else if (++stuckTicks > 60) return Status.FAILED;

        Actions.faceYaw(goal, 35);
        boolean vertical = step.move() == Step.Move.PILLAR || step.move() == Step.Move.SWIM_UP || step.move() == Step.Move.DIG_DOWN;
        o.keyUp.setDown(!vertical && horiz > 0.12);
        boolean flat = step.move() == Step.Move.WALK || step.move() == Step.Move.DIAGONAL;
        boolean canSprint = (sprint && flat && !p.isInWater() && remaining() > 2 && horiz > 0.8) || step.move() == Step.Move.PARKOUR;
        o.keySprint.setDown(canSprint);
        if (canSprint) p.setSprinting(true);

        boolean jump = false;
        if (step.move() == Step.Move.ASCEND && (p.horizontalCollision || horiz < 1.2) && p.onGround()) jump = true;
        if (step.move() == Step.Move.SWIM_UP) jump = true;
        if (step.move() == Step.Move.PARKOUR && p.onGround()) {
            // take off at the edge of our block, already facing the landing spot
            Vec3 from = Vec3.atBottomCenterOf(steps.get(index - 1).pos());
            double fromCenter = Math.hypot(p.getX() - from.x, p.getZ() - from.z);
            if (fromCenter > 0.35 && horiz > 1.2) jump = true;
        }
        if (p.isInWater() && dy > -0.5) jump = true;
        o.keyJump.setDown(jump);
        return Status.RUNNING;
    }

    private Status done() {
        Actions.releaseKeys();
        return Status.DONE;
    }
}

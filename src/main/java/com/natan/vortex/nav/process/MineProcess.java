package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.Waypoints;
import com.natan.vortex.nav.World;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the nearest matching blocks, walks to them, mines them and collects the drops.
 * When the inventory fills up it can walk home, dump loot into a chest next to the "home"
 * waypoint, and come back to keep mining (something Baritone doesn't do).
 */
public class MineProcess implements Process {
    private enum Phase { SEARCH, MINE, PICKUP, GO_HOME, DEPOSIT, RETURN }

    private final Set<Block> targets;
    private final String label;
    private final int radius;
    private final int limit;
    private final boolean depositHome;

    private Phase phase = Phase.SEARCH;
    private final List<BlockPos> found = new ArrayList<>();
    private final Set<BlockPos> blacklist = new HashSet<>();
    private BlockPos current, resumePos;
    private int mined, scanCooldown, pickupTicks, phaseTicks, depositTicks;
    private ExploreProcess wander;

    public MineProcess(Set<Block> targets, String label, int radius, int limit, boolean depositHome) {
        this.targets = targets; this.label = label; this.radius = radius; this.limit = limit; this.depositHome = depositHome;
        for (Block b : targets) com.natan.vortex.memory.WorldMemory.INSTANCE.track(b);
    }

    @Override public String name() { return "Mine"; }

    @Override
    public String status() {
        return label + " · " + mined + (limit > 0 ? "/" + limit : "") + " mined · " + found.size() + " known"
            + (phase == Phase.SEARCH && wander != null ? " · searching" : "")
            + (phase == Phase.GO_HOME || phase == Phase.DEPOSIT ? " · unloading" : "");
    }

    @Override
    public boolean isDone() { return forceStopped() || (limit > 0 && mined >= limit); }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        phaseTicks++;
        if (scanCooldown-- <= 0) { scan(); scanCooldown = 40; }

        if (depositHome && Actions.inventoryFull() && phase != Phase.GO_HOME && phase != Phase.DEPOSIT) {
            if (Waypoints.INSTANCE.get("home") == null) {
                Chat.error("Inventory full and no 'home' waypoint (.sethome). Stopping.");
                mined = Integer.MAX_VALUE / 2; // isDone() with limit 0 is false -> force stop below
                phase = Phase.SEARCH;
            } else {
                resumePos = mc.player.blockPosition();
                setPhase(Phase.GO_HOME);
            }
        }

        switch (phase) {
            case SEARCH -> {
                current = pickNext();
                if (current != null) { wander = null; setPhase(Phase.MINE); }
                else if (wander == null) wander = new ExploreProcess(48);
            }
            case MINE -> {
                if (current == null || !targets.contains(mc.level.getBlockState(current).getBlock())) {
                    if (current != null && mc.level.getBlockState(current).isAir()) { mined++; pickupTicks = 0; setPhase(Phase.PICKUP); }
                    else setPhase(Phase.SEARCH);
                    found.remove(current);
                    return;
                }
                if (phaseTicks > 20 * 60) { blacklist.add(current); found.remove(current); setPhase(Phase.SEARCH); } // unreachable
            }
            case PICKUP -> { if (++pickupTicks > 40) setPhase(Phase.SEARCH); }
            case DEPOSIT -> deposit();
            default -> {}
        }
    }

    private void setPhase(Phase p) { phase = p; phaseTicks = 0; }

    @Override
    public Goal goal() {
        return switch (phase) {
            case SEARCH -> wander != null ? wander.goal() : null;
            case MINE -> new Goal.Reach(current);
            case PICKUP -> new Goal.Block(current);
            case GO_HOME -> new Goal.Near(Waypoints.INSTANCE.get("home").pos(), 2.5);
            case RETURN -> new Goal.Near(resumePos, 2);
            case DEPOSIT -> null;
        };
    }

    @Override
    public void atGoal() {
        switch (phase) {
            case SEARCH -> { if (wander != null) wander.atGoal(); }
            case MINE -> Actions.mine(current);
            case PICKUP -> setPhase(Phase.SEARCH);
            case GO_HOME -> { depositTicks = 0; setPhase(Phase.DEPOSIT); }
            case RETURN -> setPhase(Phase.SEARCH);
            default -> {}
        }
    }

    private void deposit() {
        Minecraft mc = Minecraft.getInstance();
        depositTicks++;
        if (mc.gui.screen() instanceof ContainerScreen screen) {
            var menu = screen.getMenu();
            int chestSlots = menu.getRowCount() * 9;
            // move everything except tools, weapons, armor, food and building blocks
            for (int i = chestSlots; i < menu.slots.size(); i++) {
                var st = menu.getSlot(i).getItem();
                if (st.isEmpty() || st.isDamageableItem() || st.has(net.minecraft.core.component.DataComponents.FOOD)
                    || com.natan.vortex.util.Place.isThrowaway(st) && i >= menu.slots.size() - 9) continue;
                mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
            }
            screen.onClose();
            Chat.info("Unloaded loot at home. Going back to mine.");
            setPhase(resumePos != null ? Phase.RETURN : Phase.SEARCH);
            return;
        }
        if (depositTicks % 20 != 1) return;
        BlockPos chest = nearestChest(mc.player.blockPosition(), 5);
        if (chest == null) {
            Chat.error("No chest within 5 blocks of home. Stopping.");
            mined = Integer.MAX_VALUE / 2;
            return;
        }
        Vec3 hit = Vec3.atCenterOf(chest);
        Actions.lookAt(hit, 90);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, chest, false));
        if (depositTicks > 200) { Chat.error("Couldn't open the chest."); setPhase(Phase.RETURN); }
    }

    private BlockPos nearestChest(BlockPos c, int r) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos best = null; double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
            Block b = mc.level.getBlockState(p).getBlock();
            if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL) {
                double d = p.distSqr(c);
                if (d < bd) { bd = d; best = p.immutable(); }
            }
        }
        return best;
    }

    private BlockPos pickNext() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos me = mc.player.blockPosition();
        found.removeIf(p -> !targets.contains(mc.level.getBlockState(p).getBlock()) || blacklist.contains(p));
        return found.stream().min(Comparator.comparingDouble(p -> p.distSqr(me) + (p.getY() < me.getY() ? 0 : 4))).orElse(null);
    }

    /** Scans a box around the player for target blocks (skips lava-adjacent ones). */
    private void scan() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos me = mc.player.blockPosition();
        int vy = Math.min(radius, 32);
        List<BlockPos> hits = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minY = mc.level.getMinY(), maxY = minY + mc.level.getHeight();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (!World.loaded(me.getX() + x, me.getZ() + z)) continue;
            for (int y = -vy; y <= vy; y++) {
                int wy = me.getY() + y;
                if (wy < minY || wy >= maxY) continue;
                m.set(me.getX() + x, wy, me.getZ() + z);
                if (targets.contains(mc.level.getBlockState(m).getBlock()) && !blacklist.contains(m)) hits.add(m.immutable());
            }
        }
        hits.sort(Comparator.comparingDouble(p -> p.distSqr(me)));
        found.clear();
        found.addAll(hits.subList(0, Math.min(hits.size(), 64)));
        if (found.isEmpty()) // none in view: head toward ores remembered from chunks you've explored
            for (BlockPos p : com.natan.vortex.memory.WorldMemory.INSTANCE.nearest(targets, me, 8))
                if (!blacklist.contains(p)) found.add(p);
    }

    public boolean forceStopped() { return mined >= Integer.MAX_VALUE / 2; }
}

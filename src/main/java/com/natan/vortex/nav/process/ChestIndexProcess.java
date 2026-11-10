package com.natan.vortex.nav.process;

import com.natan.vortex.memory.ChestMemory;
import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

/**
 * ".indexchests [radius]": walks to every container nearby, opens it, lets ChestMemory record
 * the contents, closes it. Afterwards ".where <item>" finds anything in your storage.
 * ".fetch <item>" uses the same walker to go to the right chest and open it for you.
 */
public class ChestIndexProcess implements Process {
    private final Deque<BlockPos> todo = new ArrayDeque<>();
    private final boolean keepOpen;
    private final String label;
    private BlockPos current;
    private int openTicks, total;
    private boolean done;

    public ChestIndexProcess(int radius) {
        this.keepOpen = false;
        this.label = "indexing storage";
        Minecraft mc = Minecraft.getInstance();
        BlockPos me = mc.player.blockPosition();
        BlockPos.betweenClosedStream(me.offset(-radius, -6, -radius), me.offset(radius, 6, radius))
            .filter(p -> isContainer(mc.level.getBlockState(p).getBlock()))
            .map(BlockPos::immutable)
            .sorted(Comparator.comparingDouble(p -> p.distSqr(me)))
            .forEach(todo::add);
        total = todo.size();
        if (total == 0) { com.natan.vortex.util.Chat.error("No containers within " + radius + " blocks."); done = true; }
    }

    /** Fetch mode: go to one specific chest and leave it open for the player. */
    public ChestIndexProcess(BlockPos chest, String item) {
        this.keepOpen = true;
        this.label = "fetching " + item;
        todo.add(chest);
        total = 1;
    }

    static boolean isContainer(net.minecraft.world.level.block.Block b) {
        String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
        return id.equals("chest") || id.equals("trapped_chest") || id.equals("barrel") || id.endsWith("shulker_box");
    }

    @Override public String name() { return keepOpen ? "Fetch" : "Index"; }
    @Override public String status() { return label + " · " + (total - todo.size()) + "/" + total + " · " + ChestMemory.INSTANCE.size() + " known"; }
    @Override public boolean isDone() { return done; }

    @Override
    public void tick() {
        if (current == null) {
            current = todo.poll();
            openTicks = 0;
            if (current == null) { done = true; if (!keepOpen) com.natan.vortex.util.Chat.info("Indexed " + total + " containers. Try .where <item>"); }
        }
    }

    @Override public Goal goal() { return current == null ? null : new Goal.Near(current, 3.2); }

    @Override
    public void atGoal() {
        Minecraft mc = Minecraft.getInstance();
        if (current == null) return;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> scr) {
            if (++openTicks > 12) {
                if (keepOpen) { done = true; return; }
                scr.onClose();
                current = null;
            }
            return;
        }
        if (openTicks++ % 20 == 0) {
            Vec3 hit = Vec3.atCenterOf(current);
            Actions.lookAt(hit, 90);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, current, false));
        }
        if (openTicks > 120) current = null; // couldn't open (blocked chest), skip it
    }
}

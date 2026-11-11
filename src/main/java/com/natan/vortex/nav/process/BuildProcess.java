package com.natan.vortex.nav.process;

import com.natan.vortex.builder.BuildTarget;
import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds any BuildTarget (schematic or selection): walks to the nearest wrong cell, mines what's
 * wrong, places the right block (pulling it from the inventory into the hotbar), and reports
 * anything it lacked the materials for. Reuses the navigator for movement, so it climbs, bridges
 * and avoids hazards on the way.
 */
public class BuildProcess implements Process {
    private final BuildTarget target;
    private final Set<Long> missing = new HashSet<>();  // blocks we couldn't source
    private final Set<Long> blocked = new HashSet<>();  // unreachable cells
    private final Set<String> missingNames = new HashSet<>();
    private BlockPos current;
    private int placed, mined, scanCd, stuck;
    private double lastD = Double.MAX_VALUE;
    private boolean done;

    public BuildProcess(BuildTarget target) { this.target = target; }

    @Override public String name() { return "Build"; }
    @Override public String status() { return target.label() + " · " + placed + " placed · " + mined + " cleared" + (missingNames.isEmpty() ? "" : " · missing " + missingNames.size() + " types"); }
    @Override public boolean isDone() { return done; }

    private static long k(BlockPos p) { return p.asLong(); }

    private boolean wrong(Minecraft mc, BlockPos w) {
        BlockState want = target.desired(w);
        BlockState have = mc.level.getBlockState(w);
        if (want == null) return !have.isAir() && have.getFluidState().isEmpty() && have.getDestroySpeed(mc.level, w) >= 0;
        return have.getBlock() != want.getBlock();
    }

    private void rescan(Minecraft mc) {
        BlockPos me = mc.player.blockPosition();
        BlockPos prev = current;
        current = target.cells().stream()
            .filter(c -> !blocked.contains(k(c)) && !missing.contains(k(c)) && wrong(mc, c))
            .min(Comparator.comparingDouble(c -> c.distSqr(me)))
            .orElse(null);
        if (current == null || !current.equals(prev)) { lastD = Double.MAX_VALUE; stuck = 0; } // keep counting on the same cell
        if (current == null) finish();
    }

    private void finish() {
        done = true;
        if (!missingNames.isEmpty()) Chat.error("Build stopped: missing " + String.join(", ", missingNames));
        else Chat.info("Build complete: " + placed + " placed, " + mined + " cleared.");
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (scanCd-- <= 0 || current == null) { rescan(mc); scanCd = 20; }
        if (current == null) return;
        if (!wrong(mc, current)) { current = null; return; } // someone/we fixed it
        double d = mc.player.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(current));
        if (d < lastD - 0.1) { lastD = d; stuck = 0; } else if (++stuck > 160) { blocked.add(k(current)); current = null; }
    }

    @Override
    public Goal goal() { return current == null ? null : new Goal.Reach(current); }

    @Override
    public void atGoal() {
        Minecraft mc = Minecraft.getInstance();
        if (current == null) return;
        BlockState want = target.desired(current);
        BlockState have = mc.level.getBlockState(current);
        // clear a wrong block first
        // (water, lava, grass etc. are replaced by placing straight into them)
        if (!have.isAir() && !(want != null && have.canBeReplaced()) && (want == null || have.getBlock() != want.getBlock())) {
            stuck = 0; // mining is progress, even on slow blocks
            if (Actions.mine(current)) mined++;
            return;
        }
        if (want == null) { current = null; return; } // cleared
        // place the right block
        Block block = want.getBlock();
        int slot = Inv.findHotbar(s -> isBlock(s, block));
        if (slot == -1) {
            int inv = Inv.findAny(s -> isBlock(s, block));
            if (inv == -1) {
                missing.add(k(current));
                missingNames.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath());
                current = null;
                return;
            }
            Inv.click(Inv.toMenuSlot(inv), Inv.selected(), ContainerInput.SWAP);
            return;
        }
        if (Place.atOriented(current, slot, want, true)) { placed++; current = null; }
    }

    @Override
    public boolean onPathFailed() {
        if (current == null) return false;
        blocked.add(k(current)); // unreachable: skip this cell instead of stopping the whole build
        current = null;
        return true;
    }

    private static boolean isBlock(ItemStack s, Block b) {
        return s.getItem() instanceof BlockItem bi && bi.getBlock() == b;
    }
}

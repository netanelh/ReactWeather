package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    private final NumberSetting extend = add(new NumberSetting("Extend", "Also place this many blocks ahead of you", 0, 0, 4, 1));
    private final BoolSetting tower = add(new BoolSetting("Tower", "Hold jump to build straight up fast", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Look at the placement (server side)", true));
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", "Return to your previous hotbar slot", true));

    public Scaffold() { super("Scaffold", "Places blocks under your feet as you walk", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int slot = Inv.findHotbar(s -> s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().isSolidRender());
        if (slot == -1) return;
        int prev = Inv.selected();

        BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        boolean placed = place(below, slot);

        if (extend.i() > 0) {
            Vec3 dir = Vec3.directionFromRotation(0, mc.player.getYRot());
            for (int i = 1; i <= extend.i(); i++) place(BlockPos.containing(mc.player.getX() + dir.x * i, mc.player.getY() - 0.5, mc.player.getZ() + dir.z * i), slot);
        }

        if (placed && tower.on() && mc.options.keyJump.isDown() && !com.natan.vortex.util.Move.isMoving()) {
            var v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x * 0.3, 0.42, v.z * 0.3);
        }
        if (switchBack.on() && Inv.selected() != prev) Inv.select(prev);
    }

    private boolean place(BlockPos pos, int slot) {
        if (!mc.level.getBlockState(pos).canBeReplaced()) return false;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = pos.relative(d);
            if (mc.level.getBlockState(neighbor).canBeReplaced()) continue;
            Direction face = d.getOpposite();
            Vec3 hit = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (hit.distanceTo(mc.player.getEyePosition()) > 4.5) continue;
            Inv.select(slot);
            if (rotate.on()) { float[] a = Rotations.anglesTo(hit); Rotations.sendSilent(a[0], a[1]); }
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, neighbor, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }
}

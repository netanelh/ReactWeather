package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click places the held block in mid-air where you look, no support needed. */
public class AirPlace extends Module {
    private final NumberSetting distance = add(new NumberSetting("Distance", "How far in front of you", 4, 1, 5, 0.5));
    private boolean wasDown;

    public AirPlace() { super("AirPlace", "Place blocks in the air where you look", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean down = mc.options.keyUse.isDown();
        if (down && !wasDown && mc.gui.screen() == null && mc.player.getMainHandItem().getItem() instanceof BlockItem
            && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS)) {
            Vec3 at = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(distance.d()));
            BlockPos pos = BlockPos.containing(at);
            if (mc.level.getBlockState(pos).canBeReplaced())
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        wasDown = down;
    }
}

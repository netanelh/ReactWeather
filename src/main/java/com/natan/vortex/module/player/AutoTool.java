package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTool extends Module {
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", "Return to your previous slot when you stop mining", true));
    private final BoolSetting saveTools = add(new BoolSetting("Save Tools", "Don't use tools with almost no durability left", true));

    private int prevSlot = -1;

    public AutoTool() { super("AutoTool", "Picks the fastest tool for the block you mine", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean mining = mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult;
        if (!mining) {
            if (switchBack.on() && prevSlot != -1) { Inv.select(prevSlot); prevSlot = -1; }
            return;
        }
        BlockState state = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
        if (state.isAir()) return;
        int best = -1;
        float bestSpeed = mc.player.getMainHandItem().getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            var st = mc.player.getInventory().getItem(i);
            if (saveTools.on() && st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() < 5) continue;
            float sp = st.getDestroySpeed(state);
            if (sp > bestSpeed) { bestSpeed = sp; best = i; }
        }
        if (best != -1 && best != Inv.selected()) {
            if (prevSlot == -1) prevSlot = Inv.selected();
            Inv.select(best);
        }
    }
}

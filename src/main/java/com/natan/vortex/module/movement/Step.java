package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends Module {
    private static final double VANILLA = 0.6;
    private final NumberSetting height = add(new NumberSetting("Height", "Max block height to walk up", 1, 0.6, 4, 0.5));
    private final BoolSetting notSneaking = add(new BoolSetting("Not Sneaking", "Disable while sneaking", true));

    public Step() { super("Step", "Walk up full blocks without jumping", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr == null) return;
        boolean active = !(notSneaking.on() && mc.player.isShiftKeyDown());
        attr.setBaseValue(active ? height.d() : VANILLA);
    }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(VANILLA);
    }
}

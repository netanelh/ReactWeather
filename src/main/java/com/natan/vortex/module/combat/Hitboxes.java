package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Targets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Read by EntityMixin.getPickRadius: makes entities easier to click. */
public class Hitboxes extends Module {
    private final NumberSetting expand = add(new NumberSetting("Expand", "Extra hitbox size (blocks)", 0.4, 0, 2, 0.05));
    private final BoolSetting players = add(new BoolSetting("Players", "Expand player hitboxes", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Expand mob hitboxes", true));

    public Hitboxes() { super("Hitboxes", "Bigger enemy hitboxes = easier hits", Category.COMBAT); }

    public float extra(Entity e) {
        if (!isEnabled() || e == mc.player) return 0;
        if (e instanceof Player) return players.on() ? expand.f() : 0;
        return mobs.on() && !Targets.isPassive(e) ? expand.f() : 0;
    }
}

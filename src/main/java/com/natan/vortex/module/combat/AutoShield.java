package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Targets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;

/** Raises your offhand shield when an enemy or projectile gets close, lowers it to hit back. */
public class AutoShield extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Block when enemies are this close", 3.5, 1, 8, 0.5));
    private boolean blocking;

    public AutoShield() { super("AutoShield", "Blocks with your shield automatically when threatened", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.getOffhandItem().is(Items.SHIELD)) { release(); return; }
        boolean threat = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            double d = e.distanceTo(mc.player);
            if (e instanceof Projectile pr && pr.getOwner() != mc.player && d < 6 && pr.getDeltaMovement().lengthSqr() > 0.1) { threat = true; break; }
            if (Targets.valid(e, true, true, false) && d < range.d()) { threat = true; break; }
        }
        // drop the shield on the tick we're about to swing (full cooldown) so we can counter-attack
        if (threat && mc.player.getAttackStrengthScale(0.5f) >= 1f && mc.options.keyAttack.isDown()) threat = false;
        if (threat) { com.natan.vortex.util.Keys.hold(mc.options.keyUse, true); blocking = true; } else release();
    }

    @Override public void onWorldChange() { if (blocking && mc.options != null) com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); blocking = false; }

    private void release() { if (blocking) { com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); blocking = false; } }

    @Override public void onDisable() { release(); }
}

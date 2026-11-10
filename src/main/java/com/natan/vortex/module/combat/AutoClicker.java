package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", "Lowest clicks per second", 9, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", "Highest clicks per second", 13, 1, 20, 1));
    private final BoolSetting onlyHolding = add(new BoolSetting("Only Holding", "Click only while you hold attack", true));
    private final BoolSetting onlyEntities = add(new BoolSetting("Only Entities", "Don't click air or blocks", true));

    private double timer;

    public AutoClicker() { super("AutoClicker", "Clicks for you at a randomized CPS", Category.COMBAT); }

    @Override public String info() { return minCps.i() + "-" + maxCps.i(); }

    @Override
    public void onTick() {
        if (!inGame() || mc.gui.screen() != null) return;
        if (onlyHolding.on() && !mc.options.keyAttack.isDown()) return;
        if ((timer -= 1) > 0) return;
        double lo = Math.min(minCps.d(), maxCps.d()), hi = Math.max(minCps.d(), maxCps.d());
        double cps = lo + ThreadLocalRandom.current().nextDouble() * (hi - lo + 0.0001);
        timer = 20.0 / cps;
        if (mc.hitResult instanceof EntityHitResult e) {
            mc.gameMode.attack(mc.player, e.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (!onlyEntities.on()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}

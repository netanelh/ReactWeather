package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Targets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", "Attack players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Attack hostile mobs", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", "Attack passive mobs", false));
    private final NumberSetting extraDelay = add(new NumberSetting("Extra Delay", "Random extra ticks after cooldown (0 = none)", 1, 0, 10, 1));

    private int wait;

    public TriggerBot() { super("TriggerBot", "Attacks whatever entity your crosshair is on", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || !(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!Targets.valid(e, players.on(), hostiles.on(), passives.on())) return;
        if (mc.player.getAttackStrengthScale(0.5f) < 1f) { wait = -1; return; }
        if (wait < 0) wait = extraDelay.i() == 0 ? 0 : ThreadLocalRandom.current().nextInt(extraDelay.i() + 1);
        if (wait-- > 0) return;
        mc.gameMode.attack(mc.player, e);
        mc.player.swing(InteractionHand.MAIN_HAND);
        wait = -1;
    }
}

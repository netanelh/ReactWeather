package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Panic escape: when you're about to die near an enemy, throws a pearl directly away from them. */
public class AutoPearl extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", "Escape at or below this many hearts", 4, 1, 10, 0.5));
    private final NumberSetting enemyRange = add(new NumberSetting("Enemy Range", "Only if a player is this close", 6, 2, 16, 1));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Seconds between escapes", 5, 1, 30, 1));

    private int timer;

    public AutoPearl() { super("AutoPearl", "Escapes with an ender pearl when you're about to die", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (timer > 0) { timer--; return; }
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f > health.d()) return;
        Player enemy = null; double best = enemyRange.d();
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            double d = p.distanceTo(mc.player);
            if (d < best) { best = d; enemy = p; }
        }
        if (enemy == null) return;
        int slot = Inv.findHotbar(s -> s.is(Items.ENDER_PEARL));
        if (slot == -1) return;
        double dx = mc.player.getX() - enemy.getX(), dz = mc.player.getZ() - enemy.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        int prev = Inv.selected();
        Inv.select(slot);
        Rotations.sendSilent(yaw, -25f); // ~25 deg up = max distance
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        timer = cooldown.i() * 20;
        Chat.info("AutoPearl: escaped from " + enemy.getName().getString());
    }
}

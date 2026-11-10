package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Items2;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** When the player you're fighting raises a shield, swap to an axe so the next hit disables it. */
public class ShieldBreaker extends Module {
    public ShieldBreaker() { super("ShieldBreaker", "Swaps to an axe when your target blocks with a shield", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var ka = com.natan.vortex.VortexClient.modules.get(KillAura.class).target();
        Player t = ka instanceof Player p ? p : (mc.hitResult instanceof EntityHitResult e && e.getEntity() instanceof Player p2 ? p2 : null);
        if (t == null || !t.isBlocking()) return;
        int axe = Inv.findHotbar(s -> Items2.id(s).endsWith("_axe"));
        if (axe != -1) Inv.select(axe);
    }
}

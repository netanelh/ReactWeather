package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Switches to your strongest hotbar weapon whenever you attack (hooked in MultiPlayerGameModeMixin). */
public class AutoWeapon extends Module {
    public AutoWeapon() { super("AutoWeapon", "Switches to your best weapon when you attack", Category.COMBAT); }

    public static void switchToBest(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        int best = -1;
        double bestScore = score(mc.player.getMainHandItem());
        for (int i = 0; i < 9; i++) {
            double sc = score(mc.player.getInventory().getItem(i));
            if (sc > bestScore) { bestScore = sc; best = i; }
        }
        if (best != -1) Inv.select(best);
    }

    private static double score(ItemStack s) {
        if (s.isEmpty()) return 0;
        String id = com.natan.vortex.util.Items2.id(s);
        double tier = id.contains("netherite") ? 6 : id.contains("diamond") ? 5 : id.contains("iron") ? 4
            : id.contains("stone") ? 3 : id.contains("gold") ? 2 : id.contains("wood") ? 1.5 : id.contains("copper") ? 3.5 : 1;
        double type = id.contains("sword") ? 3 : id.contains("mace") ? 2.8 : id.contains("_axe") ? 2.5 : id.contains("trident") ? 2.4 : 0;
        if (type == 0) return 0;
        return type * 10 + tier + (s.isEnchanted() ? s.getEnchantments().size() * 0.2 : 0);
    }
}

package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Items2;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/** One-shot: swaps elytra <-> best chestplate, then turns itself off. Bind it to a key. */
public class ChestSwap extends Module {
    public ChestSwap() { super("ChestSwap", "Swap between elytra and chestplate (bind a key)", Category.PLAYER); showInHud = false; }

    @Override
    public void onEnable() {
        if (inGame()) {
            boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
            int slot = wearingElytra
                ? Inv.findAny(s -> Items2.id(s).endsWith("_chestplate"))
                : Inv.findAny(s -> s.is(Items.ELYTRA));
            if (slot != -1) {
                Inv.click(Inv.toMenuSlot(slot), 0, ContainerInput.PICKUP);
                Inv.click(6, 0, ContainerInput.PICKUP); // chest armor slot
                Inv.click(Inv.toMenuSlot(slot), 0, ContainerInput.PICKUP);
            }
        }
        setEnabledSilently(false);
    }
}

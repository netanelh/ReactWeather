package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Items2;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;

/** With a furnace open, auto-collects the finished output and refuels it from coal in your inventory. */
public class AutoSmelt extends Module {
    private final BoolSetting refuel = add(new BoolSetting("Refuel", "Top up fuel from coal/charcoal", true));
    private int timer;

    public AutoSmelt() { super("AutoSmelt", "Collects furnace output and refuels it", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || !(mc.gui.screen() instanceof AbstractFurnaceScreen<?> screen)) return;
        if (timer-- > 0) return;
        timer = 5;
        AbstractFurnaceMenu menu = screen.getMenu();
        // take the result
        if (!menu.getSlot(AbstractFurnaceMenu.RESULT_SLOT).getItem().isEmpty())
            mc.gameMode.handleContainerInput(menu.containerId, AbstractFurnaceMenu.RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, mc.player);
        // refuel if empty
        if (refuel.on() && menu.getSlot(AbstractFurnaceMenu.FUEL_SLOT).getItem().isEmpty()) {
            for (int i = 3; i < menu.slots.size(); i++) {
                String id = Items2.id(menu.getSlot(i).getItem());
                if (id.equals("coal") || id.equals("charcoal") || id.equals("coal_block")) {
                    mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(menu.containerId, AbstractFurnaceMenu.FUEL_SLOT, 0, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player); // put the rest back
                    break;
                }
            }
        }
    }
}

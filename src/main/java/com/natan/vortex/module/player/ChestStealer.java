package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ContainerInput;

public class ChestStealer extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between items (0 = instant)", 1, 0, 10, 1));
    private final BoolSetting autoClose = add(new BoolSetting("Auto Close", "Close the chest when it's empty", true));

    private int timer;

    public ChestStealer() { super("ChestStealer", "Empties chests into your inventory when opened", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || !(mc.gui.screen() instanceof ContainerScreen screen)) return;
        if (timer-- > 0) return;
        var menu = screen.getMenu();
        int size = menu.getRowCount() * 9;
        int moves = delay.i() == 0 ? size : 1;
        boolean any = false;
        for (int i = 0; i < size && moves > 0; i++) {
            if (!menu.getSlot(i).hasItem()) continue;
            mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
            moves--; any = true;
        }
        timer = delay.i();
        if (!any && autoClose.on()) screen.onClose();
    }
}

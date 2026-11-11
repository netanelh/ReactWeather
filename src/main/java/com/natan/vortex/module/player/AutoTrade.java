package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Chat;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * With a villager trade screen open, repeatedly does a chosen trade until you run out of materials
 * or your inventory fills. Great for rolling or bulk-buying (emeralds, mending books, ...).
 */
public class AutoTrade extends Module {
    private final NumberSetting trade = add(new NumberSetting("Trade #", "Which offer to repeat (1 = first)", 1, 1, 12, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between trades", 4, 1, 20, 1));
    private final BoolSetting stopFull = add(new BoolSetting("Stop When Full", "Stop if the inventory is full", true));
    private int timer, empties;

    public AutoTrade() { super("AutoTrade", "Repeats a villager trade automatically", Category.PLAYER); }
    @Override public void onEnable() { empties = 0; }

    @Override
    public void onTick() {
        if (!inGame() || !(mc.gui.screen() instanceof MerchantScreen screen)) return;
        if (timer-- > 0) return;
        timer = delay.i();
        MerchantMenu menu = screen.getMenu();
        int idx = trade.i() - 1;
        if (idx >= menu.getOffers().size()) { Chat.error("No trade #" + trade.i()); setEnabled(false); return; }
        if (stopFull.on() && com.natan.vortex.nav.Actions.inventoryFull()) { Chat.info("Inventory full, stopping AutoTrade."); setEnabled(false); return; }
        // select the trade; the server fills the result slot
        mc.getConnection().send(new ServerboundSelectTradePacket(idx));
        menu.setSelectionHint(idx);
        menu.tryMoveItems(idx);
        // take the result (slot 2) into the inventory
        if (!menu.getSlot(2).getItem().isEmpty()) {
            mc.gameMode.handleContainerInput(menu.containerId, 2, 0, ContainerInput.QUICK_MOVE, mc.player);
            empties = 0;
        } else if (++empties > 3) { Chat.info("Out of materials for trade #" + trade.i() + ", stopping."); setEnabled(false); }
    }
}

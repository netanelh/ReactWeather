package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Chat;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class Notifier extends Module {
    private final BoolSetting joins = add(new BoolSetting("Join/Leave", "Tell you when players join or leave the server", true));
    private final BoolSetting visualRange = add(new BoolSetting("Visual Range", "Tell you when players enter/leave render distance", true));
    private final BoolSetting modules = add(new BoolSetting("Module Toggles", "Chat message when you toggle a module", false));

    private final NumberSetting durability = add(new NumberSetting("Low Durability", "Warn in chat when held/worn gear drops below this % (0 = off)", 10, 0, 50, 1));

    private static final EquipmentSlot[] GEAR = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final Set<EquipmentSlot> warned = java.util.EnumSet.noneOf(EquipmentSlot.class);

    private Set<String> online = null, visible = new HashSet<>();
    /** ticks to stay quiet after joining/changing world while the tab list and entities fill in */
    private int grace;

    public Notifier() { super("Notifier", "Join/leave and players-in-range alerts", Category.PLAYER); }

    public boolean moduleToggles() { return isEnabled() && modules.on(); }

    @Override
    public void onTick() {
        if (!inGame() || mc.getConnection() == null) return;
        Set<String> nowOnline = new HashSet<>();
        for (PlayerInfo i : mc.getConnection().getOnlinePlayers()) nowOnline.add(i.getProfile().name());
        if (joins.on() && online != null && grace == 0) {
            for (String n : nowOnline) if (!online.contains(n)) Chat.info("§a+ " + n + " joined");
            for (String n : online) if (!nowOnline.contains(n)) Chat.info("§c- " + n + " left");
        }
        online = nowOnline;

        Set<String> nowVisible = new HashSet<>();
        for (Player p : mc.level.players()) if (p != mc.player && !p.getUUID().equals(FakePlayer.ID)) nowVisible.add(p.getName().getString());
        if (visualRange.on() && grace == 0) {
            for (String n : nowVisible) if (!visible.contains(n)) Chat.info("§e" + n + " entered your render distance");
            for (String n : visible) if (!nowVisible.contains(n)) Chat.info("§7" + n + " left your render distance");
        }
        visible = nowVisible;
        if (grace > 0) grace--;

        if (durability.i() > 0) for (EquipmentSlot slot : GEAR) {
            ItemStack st = mc.player.getItemBySlot(slot);
            int left = st.getMaxDamage() - st.getDamageValue();
            boolean low = st.isDamageableItem() && left * 100 < durability.i() * st.getMaxDamage();
            if (low && warned.add(slot)) Chat.error(st.getHoverName().getString() + " is almost broken (" + left + " durability left)");
            else if (!low) warned.remove(slot);
        }
    }

    @Override public void onWorldChange() { online = null; visible = new HashSet<>(); warned.clear(); grace = 40; }

    @Override public void onDisable() { online = null; visible.clear(); warned.clear(); }
}

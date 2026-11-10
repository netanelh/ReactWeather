package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Friends;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AutoLog extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", "Disconnect at or below these hearts (0 = off)", 3, 0, 10, 0.5));
    private final BoolSetting onPlayer = add(new BoolSetting("On Player", "Disconnect when a non-friend player comes close", false));
    private final NumberSetting playerRange = add(new NumberSetting("Player Range", "Distance for On Player", 32, 4, 128, 4).visibleWhen(onPlayer::on));
    private final BoolSetting disableAfter = add(new BoolSetting("Turn Off After", "Turn AutoLog off so you can reconnect safely", true));

    public AutoLog() { super("AutoLog", "Disconnects you when health is low or a player shows up", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.isCreative()) return;
        String reason = null;
        float hearts = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f;
        if (health.d() > 0 && hearts <= health.d()) reason = "Health was " + hearts + " hearts";
        if (reason == null && onPlayer.on()) {
            for (Player p : mc.level.players()) {
                if (p == mc.player || Friends.is(p.getName().getString())) continue;
                if (p.distanceTo(mc.player) <= playerRange.d()) { reason = p.getName().getString() + " came within " + Math.round(p.distanceTo(mc.player)) + " blocks"; break; }
            }
        }
        if (reason == null) return;
        if (disableAfter.on()) setEnabledSilently(false);
        mc.getConnection().getConnection().disconnect(Component.literal("[Zephex AutoLog] " + reason));
    }
}

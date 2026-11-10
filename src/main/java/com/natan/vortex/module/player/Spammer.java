package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/** Repeats chat messages set with ".spam add <text>". */
public class Spammer extends Module {
    public static final List<String> messages = new ArrayList<>(List.of("Zephex on top"));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Seconds between messages", 10, 1, 300, 1));
    private final BoolSetting random = add(new BoolSetting("Random Order", "Pick messages randomly", false));
    private int timer, index;

    public Spammer() { super("Spammer", "Sends your messages on a timer (.spam add/clear/list)", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || messages.isEmpty() || timer-- > 0) return;
        timer = delay.i() * 20;
        String msg = random.on() ? messages.get((int) (Math.random() * messages.size())) : messages.get(index++ % messages.size());
        mc.getConnection().sendChat(msg);
    }
}

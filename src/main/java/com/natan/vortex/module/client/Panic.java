package com.natan.vortex.module.client;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.util.Chat;

/** Bind a key to this: one press turns every module off and stops the navigator. */
public class Panic extends Module {
    public Panic() { super("Panic", "Instantly turns off every module (bind a key!)", Category.CLIENT); showInHud = false; }

    @Override
    public void onEnable() {
        Navigator.INSTANCE.stop(false);
        for (Module m : VortexClient.modules.all()) {
            if (m == this || m.category == Category.CLIENT || m instanceof com.natan.vortex.module.pathing.NavSettings) continue;
            m.setEnabled(false);
        }
        Chat.info("Panic: all modules off.");
        setEnabledSilently(false);
    }
}

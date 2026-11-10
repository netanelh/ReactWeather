package com.natan.vortex.module.pathing;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.nav.Process;

/** A module whose on/off state starts/stops a navigator process. */
public abstract class ProcessModule extends Module {
    private Process running;

    protected ProcessModule(String name, String description) { super(name, description, Category.PATHING); }

    protected abstract Process create();

    @Override
    public void onEnable() {
        if (!inGame()) { setEnabledSilently(false); return; }
        running = create();
        if (running == null) { setEnabledSilently(false); return; }
        Navigator.INSTANCE.start(running);
    }

    @Override
    public void onDisable() {
        if (running != null && Navigator.INSTANCE.process() == running) Navigator.INSTANCE.stop(true);
        running = null;
    }

    /** Called by the module manager when the navigator stopped our process on its own. */
    public void processEnded(Process p) {
        if (p == running) { running = null; setEnabledSilently(false); }
    }

    @Override public String info() {
        return running != null && Navigator.INSTANCE.process() == running ? running.status() : null;
    }
}

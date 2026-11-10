package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

/** Drawn by HudRenderer: warns about threats you might not see. */
public class DangerAlert extends Module {
    public final NumberSetting playerRange = add(new NumberSetting("Player Range", "Warn when a player is this close", 24, 4, 96, 4));
    public final BoolSetting creepers = add(new BoolSetting("Creepers/TNT", "Warn about creepers and lit TNT nearby", true));
    public final BoolSetting armor = add(new BoolSetting("Armor", "Warn when armor is about to break", true));
    public final BoolSetting totems = add(new BoolSetting("Totems", "Warn when you have no totem left", true));
    public final BoolSetting fall = add(new BoolSetting("Fall", "Warn on deadly falls", true));

    public DangerAlert() { super("DangerAlert", "Warnings for nearby players, creepers, TNT, low armor", Category.RENDER); showInHud = false; }
}

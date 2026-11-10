package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;

public class Hud extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", "Client name in the top-left", true));
    public final BoolSetting moduleList = add(new BoolSetting("Module List", "Active modules in the top-right", true));
    public final BoolSetting showInfo = add(new BoolSetting("Module Info", "Show mode/target next to module names", true));
    public final ModeSetting sort = add(new ModeSetting("Sort", "Module list order", "Length", "Length", "ABC"));
    public final BoolSetting coords = add(new BoolSetting("Coordinates", "XYZ + Nether/Overworld conversion", true));
    public final BoolSetting fps = add(new BoolSetting("FPS", "Frames per second", true));
    public final BoolSetting speed = add(new BoolSetting("Speed", "Your speed in blocks per second", false));
    public final BoolSetting armor = add(new BoolSetting("Armor", "Armor durability above the hotbar", true));
    public final BoolSetting background = add(new BoolSetting("Background", "Dark backdrop behind HUD text", true));

    public Hud() {
        super("HUD", "On-screen info overlay", Category.CLIENT);
        showInHud = false;
        setEnabled(true);
    }
}

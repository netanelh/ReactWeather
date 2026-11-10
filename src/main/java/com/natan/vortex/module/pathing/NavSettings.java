package com.natan.vortex.module.pathing;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

/** Global navigator settings. Toggling it off stops whatever is running. */
public class NavSettings extends Module {
    public final BoolSetting allowBreak = add(new BoolSetting("Allow Break", "Mine through blocks that are in the way", true));
    public final BoolSetting allowPlace = add(new BoolSetting("Allow Place", "Bridge gaps and pillar up with throwaway blocks", true));
    public final NumberSetting maxFall = add(new NumberSetting("Max Fall", "Highest drop it will take without water (blocks)", 3, 1, 20, 1));
    public final BoolSetting sprint = add(new BoolSetting("Sprint", "Sprint on long straight stretches", true));
    public final BoolSetting parkour = add(new BoolSetting("Parkour", "Sprint-jump over 1-2 block gaps instead of walking around", true));
    public final BoolSetting selfDefense = add(new BoolSetting("Self Defense", "Fight back against hostile mobs that get close while pathing", true));
    public final BoolSetting autoEat = add(new BoolSetting("Auto Eat", "Stop and eat from the hotbar when hungry during tasks", true));
    public final NumberSetting lowHealth = add(new NumberSetting("Pause Below HP", "Pause the task below these hearts until you heal (0 = off)", 4, 0, 10, 0.5));
    public final BoolSetting avoidWater = add(new BoolSetting("Avoid Water", "Prefer land routes over swimming", true));
    public final NumberSetting maxNodes = add(new NumberSetting("Search Size", "Thousands of nodes per search (bigger = smarter, slower)", 120, 20, 600, 10));
    public final NumberSetting timeout = add(new NumberSetting("Think Time", "Max seconds per path search", 3, 1, 10, 1));
    public final BoolSetting hudStatus = add(new BoolSetting("HUD Status", "Show what the navigator is doing", true));
    public final BoolSetting debug = add(new BoolSetting("Debug", "Print every path calculation to chat", false));

    public NavSettings() {
        super("Navigator", "Pathfinding engine (.help for commands)", Category.PATHING);
        showInHud = false;
        setEnabled(true);
    }

    @Override public void onDisable() { Navigator.INSTANCE.stop(true); }
}

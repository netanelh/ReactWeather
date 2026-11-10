package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.FollowProcess;
import com.natan.vortex.setting.NumberSetting;

public class Follow extends ProcessModule {
    private final NumberSetting distance = add(new NumberSetting("Distance", "How close to stay (blocks)", 3, 1, 10, 0.5));
    public static String target; // set by the .follow command; null = nearest player

    public Follow() { super("Follow", "Follows a player around (.follow <name>)"); }

    @Override protected Process create() { return new FollowProcess(target, distance.d()); }
}

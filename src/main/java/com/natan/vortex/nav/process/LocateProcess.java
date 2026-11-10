package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ".locate village" / ".locate biome cherry_grove": runs the server's /locate (needs OP or cheats,
 * which you have on your own server / singleplayer), reads the answer from chat, then walks there —
 * any distance, through unloaded chunks, re-planning as terrain loads.
 */
public class LocateProcess implements Process {
    private static final Pattern COORDS = Pattern.compile("\\[(-?\\d+), ?(~|-?\\d+), ?(-?\\d+)\\]");
    public static LocateProcess waiting;

    private final String label;
    private Goal goal;
    private int waitTicks;
    private boolean done;

    public LocateProcess(String what, boolean biome) {
        this.label = what;
        String id = what.contains(":") ? what : "minecraft:" + what;
        String cmd = biome ? "locate biome " + id : "locate structure " + (what.equals("village") ? "#minecraft:village" : id);
        waiting = this;
        Minecraft.getInstance().getConnection().sendCommand(cmd);
    }

    /** Called from the chat listener for every system message. */
    public static void onSystemMessage(String text) {
        LocateProcess p = waiting;
        if (p == null || p.goal != null) return;
        Matcher m = COORDS.matcher(text);
        if (!m.find()) return;
        int x = Integer.parseInt(m.group(1)), z = Integer.parseInt(m.group(3));
        p.goal = m.group(2).equals("~") ? new Goal.XZ(x, z) : new Goal.Near(new net.minecraft.core.BlockPos(x, Integer.parseInt(m.group(2)), z), 3);
        waiting = null;
        Chat.info("Found " + p.label + " at " + x + ", " + z + ". Walking there.");
    }

    @Override public String name() { return "Locate"; }
    @Override public String status() { return goal == null ? "asking the server about " + label + "..." : label; }

    @Override
    public void tick() {
        if (goal == null && ++waitTicks > 100) {
            Chat.error("No answer from /locate. You need OP or cheats enabled, and the name must be valid (e.g. village, stronghold, ancient_city, mansion, biome cherry_grove).");
            waiting = null;
            done = true;
        }
    }

    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { done = true; }
    @Override public boolean isDone() { return done; }
    @Override public void onStop() { if (waiting == this) waiting = null; }
}

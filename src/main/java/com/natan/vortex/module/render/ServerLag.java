package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import net.minecraft.client.Minecraft;

/** Server TPS meter and "server not responding" warning (drawn by HudMore). */
public class ServerLag extends Module {
    private static long lastPacket, lastGameTime;
    private static final double[] samples = new double[6];
    private static int idx, count;

    public ServerLag() { super("ServerLag", "Shows server TPS and warns when the server freezes", Category.RENDER); showInHud = false; }

    public static void onTime(long gameTime) {
        long now = System.currentTimeMillis();
        if (lastPacket != 0 && gameTime > lastGameTime) {
            double sec = (now - lastPacket) / 1000.0;
            if (sec > 0.05) { samples[idx++ % samples.length] = Math.min(20, (gameTime - lastGameTime) / sec); count = Math.min(count + 1, samples.length); }
        }
        lastPacket = now; lastGameTime = gameTime;
    }

    public static double tps() {
        if (count == 0) return 20;
        double s = 0; for (int i = 0; i < count; i++) s += samples[i];
        return s / count;
    }

    /** Seconds since the server last ticked for us, or 0 if fine. */
    public static double frozenFor() {
        if (lastPacket == 0 || Minecraft.getInstance().getConnection() == null) return 0;
        double s = (System.currentTimeMillis() - lastPacket) / 1000.0;
        return s > 2.0 ? s : 0;
    }

    public static void reset() { lastPacket = 0; count = 0; }
}

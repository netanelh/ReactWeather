package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.module.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/** Small info widgets stacked down the left side of the screen. */
final class HudElements {
    private static final Deque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    private static boolean lWas, rWas;

    private HudElements() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc, ClickGui gui) {
        var m = VortexClient.modules;
        Font f = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        int acc = gui.accent(0);
        trackClicks(mc);

        // left column, below the watermark (and radar, if it's in the top-left)
        int y = 20;
        Radar radar = m.get(Radar.class);
        if (radar.isEnabled() && radar.position.is("Top Left")) y += radar.size.i() + 6;
        List<String[]> lines = new ArrayList<>();

        if (m.isActive(PingHud.class) && mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            lines.add(new String[]{"Ping ", (info == null ? 0 : info.getLatency()) + "ms"});
        }
        if (m.isActive(ClockHud.class)) {
            long t = (mc.level.getGameTime() + 6000) % 24000;
            String game = String.format("%02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
            lines.add(new String[]{"Time ", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "  §7day " + (mc.level.getGameTime() / 24000) + " " + game});
        }
        if (m.isActive(Compass.class)) {
            float yaw = ((mc.player.getYRot() % 360) + 360) % 360;
            String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            lines.add(new String[]{"Facing ", dirs[Math.round(yaw / 45f) % 8] + " " + Math.round(yaw) + "°"});
        }
        if (m.isActive(ServerInfo.class) && mc.getConnection() != null) {
            String ip = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer";
            lines.add(new String[]{"Server ", ip + " §7(" + mc.getConnection().getOnlinePlayers().size() + " online)"});
        }
        if (m.isActive(TotemCounter.class)) {
            int c = 0;
            for (int i = 0; i < 36; i++) { ItemStack s = mc.player.getInventory().getItem(i); if (s.is(Items.TOTEM_OF_UNDYING)) c += s.getCount(); }
            if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) c++;
            lines.add(new String[]{"Totems ", (c == 0 ? "§c" : "") + c});
        }
        if (m.isActive(BlockInfo.class) && mc.hitResult instanceof BlockHitResult bh && !mc.level.getBlockState(bh.getBlockPos()).isAir()) {
            var st = mc.level.getBlockState(bh.getBlockPos());
            lines.add(new String[]{"Block ", BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath() + " §7" + bh.getBlockPos().toShortString()});
        }
        for (String[] l : lines) { y = line(g, f, l[0], l[1], 4, y, acc); }

        if (m.isActive(PotionHud.class)) {
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
                int secs = e.getDuration() / 20;
                String time = e.isInfiniteDuration() ? "∞" : String.format("%d:%02d", secs / 60, secs % 60);
                y = line(g, f, name + " ", time, 4, y, e.getEffect().value().getColor() | 0xFF000000);
            }
        }

        if (m.isActive(NearbyPlayers.class)) {
            List<Player> ps = new ArrayList<>(mc.level.players());
            ps.remove(mc.player);
            ps.sort(Comparator.comparingDouble(p -> p.distanceTo(mc.player)));
            for (int i = 0; i < Math.min(8, ps.size()); i++) {
                Player p = ps.get(i);
                float hp = p.getHealth() + p.getAbsorptionAmount();
                String col = hp > 14 ? "§a" : hp > 7 ? "§e" : "§c";
                y = line(g, f, p.getName().getString() + " ", col + String.format("%.0f❤", hp / 2) + " §7" + Math.round(p.distanceTo(mc.player)) + "m", 4, y, acc);
            }
        }

        if (m.isActive(InventoryHud.class)) {
            int ix = w - 9 * 18 - 4, iy = h - 3 * 18 - 30;
            g.fill(ix - 2, iy - 2, ix + 9 * 18 + 2, iy + 3 * 18 + 2, 0x90101014);
            for (int i = 9; i < 36; i++) {
                ItemStack s = mc.player.getInventory().getItem(i);
                int sx = ix + ((i - 9) % 9) * 18, sy = iy + ((i - 9) / 9) * 18;
                if (!s.isEmpty()) { g.item(s, sx + 1, sy + 1); g.itemDecorations(f, s, sx + 1, sy + 1); }
            }
        }

        if (m.isActive(Keystrokes.class)) drawKeys(g, mc, f, acc, w, h);
    }

    private static int line(GuiGraphicsExtractor g, Font f, String a, String b, int x, int y, int color) {
        int tw = f.width(a + b);
        g.fill(x - 2, y - 1, x + tw + 2, y + 10, 0x90101014);
        g.text(f, a, x, y + 1, color, true);
        g.text(f, b, x + f.width(a), y + 1, 0xFFDDDDDD, true);
        return y + 11;
    }

    private static void trackClicks(Minecraft mc) {
        long win = mc.getWindow().handle(), now = System.currentTimeMillis();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !lWas) leftClicks.add(now);
        if (r && !rWas) rightClicks.add(now);
        lWas = l; rWas = r;
        while (!leftClicks.isEmpty() && now - leftClicks.peek() > 1000) leftClicks.poll();
        while (!rightClicks.isEmpty() && now - rightClicks.peek() > 1000) rightClicks.poll();
    }

    private static void drawKeys(GuiGraphicsExtractor g, Minecraft mc, Font f, int acc, int w, int h) {
        int s = 18, gap = 2, x0 = w - 3 * s - 2 * gap - 6, y0 = h / 2 - 30;
        var o = mc.options;
        key(g, f, "W", o.keyUp.isDown(), x0 + s + gap, y0, s, s, acc);
        key(g, f, "A", o.keyLeft.isDown(), x0, y0 + s + gap, s, s, acc);
        key(g, f, "S", o.keyDown.isDown(), x0 + s + gap, y0 + s + gap, s, s, acc);
        key(g, f, "D", o.keyRight.isDown(), x0 + 2 * (s + gap), y0 + s + gap, s, s, acc);
        int mw = (3 * s + 2 * gap - gap) / 2;
        key(g, f, leftClicks.size() + " LMB", o.keyAttack.isDown(), x0, y0 + 2 * (s + gap), mw, s, acc);
        key(g, f, rightClicks.size() + " RMB", o.keyUse.isDown(), x0 + mw + gap, y0 + 2 * (s + gap), mw, s, acc);
        key(g, f, "___", o.keyJump.isDown(), x0, y0 + 3 * (s + gap), 3 * s + 2 * gap, 10, acc);
    }

    private static void key(GuiGraphicsExtractor g, Font f, String label, boolean down, int x, int y, int bw, int bh, int acc) {
        g.fill(x, y, x + bw, y + bh, down ? (acc & 0xFFFFFF) | 0xC0000000 : 0x90101014);
        g.text(f, label, x + (bw - f.width(label)) / 2, y + (bh - 8) / 2, down ? 0xFFFFFFFF : 0xFFBBBBBB, false);
    }
}

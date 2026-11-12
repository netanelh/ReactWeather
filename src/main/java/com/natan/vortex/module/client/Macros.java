package com.natan.vortex.module.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.natan.vortex.command.Commands;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Keys;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bind a key to a line of commands: ".macro g .goto 100 64 200 ; .mine diamond 20". */
public class Macros extends Module {
    private static final Map<Integer, String> binds = new LinkedHashMap<>();
    private static final Map<Integer, Boolean> was = new HashMap<>();
    private static boolean loaded;

    public Macros() { super("Macros", "Key-bound command sequences (.macro <key> <commands>)", Category.CLIENT); setEnabled(true); }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-macros.json"); }

    @SuppressWarnings("unchecked")
    private static void load() {
        loaded = true;
        try { if (Files.exists(file())) ((Map<String, String>) new Gson().fromJson(Files.readString(file()), new TypeToken<Map<String, String>>(){}.getType()))
            .forEach((k, v) -> binds.put(Integer.parseInt(k), v)); } catch (Exception ignored) {}
    }

    private static void save() {
        Map<String, String> out = new LinkedHashMap<>();
        binds.forEach((k, v) -> out.put(k.toString(), v));
        try { Files.writeString(file(), new Gson().toJson(out)); } catch (Exception ignored) {}
    }

    @Override
    public void onTick() {
        if (!loaded) load();
        if (mc.gui.screen() != null) return;
        for (var e : binds.entrySet()) {
            boolean down = Keys.isDown(e.getKey());
            if (down && !was.getOrDefault(e.getKey(), false))
                runLine(e.getValue());
            was.put(e.getKey(), down);
        }
    }

    private static final java.util.Set<String> CHAINABLE = java.util.Set.of("goto", "g", "home", "mine", "fly", "wait", "build", "fill", "clear");

    /** Several navigator tasks in one macro run one after another (via .do) instead of cancelling each other. */
    private static void runLine(String line) {
        java.util.List<String> segs = new java.util.ArrayList<>();
        for (String c : line.split(";")) { String t = c.trim(); if (t.startsWith(".")) t = t.substring(1).trim(); if (!t.isEmpty()) segs.add(t); }
        boolean chain = segs.size() > 1;
        for (String s : segs) if (!CHAINABLE.contains(s.split("\\s+")[0].toLowerCase())) { chain = false; break; }
        if (chain) Commands.execute("do " + String.join(" ; ", segs));
        else for (String s : segs) Commands.execute(s);
    }

    /** .macro <key> <commands> | .macro list | .macro clear <key> */
    public static void command(String[] a) {
        if (!loaded) load();
        if (a.length < 2 || a[1].equalsIgnoreCase("help")) {
            Chat.info("§l§6Macros§r - press a key to run commands.");
            Chat.info("§eBind:§r  .macro <key> <commands>   (separate commands with ; )");
            Chat.info("§eExample:§r  .macro g .goto 100 64 200 ; .mine diamond 20 ; .home");
            Chat.info("§eExample:§r  .macro h .toggle killaura");
            Chat.info("§eKeys:§r A-Z, 0-9, F1-F12    §eList:§r .macro list    §eRemove:§r .macro clear <key>");
            if (!binds.isEmpty()) { Chat.info("§7Current:"); binds.forEach((k, v) -> Chat.info("  §f" + keyName(k) + "§7 -> " + v)); }
            return;
        }
        if (a[1].equalsIgnoreCase("list")) { if (binds.isEmpty()) Chat.info("No macros set. Type .macro for help."); else binds.forEach((k, v) -> Chat.info("§f" + keyName(k) + "§7 -> " + v)); return; }
        boolean clear = a[1].equalsIgnoreCase("clear");
        int key = keyCode(clear && a.length > 2 ? a[2] : a[1]);
        if (key == GLFW.GLFW_KEY_UNKNOWN) { Chat.error("That is not a usable key. Use a single letter A-Z, a digit 0-9, or F1-F12."); return; }
        if (clear) { binds.remove(key); save(); Chat.info("Cleared the macro on " + keyName(key)); return; }
        StringBuilder cmd = new StringBuilder();
        for (int i = 2; i < a.length; i++) cmd.append(a[i]).append(' ');
        if (cmd.length() == 0) { Chat.error("Add commands after the key, e.g. .macro " + a[1] + " .goto 0 64 0 ; .mine diamond"); return; }
        binds.put(key, cmd.toString().trim());
        save();
        Chat.info("§aBound §f" + keyName(key) + "§a: press it to run §f" + cmd.toString().trim());
    }

    private static int keyCode(String s) {
        if (s.length() == 1) { char c = Character.toUpperCase(s.charAt(0)); if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A'); if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0'); }
        if (s.matches("(?i)f([1-9]|1[0-2])")) return GLFW.GLFW_KEY_F1 + Integer.parseInt(s.substring(1)) - 1;
        return GLFW.GLFW_KEY_UNKNOWN;
    }

    private static String keyName(int code) {
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) return String.valueOf((char) ('A' + code - GLFW.GLFW_KEY_A));
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) return String.valueOf((char) ('0' + code - GLFW.GLFW_KEY_0));
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F12) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        return "key" + code;
    }
}

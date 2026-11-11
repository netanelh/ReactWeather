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
                for (String c : e.getValue().split(";")) { String t = c.trim(); if (!t.isEmpty()) Commands.execute(t.startsWith(".") ? t.substring(1) : t); }
            was.put(e.getKey(), down);
        }
    }

    /** .macro <key> <commands> | .macro list | .macro clear <key> */
    public static void command(String[] a) {
        if (!loaded) load();
        if (a.length < 2) { Chat.info("Macros: " + (binds.isEmpty() ? "none" : "")); binds.forEach((k, v) -> Chat.info(keyName(k) + " -> " + v)); Chat.info("Usage: .macro <key> <commands>  |  .macro clear <key>"); return; }
        if (a[1].equalsIgnoreCase("list")) { if (binds.isEmpty()) Chat.info("No macros."); else binds.forEach((k, v) -> Chat.info(keyName(k) + " -> " + v)); return; }
        int key = keyCode(a[1].equalsIgnoreCase("clear") && a.length > 2 ? a[2] : a[1]);
        if (key == GLFW.GLFW_KEY_UNKNOWN) { Chat.error("Unknown key: " + a[1]); return; }
        if (a[1].equalsIgnoreCase("clear")) { binds.remove(key); save(); Chat.info("Cleared macro on " + keyName(key)); return; }
        StringBuilder cmd = new StringBuilder();
        for (int i = 2; i < a.length; i++) cmd.append(a[i]).append(' ');
        if (cmd.length() == 0) { Chat.error("Give commands: .macro " + a[1] + " .goto 0 64 0 ; .mine diamond"); return; }
        binds.put(key, cmd.toString().trim());
        save();
        Chat.info("Bound " + keyName(key) + " -> " + cmd.toString().trim());
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

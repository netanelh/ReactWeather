package com.natan.vortex.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Polls the keyboard once per tick and detects fresh presses (edge-triggered). */
public final class Keys {
    public static final int NONE = GLFW.GLFW_KEY_UNKNOWN;
    private static final Set<Integer> downLastTick = new HashSet<>();
    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift"); NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LShift");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl"); NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt"); NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "LAlt");
        NAMES.put(GLFW.GLFW_KEY_TAB, "Tab"); NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps");
        NAMES.put(GLFW.GLFW_KEY_SPACE, "Space"); NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
        NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert"); NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
        NAMES.put(GLFW.GLFW_KEY_END, "End"); NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "PgUp");
        NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "PgDn"); NAMES.put(GLFW.GLFW_KEY_UP, "Up");
        NAMES.put(GLFW.GLFW_KEY_DOWN, "Down"); NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
        NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
        for (int i = 0; i < 25; i++) NAMES.put(GLFW.GLFW_KEY_F1 + i, "F" + (i + 1));
        for (int i = 0; i < 10; i++) NAMES.put(GLFW.GLFW_KEY_KP_0 + i, "Num" + i);
    }

    private Keys() {}

    public static boolean isDown(int key) {
        if (key == NONE || key < 0) return false;
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    /**
     * Polls every given key once and returns the ones that went from up to down since last tick.
     * Called every tick (even with a screen open) so closing a GUI with its own key doesn't re-trigger it.
     */
    public static Set<Integer> update(Iterable<Integer> keys) {
        Set<Integer> fresh = new HashSet<>();
        Set<Integer> nowDown = new HashSet<>();
        for (int k : keys) {
            if (k == NONE || k < 0 || !isDown(k)) continue;
            nowDown.add(k);
            if (!downLastTick.contains(k)) fresh.add(k);
        }
        downLastTick.clear();
        downLastTick.addAll(nowDown);
        return fresh;
    }

    public static String name(int key) {
        if (key == NONE || key < 0) return "None";
        String n = NAMES.get(key);
        if (n != null) return n;
        String g = GLFW.glfwGetKeyName(key, 0);
        return g != null ? g.toUpperCase() : "Key" + key;
    }
}

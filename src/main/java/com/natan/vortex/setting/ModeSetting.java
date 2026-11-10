package com.natan.vortex.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;

    public ModeSetting(String name, String description, String def, String... modes) {
        super(name, description, def);
        this.modes = Arrays.asList(modes);
    }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        i = Math.floorMod(i + dir, modes.size());
        value = modes.get(i);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (!e.isJsonPrimitive()) return;
        String s = e.getAsString();
        for (String m : modes) if (m.equalsIgnoreCase(s)) value = m;
    }
}

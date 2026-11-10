package com.natan.vortex.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, def);
        this.min = min; this.max = max; this.step = step;
    }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step > 0) clamped = Math.round(clamped / step) * step;
        this.value = Math.max(min, Math.min(max, clamped));
    }

    public double d() { return value; }
    public float f() { return value.floatValue(); }
    public int i() { return (int) Math.round(value); }

    public String display() {
        if (step >= 1) return Integer.toString(i());
        if (step >= 0.1) return String.format("%.1f", value);
        return String.format("%.2f", value);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}

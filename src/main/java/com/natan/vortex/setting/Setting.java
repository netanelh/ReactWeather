package com.natan.vortex.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;
    protected final T defaultValue;
    private BooleanSupplier visible = () -> true;
    public String group; // optional: settings sharing a group get a master on/off toggle in the GUI

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier s) { this.visible = s; return (S) this; }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S group(String g) { this.group = g; return (S) this; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);
}

package com.natan.vortex.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), WORLD("World"), PATHING("Pathing"), CLIENT("Client");

    public final String display;
    Category(String display) { this.display = display; }
}

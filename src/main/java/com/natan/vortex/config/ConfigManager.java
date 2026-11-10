package com.natan.vortex.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.natan.vortex.VortexClient;
import com.natan.vortex.gui.ClickGuiScreen;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("vortex.json");

    public void save() { save(file); }

    public void load() { load(file); }

    public Path presetPath(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve("vortex-presets").resolve(name.replaceAll("[^a-zA-Z0-9_-]", "") + ".json");
    }

    public java.util.List<String> presets() {
        try (var st = Files.list(FabricLoader.getInstance().getConfigDir().resolve("vortex-presets"))) {
            return st.map(p -> p.getFileName().toString().replace(".json", "")).sorted().toList();
        } catch (Exception e) { return java.util.List.of(); }
    }

    public void save(Path file) {
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : VortexClient.modules.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                o.addProperty("key", m.key);
                o.addProperty("hud", m.showInHud);
                JsonObject s = new JsonObject();
                for (Setting<?> setting : m.settings) s.add(setting.name, setting.toJson());
                o.add("settings", s);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            root.add("panels", ClickGuiScreen.savePanels());
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            VortexClient.LOG.error("Failed to save config", e);
        }
    }

    public void load(Path file) {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods != null) for (Module m : VortexClient.modules.all()) {
                JsonElement e = mods.get(m.name);
                if (e == null || !e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("hud")) m.showInHud = o.get("hud").getAsBoolean();
                JsonObject s = o.getAsJsonObject("settings");
                if (s != null) for (Setting<?> setting : m.settings) {
                    JsonElement v = s.get(setting.name);
                    if (v != null) setting.fromJson(v);
                }
                // Enable last, after settings are loaded. Modules needing a world will just idle until one exists.
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
            if (root.has("panels")) ClickGuiScreen.loadPanels(root.getAsJsonObject("panels"));
        } catch (Exception e) {
            VortexClient.LOG.error("Failed to load config", e);
        }
    }
}

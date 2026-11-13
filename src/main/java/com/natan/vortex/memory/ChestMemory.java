package com.natan.vortex.memory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.natan.vortex.VortexClient;
import com.natan.vortex.util.Items2;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Remembers what's inside every chest/barrel/shulker you open, so ".where diamond" can tell you which chest has it. */
public final class ChestMemory {
    public static final ChestMemory INSTANCE = new ChestMemory();
    private static final Gson GSON = new Gson();

    /** packed pos -> (item id -> count) */
    private Map<Long, Map<String, Integer>> chests = new HashMap<>();
    private String key = "";
    public static BlockPos lastInteracted; // set by MultiPlayerGameModeMixin.useItemOn
    private int sinceOpen;
    private boolean containerOpen;

    private ChestMemory() {}

    public record Hit(BlockPos pos, int count, double distance) {}

    private Path file() { return FabricLoader.getInstance().getConfigDir().resolve("vortex-memory").resolve("chests_" + key + ".json"); }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String k = (mc.getCurrentServer() != null ? mc.getCurrentServer().ip : (mc.getSingleplayerServer() != null ? "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName() : "singleplayer")) + "_" + mc.level.dimension().identifier().getPath();
        k = k.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!k.equals(key)) { key = k; load(); }

        boolean open = mc.gui.screen() instanceof AbstractContainerScreen<?> s0 && !(s0 instanceof InventoryScreen) && !(s0 instanceof CreativeModeInventoryScreen);
        if (open) containerOpen = true;
        // only real storage blocks: villagers, horses, chest boats, furnaces etc. must not overwrite the last chest
        if (open && lastInteracted != null && mc.gui.screen() instanceof AbstractContainerScreen<?> scr
            && (scr.getMenu() instanceof net.minecraft.world.inventory.ChestMenu || scr.getMenu() instanceof net.minecraft.world.inventory.ShulkerBoxMenu)
            && com.natan.vortex.nav.process.ChestIndexProcess.isContainer(mc.level.getBlockState(lastInteracted).getBlock())) {
            // wait a few ticks so the server has sent the contents
            if (++sinceOpen % 10 != 5) return;
            var menu = scr.getMenu();
            int containerSlots = menu.slots.size() - 36;
            if (containerSlots <= 0) return;
            Map<String, Integer> items = new LinkedHashMap<>();
            for (int i = 0; i < containerSlots; i++) {
                var st = menu.getSlot(i).getItem();
                if (!st.isEmpty()) items.merge(Items2.id(st), st.getCount(), Integer::sum);
            }
            chests.put(WorldMemory.pack(lastInteracted.getX(), lastInteracted.getY(), lastInteracted.getZ()), items);
            if (sinceOpen == 5) save();
        } else if (!open) {
            if (containerOpen) { containerOpen = false; lastInteracted = null; }
            if (sinceOpen > 0) save();
            sinceOpen = 0;
        }
    }

    public List<Hit> find(String query, BlockPos from) {
        String q = query.toLowerCase();
        List<Hit> out = new ArrayList<>();
        chests.forEach((p, items) -> {
            int c = 0;
            for (var e : items.entrySet()) if (e.getKey().contains(q)) c += e.getValue();
            if (c > 0) { BlockPos b = WorldMemory.unpack(p); out.add(new Hit(b, c, Math.sqrt(b.distSqr(from)))); }
        });
        out.sort(Comparator.comparingDouble(Hit::distance));
        return out;
    }

    public int size() { return chests.size(); }
    public boolean known(BlockPos p) { return chests.containsKey(WorldMemory.pack(p.getX(), p.getY(), p.getZ())); }

    private void load() {
        chests = new HashMap<>();
        try {
            if (Files.exists(file())) {
                Map<Long, Map<String, Integer>> m = GSON.fromJson(Files.readString(file()), new TypeToken<Map<Long, Map<String, Integer>>>() {}.getType());
                if (m != null) chests.putAll(m);
            }
        } catch (Exception e) { VortexClient.LOG.warn("Could not load chest memory", e); }
    }

    private void save() {
        if (key.isEmpty()) return;
        try { Files.createDirectories(file().getParent()); Files.writeString(file(), GSON.toJson(chests)); }
        catch (Exception e) { VortexClient.LOG.warn("Could not save chest memory", e); }
    }
}

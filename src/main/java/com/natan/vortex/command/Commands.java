package com.natan.vortex.command;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.Module;
import com.natan.vortex.module.pathing.NavSettings;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.nav.Waypoints;
import com.natan.vortex.nav.process.ExploreProcess;
import com.natan.vortex.nav.process.FarmProcess;
import com.natan.vortex.nav.process.FollowProcess;
import com.natan.vortex.nav.process.GotoProcess;
import com.natan.vortex.nav.process.MineProcess;
import com.natan.vortex.nav.process.TunnelProcess;
import com.natan.vortex.util.BlockSets;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Keys;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Chat commands starting with "." (never sent to the server). */
public final class Commands {
    public static final String PREFIX = ".";

    private Commands() {}

    public static void register() {
        ClientSendMessageEvents.MODIFY_CHAT.register(msg -> {
            if (msg.startsWith(PREFIX) || VortexClient.modules == null) return msg;
            return VortexClient.modules.get(com.natan.vortex.module.client.ChatSuffix.class).apply(msg);
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.startsWith(PREFIX) || message.startsWith("..")) return true;
            Minecraft.getInstance().execute(() -> {
                try {
                    run(message.substring(PREFIX.length()).trim());
                } catch (Exception e) {
                    Chat.error("Command failed: " + e.getMessage());
                }
            });
            return false;
        });
    }

    private static void run(String line) {
        String[] a = line.split("\\s+");
        if (a.length == 0 || a[0].isEmpty()) { help(); return; }
        Minecraft mc = Minecraft.getInstance();
        Navigator nav = Navigator.INSTANCE;
        if (!VortexClient.modules.get(NavSettings.class).isEnabled() && isNavCommand(a[0])) {
            Chat.error("Navigator module is off. Turn it on in the Pathing panel.");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "help", "h", "?" -> help();
            case "stop", "cancel" -> nav.stop(true);
            case "pause" -> nav.setPaused(true);
            case "resume" -> nav.setPaused(false);
            case "path" -> Chat.info("Last search: " + nav.lastCalc());
            case "goto", "g" -> gotoCmd(a);
            case "home" -> {
                var p = Waypoints.INSTANCE.get("home");
                if (p == null) Chat.error("No home set here. Use .sethome");
                else nav.start(new GotoProcess(new Goal.Near(p.pos(), 1.5), "home"));
            }
            case "sethome" -> { Waypoints.INSTANCE.set("home", mc.player.blockPosition()); Chat.info("Home set to " + mc.player.blockPosition().toShortString()); }
            case "wp", "waypoint" -> waypoint(a);
            case "mine" -> mine(a);
            case "follow" -> nav.start(new FollowProcess(a.length > 1 ? a[1] : null, 3));
            case "explore" -> nav.start(new ExploreProcess(a.length > 1 ? Integer.parseInt(a[1]) : 64));
            case "farm" -> nav.start(new FarmProcess(a.length > 1 ? Integer.parseInt(a[1]) : 12, true));
            case "tunnel" -> {
                int len = a.length > 1 ? Integer.parseInt(a[1]) : 64;
                String size = a.length > 2 ? a[2] : "1x2";
                int w = size.startsWith("3") ? 3 : size.startsWith("2") ? 2 : 1, h = size.endsWith("3") ? 3 : 2;
                nav.start(new TunnelProcess(len, w, h));
            }
            case "hunt", "kill" -> {
                String what = a.length > 1 ? a[1] : "hostiles";
                int limit = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                nav.start(new com.natan.vortex.nav.process.HuntProcess(com.natan.vortex.nav.process.HuntProcess.byName(what), what, limit));
            }
            case "collect", "pickup" -> nav.start(new com.natan.vortex.nav.process.CollectProcess(a.length > 1 ? a[1] : null, 32));
            case "find" -> {
                if (a.length < 2) { Chat.error("Usage: .find <block>  e.g. .find crafting_table"); return; }
                var set = BlockSets.parse(a[1]);
                if (set.isEmpty()) { Chat.error("Unknown block '" + a[1] + "'"); return; }
                nav.start(new com.natan.vortex.nav.process.FindProcess(set, a[1]));
            }
            case "surface", "up" -> nav.start(new com.natan.vortex.nav.process.SimpleGoalProcess("Surface", new Goal.Surface()));
            case "flee", "run" -> {
                int dist = a.length > 1 ? Integer.parseInt(a[1]) : 50;
                var p = mc.player.blockPosition();
                nav.start(new com.natan.vortex.nav.process.SimpleGoalProcess("Flee", new Goal.Away(p.getX(), p.getZ(), dist)));
            }
            case "patrol" -> {
                if (a.length < 3) { Chat.error("Usage: .patrol <waypoint> <waypoint> [...]"); return; }
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.List<BlockPos> pts = new java.util.ArrayList<>();
                for (int i = 1; i < a.length; i++) {
                    var w = Waypoints.INSTANCE.get(a[i]);
                    if (w == null) { Chat.error("No waypoint '" + a[i] + "'"); return; }
                    names.add(a[i]); pts.add(w.pos());
                }
                nav.start(new com.natan.vortex.nav.process.PatrolProcess(names, pts));
            }
            case "locate", "scout" -> {
                if (a.length < 2) { Chat.error("Usage: .locate <structure>  or  .locate biome <biome>   e.g. .locate village, .locate stronghold"); return; }
                boolean biome = a[1].equalsIgnoreCase("biome");
                if (biome && a.length < 3) { Chat.error("Usage: .locate biome <name>"); return; }
                String what = (biome ? a[2] : a[1]).toLowerCase().replace("minecraft:", "");
                Long known = com.natan.vortex.seed.SeedStore.current();
                if (mc.getSingleplayerServer() != null) {
                    nav.start(new com.natan.vortex.nav.process.LocateProcess(what, biome)); // exact, reads your own world
                } else if (known != null) {
                    nav.start(new com.natan.vortex.nav.process.SeedLocateProcess(what, biome, known)); // exact, from the cracked seed
                } else if (what.equals("stronghold")) {
                    nav.start(new com.natan.vortex.nav.process.StrongholdProcess()); // eye of ender triangulation
                } else if (!biome && com.natan.vortex.nav.process.ScoutProcess.known(what)) {
                    nav.start(new com.natan.vortex.nav.process.ScoutProcess(what)); // explore + detect
                } else {
                    Chat.error("On multiplayer I can find: " + String.join(", ", new java.util.TreeSet<>(com.natan.vortex.nav.process.ScoutProcess.SIGNATURES.keySet())) + ", stronghold (eyes of ender). Crack the seed (.seed) to find anything, biomes included.");
                }
            }
            case "vclip" -> {
                if (a.length < 2) { Chat.error("Usage: .vclip <blocks>  (negative = down)"); return; }
                com.natan.vortex.util.Teleport.to(mc.player.position().add(0, Double.parseDouble(a[1]), 0));
            }
            case "hclip" -> {
                if (a.length < 2) { Chat.error("Usage: .hclip <blocks>  (forward through walls)"); return; }
                double yaw = Math.toRadians(mc.player.getYRot()), d = Double.parseDouble(a[1]);
                com.natan.vortex.util.Teleport.to(mc.player.position().add(-Math.sin(yaw) * d, 0, Math.cos(yaw) * d));
            }
            case "tp" -> {
                if (a.length < 4) { Chat.error("Usage: .tp <x> <y> <z>   (~ for current, max ~100 blocks per go)"); return; }
                var pos = mc.player.position();
                double x = a[1].startsWith("~") ? pos.x + (a[1].length() > 1 ? Double.parseDouble(a[1].substring(1)) : 0) : Double.parseDouble(a[1]);
                double y = a[2].startsWith("~") ? pos.y + (a[2].length() > 1 ? Double.parseDouble(a[2].substring(1)) : 0) : Double.parseDouble(a[2]);
                double z = a[3].startsWith("~") ? pos.z + (a[3].length() > 1 ? Double.parseDouble(a[3].substring(1)) : 0) : Double.parseDouble(a[3]);
                com.natan.vortex.util.Teleport.to(new net.minecraft.world.phys.Vec3(x, y, z));
            }
            case "seed" -> {
                String sub = a.length > 1 ? a[1].toLowerCase() : "";
                if (mc.getSingleplayerServer() != null) { Chat.info("In singleplayer .locate already reads your world directly."); return; }
                switch (sub) {
                    case "" -> {
                        Long k = com.natan.vortex.seed.SeedStore.current();
                        Chat.info(k != null ? "Seed: " + k + "  (.seed clear to forget)"
                            : "Seed unknown. Desert pyramids seen: " + com.natan.vortex.seed.SeedCracker.pyramids().size() + "/5" + (com.natan.vortex.seed.SeedCracker.running() ? " \u00b7 cracking: " + com.natan.vortex.seed.SeedCracker.status : "")
                              + ". It cracks by itself once you've passed 5-6 pyramids. Commands: .seed crack | .seed hunt | .seed <number>");
                    }
                    case "crack" -> com.natan.vortex.seed.SeedCracker.crack(true);
                    case "hunt" -> { nav.start(new com.natan.vortex.nav.process.ExploreProcess(160)); Chat.info("Exploring (head for deserts). .stop when done; the seed cracks automatically when there are enough."); }
                    case "clear" -> { com.natan.vortex.seed.SeedStore.set(null); Chat.info("Seed forgotten for this server."); }
                    default -> {
                        try { com.natan.vortex.seed.SeedStore.set(Long.parseLong(a[1])); Chat.info("Seed saved for this server."); }
                        catch (NumberFormatException e) { com.natan.vortex.seed.SeedStore.set((long) a[1].hashCode()); Chat.info("Text seed saved (as Minecraft hashes it)."); }
                    }
                }
            }
            case "where" -> {
                if (a.length < 2) { Chat.error("Usage: .where <item>   e.g. .where diamond"); return; }
                var hits = com.natan.vortex.memory.ChestMemory.INSTANCE.find(a[1], mc.player.blockPosition());
                if (hits.isEmpty()) { Chat.info("No remembered chest has '" + a[1] + "'. Open your chests once, or run .indexchests"); return; }
                for (int i = 0; i < Math.min(6, hits.size()); i++) {
                    var h = hits.get(i);
                    Chat.info("\u00a7e" + h.count() + "x\u00a7r in chest at " + h.pos().toShortString() + " \u00a77(" + Math.round(h.distance()) + "m)");
                }
                Chat.info("Type \u00a7e.fetch " + a[1] + "\u00a7r to walk to the nearest one and open it.");
            }
            case "fetch" -> {
                if (a.length < 2) { Chat.error("Usage: .fetch <item>"); return; }
                var hits = com.natan.vortex.memory.ChestMemory.INSTANCE.find(a[1], mc.player.blockPosition());
                if (hits.isEmpty()) { Chat.error("No remembered chest has '" + a[1] + "'."); return; }
                nav.start(new com.natan.vortex.nav.process.ChestIndexProcess(hits.get(0).pos(), a[1]));
            }
            case "indexchests", "scanchests" -> nav.start(new com.natan.vortex.nav.process.ChestIndexProcess(a.length > 1 ? Integer.parseInt(a[1]) : 24));
            case "memory" -> Chat.info("World memory: " + com.natan.vortex.memory.WorldMemory.INSTANCE.count() + " blocks in " + com.natan.vortex.memory.WorldMemory.INSTANCE.chunksScanned()
                + " chunks this session \u00b7 " + com.natan.vortex.memory.ChestMemory.INSTANCE.size() + " chests indexed");
            case "suffix" -> {
                if (a.length < 2) { Chat.info("Suffix: '" + com.natan.vortex.module.client.ChatSuffix.suffix + "'"); return; }
                com.natan.vortex.module.client.ChatSuffix.suffix = " " + line.substring(line.indexOf(' ') + 1);
                Chat.info("Chat suffix set. Turn on the ChatSuffix module to use it.");
            }
            case "toggle", "t" -> {
                Module m = find(a, 1);
                if (m != null) { m.toggle(); Chat.info(m.name + (m.isEnabled() ? " §aON" : " §cOFF")); }
            }
            case "bind" -> bind(a);
            case "preset", "config" -> preset(a);
            case "friend", "f" -> {
                if (a.length < 2 || a[1].equalsIgnoreCase("list")) { Chat.info("Friends: " + (com.natan.vortex.util.Friends.all().isEmpty() ? "none" : String.join(", ", com.natan.vortex.util.Friends.all()))); }
                else if (a.length >= 3 && a[1].equalsIgnoreCase("add")) Chat.info(com.natan.vortex.util.Friends.add(a[2]) ? "Added friend " + a[2] : a[2] + " is already a friend");
                else if (a.length >= 3 && (a[1].equalsIgnoreCase("del") || a[1].equalsIgnoreCase("remove"))) Chat.info(com.natan.vortex.util.Friends.remove(a[2]) ? "Removed " + a[2] : a[2] + " isn't a friend");
                else Chat.error("Usage: .friend add|del|list <name>");
            }
            case "spam" -> {
                var list = com.natan.vortex.module.player.Spammer.messages;
                if (a.length >= 3 && a[1].equalsIgnoreCase("add")) { list.add(line.substring(line.toLowerCase().indexOf("add") + 4)); Chat.info("Added. " + list.size() + " messages."); }
                else if (a.length >= 2 && a[1].equalsIgnoreCase("clear")) { list.clear(); Chat.info("Spam list cleared."); }
                else Chat.info("Spam messages: " + String.join(" | ", list) + "   (.spam add <text> / .spam clear)");
            }
            case "panic" -> VortexClient.modules.get(com.natan.vortex.module.client.Panic.class).setEnabled(true);
            case "modules" -> {
                StringBuilder sb = new StringBuilder();
                for (Module m : VortexClient.modules.all()) sb.append(m.isEnabled() ? "§a" : "§7").append(m.name).append("§r, ");
                Chat.info(sb.toString());
            }
            default -> Chat.error("Unknown command '" + a[0] + "'. Type .help");
        }
    }

    private static boolean isNavCommand(String c) {
        return Set.of("goto", "g", "home", "mine", "follow", "explore", "farm", "tunnel", "hunt", "kill", "collect", "pickup", "find", "surface", "up", "flee", "run", "patrol", "locate", "fetch", "indexchests", "scanchests").contains(c.toLowerCase());
    }

    private static void help() {
        Chat.info("§lNavigation§r: .goto x y z | .goto x z | .goto <waypoint> | .home | .sethome | .stop | .pause | .resume");
        Chat.info("§lTasks§r: .mine <block...> [limit N] [nohome] | .follow [player] | .explore [step] | .farm [radius] | .tunnel [length] [1x2|2x2|3x3]");
        Chat.info("§lMore§r: .hunt <mob|hostiles|animals> [N] | .collect [item] | .find <block> | .surface | .flee [blocks] | .patrol <wp> <wp>...");
        Chat.info("§lFar & storage§r: .find <block> (remembers every chunk you saw) | .locate <structure> / .locate biome <b> | .where <item> | .fetch <item> | .indexchests [r] | .memory | .seed (crack the server seed)");
        Chat.info("§lTeleport§r: .vclip <n> | .hclip <n> | .tp <x> <y> <z>  (and ClickTP: Left Alt + right click)");
        Chat.info("§lWaypoints§r: .wp add <name> | .wp del <name> | .wp list   (death point is saved automatically)");
        Chat.info("§lClient§r: .toggle <module> | .bind <module> <key|none> | .preset save|load|list <name> | .panic | .modules");
    }

    private static void gotoCmd(String[] a) {
        Minecraft mc = Minecraft.getInstance();
        Navigator nav = Navigator.INSTANCE;
        if (a.length == 4) {
            int x = coord(a[1], mc.player.getBlockX()), y = coord(a[2], mc.player.getBlockY()), z = coord(a[3], mc.player.getBlockZ());
            nav.start(new GotoProcess(new Goal.Block(x, y, z), x + " " + y + " " + z));
        } else if (a.length == 3) {
            int x = coord(a[1], mc.player.getBlockX()), z = coord(a[2], mc.player.getBlockZ());
            nav.start(new GotoProcess(new Goal.XZ(x, z), x + " " + z));
        } else if (a.length == 2) {
            var p = Waypoints.INSTANCE.get(a[1]);
            if (p == null) { Chat.error("No waypoint '" + a[1] + "' in this world/dimension."); return; }
            nav.start(new GotoProcess(new Goal.Near(p.pos(), 1.5), a[1]));
        } else Chat.error("Usage: .goto x y z | .goto x z | .goto <waypoint>");
    }

    /** Supports ~ and ~10 relative coordinates. */
    private static int coord(String s, int base) {
        if (s.startsWith("~")) return base + (s.length() > 1 ? Integer.parseInt(s.substring(1)) : 0);
        return Integer.parseInt(s);
    }

    private static void waypoint(String[] a) {
        Minecraft mc = Minecraft.getInstance();
        if (a.length < 2 || a[1].equalsIgnoreCase("list")) {
            var all = Waypoints.INSTANCE.here();
            if (all.isEmpty()) { Chat.info("No waypoints yet. .wp add <name>"); return; }
            all.forEach((n, p) -> Chat.info("§e" + n + "§r " + p.x() + " " + p.y() + " " + p.z() + " §7(" + p.dim() + ")"));
            return;
        }
        if (a.length < 3) { Chat.error("Usage: .wp add|del|goto <name>"); return; }
        switch (a[1].toLowerCase()) {
            case "add", "set" -> {
                BlockPos p = a.length >= 6 ? new BlockPos(Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5])) : mc.player.blockPosition();
                Waypoints.INSTANCE.set(a[2], p);
                Chat.info("Waypoint §e" + a[2] + "§r saved at " + p.toShortString());
            }
            case "del", "remove" -> Chat.info(Waypoints.INSTANCE.remove(a[2]) ? "Removed " + a[2] : "No waypoint " + a[2]);
            case "goto" -> gotoCmd(new String[]{"goto", a[2]});
            default -> Chat.error("Usage: .wp add|del|goto|list");
        }
    }

    private static void mine(String[] a) {
        if (a.length < 2) { Chat.error("Usage: .mine <block...> [limit N] [nohome]   e.g. .mine diamond iron   or   .mine ores"); return; }
        Set<Block> blocks = new HashSet<>();
        int limit = 0;
        boolean home = true;
        for (int i = 1; i < a.length; i++) {
            if (a[i].equalsIgnoreCase("limit") && i + 1 < a.length) { limit = Integer.parseInt(a[++i]); continue; }
            if (a[i].equalsIgnoreCase("nohome")) { home = false; continue; }
            Set<Block> parsed = BlockSets.parse(a[i]);
            if (parsed.isEmpty()) { Chat.error("Unknown block '" + a[i] + "'"); return; }
            blocks.addAll(parsed);
        }
        BlockSets.custom = blocks;
        String label = String.join(" ", Arrays.copyOfRange(a, 1, a.length)).replaceAll("(?i)\\s*(limit \\d+|nohome)", "");
        Navigator.INSTANCE.start(new MineProcess(blocks, label, 24, limit, home));
    }

    private static Module find(String[] a, int idx) {
        if (a.length <= idx) { Chat.error("Missing module name."); return null; }
        for (Module m : VortexClient.modules.all()) if (m.name.equalsIgnoreCase(a[idx])) return m;
        Chat.error("No module '" + a[idx] + "'. Type .modules");
        return null;
    }

    private static void bind(String[] a) {
        Module m = find(a, 1);
        if (m == null) return;
        if (a.length < 3) { Chat.info(m.name + " is bound to " + Keys.name(m.key)); return; }
        int key = parseKey(a[2]);
        if (key == Integer.MIN_VALUE) { Chat.error("Unknown key '" + a[2] + "'"); return; }
        m.key = key;
        VortexClient.config.save();
        Chat.info(m.name + " bound to " + Keys.name(key));
    }

    private static int parseKey(String s) {
        s = s.toLowerCase();
        if (s.equals("none") || s.equals("clear")) return Keys.NONE;
        if (s.length() == 1) {
            char c = s.charAt(0);
            if (c >= 'a' && c <= 'z') return GLFW.GLFW_KEY_A + (c - 'a');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }
        if (s.matches("f\\d{1,2}")) { int n = Integer.parseInt(s.substring(1)); if (n >= 1 && n <= 25) return GLFW.GLFW_KEY_F1 + n - 1; }
        return switch (s) {
            case "rshift" -> GLFW.GLFW_KEY_RIGHT_SHIFT; case "lshift" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "rctrl" -> GLFW.GLFW_KEY_RIGHT_CONTROL; case "lctrl" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "ralt" -> GLFW.GLFW_KEY_RIGHT_ALT; case "lalt" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "tab" -> GLFW.GLFW_KEY_TAB; case "caps" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "insert" -> GLFW.GLFW_KEY_INSERT; case "home" -> GLFW.GLFW_KEY_HOME; case "end" -> GLFW.GLFW_KEY_END;
            case "pgup" -> GLFW.GLFW_KEY_PAGE_UP; case "pgdn" -> GLFW.GLFW_KEY_PAGE_DOWN; case "delete" -> GLFW.GLFW_KEY_DELETE;
            case "up" -> GLFW.GLFW_KEY_UP; case "down" -> GLFW.GLFW_KEY_DOWN; case "left" -> GLFW.GLFW_KEY_LEFT; case "right" -> GLFW.GLFW_KEY_RIGHT;
            default -> Integer.MIN_VALUE;
        };
    }

    private static void preset(String[] a) {
        var cfg = VortexClient.config;
        if (a.length < 2 || a[1].equalsIgnoreCase("list")) {
            var list = cfg.presets();
            Chat.info(list.isEmpty() ? "No presets yet. .preset save <name>" : "Presets: " + String.join(", ", list));
            return;
        }
        if (a.length < 3) { Chat.error("Usage: .preset save|load <name>"); return; }
        switch (a[1].toLowerCase()) {
            case "save" -> { cfg.save(cfg.presetPath(a[2])); Chat.info("Saved preset §e" + a[2]); }
            case "load" -> {
                var path = cfg.presetPath(a[2]);
                if (!java.nio.file.Files.exists(path)) { Chat.error("No preset " + a[2]); return; }
                cfg.load(path);
                cfg.save();
                Chat.info("Loaded preset §e" + a[2]);
            }
            default -> Chat.error("Usage: .preset save|load|list <name>");
        }
    }
}

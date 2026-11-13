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

    public static void execute(String line) {
        try { run(line); } catch (Exception e) { Chat.error("Command failed: " + e.getMessage()); }
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
        String cmd = a[0].toLowerCase();
        switch (cmd) {
            case "help", "h", "?" -> help();
            case "stop", "cancel" -> nav.stop(true);
            case "pause" -> nav.setPaused(true);
            case "resume" -> nav.setPaused(false);
            case "path" -> Chat.info("Last search: " + nav.lastCalc());
            case "goto", "g" -> gotoCmd(a);
            case "home" -> {
                var p = Waypoints.INSTANCE.get("home");
                if (p == null) Chat.error("No home set here. Use .sethome");
                else if (!tryElytra(mc, nav, p.x(), p.y(), p.z(), new Goal.Near(p.pos(), 1.5), "home"))
                    nav.start(new GotoProcess(new Goal.Near(p.pos(), 1.5), "home"));
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
            case "schem", "schematic" -> {
                try {
                    if (a.length < 2) {
                        java.nio.file.Path d = com.natan.vortex.builder.Schematic.dir();
                        if (!java.nio.file.Files.isDirectory(d)) { Chat.info("Put .schem files in " + d); return; }
                        var names = new java.util.ArrayList<String>();
                        try (var st = java.nio.file.Files.list(d)) { st.filter(f -> f.toString().endsWith(".schem")).forEach(f -> names.add(f.getFileName().toString())); }
                        Chat.info(names.isEmpty() ? "No .schem files in " + d : "Schematics: " + String.join(", ", names));
                    } else {
                        com.natan.vortex.builder.BuildState.schematic = com.natan.vortex.builder.Schematic.load(a[1], mc.level.registryAccess());
                        var sc = com.natan.vortex.builder.BuildState.schematic;
                        Chat.info("Loaded " + sc.name + " (" + sc.width + "x" + sc.height + "x" + sc.length + "). Stand at the build corner and type .build");
                    }
                } catch (Exception e) { Chat.error("Schematic error: " + e.getMessage()); }
            }
            case "build" -> {
                if (com.natan.vortex.builder.BuildState.schematic == null) { Chat.error("Load one first: .schem <name>"); return; }
                net.minecraft.core.BlockPos origin = a.length >= 4
                    ? new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]))
                    : mc.player.blockPosition();
                nav.start(new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.SchematicTarget(com.natan.vortex.builder.BuildState.schematic, origin)));
            }
            case "litematic", "lm" -> {
                try {
                    if (a.length < 2) { Chat.info("Usage: .litematic <name>  (.litematic files in " + com.natan.vortex.builder.Schematic.dir() + ")"); return; }
                    com.natan.vortex.builder.BuildState.schematic = com.natan.vortex.builder.Litematic.load(a[1], mc.level.registryAccess());
                    var sc = com.natan.vortex.builder.BuildState.schematic;
                    Chat.info("Loaded " + sc.name + " (" + sc.width + "x" + sc.height + "x" + sc.length + "). Stand at the corner and type .build");
                } catch (Exception e) { Chat.error("Litematic error: " + e.getMessage()); }
            }
            case "walls", "shell", "sphere" -> shape(a, cmd);
            case "replace" -> {
                var s1 = com.natan.vortex.builder.BuildState.sel1; var s2 = com.natan.vortex.builder.BuildState.sel2;
                if (s1 == null || s2 == null) { Chat.error("Set corners first (.sel1 / .sel2)"); return; }
                if (selectionVolume(s1, s2) > 65536) { Chat.error("Selection too big (max 65536 blocks)."); return; }
                if (a.length < 3) { Chat.error("Usage: .replace <from> <to>   e.g. .replace stone deepslate   (.replace <from> air to remove)"); return; }
                var from = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(a[1].contains(":") ? a[1] : "minecraft:" + a[1])).orElse(null);
                if (from == null) { Chat.error("Unknown block: " + a[1]); return; }
                net.minecraft.world.level.block.state.BlockState to = null;
                if (!a[2].equalsIgnoreCase("air")) {
                    try { to = net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), new com.mojang.brigadier.StringReader(a[2]), false).blockState(); }
                    catch (Exception e) { Chat.error("Unknown block: " + a[2]); return; }
                }
                nav.start(new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.ReplaceTarget(s1, s2, from, to, "replace " + a[1] + ">" + a[2])));
            }
            case "copy" -> {
                var s1 = com.natan.vortex.builder.BuildState.sel1; var s2 = com.natan.vortex.builder.BuildState.sel2;
                if (s1 == null || s2 == null) { Chat.error("Set corners first (.sel1 / .sel2)"); return; }
                if (selectionVolume(s1, s2) > 65536) { Chat.error("Selection too big (max 65536 blocks)."); return; }
                int n = com.natan.vortex.builder.CopyBuffer.copy(s1, s2);
                Chat.info("Copied " + n + " blocks. Stand where the corner should go and type .paste");
            }
            case "paste" -> {
                if (com.natan.vortex.builder.CopyBuffer.blocks.isEmpty()) { Chat.error("Nothing copied yet (.copy)"); return; }
                net.minecraft.core.BlockPos at = a.length >= 4 ? new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])) : mc.player.blockPosition();
                nav.start(new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.PasteTarget(at)));
            }
            case "sel", "sel1", "sel2" -> {
                net.minecraft.core.BlockPos at = mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? bhr.getBlockPos() : mc.player.blockPosition().below();
                boolean two = cmd.equals("sel2") || (a.length > 1 && a[1].equals("2"));
                if (two) com.natan.vortex.builder.BuildState.sel2 = at; else com.natan.vortex.builder.BuildState.sel1 = at;
                Chat.info("Corner " + (two ? "2" : "1") + " = " + at.getX() + " " + at.getY() + " " + at.getZ());
            }
            case "fill", "clear" -> {
                var s1 = com.natan.vortex.builder.BuildState.sel1; var s2 = com.natan.vortex.builder.BuildState.sel2;
                if (s1 == null || s2 == null) { Chat.error("Set both corners first: look at a block and type .sel1 then .sel2"); return; }
                long vol = selectionVolume(s1, s2);
                if (vol > 65536) { Chat.error("Selection too big (" + vol + " blocks, max 65536)."); return; }
                net.minecraft.world.level.block.state.BlockState bs = null;
                if (cmd.equals("fill")) {
                    if (a.length < 2) { Chat.error("Usage: .fill <block>   e.g. .fill stone"); return; }
                    try { bs = net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), new com.mojang.brigadier.StringReader(a[1]), false).blockState(); }
                    catch (Exception e) { Chat.error("Unknown block: " + a[1]); return; }
                }
                nav.start(new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.CuboidTarget(s1, s2, bs, cmd.equals("fill") ? "fill " + a[1] : "clear")));
            }
            case "y", "ylevel" -> {
                if (a.length < 2) { Chat.error("Usage: .y <level>   (walk to a Y height, e.g. .y 11)"); return; }
                nav.start(new GotoProcess(new Goal.YLevel(Integer.parseInt(a[1])), "y=" + a[1]));
            }
            case "macro" -> com.natan.vortex.module.client.Macros.command(a);
            case "do", "chain" -> {
                String rest = line.substring(a[0].length()).trim();
                if (rest.isEmpty()) { Chat.error("Usage: .do goto 100 64 200 ; mine diamond 20 ; home   (tasks run one after another)"); return; }
                java.util.List<com.natan.vortex.nav.Process> steps = new java.util.ArrayList<>();
                for (String seg : rest.split(";")) {
                    String t = seg.trim(); if (t.isEmpty()) continue;
                    com.natan.vortex.nav.Process pr = chainProcess(t.split("\\s+"));
                    if (pr == null) { Chat.error("Can't chain: " + t + "  (use goto/mine/fly/build/fill/clear/wait/home)"); return; }
                    steps.add(pr);
                }
                if (steps.isEmpty()) return;
                nav.start(new com.natan.vortex.nav.process.SequenceProcess(steps, steps.size() + " tasks"));
            }
            case "fly" -> {
                if (a.length < 4) { Chat.error("Usage: .fly <x> <y> <z>   (elytra travel; needs an elytra + fireworks)"); return; }
                nav.start(new com.natan.vortex.nav.process.FlyProcess(
                    new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])), a[1] + " " + a[2] + " " + a[3]));
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
                    case "hunt" -> {
                        if (!VortexClient.modules.get(NavSettings.class).isEnabled()) { Chat.error("Navigator module is off. Turn it on in the Pathing panel."); return; }
                        nav.start(new com.natan.vortex.nav.process.ExploreProcess(160)); Chat.info("Exploring (head for deserts). .stop when done; the seed cracks automatically when there are enough."); }
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

    private static long selectionVolume(BlockPos s1, BlockPos s2) {
        return (long) (Math.abs(s1.getX() - s2.getX()) + 1) * (Math.abs(s1.getY() - s2.getY()) + 1) * (Math.abs(s1.getZ() - s2.getZ()) + 1);
    }

    private static boolean isNavCommand(String c) {
        return Set.of("goto", "g", "home", "mine", "follow", "explore", "farm", "tunnel", "hunt", "kill", "collect", "pickup", "find", "surface", "up", "flee", "run", "patrol", "locate", "scout", "fetch", "indexchests", "scanchests", "do", "chain", "fly", "build", "fill", "clear", "replace", "walls", "shell", "sphere", "paste", "y", "ylevel").contains(c.toLowerCase());
    }

    private static void help() {
        Chat.info("§lNavigation§r: .goto x y z | .goto x z | .goto <waypoint> | .home | .sethome | .stop | .pause | .resume");
        Chat.info("§lTasks§r: .mine <block...> [limit N] [nohome] | .follow [player] | .explore [step] | .farm [radius] | .tunnel [length] [1x2|2x2|3x3]");
        Chat.info("§lMore§r: .hunt <mob|hostiles|animals> [N] | .collect [item] | .find <block> | .surface | .flee [blocks] | .patrol <wp> <wp>...");
        Chat.info("§lFar & storage§r: .find <block> (remembers every chunk you saw) | .locate <structure> / .locate biome <b> | .where <item> | .fetch <item> | .indexchests [r] | .memory | .seed (crack the server seed)");
        Chat.info("§lChain§r: .do goto x y z ; mine diamond 20 ; home  (tasks run in order)");
        Chat.info("§lBuild§r: .schem / .litematic <name> | .build | .sel1 / .sel2 | .fill / .walls / .shell / .sphere <block> | .replace <a> <b> | .clear | .copy / .paste");
        Chat.info("§lTeleport§r: .vclip <n> | .hclip <n> | .tp <x> <y> <z> | .fly <x> <y> <z>  (and ClickTP: Left Alt + right click)");
        Chat.info("§lWaypoints§r: .wp add <name> | .wp del <name> | .wp list   (death point is saved automatically)");
        Chat.info("§lClient§r: .toggle <module> | .bind <module> <key|none> | .preset save|load|list <name> | .panic | .modules");
    }

    /** Builds a single chainable task; returns null if the verb can't be chained. */
    private static void shape(String[] a, String cmd) {
        Minecraft mc = Minecraft.getInstance();
        var s1 = com.natan.vortex.builder.BuildState.sel1; var s2 = com.natan.vortex.builder.BuildState.sel2;
        if (s1 == null || s2 == null) { Chat.error("Set corners first: look at a block and type .sel1 then .sel2"); return; }
        if (selectionVolume(s1, s2) > 65536) { Chat.error("Selection too big (max 65536 blocks)."); return; }
        if (a.length < 2) { Chat.error("Usage: ." + cmd + " <block>"); return; }
        net.minecraft.world.level.block.state.BlockState bs;
        try { bs = net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), new com.mojang.brigadier.StringReader(a[1]), false).blockState(); }
        catch (Exception e) { Chat.error("Unknown block: " + a[1]); return; }
        var shape = switch (cmd) { case "walls" -> com.natan.vortex.builder.ShapeTarget.Shape.WALLS; case "sphere" -> com.natan.vortex.builder.ShapeTarget.Shape.SPHERE; default -> com.natan.vortex.builder.ShapeTarget.Shape.SHELL; };
        Navigator.INSTANCE.start(new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.ShapeTarget(s1, s2, bs, shape, cmd + " " + a[1])));
    }

    private static com.natan.vortex.nav.Process chainProcess(String[] a) {
        Minecraft mc = Minecraft.getInstance();
        switch (a[0].toLowerCase()) {
            case "goto", "g" -> {
                if (a.length == 4) return new GotoProcess(new Goal.Block(coord(a[1], mc.player.getBlockX()), coord(a[2], mc.player.getBlockY()), coord(a[3], mc.player.getBlockZ())), "goto");
                if (a.length == 3) return new GotoProcess(new Goal.XZ(coord(a[1], mc.player.getBlockX()), coord(a[2], mc.player.getBlockZ())), "goto");
                if (a.length == 2) { var wp = Waypoints.INSTANCE.get(a[1]); return wp == null ? null : new GotoProcess(new Goal.Near(wp.pos(), 1.5), a[1]); }
                return null;
            }
            case "home" -> { var wp = Waypoints.INSTANCE.get("home"); return wp == null ? null : new GotoProcess(new Goal.Near(wp.pos(), 1.5), "home"); }
            case "mine" -> {
                if (a.length < 2) return null;
                java.util.Set<net.minecraft.world.level.block.Block> blocks = com.natan.vortex.util.BlockSets.parse(a[1]);
                if (blocks.isEmpty()) return null;
                int limit = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                return new MineProcess(blocks, a[1], 24, limit, false);
            }
            case "fly" -> { if (a.length < 4) return null; return new com.natan.vortex.nav.process.FlyProcess(new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])), "fly"); }
            case "wait" -> { return new com.natan.vortex.nav.process.WaitProcess(a.length > 1 ? Double.parseDouble(a[1]) : 1); }
            case "build" -> {
                if (com.natan.vortex.builder.BuildState.schematic == null) return null;
                net.minecraft.core.BlockPos o = a.length >= 4 ? new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])) : mc.player.blockPosition();
                return new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.SchematicTarget(com.natan.vortex.builder.BuildState.schematic, o));
            }
            case "fill", "clear" -> {
                var s1 = com.natan.vortex.builder.BuildState.sel1; var s2 = com.natan.vortex.builder.BuildState.sel2;
                if (s1 == null || s2 == null) return null;
                if (selectionVolume(s1, s2) > 65536) { Chat.error("Selection too big (max 65536 blocks)."); return null; }
                net.minecraft.world.level.block.state.BlockState bs = null;
                if (a[0].equalsIgnoreCase("fill")) {
                    if (a.length < 2) return null;
                    try { bs = net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), new com.mojang.brigadier.StringReader(a[1]), false).blockState(); }
                    catch (Exception e) { return null; }
                }
                return new com.natan.vortex.nav.process.BuildProcess(new com.natan.vortex.builder.CuboidTarget(s1, s2, bs, a[0]));
            }
            default -> { return null; }
        }
    }

    /** Elytra flight for far targets, then walks the rest of the way to {@code finish}. */
    private static boolean tryElytra(Minecraft mc, Navigator nav, int x, int y, int z, Goal finish, String label) {
        if (!VortexClient.modules.get(com.natan.vortex.module.pathing.NavSettings.class).autoElytra.on()) return false;
        if (mc.level.dimension() == net.minecraft.world.level.Level.NETHER) return false; // roof: walk instead
        var chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.getItem() != net.minecraft.world.item.Items.ELYTRA) return false;
        if (chest.getMaxDamage() - chest.getDamageValue() <= 20) { Chat.info("Elytra almost broken, walking instead."); return false; }
        if (com.natan.vortex.util.Inv.findHotbar(st -> st.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)) == -1) return false;
        double dist = Math.hypot(x + 0.5 - mc.player.getX(), z + 0.5 - mc.player.getZ());
        if (dist < 200) return false;
        nav.start(new com.natan.vortex.nav.process.SequenceProcess(java.util.List.of(
            new com.natan.vortex.nav.process.FlyProcess(new net.minecraft.core.BlockPos(x, y, z), label),
            new GotoProcess(finish, label)), label));
        Chat.info("Flying to " + label + " with the elytra (" + (int) dist + " blocks).");
        return true;
    }

    private static void gotoCmd(String[] a) {
        Minecraft mc = Minecraft.getInstance();
        Navigator nav = Navigator.INSTANCE;
        if (a.length == 4) {
            int x = coord(a[1], mc.player.getBlockX()), y = coord(a[2], mc.player.getBlockY()), z = coord(a[3], mc.player.getBlockZ());
            if (tryElytra(mc, nav, x, y, z, new Goal.Block(x, y, z), x + " " + y + " " + z)) return;
            nav.start(new GotoProcess(new Goal.Block(x, y, z), x + " " + y + " " + z));
        } else if (a.length == 3) {
            int x = coord(a[1], mc.player.getBlockX()), z = coord(a[2], mc.player.getBlockZ());
            nav.start(new GotoProcess(new Goal.XZ(x, z), x + " " + z));
        } else if (a.length == 2) {
            var p = Waypoints.INSTANCE.get(a[1]);
            if (p == null) { Chat.error("No waypoint '" + a[1] + "' in this world/dimension."); return; }
            if (tryElytra(mc, nav, p.x(), p.y(), p.z(), new Goal.Near(p.pos(), 1.5), a[1])) return;
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

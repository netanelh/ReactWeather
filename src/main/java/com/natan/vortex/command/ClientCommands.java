package com.natan.vortex.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.natan.vortex.VortexClient;
import com.natan.vortex.nav.Waypoints;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


/**
 * Registers every chat command as a real "/" client command too, so Minecraft gives tab-completion
 * for each word (module names, block ids, waypoints). The "." chat prefix still works as before;
 * these just forward to the same dispatcher (Commands.execute). Every command also lives under "/vortex <cmd>";
 * names that clash with vanilla/server commands (SHADOWED) are only registered there.
 */
public final class ClientCommands {
    private ClientCommands() {}

    private enum Vocab { NONE, MODULE, BLOCK, ITEM, WAYPOINT, PRESET, PLAYER }

    // command -> aliases (first is canonical) ; and the suggestion vocab for its arguments
    private record Cmd(Vocab vocab, String... names) {}

    private static final List<Cmd> COMMANDS = List.of(
        new Cmd(Vocab.NONE, "help", "h"),
        new Cmd(Vocab.NONE, "stop", "cancel"),
        new Cmd(Vocab.NONE, "pause"), new Cmd(Vocab.NONE, "resume"), new Cmd(Vocab.NONE, "path"),
        new Cmd(Vocab.WAYPOINT, "goto", "g"),
        new Cmd(Vocab.NONE, "home"), new Cmd(Vocab.NONE, "sethome"),
        new Cmd(Vocab.WAYPOINT, "wp", "waypoint"),
        new Cmd(Vocab.BLOCK, "mine"),
        new Cmd(Vocab.PLAYER, "follow"),
        new Cmd(Vocab.NONE, "explore"), new Cmd(Vocab.NONE, "farm"), new Cmd(Vocab.NONE, "tunnel"),
        new Cmd(Vocab.NONE, "hunt", "kill"), new Cmd(Vocab.NONE, "collect", "pickup"),
        new Cmd(Vocab.BLOCK, "find"),
        new Cmd(Vocab.NONE, "surface"), new Cmd(Vocab.NONE, "flee", "run"), new Cmd(Vocab.NONE, "patrol"),
        new Cmd(Vocab.NONE, "locate", "scout"),
        new Cmd(Vocab.NONE, "schem", "schematic"), new Cmd(Vocab.NONE, "litematic", "lm"),
        new Cmd(Vocab.NONE, "build"),
        new Cmd(Vocab.BLOCK, "fill"), new Cmd(Vocab.BLOCK, "walls"), new Cmd(Vocab.BLOCK, "shell"), new Cmd(Vocab.BLOCK, "sphere"),
        new Cmd(Vocab.BLOCK, "replace"),
        new Cmd(Vocab.NONE, "clear"), new Cmd(Vocab.NONE, "copy"), new Cmd(Vocab.NONE, "paste"),
        new Cmd(Vocab.NONE, "sel"), new Cmd(Vocab.NONE, "sel1"), new Cmd(Vocab.NONE, "sel2"),
        new Cmd(Vocab.NONE, "y", "ylevel"),
        new Cmd(Vocab.NONE, "macro"), new Cmd(Vocab.NONE, "do", "chain"),
        new Cmd(Vocab.NONE, "fly"), new Cmd(Vocab.NONE, "vclip"), new Cmd(Vocab.NONE, "hclip"), new Cmd(Vocab.NONE, "tp"),
        new Cmd(Vocab.NONE, "seed"),
        new Cmd(Vocab.ITEM, "fetch"), new Cmd(Vocab.ITEM, "where"), new Cmd(Vocab.NONE, "indexchests"), new Cmd(Vocab.NONE, "memory"),
        new Cmd(Vocab.NONE, "suffix"),
        new Cmd(Vocab.MODULE, "toggle"), new Cmd(Vocab.MODULE, "bind"),
        new Cmd(Vocab.PRESET, "preset"),
        new Cmd(Vocab.PLAYER, "friend"),
        new Cmd(Vocab.NONE, "spam"), new Cmd(Vocab.NONE, "panic"), new Cmd(Vocab.NONE, "modules")
    );

    /** Names that are also vanilla (or common server plugin) commands: only reachable as ".name" or "/vortex name",
     *  so "/kill", "/tp", "/fill" etc. keep going to the server. */
    private static final Set<String> SHADOWED = Set.of("help", "stop", "kill", "clear", "fill", "tp", "locate", "seed",
        "waypoint", "home", "sethome", "fly");

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = LiteralArgumentBuilder.<FabricClientCommandSource>literal("vortex")
                .executes(ctx -> { Commands.execute("help"); return 1; });
            for (Cmd c : COMMANDS) {
                String canonical = c.names()[0];
                SuggestionProvider<FabricClientCommandSource> sugg = provider(c.vocab());
                for (String name : c.names()) {
                    root.then(node(name, canonical, sugg));
                    if (!SHADOWED.contains(name)) dispatcher.register(node(name, canonical, sugg));
                }
            }
            dispatcher.register(root);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> node(String name, String canonical, SuggestionProvider<FabricClientCommandSource> sugg) {
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal(name)
            .executes(ctx -> { Commands.execute(canonical); return 1; })
            .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("args", StringArgumentType.greedyString())
                .suggests(sugg)
                .executes(ctx -> { Commands.execute(canonical + " " + StringArgumentType.getString(ctx, "args")); return 1; }));
    }

    private static SuggestionProvider<FabricClientCommandSource> provider(Vocab v) {
        if (v == Vocab.NONE) return (ctx, b) -> b.buildFuture();
        Supplier<List<String>> words = switch (v) {
            case MODULE -> () -> VortexClient.modules.all().stream().map(m -> m.name.toLowerCase()).toList();
            case BLOCK -> () -> BuiltInRegistries.BLOCK.keySet().stream().map(k -> k.getPath()).toList();
            case ITEM -> () -> BuiltInRegistries.ITEM.keySet().stream().map(k -> k.getPath()).toList();
            case WAYPOINT -> () -> List.copyOf(Waypoints.INSTANCE.here().keySet());
            case PRESET -> () -> VortexClient.config == null ? List.of() : VortexClient.config.presets();
            case PLAYER -> () -> net.minecraft.client.Minecraft.getInstance().level == null ? List.of()
                : net.minecraft.client.Minecraft.getInstance().level.players().stream().map(p -> p.getName().getString()).toList();
            default -> List::of;
        };
        return (ctx, builder) -> completeLastWord(builder, words.get());
    }

    /** Suggest for the word currently being typed inside the greedy argument. */
    private static CompletableFuture<Suggestions> completeLastWord(SuggestionsBuilder builder, List<String> options) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        String word = remaining.substring(lastSpace + 1).toLowerCase();
        SuggestionsBuilder b = builder.createOffset(builder.getStart() + lastSpace + 1);
        for (String o : options) if (o.toLowerCase().startsWith(word)) b.suggest(o);
        return b.buildFuture();
    }
}

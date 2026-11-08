package dev.netanel.lyricsplus.render;

import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.LyricsEngine.Status;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.DisplayMode;
import dev.netanel.lyricsplus.mixin.DisplayAccessor;
import dev.netanel.lyricsplus.mixin.TextDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Floating in the world" mode: each lyric line is a client-only text display entity
 * placed in front of the player. Old lines drift upward and fade out.
 */
public final class WorldLyrics {

    private static final int RETIRE_TICKS = 14;
    private static int nextId = -1_900_000;

    private static final class Floating {
        final Display.TextDisplay entity;
        int age;
        int retiredAge = -1;

        Floating(Display.TextDisplay entity) {
            this.entity = entity;
        }
    }

    private static Floating current;
    private static String currentKey = "";
    private static final List<Floating> retiring = new ArrayList<>();

    private WorldLyrics() {
    }

    public static void tick(Snapshot s) {
        Minecraft mc = Minecraft.getInstance();
        LyricsConfig cfg = LyricsConfig.get();
        ClientLevel level = mc.level;
        Player player = mc.player;

        boolean active = cfg.enabled && cfg.displayMode == DisplayMode.WORLD
                && level != null && player != null && s.status() != Status.NO_PLAYER;
        if (!active) {
            clearAll();
            return;
        }
        if (current != null && current.entity.level() != level) {
            current = null; // world changed, the entity is gone
            retiring.clear();
            currentKey = "";
        }

        Component text = HudRenderer.currentComponent(s, cfg);
        String key = s.title() + "|" + s.index() + "|" + s.status();

        if (!key.equals(currentKey)) {
            currentKey = key;
            if (current != null) {
                current.retiredAge = current.age;
                retiring.add(current);
                current = null;
            }
            if (!text.getString().isBlank()) {
                current = spawn(level, player, cfg);
            }
        }

        if (current != null) {
            current.age++;
            TextDisplayAccessor td = (TextDisplayAccessor) current.entity;
            td.lyricsplus$setText(text);
            td.lyricsplus$setBackgroundColor(cfg.background ? Math.round(cfg.backgroundOpacity * 255) << 24 : 0);
            td.lyricsplus$setFlags((byte) (cfg.shadow ? 1 : 0));
            float pop = cfg.animations ? Math.min(1f, 0.6f + current.age * 0.1f) : 1f;
            transform(current.entity, cfg.scale * pop, 0f);
            td.lyricsplus$setTextOpacity((byte) 255);

            // Walked away? Bring the line back in front of the player.
            if (current.entity.position().distanceTo(anchor(player, cfg)) > cfg.worldDistance + 6) {
                Vec3 a = anchor(player, cfg);
                current.entity.setPos(a.x, a.y, a.z);
            }
        }

        Iterator<Floating> it = retiring.iterator();
        while (it.hasNext()) {
            Floating f = it.next();
            f.age++;
            int t = f.age - f.retiredAge;
            if (t > RETIRE_TICKS || !cfg.animations) {
                remove(level, f);
                it.remove();
                continue;
            }
            float k = t / (float) RETIRE_TICKS;
            transform(f.entity, cfg.scale * (1f - 0.3f * k), k * 0.6f);
            int opacity = Math.max(26, Math.round(255 * (1f - k)));
            ((TextDisplayAccessor) f.entity).lyricsplus$setTextOpacity((byte) opacity);
        }
    }

    private static Floating spawn(ClientLevel level, Player player, LyricsConfig cfg) {
        Display.TextDisplay e = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        Vec3 a = anchor(player, cfg);
        e.setPos(a.x, a.y, a.z);
        e.setId(nextId--);
        DisplayAccessor d = (DisplayAccessor) e;
        d.lyricsplus$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        d.lyricsplus$setBrightnessOverride(Brightness.FULL_BRIGHT);
        d.lyricsplus$setTransformationInterpolationDuration(3);
        ((TextDisplayAccessor) e).lyricsplus$setLineWidth(320);
        level.addEntity(e);
        return new Floating(e);
    }

    private static Vec3 anchor(Player player, LyricsConfig cfg) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(0, 0, 1);
        }
        flat = flat.normalize();
        return eye.add(flat.scale(cfg.worldDistance)).add(0, 0.1, 0);
    }

    private static void transform(Display.TextDisplay e, float scale, float rise) {
        DisplayAccessor d = (DisplayAccessor) e;
        d.lyricsplus$setTransformationInterpolationDelay(0);
        d.lyricsplus$setTransformation(new Transformation(
                new Vector3f(0f, rise, 0f), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
    }

    private static void remove(ClientLevel level, Floating f) {
        if (f.entity.level() == level) {
            level.removeEntity(f.entity.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    public static void clearAll() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            if (current != null) {
                remove(level, current);
            }
            for (Floating f : retiring) {
                remove(level, f);
            }
        }
        current = null;
        retiring.clear();
        currentKey = "";
    }
}

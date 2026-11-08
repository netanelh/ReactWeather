package dev.netanel.lyricsplus.render;

import com.mojang.math.Transformation;
import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.LyricsEngine.Status;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.DisplayMode;
import dev.netanel.lyricsplus.config.LyricsConfig.WorldStyle;
import dev.netanel.lyricsplus.mixin.DisplayAccessor;
import dev.netanel.lyricsplus.mixin.TextDisplayAccessor;
import dev.netanel.lyricsplus.render.LineFormatter.Paint;
import dev.netanel.lyricsplus.render.LineFormatter.WordTiming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Floating in the world" mode. Each word (or the whole line) is a client-only text
 * display entity in front of the player. Words pop in as they are sung, can follow the
 * player, and float away when the line ends.
 */
public final class WorldLyrics {

    /** Text displays render at 1/40 block per font pixel. */
    private static final float PX = 0.025f;
    private static final int RETIRE_TICKS = 14;
    private static final int[] RANDOM_PALETTE = {
            0xFF5555, 0xFFAA00, 0xFFE066, 0x55FF77, 0x55FFFF, 0x55AAFF, 0xB57BFF, 0xFF77C8
    };
    private static int nextId = -1_900_000;

    private static final class Piece {
        final Display.TextDisplay entity;
        final float lateral; // blocks along the line, from its center
        final float row;     // blocks below the first row
        final float depth;   // blocks toward (-) or away from (+) the player
        final float tilt;    // radians, scatter mode only
        final float sizeMul;
        final int index;
        int age;
        int retiredAge = -1;

        Piece(Display.TextDisplay entity, float lateral, float row, int index) {
            this(entity, lateral, row, 0f, 0f, 1f, index);
        }

        Piece(Display.TextDisplay entity, float lateral, float row, float depth, float tilt, float sizeMul, int index) {
            this.entity = entity;
            this.lateral = lateral;
            this.row = row;
            this.depth = depth;
            this.tilt = tilt;
            this.sizeMul = sizeMul;
            this.index = index;
        }
    }

    /** Layout of the current line, computed once when the line starts. */
    private static String lineKey = "";
    private static String[] words = new String[0];
    private static float[] laterals = new float[0];
    private static float[] rows = new float[0];
    private static float[] depths = new float[0];
    private static float[] tilts = new float[0];
    private static float[] sizes = new float[0];
    private static boolean scatter;
    private static Piece[] spawned = new Piece[0];
    private static Vec3 fixedBase;
    private static Vec3 fixedRight;
    private static final List<Piece> retiring = new ArrayList<>();
    private static ClientLevel lastLevel;

    private WorldLyrics() {
    }

    public static void tick(Snapshot s) {
        Minecraft mc = Minecraft.getInstance();
        LyricsConfig cfg = LyricsConfig.get();
        ClientLevel level = mc.level;
        Player player = mc.player;

        boolean active = cfg.enabled && cfg.displayMode == DisplayMode.WORLD
                && level != null && player != null && s.status() != Status.NO_PLAYER && s.status() != Status.AD;
        if (!active) {
            clearAll();
            return;
        }
        if (level != lastLevel) { // world changed: old entities are gone with it
            lastLevel = level;
            spawned = new Piece[0];
            retiring.clear();
            lineKey = "";
        }

        boolean realLine = s.status() == Status.READY && s.hasLine() && !s.instrumental();
        boolean wordMode = realLine && cfg.worldStyle != WorldStyle.LINE;
        String key = s.title() + "|" + s.index() + "|" + s.status() + "|" + wordMode + cfg.worldStyle + "|" + (realLine ? "" : s.instrumental());

        if (!key.equals(lineKey)) {
            lineKey = key;
            retireCurrent();
            layout(s, cfg, wordMode, player);
        }

        Vec3 base = cfg.worldFollow ? anchor(player, cfg) : fixedBase;
        Vec3 right = cfg.worldFollow ? rightOf(player) : fixedRight;
        if (!cfg.worldFollow && base.distanceTo(anchor(player, cfg)) > cfg.worldDistance + 6) {
            fixedBase = anchor(player, cfg); // walked away: bring the line back
            fixedRight = rightOf(player);
            base = fixedBase;
            right = fixedRight;
        }

        if (wordMode) {
            tickWords(s, cfg, level, base, right);
        } else {
            tickLine(s, cfg, level, base, right);
        }
        tickRetiring(cfg, level, base, right);
    }

    // ------------------------------------------------------------------ layout

    private static void layout(Snapshot s, LyricsConfig cfg, boolean wordMode, Player player) {
        fixedBase = anchor(player, cfg);
        fixedRight = rightOf(player);
        var font = Minecraft.getInstance().font;

        if (!wordMode) {
            words = new String[]{""};
            laterals = new float[]{0f};
            rows = new float[]{0f};
            spawned = new Piece[1];
            return;
        }

        WordTiming t = LineFormatter.timing(s);
        words = t.words();
        int n = words.length;
        laterals = new float[n];
        rows = new float[n];
        depths = new float[n];
        tilts = new float[n];
        sizes = new float[n];
        java.util.Arrays.fill(sizes, 1f);
        spawned = new Piece[n];
        scatter = cfg.worldStyle == WorldStyle.SCATTER;
        if (scatter) {
            scatterLayout(s, cfg, n);
            return;
        }

        float scale = cfg.wordScale;
        float space = font.width(" ") * PX * scale * 1.6f * cfg.wordSpacing;
        float maxRow = Math.max(2.5f, cfg.worldDistance * 1.3f);
        float rowHeight = font.lineHeight * PX * scale * 1.35f;
        boolean rtl = LineFormatter.isRtl(s.line(0));

        // Greedy wrap into rows, then center each row.
        List<int[]> rowRanges = new ArrayList<>();
        float[] widths = new float[n];
        int start = 0;
        float rowW = 0;
        for (int i = 0; i < n; i++) {
            widths[i] = font.width(words[i]) * PX * scale;
            float add = (i == start ? 0 : space) + widths[i];
            if (i > start && rowW + add > maxRow) {
                rowRanges.add(new int[]{start, i});
                start = i;
                rowW = widths[i];
            } else {
                rowW += add;
            }
        }
        if (n > 0) rowRanges.add(new int[]{start, n});

        float totalRows = rowRanges.size();
        for (int r = 0; r < rowRanges.size(); r++) {
            int a = rowRanges.get(r)[0], b = rowRanges.get(r)[1];
            float w = 0;
            for (int i = a; i < b; i++) w += widths[i] + (i > a ? space : 0);
            float cursor = -w / 2f;
            for (int i = a; i < b; i++) {
                float center = cursor + widths[i] / 2f;
                laterals[i] = rtl ? -center : center; // RTL: first word on the right
                rows[i] = (r - (totalRows - 1) / 2f) * rowHeight;
                cursor += widths[i] + space;
            }
        }
    }

    /** Random, non-overlapping spots in front of the player, stable for the whole line. */
    private static void scatterLayout(Snapshot s, LyricsConfig cfg, int n) {
        java.util.Random rnd = new java.util.Random((s.title() + "#" + s.index()).hashCode());
        float spreadX = cfg.scatterSpread * Math.max(1.2f, cfg.worldDistance * 0.55f);
        float spreadY = cfg.scatterSpread * Math.max(0.6f, cfg.worldDistance * 0.28f);
        float minGap = 0.45f * cfg.wordScale;
        for (int i = 0; i < n; i++) {
            float bx = 0, by = 0;
            for (int attempt = 0; attempt < 14; attempt++) {
                bx = (rnd.nextFloat() * 2f - 1f) * spreadX;
                by = (rnd.nextFloat() * 2f - 1f) * spreadY;
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    float dx = (bx - laterals[j]) * 0.6f; // words are wide: horizontal gaps matter less
                    float dy = by + rows[j];
                    if (dx * dx + dy * dy < minGap * minGap) {
                        ok = false;
                        break;
                    }
                }
                if (ok) break;
            }
            laterals[i] = bx;
            rows[i] = -by;
            depths[i] = (rnd.nextFloat() * 2f - 1f) * 0.6f * cfg.scatterSpread;
            tilts[i] = (float) Math.toRadians((rnd.nextFloat() * 2f - 1f) * cfg.scatterTilt);
            sizes[i] = 1f + (rnd.nextFloat() * 2f - 1f) * cfg.scatterSizeVariation;
        }
    }

    // ------------------------------------------------------------------ ticking

    private static void tickWords(Snapshot s, LyricsConfig cfg, ClientLevel level, Vec3 base, Vec3 right) {
        WordTiming t = LineFormatter.timing(s);
        int reached = t.reached(s.positionMs());
        boolean lineSung = s.positionMs() >= t.singEndMs();

        for (int i = 0; i < spawned.length && i < t.words().length; i++) {
            if (spawned[i] == null && i <= reached) {
                spawned[i] = new Piece(spawn(level, base), laterals[i], rows[i], depths[i], tilts[i], sizes[i], i);
            }
            Piece p = spawned[i];
            if (p == null) continue;
            p.age++;

            boolean current = i == reached && !lineSung;
            Paint paint = switch (cfg.wordColors) {
                case SINGLE -> Paint.text(cfg);
                case HIGHLIGHT -> current ? Paint.highlight(cfg) : Paint.text(cfg);
                case RAINBOW -> new Paint(0, true);
                case RANDOM -> new Paint(RANDOM_PALETTE[Math.floorMod(words[i].hashCode() + i, RANDOM_PALETTE.length)], false);
            };
            Component c = Component.literal(t.words()[i]).withStyle(LineFormatter.style(paint.at(i)));
            float size = cfg.wordScale * p.sizeMul * (current ? cfg.currentWordScale : 1f);
            apply(p, c, cfg, base, right, size, 0f, 255);
        }
    }

    private static void tickLine(Snapshot s, LyricsConfig cfg, ClientLevel level, Vec3 base, Vec3 right) {
        Component text = HudRenderer.currentComponent(s, cfg);
        if (text.getString().isBlank()) {
            return;
        }
        if (spawned.length == 0) {
            spawned = new Piece[1];
        }
        if (spawned[0] == null) {
            spawned[0] = new Piece(spawn(level, base), 0f, 0f, 0);
        }
        Piece p = spawned[0];
        p.age++;
        apply(p, text, cfg, base, right, cfg.wordScale * 0.8f, 0f, 255);
    }

    private static void tickRetiring(LyricsConfig cfg, ClientLevel level, Vec3 base, Vec3 right) {
        Iterator<Piece> it = retiring.iterator();
        while (it.hasNext()) {
            Piece p = it.next();
            p.age++;
            int t = p.age - p.retiredAge;
            if (t > RETIRE_TICKS || !cfg.animations) {
                remove(level, p);
                it.remove();
                continue;
            }
            float k = t / (float) RETIRE_TICKS;
            place(p, base, right, cfg);
            transform(p.entity, cfg.wordScale * p.sizeMul * (1f - 0.4f * k), new Vector3f(0, k * 0.8f, 0), p.tilt);
            ((TextDisplayAccessor) p.entity).lyricsplus$setTextOpacity((byte) Math.max(26, Math.round(255 * (1f - k))));
        }
    }

    // ------------------------------------------------------------------ entity helpers

    private static void apply(Piece p, Component text, LyricsConfig cfg, Vec3 base, Vec3 right,
                              float size, float extraRise, int opacity) {
        TextDisplayAccessor td = (TextDisplayAccessor) p.entity;
        td.lyricsplus$setText(text);
        td.lyricsplus$setBackgroundColor(cfg.background ? Math.round(cfg.backgroundOpacity * 255) << 24 : 0);
        td.lyricsplus$setFlags((byte) (cfg.shadow ? 1 : 0));
        td.lyricsplus$setTextOpacity((byte) opacity);
        place(p, base, right, cfg);

        // Entry animation, measured in ticks since the word appeared.
        float k = cfg.animations ? Math.min(1f, p.age / 5f) : 1f;
        float ease = 1f - (1f - k) * (1f - k);
        Vector3f offset = new Vector3f(0, extraRise, 0);
        float scale = size;
        float spin = 0f;
        switch (cfg.wordAnimation) {
            case POP -> scale = size * (k < 1f ? 0.2f + 1.0f * ease + 0.15f * (float) Math.sin(k * Math.PI) : 1f);
            case RISE -> offset.y -= (1f - ease) * 0.5f;
            case DROP -> offset.y += (1f - ease) * 0.7f;
            case SPIN -> spin = (1f - ease) * (float) Math.PI * 2f;
            case NONE -> { }
        }
        if (!cfg.animations) {
            offset.y = extraRise;
            scale = size;
            spin = 0f;
        }
        transform(p.entity, scale, offset, spin + p.tilt);
    }

    private static void place(Piece p, Vec3 base, Vec3 right, LyricsConfig cfg) {
        Vec3 forward = new Vec3(right.z, 0, -right.x);
        Vec3 pos = base.add(right.scale(p.lateral)).add(0, -p.row, 0).add(forward.scale(p.depth));

        // Scatter mode: fly from the chosen origin to the word's spot.
        if (scatter && cfg.animations && p.retiredAge < 0 && p.age < 7) {
            float k = p.age / 7f;
            float ease = 1f - (1f - k) * (1f - k) * (1f - k);
            Vec3 from = switch (cfg.scatterFrom) {
                case CENTER -> base;
                case SIDES -> base.add(right.scale(Math.signum(p.lateral == 0 ? 1 : p.lateral) * (cfg.worldDistance + 2)));
                case BELOW -> pos.add(0, -2.5, 0);
                case ABOVE -> pos.add(0, 2.5, 0);
                case PLAYER -> base.subtract(forward.scale(cfg.worldDistance - 0.8)).add(0, -0.4, 0);
            };
            pos = from.add(pos.subtract(from).scale(ease));
        }
        p.entity.setPos(pos.x, pos.y, pos.z);
    }

    private static Display.TextDisplay spawn(ClientLevel level, Vec3 at) {
        Display.TextDisplay e = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        e.setPos(at.x, at.y, at.z);
        e.setId(nextId--);
        DisplayAccessor d = (DisplayAccessor) e;
        d.lyricsplus$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        d.lyricsplus$setBrightnessOverride(Brightness.FULL_BRIGHT);
        d.lyricsplus$setTransformationInterpolationDuration(2);
        ((TextDisplayAccessor) e).lyricsplus$setLineWidth(400);
        level.addEntity(e);
        return e;
    }

    private static Vec3 anchor(Player player, LyricsConfig cfg) {
        Vec3 flat = flatLook(player);
        return player.getEyePosition().add(flat.scale(cfg.worldDistance)).add(0, 0.1 + cfg.worldHeight, 0);
    }

    private static Vec3 rightOf(Player player) {
        Vec3 f = flatLook(player);
        return new Vec3(-f.z, 0, f.x);
    }

    private static Vec3 flatLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static void transform(Display.TextDisplay e, float scale, Vector3f offset, float spin) {
        DisplayAccessor d = (DisplayAccessor) e;
        d.lyricsplus$setTransformationInterpolationDelay(0);
        d.lyricsplus$setTransformation(new Transformation(
                offset, new Quaternionf().rotateZ(spin), new Vector3f(scale, scale, scale), new Quaternionf()));
    }

    private static void retireCurrent() {
        for (Piece p : spawned) {
            if (p != null) {
                p.retiredAge = p.age;
                retiring.add(p);
            }
        }
        spawned = new Piece[0];
    }

    private static void remove(ClientLevel level, Piece p) {
        if (level != null && p.entity.level() == level) {
            level.removeEntity(p.entity.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    public static void clearAll() {
        ClientLevel level = Minecraft.getInstance().level;
        for (Piece p : spawned) {
            if (p != null) remove(level, p);
        }
        for (Piece p : retiring) {
            remove(level, p);
        }
        spawned = new Piece[0];
        retiring.clear();
        lineKey = "";
    }
}

package dev.netanel.lyricsplus.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finds the album cover for the current song (iTunes search), uploads it as a texture,
 * and extracts a vivid dominant color for the "colors from album" option.
 */
public final class AlbumArt {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("lyricsplus", "album_art");
    private static final int SIZE = 64;

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LyricsPlus-AlbumArt");
        t.setDaemon(true);
        return t;
    });

    private static volatile String wantedKey = "";
    private static volatile boolean ready;
    private static volatile int dominant = -1;

    private AlbumArt() {
    }

    public static boolean ready() {
        return ready;
    }

    /** Vivid color from the cover, or -1 if there is none yet. */
    public static int dominant() {
        return dominant;
    }

    public static void request(String artist, String title) {
        String key = artist + "|" + title;
        if (key.equals(wantedKey)) {
            return;
        }
        wantedKey = key;
        ready = false;
        dominant = -1;
        if (artist.isBlank() || title.isBlank()) {
            return;
        }
        WORKER.submit(() -> {
            try {
                BufferedImage img = fetch(artist, title);
                if (img == null || !key.equals(wantedKey)) {
                    return;
                }
                BufferedImage small = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = small.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img, 0, 0, SIZE, SIZE, null);
                g.dispose();
                int[] pixels = small.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
                int color = vividAverage(pixels);
                Minecraft.getInstance().execute(() -> upload(key, pixels, color));
            } catch (Throwable e) {
                System.err.println("[Lyrics Plus] album art failed: " + e.getMessage());
            }
        });
    }

    private static void upload(String key, int[] pixels, int color) {
        if (!key.equals(wantedKey)) {
            return;
        }
        try {
            NativeImage image = new NativeImage(SIZE, SIZE, false);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    image.setPixel(x, y, pixels[y * SIZE + x] | 0xFF000000);
                }
            }
            DynamicTexture texture = new DynamicTexture(() -> "lyricsplus album art", image);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            dominant = color;
            ready = true;
        } catch (Throwable e) {
            System.err.println("[Lyrics Plus] album art upload failed: " + e.getMessage());
        }
    }

    private static BufferedImage fetch(String artist, String title) throws Exception {
        String term = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
        HttpResponse<String> search = CLIENT.send(HttpRequest.newBuilder(
                        URI.create("https://itunes.apple.com/search?entity=song&limit=1&term=" + term))
                .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (search.statusCode() != 200) {
            return null;
        }
        JsonObject root = JsonParser.parseString(search.body()).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        if (results == null || results.isEmpty()) {
            return null;
        }
        JsonObject first = results.get(0).getAsJsonObject();
        if (!first.has("artworkUrl100")) {
            return null;
        }
        String url = first.get("artworkUrl100").getAsString().replace("100x100bb", "200x200bb");
        HttpResponse<byte[]> img = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (img.statusCode() != 200) {
            return null;
        }
        return ImageIO.read(new ByteArrayInputStream(img.body()));
    }

    /** Saturation-weighted average, then pushed bright and colorful so text stays readable. */
    private static int vividAverage(int[] argb) {
        double r = 0, g = 0, b = 0, wsum = 0;
        for (int p : argb) {
            int pr = (p >> 16) & 0xFF, pg = (p >> 8) & 0xFF, pb = p & 0xFF;
            int max = Math.max(pr, Math.max(pg, pb)), min = Math.min(pr, Math.min(pg, pb));
            double sat = max == 0 ? 0 : (max - min) / (double) max;
            double w = 0.05 + sat * sat * (max / 255.0);
            r += pr * w;
            g += pg * w;
            b += pb * w;
            wsum += w;
        }
        float[] hsv = java.awt.Color.RGBtoHSB((int) (r / wsum), (int) (g / wsum), (int) (b / wsum), null);
        float s = Math.max(hsv[1], 0.55f);
        float v = Math.max(hsv[2], 0.9f);
        return java.awt.Color.HSBtoRGB(hsv[0], s, v) & 0xFFFFFF;
    }
}

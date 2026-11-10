package com.natan.vortex.util;

public final class Colors {
    public static final String[] NAMES = {"Purple", "Blue", "Cyan", "Green", "Lime", "Yellow", "Orange", "Red", "Pink", "White", "Rainbow"};

    private Colors() {}

    /** Returns opaque ARGB for a named color. "Rainbow" shifts over time; offset staggers rows. */
    public static int named(String name, int offset) {
        return switch (name) {
            case "Purple" -> 0xFF9B5CFF;
            case "Blue" -> 0xFF3D7BFF;
            case "Cyan" -> 0xFF22D3EE;
            case "Green" -> 0xFF22C55E;
            case "Lime" -> 0xFFA3E635;
            case "Yellow" -> 0xFFFACC15;
            case "Orange" -> 0xFFFF8A1F;
            case "Red" -> 0xFFEF4444;
            case "Pink" -> 0xFFF472B6;
            case "White" -> 0xFFFFFFFF;
            default -> rainbow(offset);
        };
    }

    public static int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset * 120L) % 4000L) / 4000f;
        return hsv(hue, 0.6f, 1f);
    }

    public static int hsv(float h, float s, float v) {
        float r, g, b;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6), p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int withAlpha(int argb, int alpha) { return (alpha << 24) | (argb & 0xFFFFFF); }

    public static int lerp(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
            | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}

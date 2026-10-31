package com.example.spcombat;

/** GUI, HUD ve menu renk temalari. Tema GUI'nin son sutunundan secilir. */
public final class Theme {
    public final String name;
    public final int accent; // ARGB, alfa her zaman 0xFF

    private Theme(String name, int accent) {
        this.name = name;
        this.accent = accent;
    }

    public static final Theme[] ALL = {
            new Theme("Abyss Blue", 0xFF3B82F6),
            new Theme("Sakura", 0xFFFF7EC8),
            new Theme("Cyber Dragon", 0xFF22D3EE),
            new Theme("Aurora", 0xFF8B5CF6),
            new Theme("Matrix", 0xFF22FF66),
            new Theme("Hellfire", 0xFFFF5A1F),
            new Theme("Frozen Soul", 0xFF9BE7FF),
            new Theme("Gold Dust", 0xFFFFC53D),
            new Theme("Neon Wave", 0xFFFF3DD8),
            new Theme("Desert Rose", 0xFFE88A93),
            new Theme("Ghost", 0xFFB8BCC4)
    };

    private static int index = 1; // Sakura

    public static Theme cur() { return ALL[index]; }

    public static int index() { return index; }

    public static void set(int i) {
        if (i >= 0 && i < ALL.length) index = i;
    }

    /** RGB'yi korur, alfayi degistirir. */
    public static int alpha(int argb, int a) {
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** RGB'yi f ile carpip koyulastirir (opak dondurur). */
    public static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}

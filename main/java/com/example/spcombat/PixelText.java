package com.example.spcombat;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Kare kare cizilen kucuk piksel yazi tipi.
 * Font yuklenmeden once de (acilis ekraninda) calissin diye font kullanmaz.
 * Desteklenen harfler: f e m b o y F l i x D L C ve bosluk.
 */
public final class PixelText {
    private PixelText() {}

    private static final Map<Character, String[]> G = new HashMap<>();

    private static void g(char c, String... rows) { G.put(c, rows); }

    static {
        g('f', "..##.", ".#...", "####.", ".#...", ".#...", ".#...", ".#...");
        g('e', ".....", ".###.", "#...#", "#####", "#....", "#...#", ".###.");
        g('m', ".....", "##.#.", "#.#.#", "#.#.#", "#.#.#", "#...#", "#...#");
        g('b', "#....", "#....", "####.", "#...#", "#...#", "#...#", "####.");
        g('o', ".....", ".###.", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('y', ".....", "#...#", "#...#", "#...#", ".####", "....#", ".###.");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('l', ".##..", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.");
        g('i', "..#..", ".....", ".##..", "..#..", "..#..", "..#..", ".###.");
        g('x', ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
    }

    /** Yazinin piksel (olcek 1) genisligi. */
    public static int width(String text) {
        int w = 0;
        for (char c : text.toCharArray()) w += (c == ' ') ? 3 : 6;
        return Math.max(0, w - 1);
    }

    public static int height() { return 7; }

    public static void draw(DrawContext ctx, String text, int x, int y, int scale, int color) {
        int cx = x;
        for (char c : text.toCharArray()) {
            if (c == ' ') { cx += 3 * scale; continue; }
            String[] rows = G.get(c);
            if (rows == null) { cx += 6 * scale; continue; }
            for (int r = 0; r < rows.length; r++) {
                for (int col = 0; col < rows[r].length(); col++) {
                    if (rows[r].charAt(col) == '#') {
                        int px = cx + col * scale;
                        int py = y + r * scale;
                        ctx.fill(px, py, px + scale, py + scale, color);
                    }
                }
            }
            cx += 6 * scale;
        }
    }

    public static void drawCentered(DrawContext ctx, String text, int centerX, int y, int scale, int color) {
        draw(ctx, text, centerX - (width(text) * scale) / 2, y, scale, color);
    }
}

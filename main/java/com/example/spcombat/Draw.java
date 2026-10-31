package com.example.spcombat;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Yumusak (anti-aliased) yuvarlak koseli kutular. Kenarlari pikselli degil:
 * 64x64'luk yuvarlak kutu resmi 9 parcaya bolunup (koseler + kenarlar + orta) istenen boyuta
 * gerilir, resim lineer filtreli oldugu icin koseler puruzsuz gorunur. Renk (alfa dahil) carpan olarak uygulanir.
 */
public final class Draw {
    private Draw() {}

    private static final Identifier RR = Identifier.of("spcombat", "textures/gui/rr.png");
    private static final int TEX = 64;   // resim boyutu
    private static final int SRC = 24;   // kose parcasi (resimde)

    public static void rbox(DrawContext ctx, int x, int y, int w, int h, int color) {
        rbox(ctx, x, y, w, h, color, 5);
    }

    public static void rbox(DrawContext ctx, int x, int y, int w, int h, int color, int r) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r < 2) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        int mid = TEX - 2 * SRC;
        // koseler
        part(ctx, x, y, 0, 0, r, r, SRC, SRC, color);
        part(ctx, x + w - r, y, TEX - SRC, 0, r, r, SRC, SRC, color);
        part(ctx, x, y + h - r, 0, TEX - SRC, r, r, SRC, SRC, color);
        part(ctx, x + w - r, y + h - r, TEX - SRC, TEX - SRC, r, r, SRC, SRC, color);
        // kenarlar
        if (w - 2 * r > 0) {
            part(ctx, x + r, y, SRC, 0, w - 2 * r, r, mid, SRC, color);
            part(ctx, x + r, y + h - r, SRC, TEX - SRC, w - 2 * r, r, mid, SRC, color);
        }
        if (h - 2 * r > 0) {
            part(ctx, x, y + r, 0, SRC, r, h - 2 * r, SRC, mid, color);
            part(ctx, x + w - r, y + r, TEX - SRC, SRC, r, h - 2 * r, SRC, mid, color);
        }
        // orta
        if (w - 2 * r > 0 && h - 2 * r > 0) {
            part(ctx, x + r, y + r, SRC, SRC, w - 2 * r, h - 2 * r, mid, mid, color);
        }
    }

    /** Kutunun etrafina yumusak golge. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r) {
        rbox(ctx, x - 3, y - 2, w + 6, h + 7, 0x18000000, r + 3);
        rbox(ctx, x - 2, y - 1, w + 4, h + 4, 0x20000000, r + 2);
        rbox(ctx, x - 1, y, w + 2, h + 2, 0x28000000, r + 1);
    }

    private static void part(DrawContext ctx, int x, int y, int u, int v, int w, int h,
                             int rw, int rh, int color) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, RR, x, y, (float) u, (float) v, w, h, rw, rh, TEX, TEX, color);
    }
}

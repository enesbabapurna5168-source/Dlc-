package com.example.spcombat;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.TranslatableTextContent;

/**
 * Oyun menusundeki "Kaydet ve basliga don" (sunucuda "Baglantiyi kes") butonunu
 * guzellestirir, bir miktar asagi kaydirir ve ustune "Menemen DLC" yazar.
 * Butonun kendisi (tiklama davranisi) degismez, ustune renkli bir katman cizilir.
 * Renkler secili temadan gelir.
 */
public final class MenuStyle {
    private MenuStyle() {}

    /** Ekranin ustunde buyuk "Menemen DLC" basligi: cam plaka, parlayan cizgi, alt yazi. */
    private static void banner(net.minecraft.client.gui.DrawContext ctx, int sw, int accent) {
        double tt = System.currentTimeMillis() / 1000.0;
        float size = 26f;
        float tw = Fnt.widthTitle("Menemen DLC", size);
        int pw = (int) tw + 64, ph = 50;
        int px = (sw - pw) / 2, py = 8;

        Draw.shadow(ctx, px, py, pw, ph, 12);
        Draw.rbox(ctx, px, py, pw, ph, Theme.alpha(accent, 0x90), 12);
        Draw.rbox(ctx, px + 1, py + 1, pw - 2, ph - 2, 0xF0090B0F, 11);
        ctx.fillGradient(px + 6, py + 4, px + pw - 6, py + ph / 2, 0x16FFFFFF, 0x00FFFFFF);

        float tx = px + (pw - tw) / 2f;
        float ty = py + 4;
        Fnt.drawTitle(ctx, "Menemen", tx + 2, ty + 2, size, 0x66000000);
        Fnt.drawTitle(ctx, "Menemen", tx, ty, size, 0xFFFFFFFF);
        float dx = tx + Fnt.widthTitle("Menemen ", size);
        Fnt.drawTitle(ctx, "DLC", dx + 1, ty + 1, size, 0x55000000);
        Fnt.drawTitle(ctx, "DLC", dx, ty, size, accent);

        // alt cizgi: kenarlara dogru sonen, uzerinde gezen parlama
        int ly = py + ph - 13;
        int lx0 = px + 18, lx1 = px + pw - 18;
        ctx.fill(lx0, ly, lx1, ly + 1, Theme.alpha(accent, 0x70));
        int gl = (int) (lx0 + ((tt * 0.5) % 1.0) * (lx1 - lx0));
        ctx.fillGradient(Math.max(lx0, gl - 22), ly - 1, Math.min(lx1, gl + 22), ly + 2, 0x00FFFFFF, 0x00FFFFFF);
        Draw.rbox(ctx, Math.max(lx0, gl - 14), ly - 1, 28, 3, 0xD0FFFFFF, 1);

        int on = 0;
        for (Module mo : Modules.ALL) if (mo.enabled && !mo.cosmetic && !mo.actionKey) on++;
        String sub = "PREMIUM CLIENT   \u00B7   " + on + " modul aktif";
        Fnt.drawCentered(ctx, sub, sw / 2f, py + ph - 10, 7f, 0xFF9BA0AA, false);
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof GameMenuScreen)) return;

            ClickableWidget found = null;
            for (ClickableWidget b : Screens.getButtons(screen)) {
                if (b.getMessage().getContent() instanceof TranslatableTextContent t) {
                    String k = t.getKey();
                    if (k.equals("menu.returnToMenu") || k.equals("menu.disconnect")) found = b;
                }
            }
            if (found == null) return;

            final ClickableWidget btn = found;
            btn.setY(btn.getY() + 6); // yazi icin bosluk ac

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                int accent = Theme.cur().accent;
                int x = btn.getX(), y = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
                boolean hover = mx >= x && mx < x + bw && my >= y && my < y + bh;

                ctx.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, accent);
                ctx.fill(x, y, x + bw, y + bh, Theme.shade(accent, hover ? 0.75f : 0.45f));
                ctx.fill(x, y, x + bw, y + bh / 2, hover ? 0x33FFFFFF : 0x22FFFFFF); // ust parlama

                MinecraftClient mc = MinecraftClient.getInstance();
                Fnt.drawCentered(ctx, btn.getMessage().getString(), x + bw / 2f, y + (bh - 9) / 2f, 9, 0xFFFFFFFF, true);

                banner(ctx, s.width, accent);
            });
        });
    }
}

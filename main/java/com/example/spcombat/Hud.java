package com.example.spcombat;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Ekran ustu paneller: bilgi/modul listesi, zirh dayanikliligi, hedef cani. */
public final class Hud {
    private Hud() {}

    public static void init() {
        HudRenderCallback.EVENT.register(Hud::render);
    }

    private static int tw(String s) { return Fnt.w(s, 9f); }

    private static void txt(DrawContext ctx, String s, int x, int y, int color) {
        Fnt.drawShadow(ctx, s, x, y, 9f, color, false);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;
        int accent = Theme.cur().accent;
        Extras.render(ctx, mc);

        if (Modules.HUD.enabled) basic(ctx, mc, tr, accent);
        if (Modules.ARMORHUD.enabled) armor(ctx, mc, tr, accent);
        if (Modules.TARGETHUD.enabled) targetHud(ctx, tr, accent);
        int bottom = Modules.KEYBINDS.enabled ? keybinds(ctx, tr, accent) : 4;
        if (Modules.BIRTANEM.enabled) birtanem(ctx, bottom, accent);
    }

    // ---------------- acik modullerin listesi (ekrani kapatmaz, kose paneli) ----------------
    private static int keybinds(DrawContext ctx, TextRenderer tr, int accent) {
        List<Module> list = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (m.cosmetic || !m.enabled) continue;
            if (m.actionKey && m.bind < 0) continue; // tusu olmayan eylem modulu gosterme
            list.add(m);
        }
        if (list.isEmpty()) return 4;

        boolean keys = Modules.KEYBINDS.bool("Show keys");
        boolean right = Modules.KEYBINDS.index("Side") == 1;

        int rowH = 12, headH = 16, w = 100;
        for (Module m : list) {
            int need = tw(m.name) + 16 + (keys ? 34 : 0);
            if (need > w) w = need;
        }
        int h = headH + list.size() * rowH + 4;
        int x = right ? ctx.getScaledWindowWidth() - w - 4 : 4;
        int y = 4;

        // govde: koyu yuvarlak panel, ustte tema renginde baslik
        pill(ctx, x, y, w, h, accent, 0xE0101216);
        Draw.rbox(ctx, x + 2, y + 2, w - 4, headH - 2, Theme.alpha(accent, 0xC0), 4);
        String title = "Keybinds";
        txt(ctx, title, x + (w - tw(title)) / 2, y + 4, 0xFFFFFFFF);

        int ry = y + headH + 3;
        for (Module m : list) {
            txt(ctx, m.name, x + 7, ry, 0xFFFFFFFF);
            if (keys) {
                String k = m.bind < 0 ? "-" : Keys.name(m.bind);
                txt(ctx, k, x + w - 7 - tw(k), ry, 0xFF9BA0AA);
            }
            ry += rowH;
        }
        return y + h;
    }

    private static final String[] HEART = {
            ".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};

    /** Keybinds panelinin altinda "Cemre <kalp> Burak" yazisi. */
    private static void birtanem(DrawContext ctx, int top, int accent) {
        boolean right = Modules.KEYBINDS.index("Side") == 1;
        float fs = 9f;
        int hw = 7 * 2 + 4;
        int w = Fnt.w("Cemre", fs) + Fnt.w("Burak", fs) + hw + 22;
        int h = 18;
        int x = right ? ctx.getScaledWindowWidth() - w - 4 : 4;
        int y = top + (top > 4 ? 4 : 0);
        pill(ctx, x, y, w, h, accent, 0xE0101216);
        int tx = x + 9;
        Fnt.drawShadow(ctx, "Cemre", tx, y + 4, fs, 0xFFFFFFFF, true);
        tx += Fnt.w("Cemre", fs) + 4;
        float beat = 1f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 180.0);
        int px = Math.max(1, Math.round(2 * beat));
        int hy = y + (h - 6 * px) / 2;
        for (int r = 0; r < HEART.length; r++) {
            for (int c = 0; c < 7; c++) {
                if (HEART[r].charAt(c) == '#') {
                    ctx.fill(tx + c * px, hy + r * px, tx + (c + 1) * px, hy + (r + 1) * px, 0xFFFF3B5C);
                }
            }
        }
        tx += hw;
        Fnt.drawShadow(ctx, "Burak", tx, y + 4, fs, 0xFFFFFFFF, true);
    }

    /** Kenarlikli yumusak yuvarlak kutu. */
    private static void pill(DrawContext ctx, int x, int y, int w, int h, int border, int fill) {
        Draw.shadow(ctx, x, y, w, h, 6);
        Draw.rbox(ctx, x, y, w, h, border, 6);
        Draw.rbox(ctx, x + 1, y + 1, w - 2, h - 2, fill, 5);
    }

    // ---------------- yazi, koordinat, modul listesi ----------------
    private static void basic(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent) {
        Module hud = Modules.HUD;

        if (hud.bool("Watermark")) {
            String name = mc.player.getName().getString();
            int uid = 10000 + Math.floorMod(name.hashCode(), 90000);
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
            String txt = "MENEMEN CLIENT \u00BB " + name + " \u00BB UID: " + uid + " \u00BB " + time;
            int w = tw(txt) + 14;
            pill(ctx, 4, 4, w, 16, Theme.alpha(accent, 0xFF), 0xE0101216);
            txt(ctx, txt, 11, 8, 0xFFFFFFFF);
        }

        if (hud.bool("Coords")) {
            String s = String.format("XYZ: %.1f / %.1f / %.1f   FPS: %d",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.getCurrentFps());
            txt(ctx, s, 6, 26, 0xFFFFFFFF);
        }

        if (hud.bool("Module list")) {
            List<String> names = new ArrayList<>();
            for (Module m : Modules.ALL) {
                if (!m.cosmetic && !m.actionKey && m.enabled) names.add(m.name);
            }
            names.sort((a, b) -> tw(b) - tw(a));
            int w = ctx.getScaledWindowWidth();
            int y = 4;
            for (String n : names) {
                int nw = tw(n);
                ctx.fill(w - nw - 8, y - 1, w - 2, y + 9, 0x70000000);
                ctx.fill(w - 3, y - 1, w - 2, y + 9, accent);
                txt(ctx, n, w - 6 - nw, y, accent);
                y += 10;
            }
        }
    }

    // ---------------- zirh paneli ----------------
    private static void armor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int accent) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) pieces.add(st);
        }
        if (pieces.isEmpty()) return;

        int rowH = 16, w = 156, headH = 14;
        int h = headH + pieces.size() * rowH + 3;
        int x = ctx.getScaledWindowWidth() / 2 - w / 2;
        int y = 4;

        ctx.fill(x, y, x + w, y + h, 0xB0101216);
        ctx.fill(x, y, x + w, y + headH, 0xD0101216);
        ctx.fill(x, y + headH - 1, x + w, y + headH, Theme.alpha(accent, 0xB0));
        ctx.fill(x + 6, y + 5, x + 10, y + 9, accent);
        txt(ctx, "Armor", x + 14, y + 3, 0xFFFFFFFF);
        String cnt = String.valueOf(pieces.size());
        txt(ctx, cnt, x + w - 6 - tw(cnt), y + 3, 0xFF9BA0AA);

        int ry = y + headH + 1;
        for (ItemStack st : pieces) {
            ctx.drawItem(st, x + 3, ry);

            String dur;
            int color = 0xFF55FF55;
            if (st.isDamageable()) {
                int max = st.getMaxDamage();
                int left = max - st.getDamage();
                dur = left + " / " + max;
                color = durColor(max <= 0 ? 1f : (float) left / max);
            } else {
                dur = "-";
            }
            int dw = tw(dur);
            txt(ctx, dur, x + w - 5 - dw, ry + 4, color);

            String name = Fnt.trim(st.getName().getString(), 9, w - dw - 36);
            txt(ctx, name, x + 22, ry + 4, 0xFFFFFFFF);
            ry += rowH;
        }
    }

    private static int durColor(float f) {
        if (f > 0.5f) return 0xFF55FF55;
        if (f > 0.25f) return 0xFFFFD255;
        return 0xFFFF5555;
    }

    // ---------------- hedef cani ----------------
    private static void targetHud(DrawContext ctx, TextRenderer tr, int accent) {
        LivingEntity t = SpCombatClient.hudTarget;
        if (t == null || !t.isAlive() || SpCombatClient.ticks - SpCombatClient.hudTargetTick > 60) return;

        int w = 118, h = 46;
        int x = ctx.getScaledWindowWidth() / 2 + 24;
        int y = ctx.getScaledWindowHeight() / 2 + 24;

        ctx.fill(x, y, x + w, y + h, 0xB0101216);
        ctx.fill(x, y, x + 2, y + h, accent);

        String name = Fnt.trim(t.getName().getString(), 9, w - 12);
        txt(ctx, name, x + 6, y + 4, 0xFFFFFFFF);

        float hp = t.getHealth();
        float max = Math.max(1f, t.getMaxHealth());
        int bw = w - 12;
        int fill = (int) (bw * MathHelper.clamp(hp / max, 0f, 1f));
        ctx.fill(x + 6, y + 16, x + 6 + bw, y + 22, 0x80000000);
        ctx.fill(x + 6, y + 16, x + 6 + fill, y + 22, accent);
        txt(ctx, String.format("%.1f / %.1f", hp, max), x + 6, y + 25, 0xFFC8CCD4);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            txt(ctx, String.format("Mesafe: %.1f", mc.player.distanceTo(t)), x + 6, y + 35, 0xFF9BA0AA);
        }
    }
}

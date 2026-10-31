package com.example.spcombat;

import com.example.spcombat.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AltManager: takma ad (offline) listesi. Bir isme tiklayinca oyun o isimle devam eder.
 * Sadece "cracked/offline" sunucularda ise yarar (kendi Aternos sunucunda "Cracked" acik olmali).
 * Resmi (online) sunucular ve skin/hesap dogrulamasi bundan etkilenmez, orada eski hesap gecerlidir.
 */
public class AltScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;
    private String message = "";
    private long messageUntil;
    private int scrollRow;
    private boolean lastL;
    private static final int[] PALETTE = {0xFF3B82F6, 0xFFEC4899, 0xFF22D3EE, 0xFF8B5CF6,
            0xFF22C55E, 0xFFF97316, 0xFFEAB308, 0xFFEF4444};
    private static final String[] ADJ = {"Swift", "Dark", "Frozen", "Silent", "Lucky", "Crimson", "Mystic",
            "Wild", "Shadow", "Neon", "Royal", "Angry", "Cosmic", "Iron", "Golden"};
    private static final String[] NOUN = {"Wolf", "Fox", "Dragon", "Ninja", "Knight", "Panda", "Hawk",
            "Viper", "Tiger", "Ghost", "Ranger", "Pilot", "Bear", "Falcon", "Storm"};

    public AltScreen(Screen parent) {
        super(Text.literal("AltManager"));
        this.parent = parent;
    }

    /** Oyun acilinca son secilen ismi geri yukle. */
    public static void applySaved(MinecraftClient mc) {
        if (Config.currentAlt != null && valid(Config.currentAlt)) apply(mc, Config.currentAlt);
    }

    private static boolean valid(String n) {
        return n != null && n.matches("[A-Za-z0-9_]{3,16}");
    }

    private static void apply(MinecraftClient mc, String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        Session s = new Session(name, id, "", Optional.empty(), Optional.empty());
        ((MinecraftClientAccessor) mc).spcSetSession(s);
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(this.textRenderer, 0, 0, 150, 14, Text.literal("Name"));
        field.setMaxLength(16);
        this.addSelectableChild(field);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private void msg(String m) {
        message = m;
        messageUntil = System.currentTimeMillis() + 3000;
    }

    private void add(String n) {
        if (!valid(n)) {
            msg("Isim 3-16 karakter: harf, rakam veya _");
            return;
        }
        if (!Config.alts.add(n)) msg("Zaten listede: " + n);
        else msg("Eklendi: " + n);
        Config.save();
        field.setText("");
    }

    private void use(String name) {
        apply(this.client, name);
        Config.currentAlt = name;
        Config.save();
        msg("Giris: " + name);
    }

    private void remove(String name) {
        Config.alts.remove(name);
        if (name.equals(Config.currentAlt)) Config.currentAlt = "";
        Config.save();
    }

    private static String randomName() {
        java.util.Random r = new java.util.Random();
        String n = ADJ[r.nextInt(ADJ.length)] + NOUN[r.nextInt(NOUN.length)] + r.nextInt(100);
        return n.length() > 16 ? n.substring(0, 16) : n;
    }

    private static int colorOf(String name) {
        return PALETTE[Math.floorMod(name.hashCode(), PALETTE.length)];
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static final int LIST_ROWS = 7, ROW = 24;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollRow -= (int) Math.signum(vertical);
        return true;
    }

    /** Kucuk yuvarlak dugme; tiklandiysa true. */
    private boolean button(DrawContext ctx, String label, int x, int y, int w, int h, int mx, int my,
                           boolean click, int accent, boolean primary) {
        boolean over = in(mx, my, x, y, w, h);
        int col = primary ? Theme.alpha(accent, over ? 0xE0 : 0xA0) : (over ? 0x50FFFFFF : 0x28FFFFFF);
        Draw.rbox(ctx, x, y, w, h, col, 6);
        Fnt.drawCentered(ctx, label, x + w / 2f, y + (h - 6.2f) / 2f - 0.8f, 8.5f, 0xFFFFFFFF, true);
        return over && click;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int accent = Theme.cur().accent;
        int w = this.width, h = this.height;
        double tt = System.currentTimeMillis() / 1000.0;

        long win = this.client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = l && !lastL;
        lastL = l;

        // arka plan
        ctx.fillGradient(0, 0, w, h, 0xFF04070B, 0xFF0A1418);
        for (int i = 0; i < 3; i++) {
            double cx = w * (0.25 + 0.25 * i) + Math.sin(tt * 0.12 + i * 1.7) * w * 0.08;
            double cy = h * 0.5 + Math.cos(tt * 0.10 + i * 2.3) * h * 0.15;
            int rad = (int) (h * (0.28 + 0.04 * i));
            Draw.rbox(ctx, (int) cx - rad, (int) cy - rad, rad * 2, rad * 2, Theme.alpha(accent, 0x0E), rad);
        }
        super.render(ctx, mx, my, delta);

        List<String> list = new ArrayList<>(Config.alts);
        String cur = this.client.getSession().getUsername();

        int pw = 320;
        int rows = Math.min(LIST_ROWS, Math.max(1, list.size()));
        int ph = 62 + 24 + 8 + rows * ROW + 40;
        int px = (w - pw) / 2;
        int py = Math.max(8, (h - ph) / 2);

        Draw.shadow(ctx, px, py, pw, ph, 10);
        Draw.rbox(ctx, px, py, pw, ph, 0xE60D0F13, 10);

        // baslik + aktif hesap
        Fnt.drawTitle(ctx, "AltManager", px + 16, py + 10, 18, 0xFFFFFFFF);
        Draw.rbox(ctx, px + 16, py + 32, 24, 24, colorOf(cur), 7);
        Fnt.drawCentered(ctx, cur.substring(0, 1).toUpperCase(), px + 28, py + 36, 14, 0xFFFFFFFF, true);
        Fnt.draw(ctx, "Aktif hesap", px + 48, py + 33, 7.5f, 0xFF8E939C);
        Fnt.draw(ctx, Fnt.trim(cur, 10, pw - 80, true), px + 48, py + 43, 10, 0xFFFFFFFF, true);
        String tag = Config.alts.contains(cur) ? "KAYITLI" : "OFFLINE";
        int tgw = Fnt.w(tag, 7.5f) + 12;
        Draw.rbox(ctx, px + pw - tgw - 14, py + 38, tgw, 14, Theme.alpha(accent, 0x70), 7);
        Fnt.drawCentered(ctx, tag, px + pw - tgw / 2f - 14, py + 41.5f, 7.5f, 0xFFFFFFFF, true);

        // ekleme satiri
        int fy = py + 62;
        int fw = pw - 32 - 120;
        field.setX(px + 24);
        field.setY(fy + 2);
        field.setWidth(fw - 14);
        boolean foc = field.isFocused();
        Draw.rbox(ctx, px + 16, fy, fw, 18, foc ? Theme.alpha(accent, 0xC0) : 0x20FFFFFF, 7);
        Draw.rbox(ctx, px + 17, fy + 1, fw - 2, 16, 0xFF16181D, 6);
        String ft = field.getText();
        if (ft.isEmpty() && !foc) {
            Fnt.draw(ctx, "Yeni isim yaz...", px + 24, fy + 5, 8.5f, 0xFF6A6F78);
        } else {
            String sh = Fnt.trim(ft, 8.5f, fw - 16);
            Fnt.draw(ctx, sh, px + 24, fy + 5, 8.5f, 0xFFFFFFFF);
            if (foc && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cxx = px + 24 + Fnt.w(sh, 8.5f) + 1;
                ctx.fill(cxx, fy + 4, cxx + 1, fy + 14, 0xFFFFFFFF);
            }
        }
        int bx = px + 16 + fw + 4;
        if (button(ctx, "Ekle", bx, fy, 32, 18, mx, my, click, accent, true)) add(ft.trim());
        if (button(ctx, "Rastgele", bx + 34, fy, 42, 18, mx, my, click, accent, false)) { field.setText(randomName()); }
        if (button(ctx, "Yapistir", bx + 78, fy, 38, 18, mx, my, click, accent, false)) {
            String clip = GLFW.glfwGetClipboardString(win);
            if (clip != null) {
                String c2 = clip.trim().replaceAll("[^A-Za-z0-9_]", "");
                field.setText(c2.substring(0, Math.min(16, c2.length())));
            }
        }

        // liste
        int ly = fy + 26;
        int maxScroll = Math.max(0, list.size() - LIST_ROWS);
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));
        if (list.isEmpty()) {
            Fnt.drawCentered(ctx, "Henuz kayitli hesap yok", px + pw / 2f, ly + 8, 8.5f, 0xFF6A6F78, false);
        }
        String toRemove = null, toUse = null;
        for (int i = 0; i < LIST_ROWS && i + scrollRow < list.size(); i++) {
            String name = list.get(i + scrollRow);
            int ry = ly + i * ROW;
            boolean active = name.equals(cur);
            boolean over = in(mx, my, px + 12, ry, pw - 24, ROW - 2);
            Draw.rbox(ctx, px + 12, ry, pw - 24, ROW - 2, active ? Theme.alpha(accent, 0x38) : (over ? 0x24FFFFFF : 0x14FFFFFF), 7);
            Draw.rbox(ctx, px + 18, ry + 3, 16, 16, colorOf(name), 5);
            Fnt.drawCentered(ctx, name.substring(0, 1).toUpperCase(), px + 26, ry + 5.5f, 9, 0xFFFFFFFF, true);
            Fnt.draw(ctx, Fnt.trim(name, 9, 150, active), px + 42, ry + 5, 9, active ? 0xFFFFFFFF : 0xFFC8CCD4, active);
            if (button(ctx, active ? "Aktif" : "Kullan", px + pw - 92, ry + 3, 44, 16, mx, my, click, accent, !active) && !active) toUse = name;
            if (button(ctx, "Sil", px + pw - 44, ry + 3, 26, 16, mx, my, click, accent, false)) toRemove = name;
        }
        if (toUse != null) use(toUse);
        if (toRemove != null) remove(toRemove);
        if (list.size() > LIST_ROWS) {
            Fnt.draw(ctx, (scrollRow + 1) + "-" + Math.min(list.size(), scrollRow + LIST_ROWS) + " / " + list.size()
                    + "  (tekerlek)", px + 16, ly + LIST_ROWS * ROW + 2, 7.5f, 0xFF6A6F78);
        }

        // alt dugmeler
        int by = py + ph - 28;
        if (button(ctx, "Geri", px + 16, by, 60, 18, mx, my, click, accent, true)) { this.client.setScreen(parent); return; }
        if (button(ctx, "Hepsini sil", px + 82, by, 62, 18, mx, my, click, accent, false)) {
            Config.alts.clear();
            Config.currentAlt = "";
            Config.save();
            msg("Liste temizlendi");
        }
        if (!message.isEmpty() && System.currentTimeMillis() < messageUntil) {
            Fnt.drawRight(ctx, Fnt.trim(message, 8, pw - 180), px + pw - 16, by + 5, 8, accent, false);
        }
        Fnt.draw(ctx, "Sadece cracked/offline sunucularda ise yarar", px + pw / 2f - Fnt.width("Sadece cracked/offline sunucularda ise yarar", 7) / 2f,
                py + ph + 6, 7, 0xFF6A6F78);
    }
}

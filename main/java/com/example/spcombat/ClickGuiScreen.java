package com.example.spcombat;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ayar ekrani: 5 yan yana koyu panel (Combat / Movement / Render / Player / Misc),
 * altinda arama kutusu ve tema renkleri, sag altta "Configs" paneli.
 *
 *  - Modul satirina SOL TIK: ac/kapat
 *  - Modul satirina SAG TIK: ayarlari ac/kapat
 *  - Ayar satiri: sayilar slider (basili tut ve surukle), digerleri sol/sag tik
 *  - "Bind" satiri: sol tik -> tusa bas, sag tik veya Backspace -> temizle
 *  - Fare tekerlegi: sutunu kaydirir
 *  - Sag Shift / ESC: kapatir
 *
 * Fare ve tus girisi GLFW ile okunuyor (surumler arasi Screen imza degisikliklerinden etkilenmesin diye).
 */
public class ClickGuiScreen extends Screen {
    private static final int HEAD_H = 20;
    private static final int ROW_H = 16;
    private static final int SET_H = 14;
    private static final int GAP = 6;
    private static final int COLS = 5;
    private static final int VIEW_ROWS = 11;
    private static final float F = 8.5f;   // normal yazi
    private static final float FS = 7.5f;  // ayar yazisi
    private static final int CFG_W = 176;

    private static final int PANEL = 0xE60D0F13;
    private static final int BAND = 0x30FFFFFF;
    private static final int TXT_ON = 0xFFFFFFFF;
    private static final int TXT_OFF = 0xFF8E939C;

    private static final Set<Module> OPEN = new HashSet<>();
    private static final int[] scroll = new int[COLS];
    private static String searchText = "";
    private static String createText = "";

    private final int[] colX = new int[COLS];
    private int colW, headTop, bodyTop, viewH, panelH;
    private int searchX, searchY, searchW, swY, extraY;
    private int cfgX, cfgY, cfgH;

    private TextFieldWidget searchField, createField;
    private List<String> profiles = new ArrayList<>();
    private String status = "";
    private long statusUntil;

    private static final java.util.Map<String, String> DESC = java.util.Map.ofEntries(
            java.util.Map.entry("AttackAura", "Menzildeki hedefe otomatik vurur"),
            java.util.Map.entry("TriggerBot", "Nisangah hedefteyken vurur"),
            java.util.Map.entry("AutoTotem", "Totemi sol ele koyar"),
            java.util.Map.entry("AutoPotion", "Can azalinca iyilestirme iksiri atar"),
            java.util.Map.entry("AutoGApple", "Can azalinca altin elma yer"),
            java.util.Map.entry("ClickPearl", "Tusa basinca ender incisi atar"),
            java.util.Map.entry("AutoSprint", "Ileri giderken otomatik kosar"),
            java.util.Map.entry("Speed", "Yerde hizi artirir"),
            java.util.Map.entry("ElytraTarget", "Ucarken en yakin oyuncuya doner"),
            java.util.Map.entry("FireworkKey", "Bind tusuna basinca fisek atar"),
            java.util.Map.entry("ElytraBooster", "Hiz dusunce fisek atar"),
            java.util.Map.entry("AirStuck", "Havada dondurur / asili tutar"),
            java.util.Map.entry("Velocity", "Geri tepmeyi azaltir"),
            java.util.Map.entry("GuiMove", "Envanter acikken yurur"),
            java.util.Map.entry("FullBright", "Her yer aydinlik"),
            java.util.Map.entry("Ambience", "Gokyuzune yumusak renk tonu"),
            java.util.Map.entry("ClientSound", "Vurunca ses calar"),
            java.util.Map.entry("HUD", "Yazi, koordinat, FPS"),
            java.util.Map.entry("ArmorHUD", "Zirh dayaniklilik paneli"),
            java.util.Map.entry("TargetHUD", "Hedefin cani ve mesafesi"),
            java.util.Map.entry("TargetESP", "Hedefin etrafinda halka / parlama"),
            java.util.Map.entry("Wings", "Karaktere elytra kanatlari cizer"),
            java.util.Map.entry("SwingAnimation", "El sallamayi yavas ve akici yapar"),
            java.util.Map.entry("Birtanem", "Keybinds altinda sevgi yazisi"),
            java.util.Map.entry("ESP", "Oyunculari kutu / parlama ile gosterir"),
            java.util.Map.entry("NameTags", "Oyuncu ismi ve cani"),
            java.util.Map.entry("Tracers", "Oyunculara cizgi cizer"),
            java.util.Map.entry("ItemESP", "Yerdeki esyalari parlatir"),
            java.util.Map.entry("HitParticles", "Vurunca parcacik cikar"),
            java.util.Map.entry("JumpCircle", "Ziplayinca halka efekti"),
            java.util.Map.entry("KillEffect", "Oldurunce efekt"),
            java.util.Map.entry("AutoEat", "Acikinca otomatik yemek yer"),
            java.util.Map.entry("FastUse", "Kullanma bekleme suresini kaldirir"),
            java.util.Map.entry("NoJumpDelay", "Ziplama gecikmesi yok"),
            java.util.Map.entry("Flight", "Ucma"),
            java.util.Map.entry("Step", "Bloklara tirmanir"),
            java.util.Map.entry("NoFall", "Dusme hasarini engeller"),
            java.util.Map.entry("Jesus", "Suda yurur"),
            java.util.Map.entry("Parkour", "Kenarda otomatik zipla"),
            java.util.Map.entry("SafeWalk", "Kenardan dusmez"),
            java.util.Map.entry("Spider", "Duvara tirmanir"),
            java.util.Map.entry("ElytraRecast", "Dusunce elytrayi tekrar acar"),
            java.util.Map.entry("VisualRange", "Oyuncu yaklasinca haber verir"),
            java.util.Map.entry("AutoEZ", "Oldurunce sohbete mesaj yazar"),
            java.util.Map.entry("ViewModel", "Eldeki esyanin konum / boyut / aci"),
            java.util.Map.entry("BlockOverlay", "Baktigin blogu cerceveler"),
            java.util.Map.entry("FireFly", "Etrafinda isik parcaciklari"),
            java.util.Map.entry("Trails", "Arkanda parcacik izi birakir"),
            java.util.Map.entry("FastExp", "Tecrube sisesini hizli atar"),
            java.util.Map.entry("TapeMouse", "Otomatik tiklar"),
            java.util.Map.entry("ElytraTakeoff", "Elytra ile otomatik kalkis + fisek"),
            java.util.Map.entry("Panic", "Bind tusuna basinca tum modulleri kapatir"),
            java.util.Map.entry("AutoLeave", "Can azalinca / oyuncu yaklasinca cikar"),
            java.util.Map.entry("AutoTpaccept", "TPA isteklerini otomatik kabul eder"),
            java.util.Map.entry("DeathCoords", "Olunce koordinati yazar"),
            java.util.Map.entry("AntiAFK", "AFK atilmamak icin ziplar"),
            java.util.Map.entry("Keybinds", "Acik modullerin listesi"),
            java.util.Map.entry("Photo", "GUI altinda resimler"),
            java.util.Map.entry("AutoTool", "Blok icin en hizli aleti secer"),
            java.util.Map.entry("FastBreak", "Blok kirmayi hizlandirir"),
            java.util.Map.entry("AutoArmor", "Daha iyi zirhi giyer"),
            java.util.Map.entry("ChestStealer", "Sandiktaki esyalari alir"),
            java.util.Map.entry("Scaffold", "Ayagin altina blok koyar"),
            java.util.Map.entry("AutoRespawn", "Olunce otomatik dogar"));
    private Module hoverMod;
    private int hoverTicks;
    private Module lastHover;

    private final long openMs = System.currentTimeMillis();
    private Module binding;
    private Setting dragging;
    private int dragX, dragW;
    private boolean lastL, lastR, lastShift = true, shiftReleased;

    public ClickGuiScreen() {
        super(Text.literal("Menemen Client"));
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(this.textRenderer, 0, 0, 120, 14, Text.literal("Search"));
        searchField.setMaxLength(20);
        searchField.setText(searchText);
        searchField.setChangedListener(t -> searchText = t);
        this.addSelectableChild(searchField);

        createField = new TextFieldWidget(this.textRenderer, 0, 0, 100, 14, Text.literal("Config"));
        createField.setMaxLength(16);
        createField.setText(createText);
        createField.setChangedListener(t -> createText = t);
        this.addSelectableChild(createField);

        profiles = Config.listProfiles();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ESC tus atama modunda iptal icin kullanilir, ekrani kapatmasin
    public boolean shouldCloseOnEsc() {
        return binding == null;
    }

    // Ekran kapanirken ayarlari kaydet
    public void removed() {
        Config.save();
    }

    // Fare tekerlegi: imza 1.20.2 - 1.21.x arasinda ayni
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int c = 0; c < COLS; c++) {
            if (mouseX >= colX[c] && mouseX < colX[c] + colW
                    && mouseY >= bodyTop && mouseY < bodyTop + viewH) {
                scroll[c] -= (int) (vertical * ROW_H * 1.5);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        float pr0 = Math.min(1f, (System.currentTimeMillis() - openMs) / 220f);
        ctx.fill(0, 0, this.width, this.height, ((int) (0x66 * pr0) << 24));

        long win = this.client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean clickL = l && !lastL;
        boolean clickR = r && !lastR;
        lastL = l;
        lastR = r;

        // Sag Shift tekrar basilinca kapat (acarken basili tutulan tusu saymaz)
        boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shift) shiftReleased = true;
        if (shift && shiftReleased && !lastShift && binding == null) {
            this.close();
            return;
        }
        lastShift = shift;

        if (binding != null) captureBind(win);

        layout();

        // slider surukleme
        if (dragging != null) {
            if (l) dragging.setFraction((mx - dragX) / (double) dragW);
            else dragging = null;
        }

        int accent = Theme.cur().accent;
        Module.Category[] cats = Module.Category.values();
        for (int c = 0; c < COLS; c++) {
            drawCategory(ctx, c, cats[c], mx, my, clickL, clickR, accent);
        }
        drawSearch(ctx, accent);
        drawSwatches(ctx, mx, my, clickL, accent);
        drawServerButton(ctx, mx, my, clickL, accent);
        drawPhotos(ctx, accent);
        drawConfigs(ctx, mx, my, clickL, accent);

        // sol altta marka yazisi
        Fnt.draw(ctx, "Menemen", 10, this.height - 24, 14, 0xCCFFFFFF, true);
        Fnt.draw(ctx, "DLC", 10 + Fnt.width("Menemen ", 14, true), this.height - 24, 14, accent, true);

        // ipucu: bir modulun ustunde ~0.4 sn durunca kisa aciklama
        if (hoverMod != null && hoverMod == lastHover) hoverTicks++; else hoverTicks = 0;
        lastHover = hoverMod;
        String tip = hoverMod == null ? null : DESC.get(hoverMod.name);
        if (tip != null && hoverTicks > 8) {
            int tw = Fnt.w(tip, F) + 14;
            int tx = Math.min(mx + 12, this.width - tw - 4);
            int ty = my + 14;
            Draw.shadow(ctx, tx, ty, tw, 15, 6);
            Draw.rbox(ctx, tx - 1, ty - 1, tw + 2, 17, Theme.alpha(accent, 0xB0), 6);
            Draw.rbox(ctx, tx, ty, tw, 15, 0xF2101216, 6);
            Fnt.draw(ctx, tip, tx + 7, tyc(ty, 15, F), F, 0xFFE6E8EC);
        }
        hoverMod = null;
    }

    /** Yaziyi satirin ortasina oturtmak icin y. */
    private static float tyc(int rowY, int rowH, float size) {
        return rowY + (rowH - 0.727f * size) / 2f - 0.09f * size;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------- arama kutusu ----------------
    private void drawSearch(DrawContext ctx, int accent) {
        searchField.setX(searchX + 20);
        searchField.setY(searchY + 1);
        searchField.setWidth(searchW - 26);

        boolean foc = searchField.isFocused();
        Draw.shadow(ctx, searchX, searchY, searchW, 16, 8);
        Draw.rbox(ctx, searchX, searchY, searchW, 16, foc ? Theme.alpha(accent, 0xC0) : 0x00000000, 8);
        Draw.rbox(ctx, searchX + 1, searchY + 1, searchW - 2, 14, 0xE60D0F13, 7);
        // buyutec simgesi
        Draw.rbox(ctx, searchX + 8, searchY + 4, 7, 7, 0xFF8E939C, 3);
        Draw.rbox(ctx, searchX + 9, searchY + 5, 5, 5, 0xFF0D0F13, 2);
        String t = searchText;
        if (t.isEmpty() && !foc) {
            Fnt.draw(ctx, "Search...", searchX + 21, tyc(searchY, 16, F), F, 0xFF6A6F78);
        } else {
            String shown = Fnt.trim(t, F, searchW - 30);
            Fnt.draw(ctx, shown, searchX + 21, tyc(searchY, 16, F), F, 0xFFFFFFFF);
            if (foc && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = searchX + 21 + Fnt.w(shown, F) + 1;
                ctx.fill(cx, searchY + 4, cx + 1, searchY + 12, 0xFFFFFFFF);
            }
        }
    }

    // ---------------- tema renkleri ----------------
    private void drawSwatches(DrawContext ctx, int mx, int my, boolean clickL, int accent) {
        int n = Theme.ALL.length, s = 12, g = 6;
        int total = n * s + (n - 1) * g;
        int x = (this.width - total) / 2;
        String tip = null;
        for (int i = 0; i < n; i++) {
            Theme t = Theme.ALL[i];
            int sx = x + i * (s + g);
            boolean over = in(mx, my, sx - 1, swY - 1, s + 2, s + 2);
            if (i == Theme.index()) Draw.rbox(ctx, sx - 2, swY - 2, s + 4, s + 4, 0xFFFFFFFF, 6);
            else if (over) Draw.rbox(ctx, sx - 2, swY - 2, s + 4, s + 4, 0x90FFFFFF, 6);
            Draw.rbox(ctx, sx, swY, s, s, t.accent, 5);
            if (over) {
                tip = t.name;
                if (clickL) Theme.set(i);
            }
        }
        if (tip != null) {
            int tw = Fnt.w(tip, FS) + 12;
            int tx = MathHelper.clamp(mx - tw / 2, 4, this.width - tw - 4);
            int ty = swY + 18;
            Draw.rbox(ctx, tx, ty, tw, 13, 0xF2101216, 6);
            Fnt.draw(ctx, tip, tx + 6, tyc(ty, 13, FS), FS, 0xFFE6E8EC);
        }
    }

    // ---------------- Configs paneli ----------------
    private void setStatus(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    private void drawConfigs(DrawContext ctx, int mx, int my, boolean clickL, int accent) {
        int rowH = 18;
        int shownRows = Math.min(profiles.size(), 6);
        cfgH = 22 + 24 + shownRows * rowH + 14;
        cfgX = this.width - CFG_W - 8;
        cfgY = this.height - cfgH - 8;

        Draw.shadow(ctx, cfgX, cfgY, CFG_W, cfgH, 8);
        Draw.rbox(ctx, cfgX, cfgY, CFG_W, cfgH, PANEL, 8);
        Draw.rbox(ctx, cfgX + 8, cfgY + 8, 6, 6, accent, 3);
        Fnt.draw(ctx, "Configs", cfgX + 19, tyc(cfgY, 22, 9.5f), 9.5f, 0xFFFFFFFF, true);

        // olustur satiri
        int fy = cfgY + 22;
        int fw = CFG_W - 16 - 26;
        createField.setX(cfgX + 14);
        createField.setY(fy + 2);
        createField.setWidth(fw - 12);
        boolean foc = createField.isFocused();
        Draw.rbox(ctx, cfgX + 8, fy, fw, 16, foc ? Theme.alpha(accent, 0xC0) : 0x20FFFFFF, 7);
        Draw.rbox(ctx, cfgX + 9, fy + 1, fw - 2, 14, 0xFF16181D, 6);
        if (createText.isEmpty() && !foc) {
            Fnt.draw(ctx, "Create config...", cfgX + 14, tyc(fy, 16, FS), FS, 0xFF6A6F78);
        } else {
            String sh = Fnt.trim(createText, FS, fw - 14);
            Fnt.draw(ctx, sh, cfgX + 14, tyc(fy, 16, FS), FS, 0xFFFFFFFF);
            if (foc && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = cfgX + 14 + Fnt.w(sh, FS) + 1;
                ctx.fill(cx, fy + 4, cx + 1, fy + 12, 0xFFFFFFFF);
            }
        }
        int px = cfgX + 8 + fw + 4, pw = 18;
        boolean pover = in(mx, my, px, fy, pw, 16);
        Draw.rbox(ctx, px, fy, pw, 16, pover ? Theme.alpha(accent, 0xD0) : Theme.alpha(accent, 0x90), 7);
        Fnt.drawCentered(ctx, "+", px + pw / 2f, tyc(fy, 16, 10), 10, 0xFFFFFFFF, true);
        if (pover && clickL) {
            if (Config.saveProfile(createText)) {
                setStatus("Saved: " + Config.sanitize(createText));
                createText = "";
                createField.setText("");
                profiles = Config.listProfiles();
            } else {
                setStatus("Write a name first");
            }
        }

        // satirlar
        int ry = fy + 22;
        for (int i = 0; i < shownRows; i++) {
            String name = profiles.get(i);
            boolean isDef = name.equals("default");
            int rx = cfgX + 8, rw = CFG_W - 16;
            Draw.rbox(ctx, rx, ry, rw, rowH - 2, 0x20FFFFFF, 6);

            String[] btn = isDef ? new String[]{"Reset", "Save", "Load"} : new String[]{"Reset", "Save", "Load", "x"};
            int bx = rx + rw - 4;
            int[] bxs = new int[btn.length];
            int[] bws = new int[btn.length];
            for (int b = btn.length - 1; b >= 0; b--) {
                int bw = Fnt.w(btn[b], FS) + 8;
                bx -= bw;
                bxs[b] = bx;
                bws[b] = bw;
                bx -= 2;
            }
            for (int b = 0; b < btn.length; b++) {
                boolean over = in(mx, my, bxs[b], ry + 2, bws[b], rowH - 6);
                int col = over ? 0x50FFFFFF : 0x28FFFFFF;
                if (btn[b].equals("Load") && over) col = Theme.alpha(accent, 0xA0);
                Draw.rbox(ctx, bxs[b], ry + 2, bws[b], rowH - 6, col, 5);
                Fnt.drawCentered(ctx, btn[b], bxs[b] + bws[b] / 2f, tyc(ry + 2, rowH - 6, FS), FS, 0xFFFFFFFF, false);
                if (over && clickL) {
                    switch (btn[b]) {
                        case "Reset" -> { Config.resetDefaults(); setStatus("Reset to defaults"); }
                        case "Save" -> { Config.saveProfile(name); setStatus("Saved: " + name); profiles = Config.listProfiles(); }
                        case "Load" -> setStatus(Config.loadProfile(name) ? "Loaded: " + name : "Not saved yet: " + name);
                        default -> { Config.deleteProfile(name); setStatus("Deleted: " + name); profiles = Config.listProfiles(); }
                    }
                }
            }
            Fnt.draw(ctx, Fnt.trim(name, F, bxs[0] - rx - 12), rx + 7, tyc(ry, rowH - 2, F), F, 0xFFE6E8EC);
            ry += rowH;
        }

        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            Fnt.draw(ctx, status, cfgX + 10, cfgY + cfgH - 13, FS, accent);
        }
    }

    // Sunucudayken: modullerin bu sunucuda calismasina izin ver / verme dugmesi.
    // Tek oyunculuda gorunmez (orada zaten her zaman calisir).
    private void drawServerButton(DrawContext ctx, int mx, int my, boolean clickL, int accent) {
        String server = Access.currentServer(this.client);
        if (server == null) return;

        boolean allowed = Config.servers.contains(server);
        String label = "Bu sunucuda izin: " + (allowed ? "ACIK" : "KAPALI") + "   (" + server + ")";
        int w = Math.min(this.width - 20, Fnt.w(label, F) + 20);
        int h = 16;
        int x = (this.width - w) / 2;
        int y = extraY;

        boolean over = in(mx, my, x, y, w, h);
        Draw.rbox(ctx, x, y, w, h, allowed ? Theme.alpha(accent, 0xB0) : 0xE60D0F13, 7);
        if (over) Draw.rbox(ctx, x, y, w, h, 0x20FFFFFF, 7);
        Fnt.draw(ctx, Fnt.trim(label, F, w - 14), x + 8, tyc(y, h, F), F, 0xFFFFFFFF);

        if (over && clickL) Access.toggleCurrent(this.client);
    }

    // Photo modulu acikken sutunlarin altinda resimler gorunur
    private static final Identifier[] PHOTO_IDS = {
            Identifier.of("spcombat", "textures/gui/photo0.png"),
            Identifier.of("spcombat", "textures/gui/photo1.png"),
            Identifier.of("spcombat", "textures/gui/photo2.png"),
            Identifier.of("spcombat", "textures/gui/photo3.png")};
    private static final int[] PHOTO_W = {161, 199, 192, 228};
    private static final int PHOTO_TEX_H = 192;

    private void drawPhotos(DrawContext ctx, int accent) {
        if (!Modules.PHOTO.enabled) return;
        int h = (int) Modules.PHOTO.num("Size");
        int gap = 6;
        int[] w = new int[PHOTO_IDS.length];
        int total = gap * (PHOTO_IDS.length - 1);
        for (int i = 0; i < w.length; i++) {
            w[i] = PHOTO_W[i] * h / PHOTO_TEX_H;
            total += w[i];
        }
        int x = (this.width - total) / 2;
        int y = extraY + (Access.currentServer(this.client) != null ? 24 : 0);
        for (int i = 0; i < w.length; i++) {
            Draw.rbox(ctx, x - 2, y - 2, w[i] + 4, h + 4, Theme.alpha(accent, 0xB0), 6);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, PHOTO_IDS[i], x, y, 0f, 0f,
                    w[i], h, PHOTO_W[i], PHOTO_TEX_H, PHOTO_W[i], PHOTO_TEX_H);
            x += w[i] + gap;
        }
    }

    private void layout() {
        colW = MathHelper.clamp((this.width - 40 - GAP * (COLS - 1)) / COLS, 88, 112);
        int totalW = colW * COLS + GAP * (COLS - 1);
        int x0 = (this.width - totalW) / 2;
        for (int c = 0; c < COLS; c++) colX[c] = x0 + c * (colW + GAP);

        int extras = 0;
        if (Access.currentServer(this.client) != null) extras += 24;
        if (Modules.PHOTO.enabled) extras += (int) Modules.PHOTO.num("Size") + 8;
        int below = 8 + 16 + 8 + 12 + 8; // arama + renkler
        int avail = this.height - 16 - HEAD_H - 4 - below - extras;
        viewH = MathHelper.clamp(ROW_H * VIEW_ROWS + 4, 60, Math.max(60, avail));
        panelH = HEAD_H + viewH + 4;
        int total = panelH + below + extras;
        headTop = Math.max(8, (this.height - total) / 2);
        // acilista paneller asagidan yumusakca yukari kayar
        float pr = Math.min(1f, (System.currentTimeMillis() - openMs) / 220f);
        float ease = 1f - (1f - pr) * (1f - pr) * (1f - pr);
        headTop += (int) ((1f - ease) * 18f);
        bodyTop = headTop + HEAD_H;

        searchW = 150;
        searchX = (this.width - searchW) / 2;
        searchY = headTop + panelH + 8;
        swY = searchY + 16 + 8;
        extraY = swY + 12 + 8;
    }

    // ---------------- Tus atama ----------------
    private void captureBind(long win) {
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            binding = null;
            return;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS) {
            binding.bind = -1;
            binding = null;
            return;
        }
        for (int[] range : Keys.RANGES) {
            for (int k = range[0]; k <= range[1]; k++) {
                if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE
                        || k == GLFW.GLFW_KEY_DELETE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
                if (GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS) {
                    binding.bind = k;
                    binding.bindDown = true; // hemen tetiklenmesin
                    binding = null;
                    return;
                }
            }
        }
    }

    // ---------------- Kategori sutunu ----------------
    private List<Module> filtered(Module.Category cat) {
        String q = searchText.trim().toLowerCase();
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.byCategory(cat)) {
            if (q.isEmpty() || m.name.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    private void drawCategory(DrawContext ctx, int c, Module.Category cat, int mx, int my,
                              boolean clickL, boolean clickR, int accent) {
        List<Module> mods = filtered(cat);
        int x = colX[c];

        // panel + baslik
        Draw.shadow(ctx, x, headTop, colW, panelH, 8);
        Draw.rbox(ctx, x, headTop, colW, panelH, PANEL, 8);
        Draw.rbox(ctx, x + 7, headTop + 6, 8, 8, accent, 4);
        Fnt.drawCentered(ctx, cat.title, x + colW / 2f, tyc(headTop, HEAD_H, 9.5f), 9.5f, 0xFFFFFFFF, true);
        ctx.fill(x + 8, headTop + HEAD_H - 1, x + colW - 8, headTop + HEAD_H, 0x18FFFFFF);

        ctx.enableScissor(x, bodyTop, x + colW, bodyTop + viewH);

        boolean hoverCol = in(mx, my, x, bodyTop, colW, viewH);
        int y = bodyTop + 2 - scroll[c];

        for (Module m : mods) {
            boolean open = OPEN.contains(m);
            boolean over = hoverCol && my >= y && my < y + ROW_H;

            if (m.enabled) Draw.rbox(ctx, x + 4, y + 1, colW - 8, ROW_H - 2, BAND, 5);
            else if (over) Draw.rbox(ctx, x + 4, y + 1, colW - 8, ROW_H - 2, 0x14FFFFFF, 5);

            int right = x + colW - 10;
            if (!m.settings.isEmpty() || true) {
                String marker = open ? "-" : "+";
                Fnt.drawRight(ctx, marker, right, tyc(y, ROW_H, F), F, 0xFF6A6F78, false);
                right -= Fnt.w(marker, F) + 5;
            }
            int labelMax = right - (x + 12);
            if (m.bind >= 0) {
                String bn = Keys.name(m.bind).toUpperCase();
                if (bn.length() > 6) bn = bn.substring(0, 6);
                int bw = Fnt.w(bn, FS);
                Draw.rbox(ctx, right - bw - 6, y + 3, bw + 6, ROW_H - 6, 0x50000000, 4);
                Fnt.draw(ctx, bn, right - bw - 3, tyc(y + 3, ROW_H - 6, FS), FS, 0xFFB4B8C0);
                labelMax -= bw + 10;
            }
            Fnt.draw(ctx, Fnt.trim(m.name, F, Math.max(20, labelMax), m.enabled), x + 12, tyc(y, ROW_H, F), F,
                    m.enabled ? TXT_ON : TXT_OFF, m.enabled);

            if (over) hoverMod = m;
            if (over && clickL) m.enabled = !m.enabled;
            if (over && clickR) {
                if (!OPEN.remove(m)) OPEN.add(m);
            }
            y += ROW_H;

            if (!open) continue;

            int blockH = (m.settings.size() + 1) * SET_H + 4;
            Draw.rbox(ctx, x + 4, y, colW - 8, blockH, 0x40000000, 5);
            y += 2;

            // Bind satiri
            boolean bo = hoverCol && my >= y && my < y + SET_H;
            if (bo) Draw.rbox(ctx, x + 6, y, colW - 12, SET_H, 0x14FFFFFF, 4);
            Fnt.draw(ctx, "Bind", x + 11, tyc(y, SET_H, FS), FS, 0xFFC8CCD4);
            String bv = binding == m ? "..." : (m.bind < 0 ? "None" : Keys.name(m.bind));
            Fnt.drawRight(ctx, bv, x + colW - 11, tyc(y, SET_H, FS), FS, binding == m ? 0xFFFFD255 : accent, false);
            if (bo && clickL) binding = (binding == m) ? null : m;
            if (bo && clickR) { m.bind = -1; if (binding == m) binding = null; }
            y += SET_H;

            // Ayar satirlari
            for (Setting s : m.settings) {
                boolean so = hoverCol && my >= y && my < y + SET_H;
                int sx = x + 7, sw = colW - 14;

                if (s.type == Setting.Type.NUM) {
                    Draw.rbox(ctx, sx, y + 1, sw, SET_H - 2, 0x28FFFFFF, 4);
                    int fw = (int) (sw * s.fraction());
                    if (fw > 0) Draw.rbox(ctx, sx, y + 1, Math.max(8, fw), SET_H - 2, Theme.alpha(accent, 0x80), 4);
                    if (so && clickL) {
                        dragging = s;
                        dragX = sx;
                        dragW = sw;
                        s.setFraction((mx - sx) / (double) sw);
                    }
                } else {
                    if (so) Draw.rbox(ctx, sx, y + 1, sw, SET_H - 2, 0x14FFFFFF, 4);
                    if (so && clickL) s.click(true);
                    if (so && clickR) s.click(false);
                }

                String v = s.display();
                int vw = Fnt.w(v, FS);
                int vColor = s.type == Setting.Type.BOOL ? (s.bool ? accent : 0xFF8A8F99) : 0xFFFFFFFF;
                Fnt.draw(ctx, Fnt.trim(s.name, FS, Math.max(16, sw - vw - 12)), sx + 4, tyc(y, SET_H, FS), FS, 0xFFC8CCD4);
                Fnt.draw(ctx, v, sx + sw - 4 - vw, tyc(y, SET_H, FS), FS, vColor);
                y += SET_H;
            }
            y += 2;
        }

        ctx.disableScissor();
        finishColumn(ctx, c, x, y);
    }

    private void finishColumn(DrawContext ctx, int c, int x, int yEnd) {
        int contentH = (yEnd + scroll[c]) - bodyTop + 2;
        int max = Math.max(0, contentH - viewH);
        scroll[c] = MathHelper.clamp(scroll[c], 0, max);

        if (max > 0) {
            int barH = Math.max(14, (int) ((long) viewH * viewH / contentH));
            int barY = bodyTop + (int) ((long) (viewH - barH) * scroll[c] / max);
            Draw.rbox(ctx, x + colW - 4, barY, 2, barH, Theme.alpha(Theme.cur().accent, 0xB0), 1);
        }
    }
}

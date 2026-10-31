package com.example.spcombat;

import java.util.List;

public class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), MISC("Misc");

        public final String title;

        Category(String title) { this.title = title; }
    }

    public final String name;
    public final Category category;
    public final List<Setting> settings;
    public boolean enabled;

    /** Gorsel/ses modulu: tek oyunculu kisiti disinda her yerde calisir. */
    public boolean cosmetic;
    /** Tus modulu ac/kapat yapmaz, modulun kendi eylemini tetikler (ON = hazir). */
    public boolean actionKey;

    /** GUI'den atanan tus (GLFW kodu), -1 = atanmamis. */
    public int bind = -1;
    /** Atanan tus su an basili mi (hold-to-repeat ve kenar algilama icin). */
    public boolean bindDown;
    private boolean triggered;

    public Module(String name, Category category, Setting... settings) {
        this.name = name;
        this.category = category;
        this.settings = List.of(settings);
    }

    /** Varsayilan olarak acik baslat. */
    public Module on() { this.enabled = true; return this; }

    public Module cosmetic() { this.cosmetic = true; return this; }

    public Module action() { this.actionKey = true; return this; }

    public void trigger() { triggered = true; }

    /** Tetiklendiyse true dondurur ve bayragi temizler. */
    public boolean consume() {
        boolean t = triggered;
        triggered = false;
        return t;
    }

    private Setting find(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s;
        throw new IllegalArgumentException("Ayar yok: " + n);
    }

    public double num(String n) { return find(n).num; }
    public boolean bool(String n) { return find(n).bool; }
    public int index(String n) { return find(n).index; }
}

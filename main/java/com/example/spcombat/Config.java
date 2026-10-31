package com.example.spcombat;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Ayarlari .minecraft/config/spcombat.properties dosyasina kaydeder/yukler.
 * Ayrica adlandirilmis ayar profilleri (config/spcombat-profiles/ad.properties) vardir:
 * GUI'nin sag alt kosesindeki "Configs" panelinden olusturulur / kaydedilir / yuklenir.
 */
public final class Config {
    private Config() {}

    /** Modullerin calismasina izin verilen sunucu adresleri (kucuk harf). */
    public static final Set<String> servers = new LinkedHashSet<>();

    /** Kayitli takma adlar (AltManager) ve son secilen. */
    public static final Set<String> alts = new LinkedHashSet<>();
    public static String currentAlt = "";

    private static Properties defaults;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spcombat.properties");
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("spcombat-profiles");
    }

    private static String clean(String s) {
        return s.replaceAll("[^A-Za-z0-9]+", "_");
    }

    private static String key(Module m, String suffix) {
        return clean(m.name) + "." + clean(suffix);
    }

    /** Fabrika ayarlarini (kayit yuklenmeden once) hatirlar. */
    public static void captureDefaults() {
        defaults = toProps();
    }

    /** Tum modulleri ilk haline dondurur (tus atamalari ve tema dahil degil). */
    public static void resetDefaults() {
        if (defaults == null) return;
        apply(defaults, false);
        for (Module m : Modules.ALL) m.bind = -1;
    }

    private static Properties toProps() {
        Properties pr = new Properties();
        for (Module m : Modules.ALL) {
            pr.setProperty(key(m, "enabled"), String.valueOf(m.enabled));
            pr.setProperty(key(m, "bind"), String.valueOf(m.bind));
            for (Setting s : m.settings) {
                pr.setProperty(key(m, "s_" + s.name), s.serialize());
            }
        }
        pr.setProperty("theme", String.valueOf(Theme.index()));
        return pr;
    }

    private static void apply(Properties pr, boolean migrate) {
        for (Module m : Modules.ALL) {
            String en = pr.getProperty(key(m, "enabled"));
            if (en != null) m.enabled = Boolean.parseBoolean(en);

            String b = pr.getProperty(key(m, "bind"));
            if (b != null) {
                try {
                    int k = Integer.parseInt(b);
                    m.bind = (k >= 0 && Keys.valid(k)) ? k : -1;
                } catch (NumberFormatException ignored) {
                }
            }

            for (Setting s : m.settings) {
                String v = pr.getProperty(key(m, "s_" + s.name));
                if (v != null) s.deserialize(v);
            }
        }

        if (migrate) {
            // Eski ayar dosyalarinda aura "Rotate" Smooth kalmis olabilir: bir kereligine Silent'a al
            int ver = 1;
            try { ver = Integer.parseInt(pr.getProperty("cfgVersion", "1")); } catch (NumberFormatException ignored) {}
            if (ver < 4) {
                for (Setting st : Modules.AURA.settings) {
                    if (st.name.equals("Players")) st.bool = true;
                    if (st.name.equals("Auto jump")) st.bool = true;
                }
            }
            if (ver < 3) {
                for (Setting st : Modules.AURA.settings) {
                    if (st.name.equals("Rotate")) st.index = Rotations.SILENT;
                    if (st.name.equals("Range")) st.num = 3.4;
                    if (st.name.equals("Switch delay (tick)")) st.num = 2;
                    if (st.name.equals("Crit jump")) st.bool = true;
                }
            } else if (ver < 5) {
                // Eski liste "Off/Snap/Smooth/Silent" idi; yeni Celestial listesine tasi.
                String rv = pr.getProperty(key(Modules.AURA, "s_Rotate"));
                if (rv != null) {
                    try {
                        int old = Integer.parseInt(rv.trim());
                        int idx = old == 1 ? Rotations.SNAP
                                : old == 2 ? Rotations.SPOOKYTIME
                                : old == 3 ? Rotations.SILENT
                                : Rotations.OFF;
                        for (Setting st : Modules.AURA.settings) {
                            if (st.name.equals("Rotate")) st.index = idx;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String t = pr.getProperty("theme");
        if (t != null) {
            try {
                Theme.set(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static Properties read(Path f) {
        if (!Files.exists(f)) return null;
        Properties pr = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            pr.load(in);
        } catch (IOException e) {
            return null;
        }
        return pr;
    }

    private static void write(Path f, Properties pr) {
        try {
            Files.createDirectories(f.getParent());
            try (OutputStream out = Files.newOutputStream(f)) {
                pr.store(out, "Menemen Client");
            }
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        Properties pr = read(file());
        if (pr == null) return;
        apply(pr, true);

        String sv = pr.getProperty("servers");
        if (sv != null) {
            for (String part : sv.split(",")) {
                String a = part.trim().toLowerCase();
                if (!a.isEmpty()) servers.add(a);
            }
        }

        String av = pr.getProperty("alts");
        if (av != null) {
            for (String part : av.split(",")) {
                String a = part.trim();
                if (!a.isEmpty()) alts.add(a);
            }
        }
        currentAlt = pr.getProperty("altCurrent", "");
    }

    public static void save() {
        Properties pr = toProps();
        pr.setProperty("cfgVersion", "5");
        pr.setProperty("servers", String.join(",", servers));
        pr.setProperty("alts", String.join(",", alts));
        pr.setProperty("altCurrent", currentAlt == null ? "" : currentAlt);
        write(file(), pr);
    }

    // ---------------- profiller ----------------
    public static String sanitize(String n) {
        String s = n == null ? "" : n.trim().replaceAll("[^A-Za-z0-9_-]", "");
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        out.add("default");
        try (var st = Files.list(profilesDir())) {
            st.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".properties"))
                    .map(n -> n.substring(0, n.length() - ".properties".length()))
                    .filter(n -> !n.equals("default"))
                    .sorted()
                    .forEach(out::add);
        } catch (IOException ignored) {
        }
        return out;
    }

    public static boolean saveProfile(String name) {
        String n = sanitize(name);
        if (n.isEmpty()) return false;
        write(profilesDir().resolve(n + ".properties"), toProps());
        return true;
    }

    public static boolean loadProfile(String name) {
        Properties pr = read(profilesDir().resolve(sanitize(name) + ".properties"));
        if (pr == null) return false;
        apply(pr, false);
        return true;
    }

    public static void deleteProfile(String name) {
        try {
            Files.deleteIfExists(profilesDir().resolve(sanitize(name) + ".properties"));
        } catch (IOException ignored) {
        }
    }
}

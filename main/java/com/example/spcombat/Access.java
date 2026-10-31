package com.example.spcombat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

/**
 * Modullerin nerede calisabilecegi: tek oyunculu dunyalar her zaman,
 * sunucular ise sadece GUI'den "Bu sunucuda izin: ACIK" yaptigin adresler.
 */
public final class Access {
    private Access() {}

    public static boolean allowed(MinecraftClient mc) {
        if (mc.isInSingleplayer()) return true;
        String s = currentServer(mc);
        return s != null && Config.servers.contains(s);
    }

    /** Bagli oldugun sunucunun adresi (kucuk harf), tek oyunculuda veya bagli degilken null. */
    public static String currentServer(MinecraftClient mc) {
        if (mc == null || mc.isInSingleplayer()) return null;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return null;
        return info.address.trim().toLowerCase(Locale.ROOT);
    }

    public static void toggleCurrent(MinecraftClient mc) {
        String s = currentServer(mc);
        if (s == null) return;
        if (!Config.servers.remove(s)) Config.servers.add(s);
        Config.save();
    }
}

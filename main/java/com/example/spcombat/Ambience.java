package com.example.spcombat;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Ambience: gokyuzu gorunurken ekranin ust kismina yumusak, yavasca renk degistiren
 * bir gradyan tonu ekler (gunbatimi / alacakaranlik / pastel havasi).
 *
 * Bu bir ekran filtresidir: gercek gokyuzu cizimine dokunmaz, tek bir
 * yari saydam dikdortgen cizdigi icin FPS'e etkisi yok denecek kadar azdir.
 * Yukari baktikca daha belirgin olur, yer altinda/binada otomatik solar.
 */
public final class Ambience {
    private Ambience() {}

    private static float fade = 0f;

    public static void init() {
        HudRenderCallback.EVENT.register(Ambience::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) { fade = 0f; return; }

        Module m = Modules.AMBIENCE;
        boolean skyVisible = m.enabled
                && mc.world.getRegistryKey() == World.OVERWORLD
                && mc.world.isSkyVisible(mc.player.getBlockPos());

        // yumusak gecis (acilirken / kapanirken ani kesilme olmasin)
        fade += ((skyVisible ? 1f : 0f) - fade) * 0.05f;
        if (fade < 0.01f) return;

        // yukari bakinca daha guclu
        float look = MathHelper.clamp((20f - mc.player.getPitch()) / 90f, 0.25f, 1f);
        int alpha = (int) (m.num("Intensity %") / 100.0 * 90.0 * fade * look);
        if (alpha <= 0) return;

        // yavas renk dalgasi
        float wave = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 4000.0);

        int top, bottom;
        switch (m.index("Style")) {
            case 1:  // Twilight
                top = lerp(0x5B3FA8, 0xB04FA0, wave);
                bottom = 0xFF8FD0;
                break;
            case 2:  // Pastel
                top = lerp(0x8FD3FF, 0xC8A8FF, wave);
                bottom = 0xFFD1F0;
                break;
            default: // Sunset
                top = lerp(0xFF8A3D, 0xFF5E9C, wave);
                bottom = 0xFFC9A0;
        }

        int w = ctx.getScaledWindowWidth();
        int h = (int) (ctx.getScaledWindowHeight() * 0.65f);
        ctx.fillGradient(0, 0, w, h, (alpha << 24) | top, bottom); // alt kisim alfa=0 (seffaf)
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }
}

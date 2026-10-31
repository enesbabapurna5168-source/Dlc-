package com.example.spcombat.mixin;

import com.example.spcombat.PixelText;
import com.example.spcombat.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Acilis (yukleme) ekraninda Mojang logosunun ustunu kapatip "femboy Felix" yazar. */
@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void spcombat$cover(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        ctx.fill(0, 0, w, h, 0xFF120A18);

        String text = "femboy Felix";
        int scale = Math.max(2, (int) (w * 0.5 / PixelText.width(text)));
        PixelText.drawCentered(ctx, text, w / 2, h / 2 - (PixelText.height() * scale) / 2, scale,
                Theme.cur().accent);
    }
}

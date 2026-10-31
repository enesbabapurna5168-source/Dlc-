package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ViewModel: birinci sahis el / esya konumunu, buyuklugunu ve acisini degistirir (sadece gorsel). */
@Mixin(HeldItemRenderer.class)
public class ViewModelMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void spcombat$viewmodel(CallbackInfo ci,
                                    @Local(argsOnly = true) MatrixStack matrices,
                                    @Local(argsOnly = true) Hand hand) {
        if (!Modules.VIEWMODEL.enabled) return;
        int which = Modules.VIEWMODEL.index("Hand");        // 0 Both, 1 Main, 2 Off
        boolean main = hand == Hand.MAIN_HAND;
        if ((which == 1 && !main) || (which == 2 && main)) return;

        double side = main ? 1.0 : -1.0;                     // sol el aynalanir
        matrices.translate(Modules.VIEWMODEL.num("X") * side, Modules.VIEWMODEL.num("Y"), Modules.VIEWMODEL.num("Z"));
        float s = (float) (Modules.VIEWMODEL.num("Scale %") / 100.0);
        matrices.scale(s, s, s);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Modules.VIEWMODEL.num("Rotate X")));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (Modules.VIEWMODEL.num("Rotate Y") * side)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (Modules.VIEWMODEL.num("Rotate Z") * side)));
    }
}

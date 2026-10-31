package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SwingAnimation: sadece kendi oyuncumuzun el sallama suresini uzatir (gorsel). */
@Mixin(LivingEntity.class)
public class SwingMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void spcombat$swing(CallbackInfoReturnable<Integer> cir) {
        if (!Modules.SWING.enabled) return;
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        cir.setReturnValue((int) Modules.SWING.num("Duration (tick)"));
    }
}

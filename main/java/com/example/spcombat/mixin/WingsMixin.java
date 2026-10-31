package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wings: sadece bizim oyuncumuzun cizimine elytra kanatlari ekler (gorsel). Envanter ve zirh
 * degismez. PlayerEntityRenderer'in updateRenderState'i zirh bilgisini en son doldurdugu icin
 * (TAIL) sonrasinda ekliyoruz, yoksa ustune yazilirdi.
 */
@Mixin(PlayerEntityRenderer.class)
public class WingsMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void spcombat$wings(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!Modules.WINGS.enabled) return;
        if (entity != MinecraftClient.getInstance().player) return;
        if (state.equippedChestStack.isEmpty()) {
            state.equippedChestStack = new ItemStack(Items.ELYTRA);
        }
    }
}

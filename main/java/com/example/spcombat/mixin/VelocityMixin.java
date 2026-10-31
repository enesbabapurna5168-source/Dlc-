package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Velocity: hiz paketi islenmeden once oyuncunun hizini kaydeder, islendikten sonra
 * aradaki farki (knockback) yuzde ayarina gore kucultur. Paket baska bir varliga aitse
 * oyuncunun hizi degismez, yani fark sifirdir ve hicbir sey yapilmaz.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class VelocityMixin {

    @Unique
    private Vec3d spcombat$before;

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"))
    private void spcombat$head(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        spcombat$before = p == null ? null : p.getVelocity();
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void spcombat$tail(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (!Modules.VELOCITY.enabled || spcombat$before == null) return;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;

        Vec3d before = spcombat$before;
        Vec3d delta = p.getVelocity().subtract(before);
        if (delta.lengthSquared() < 1.0E-9) return;

        double h = Modules.VELOCITY.num("Horizontal %") / 100.0;
        double v = Modules.VELOCITY.num("Vertical %") / 100.0;
        p.setVelocity(before.x + delta.x * h, before.y + delta.y * v, before.z + delta.z * h);
    }
}

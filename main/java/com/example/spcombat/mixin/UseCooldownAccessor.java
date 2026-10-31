package com.example.spcombat.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** FastUse: sag tik tekrar bekleme suresini sifirlamak icin. */
@Mixin(MinecraftClient.class)
public interface UseCooldownAccessor {
    @Accessor("itemUseCooldown")
    void spcSetUseCooldown(int v);
}

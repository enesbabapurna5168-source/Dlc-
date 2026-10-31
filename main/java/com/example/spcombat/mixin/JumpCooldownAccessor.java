package com.example.spcombat.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** NoJumpDelay: ziplama bekleme suresini sifirlamak icin. */
@Mixin(LivingEntity.class)
public interface JumpCooldownAccessor {
    @Accessor("jumpingCooldown")
    void spcSetJumpCooldown(int v);
}

package com.example.spcombat;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/** Client Sound: bir canliya vurdugunda (elle ya da modullerle) hos bir ses calar. */
public final class HitSound {
    private HitSound() {}

    private static final SoundEvent[] SOUNDS = {
            SoundEvents.ENTITY_ARROW_HIT_PLAYER,
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
            SoundEvents.ENTITY_PLAYER_LEVELUP
    };

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && Modules.CLIENTSOUND.enabled && entity instanceof LivingEntity) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (player == mc.player) {
                    Module m = Modules.CLIENTSOUND;
                    float volume = (float) (m.num("Volume %") / 100.0);
                    float pitch = (float) (m.num("Pitch %") / 100.0);
                    // sadece bize calar (oyuncunun kendi sesi)
                    mc.player.playSound(SOUNDS[m.index("Sound")], volume, pitch);
                }
            }
            return ActionResult.PASS;
        });
    }
}

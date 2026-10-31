package com.example.spcombat;

import com.example.spcombat.mixin.JumpCooldownAccessor;
import com.example.spcombat.mixin.UseCooldownAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ek moduller: ESP / NameTags / Tracers / ItemESP, HitParticles / JumpCircle / KillEffect,
 * AutoEat / FastUse / NoJumpDelay, Flight / Step / NoFall / Jesus / Parkour / SafeWalk / Spider /
 * ElytraRecast, VisualRange / AutoEZ.
 * Izinsiz sunucuda gorsel olmayan her sey kapali sayilir (Module.cosmetic kurali).
 */
public final class Extras {
    private Extras() {}

    private static final Map<Integer, double[]> pos = new HashMap<>();   // id -> {onceki xyz, simdiki xyz}
    private static long tickNs = System.nanoTime();
    private static final Set<Integer> glowed = new HashSet<>();
    private static final Set<UUID> inRange = new HashSet<>();
    private static boolean stepApplied;
    private static boolean wasOnGround = true;
    private static int recastCd, ezCd;
    private static LivingEntity fxTarget;
    private static boolean fxDone;
    private static boolean eatActive;
    private static int eatTicks, eatPrev = -1;
    private static final java.util.Random RNG = new java.util.Random();

    private static int afkTicks, tapeTicks, rocketCd;
    private static boolean afkJump, tapeDown, deathShown;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!Modules.AUTOTPACCEPT.enabled || overlay) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() == null) return;
            String s = message.getString().toLowerCase();
            if (s.contains("tpa") || s.contains("teleport") || s.contains("isinlan")) {
                if (s.contains("request") || s.contains("istek") || s.contains("wants") || s.contains("tpaccept")) {
                    mc.getNetworkHandler().sendChatCommand("tpaccept");
                }
            }
        });
    }

    private static boolean on(Module m, boolean sp) {
        return m.enabled && (m.cosmetic || sp);
    }

    // ==================================================================
    //  TICK
    // ==================================================================
    public static void tick(MinecraftClient mc, ClientPlayerEntity p, boolean sp, boolean idle) {
        tickNs = System.nanoTime();
        if (recastCd > 0) recastCd--;
        if (ezCd > 0) ezCd--;

        // konum izleyici (kutu / etiket akici gorunsun diye)
        Set<Integer> alive = new HashSet<>();
        for (PlayerEntity e : mc.world.getPlayers()) {
            int id = e.getId();
            double[] a = pos.get(id);
            if (a == null) {
                pos.put(id, new double[]{e.getX(), e.getY(), e.getZ(), e.getX(), e.getY(), e.getZ()});
            } else {
                a[0] = a[3]; a[1] = a[4]; a[2] = a[5];
                a[3] = e.getX(); a[4] = e.getY(); a[5] = e.getZ();
            }
            alive.add(id);
        }
        pos.keySet().retainAll(alive);

        extra(mc, p, sp);
        glow(mc, p, sp);
        visualRange(mc, p, sp);
        effects(mc, p, sp);
        step(p, sp);

        if (idle) {
            if (eatActive) stopEat(mc, p);
            return;
        }
        if (on(Modules.FASTUSE, sp)) {
            try { ((UseCooldownAccessor) (Object) mc).spcSetUseCooldown(0); }
            catch (ClassCastException | LinkageError ignored) { }
        }
        if (on(Modules.NOJUMPDELAY, sp)) {
            try { ((JumpCooldownAccessor) (Object) p).spcSetJumpCooldown(0); }
            catch (ClassCastException | LinkageError ignored) { }
        }
        if (on(Modules.AUTOEAT, sp)) autoEat(mc, p); else if (eatActive) stopEat(mc, p);
        if (on(Modules.FLIGHT, sp)) flight(mc, p);
        if (on(Modules.NOFALL, sp)) noFall(mc, p);
        if (on(Modules.JESUS, sp)) jesus(p);
        if (on(Modules.SPIDER, sp)) spider(p);
        if (on(Modules.PARKOUR, sp)) parkour(mc, p);
        if (on(Modules.SAFEWALK, sp)) safeWalk(mc, p);
        if (on(Modules.ELYRECAST, sp)) elyRecast(mc, p);
    }

    // ---------------- Onetap'ten esinlenen ekler ----------------
    private static void extra(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        if (rocketCd > 0) rocketCd--;

        // DeathCoords
        if (!p.isAlive()) {
            if (!deathShown && Modules.DEATHCOORDS.enabled) {
                p.sendMessage(Text.literal("\u00A7c[DeathCoords] \u00A7fOldugun yer: " + (int) p.getX() + " "
                        + (int) p.getY() + " " + (int) p.getZ()), false);
            }
            deathShown = true;
        } else {
            deathShown = false;
        }

        // Panic: bind tusuna basinca her sey kapanir
        if (Modules.PANIC.consume()) {
            for (Module m : Modules.ALL) if (!m.actionKey) m.enabled = false;
            p.sendMessage(Text.literal("\u00A7e[Panic] \u00A7fTum moduller kapatildi."), true);
        }

        // FireFly / Trails (parcacik)
        if (Modules.FIREFLY.enabled) {
            ParticleEffect t = switch (Modules.FIREFLY.index("Type")) {
                case 1 -> ParticleTypes.ENCHANT;
                case 2 -> ParticleTypes.GLOW;
                default -> ParticleTypes.END_ROD;
            };
            int n = (int) Modules.FIREFLY.num("Amount");
            for (int i = 0; i < n; i++) {
                mc.particleManager.addParticle(t,
                        p.getX() + (RNG.nextDouble() - 0.5) * 6, p.getY() + RNG.nextDouble() * 3,
                        p.getZ() + (RNG.nextDouble() - 0.5) * 6,
                        (RNG.nextDouble() - 0.5) * 0.02, (RNG.nextDouble() - 0.3) * 0.02, (RNG.nextDouble() - 0.5) * 0.02);
            }
        }
        if (Modules.TRAILS.enabled && Math.hypot(p.getVelocity().x, p.getVelocity().z) > 0.05) {
            ParticleEffect t = switch (Modules.TRAILS.index("Type")) {
                case 1 -> ParticleTypes.CLOUD;
                case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
                default -> ParticleTypes.FLAME;
            };
            int n = (int) Modules.TRAILS.num("Amount");
            for (int i = 0; i < n; i++) {
                mc.particleManager.addParticle(t, p.getX() + (RNG.nextDouble() - 0.5) * 0.4, p.getY() + 0.1,
                        p.getZ() + (RNG.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
            }
        }

        if (!sp || !p.isAlive()) return;

        // AutoLeave
        if (Modules.AUTOLEAVE.enabled && mc.getNetworkHandler() != null) {
            boolean leave = p.getHealth() <= (float) Modules.AUTOLEAVE.num("Health (hp)");
            if (!leave && Modules.AUTOLEAVE.bool("Player nearby")) {
                double r = Modules.AUTOLEAVE.num("Range");
                for (PlayerEntity e : mc.world.getPlayers()) {
                    if (e != p && !e.isSpectator() && p.squaredDistanceTo(e) <= r * r) { leave = true; break; }
                }
            }
            if (leave) {
                Modules.AUTOLEAVE.enabled = false;
                mc.getNetworkHandler().getConnection().disconnect(Text.literal("Menemen AutoLeave"));
                return;
            }
        }

        if (mc.currentScreen != null) return;

        // FastExp: tecrube sisesi icin bekleme yok
        if (Modules.FASTEXP.enabled && p.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            try { ((UseCooldownAccessor) (Object) mc).spcSetUseCooldown(0); }
            catch (ClassCastException | LinkageError ignored) { }
        }

        // AntiAFK: belirli aralikla bir kere zipla
        if (Modules.ANTIAFK.enabled) {
            if (afkJump) { mc.options.jumpKey.setPressed(false); afkJump = false; }
            if (++afkTicks >= Modules.ANTIAFK.num("Interval (sec)") * 20) {
                afkTicks = 0;
                mc.options.jumpKey.setPressed(true);
                afkJump = true;
            }
        }

        // TapeMouse: belirli aralikla tikla
        if (Modules.TAPEMOUSE.enabled) {
            var key = Modules.TAPEMOUSE.index("Button") == 0 ? mc.options.attackKey : mc.options.useKey;
            if (tapeDown) { key.setPressed(false); tapeDown = false; }
            if (++tapeTicks >= Modules.TAPEMOUSE.num("Delay (tick)")) {
                tapeTicks = 0;
                key.setPressed(true);
                tapeDown = true;
            }
        }

        // ElytraTakeoff: yerde zipla, havada elytrayi ac, istenirse otomatik fisek at
        if (Modules.ELYTAKEOFF.enabled && p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
                && !p.hasVehicle() && !p.isTouchingWater() && mc.getNetworkHandler() != null) {
            if (!p.isGliding()) {
                if (p.isOnGround()) {
                    if (mc.options.forwardKey.isPressed()) p.jump();
                } else if (p.getVelocity().y < -0.1 && recastCd <= 0) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    recastCd = 10;
                }
            } else if (Modules.ELYTAKEOFF.bool("Auto firework") && rocketCd <= 0) {
                if (SpCombatClient.rocket(mc, p)) rocketCd = (int) Modules.ELYTAKEOFF.num("Firework delay (tick)");
                else rocketCd = 40;
            }
        }
    }

    // ---------------- parlama (ESP Glow, ItemESP) ----------------
    private static void glow(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        Set<Integer> want = new HashSet<>();
        if (on(Modules.ESP, sp) && Modules.ESP.index("Mode") >= 1) {
            double r = Modules.ESP.num("Range");
            for (PlayerEntity e : mc.world.getPlayers()) {
                if (e == p || e.isSpectator()) continue;
                if (p.squaredDistanceTo(e) > r * r) continue;
                e.setGlowing(true);
                want.add(e.getId());
            }
        }
        if (on(Modules.ITEMESP, sp)) {
            double r = Modules.ITEMESP.num("Range");
            Box box = p.getBoundingBox().expand(r);
            for (ItemEntity e : mc.world.getEntitiesByClass(ItemEntity.class, box, x -> true)) {
                e.setGlowing(true);
                want.add(e.getId());
            }
        }
        for (Integer id : glowed) {
            if (want.contains(id)) continue;
            Entity en = mc.world.getEntityById(id);
            if (en != null) en.setGlowing(false);
        }
        glowed.clear();
        glowed.addAll(want);
    }

    // ---------------- VisualRange ----------------
    private static void visualRange(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        if (!on(Modules.VISUALRANGE, sp)) { inRange.clear(); return; }
        double r = Modules.VISUALRANGE.num("Range");
        Set<UUID> now = new HashSet<>();
        for (PlayerEntity e : mc.world.getPlayers()) {
            if (e == p || e.isSpectator()) continue;
            if (p.squaredDistanceTo(e) > r * r) continue;
            now.add(e.getUuid());
            if (!inRange.contains(e.getUuid())) {
                p.sendMessage(Text.literal("§b[VisualRange] §f" + e.getName().getString()
                        + " §7yakinda (" + (int) p.distanceTo(e) + " blok)"), false);
            }
        }
        inRange.clear();
        inRange.addAll(now);
    }

    // ---------------- JumpCircle, KillEffect, AutoEZ ----------------
    private static void effects(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        boolean g = p.isOnGround();
        if (on(Modules.JUMPCIRCLE, sp) && wasOnGround && !g && p.getVelocity().y > 0.2) {
            ParticleEffect t = switch (Modules.JUMPCIRCLE.index("Type")) {
                case 1 -> ParticleTypes.CLOUD;
                case 2 -> ParticleTypes.ENCHANT;
                default -> ParticleTypes.END_ROD;
            };
            ring(mc, p.getX(), p.getY() + 0.05, p.getZ(), t, 28, 0.12);
        }
        wasOnGround = g;

        LivingEntity t = SpCombatClient.hudTarget;
        if (t != fxTarget) { fxTarget = t; fxDone = false; }
        if (t != null && !fxDone && !t.isAlive() && SpCombatClient.ticks - SpCombatClient.hudTargetTick < 100) {
            fxDone = true;
            if (on(Modules.KILLEFFECT, sp)) {
                for (int i = 0; i < 40; i++) {
                    mc.particleManager.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                            t.getX() + (RNG.nextDouble() - 0.5), t.getY() + RNG.nextDouble() * t.getHeight(),
                            t.getZ() + (RNG.nextDouble() - 0.5),
                            (RNG.nextDouble() - 0.5) * 0.4, RNG.nextDouble() * 0.4, (RNG.nextDouble() - 0.5) * 0.4);
                }
            }
            if (on(Modules.AUTOEZ, sp) && ezCd <= 0 && t instanceof PlayerEntity && mc.getNetworkHandler() != null) {
                String[] msgs = {"ez", "gg", "Menemen DLC"};
                mc.getNetworkHandler().sendChatMessage(msgs[Modules.AUTOEZ.index("Message")]);
                ezCd = 100;
            }
        }
    }

    private static void ring(MinecraftClient mc, double x, double y, double z, ParticleEffect type, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            mc.particleManager.addParticle(type, x + Math.cos(a) * 0.4, y, z + Math.sin(a) * 0.4,
                    Math.cos(a) * speed, 0.0, Math.sin(a) * speed);
        }
    }

    /** AttackAura her vurdugunda cagrilir. */
    public static void onHit(MinecraftClient mc, LivingEntity t) {
        if (!Modules.HITPARTICLES.enabled || t == null) return;
        ParticleEffect type = switch (Modules.HITPARTICLES.index("Type")) {
            case 1 -> ParticleTypes.HEART;
            case 2 -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> ParticleTypes.CRIT;
        };
        int n = (int) Modules.HITPARTICLES.num("Amount");
        for (int i = 0; i < n; i++) {
            mc.particleManager.addParticle(type,
                    t.getX() + (RNG.nextDouble() - 0.5) * t.getWidth(),
                    t.getY() + RNG.nextDouble() * t.getHeight(),
                    t.getZ() + (RNG.nextDouble() - 0.5) * t.getWidth(),
                    (RNG.nextDouble() - 0.5) * 0.3, RNG.nextDouble() * 0.3, (RNG.nextDouble() - 0.5) * 0.3);
        }
    }

    // ---------------- Step ----------------
    private static void step(ClientPlayerEntity p, boolean sp) {
        EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr == null) return;
        if (on(Modules.STEP, sp)) {
            attr.setBaseValue(Modules.STEP.num("Height"));
            stepApplied = true;
        } else if (stepApplied) {
            attr.setBaseValue(0.6);
            stepApplied = false;
        }
    }

    // ---------------- AutoEat ----------------
    private static boolean isFood(ItemStack s) {
        if (s.isEmpty() || !s.contains(DataComponentTypes.FOOD)) return false;
        return !(s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.POISONOUS_POTATO)
                || s.isOf(Items.PUFFERFISH) || s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || s.isOf(Items.CHORUS_FRUIT));
    }

    private static void autoEat(MinecraftClient mc, ClientPlayerEntity p) {
        var inv = p.getInventory();
        if (!eatActive) {
            if (p.isUsingItem()) return;
            var hm = p.getHungerManager();
            if (!hm.isNotFull() || hm.getFoodLevel() > (int) Modules.AUTOEAT.num("Hunger")) return;
            for (int i = 0; i < 9; i++) {
                if (!isFood(inv.getStack(i))) continue;
                eatPrev = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                mc.options.useKey.setPressed(true);
                eatActive = true;
                eatTicks = 0;
                return;
            }
            return;
        }
        eatTicks++;
        boolean done = eatTicks > 3 && !p.isUsingItem();
        if (done || eatTicks > 80 || !p.getHungerManager().isNotFull()) stopEat(mc, p);
    }

    private static void stopEat(MinecraftClient mc, ClientPlayerEntity p) {
        mc.options.useKey.setPressed(false);
        if (eatPrev >= 0) p.getInventory().setSelectedSlot(eatPrev);
        eatPrev = -1;
        eatActive = false;
    }

    // ---------------- hareket ----------------
    private static void flight(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isGliding() || p.hasVehicle()) return;
        double s = Modules.FLIGHT.num("Speed");
        double fwd = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double str = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        double rad = Math.toRadians(p.getYaw());
        double vx = -Math.sin(rad) * fwd + Math.cos(rad) * str;
        double vz = Math.cos(rad) * fwd + Math.sin(rad) * str;
        double len = Math.hypot(vx, vz);
        if (len > 1.0) { vx /= len; vz /= len; }
        double vy = (mc.options.jumpKey.isPressed() ? 1 : 0) - (mc.options.sneakKey.isPressed() ? 1 : 0);
        p.setVelocity(vx * s, vy * s, vz * s);
        p.fallDistance = 0;
    }

    private static void noFall(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isGliding() || p.isOnGround() || mc.getNetworkHandler() == null) return;
        if (p.fallDistance > Modules.NOFALL.num("Distance")) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    p.getYaw(), p.getPitch(), true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }

    private static void jesus(ClientPlayerEntity p) {
        if (p.isSneaking() || p.isGliding()) return;
        if (p.isTouchingWater()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.12, v.z);
        }
    }

    private static void spider(ClientPlayerEntity p) {
        if (p.horizontalCollision && !p.isOnGround() && !p.isGliding()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.2, v.z);
        }
    }

    /** Ilerideki zeminde blok yok mu? */
    private static boolean voidAhead(MinecraftClient mc, ClientPlayerEntity p, double ticksAhead) {
        Vec3d v = p.getVelocity();
        Box b = p.getBoundingBox().offset(v.x * ticksAhead, -0.6, v.z * ticksAhead);
        return !mc.world.getBlockCollisions(p, b).iterator().hasNext();
    }

    private static void parkour(MinecraftClient mc, ClientPlayerEntity p) {
        if (!p.isOnGround() || p.isSneaking() || p.hasVehicle()) return;
        Vec3d v = p.getVelocity();
        if (Math.hypot(v.x, v.z) < 0.05) return;
        if (voidAhead(mc, p, 2.0)) p.jump();
    }

    private static void safeWalk(MinecraftClient mc, ClientPlayerEntity p) {
        if (!p.isOnGround() || p.hasVehicle()) return;
        Vec3d v = p.getVelocity();
        if (Math.hypot(v.x, v.z) < 0.01) return;
        if (voidAhead(mc, p, 1.0)) p.setVelocity(0, v.y, 0);
    }

    private static void elyRecast(MinecraftClient mc, ClientPlayerEntity p) {
        if (recastCd > 0 || mc.getNetworkHandler() == null) return;
        if (p.isGliding() || p.isOnGround() || p.isTouchingWater() || p.hasVehicle()) return;
        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;
        if (p.getVelocity().y > -0.1) return;
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        recastCd = 10;
    }

    // ==================================================================
    //  CIZIM: ESP kutusu, NameTags, Tracers (3D -> ekran donusumu elle yapilir)
    // ==================================================================
    private static double[] project(MinecraftClient mc, ClientPlayerEntity p, Vec3d eye, double x, double y, double z,
                                    int sw, int sh) {
        double dx = x - eye.x, dy = y - eye.y, dz = z - eye.z;
        double yaw = Math.toRadians(p.getYaw()), pitch = Math.toRadians(p.getPitch());
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        // yukari = sag x ileri
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.05) return null;
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double scale = (sh / 2.0) / Math.tan(fov / 2.0);
        return new double[]{sw / 2.0 + xc / zc * scale, sh / 2.0 - yc / zc * scale};
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        boolean sp = Access.allowed(mc);
        boolean box = on(Modules.ESP, sp) && Modules.ESP.index("Mode") != 1;
        boolean tags = on(Modules.NAMETAGS, sp);
        boolean lines = on(Modules.TRACERS, sp);
        boolean blockBox = Modules.BLOCKOVERLAY.enabled;
        if (!box && !tags && !lines && !blockBox) return;

        double[] me = pos.get(p.getId());
        if (me == null) return;
        double part = Math.min(1.0, (System.nanoTime() - tickNs) / 50_000_000.0);
        Vec3d eye = new Vec3d(me[0] + (me[3] - me[0]) * part,
                me[1] + (me[4] - me[1]) * part + (p.getEyeY() - p.getY()),
                me[2] + (me[5] - me[2]) * part);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int accent = Theme.cur().accent;

        if (blockBox && mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bhr
                && bhr.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            var bp = bhr.getBlockPos();
            double[][] c = new double[8][];
            for (int i = 0; i < 8; i++) {
                c[i] = project(mc, p, eye, bp.getX() + (i & 1), bp.getY() + ((i >> 1) & 1), bp.getZ() + ((i >> 2) & 1), sw, sh);
            }
            int[][] edges = {{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
            for (int[] ed : edges) {
                if (c[ed[0]] == null || c[ed[1]] == null) continue;
                line(ctx, c[ed[0]][0], c[ed[0]][1], c[ed[1]][0], c[ed[1]][1], Theme.alpha(accent, 0xE0));
            }
        }

        for (PlayerEntity e : mc.world.getPlayers()) {
            if (!(box || tags || lines)) break;
            if (e == p || e.isSpectator() || !e.isAlive()) continue;
            double[] a = pos.get(e.getId());
            if (a == null) continue;
            double x = a[0] + (a[3] - a[0]) * part, y = a[1] + (a[4] - a[1]) * part, z = a[2] + (a[5] - a[2]) * part;
            double dist = eye.distanceTo(new Vec3d(x, y + e.getHeight() / 2, z));
            double w = e.getWidth() / 2.0, h = e.getHeight();

            if (box && dist <= Modules.ESP.num("Range")) {
                double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
                boolean okAll = true;
                for (int i = 0; i < 8 && okAll; i++) {
                    double[] s = project(mc, p, eye, x + ((i & 1) == 0 ? -w : w), y + ((i & 2) == 0 ? 0 : h),
                            z + ((i & 4) == 0 ? -w : w), sw, sh);
                    if (s == null) { okAll = false; break; }
                    minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
                    minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
                }
                if (okAll) {
                    int x0 = (int) minX, y0 = (int) minY, x1 = (int) maxX, y1 = (int) maxY;
                    ctx.fill(x0, y0, x1, y1, Theme.alpha(accent, 0x18));
                    outline(ctx, x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, 0xC0000000);
                    outline(ctx, x0, y0, x1 - x0, y1 - y0, accent);
                    float f = MathHelper.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
                    int bh = (int) ((y1 - y0) * f);
                    ctx.fill(x0 - 4, y0 - 1, x0 - 1, y1 + 1, 0xA0000000);
                    int hc = f > 0.5f ? 0xFF55FF55 : f > 0.25f ? 0xFFFFD255 : 0xFFFF5555;
                    ctx.fill(x0 - 3, y1 - bh, x0 - 2, y1, hc);
                }
            }

            if (tags && dist <= Modules.NAMETAGS.num("Range")) {
                double[] s = project(mc, p, eye, x, y + h + 0.35, z, sw, sh);
                if (s != null) {
                    StringBuilder sb = new StringBuilder(e.getName().getString());
                    if (Modules.NAMETAGS.bool("Show HP")) sb.append("  ").append((int) Math.ceil(e.getHealth())).append(" HP");
                    if (Modules.NAMETAGS.bool("Show distance")) sb.append("  ").append((int) dist).append("m");
                    String t = sb.toString();
                    int tw = Fnt.w(t, 8.5f) + 10;
                    int tx = (int) s[0] - tw / 2, ty = (int) s[1] - 12;
                    Draw.rbox(ctx, tx, ty, tw, 12, 0xB0090B0F, 5);
                    Draw.rbox(ctx, tx + 3, ty + 11, tw - 6, 1, Theme.alpha(accent, 0xE0), 0);
                    Fnt.draw(ctx, t, tx + 5, ty + 2.5f, 8.5f, 0xFFFFFFFF);
                }
            }

            if (lines && dist <= Modules.TRACERS.num("Range")) {
                double[] s = project(mc, p, eye, x, y + h / 2, z, sw, sh);
                if (s != null) {
                    double sx = sw / 2.0, sy = sh / 2.0;
                    double len = Math.hypot(s[0] - sx, s[1] - sy);
                    int n = (int) Math.min(900, len);
                    for (int i = 0; i <= n; i++) {
                        double f = n == 0 ? 0 : (double) i / n;
                        int px = (int) (sx + (s[0] - sx) * f), py = (int) (sy + (s[1] - sy) * f);
                        ctx.fill(px, py, px + 1, py + 1, Theme.alpha(accent, 0xB0));
                    }
                }
            }
        }
    }

    private static void line(DrawContext ctx, double x0, double y0, double x1, double y1, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int n = (int) Math.min(900, len);
        for (int i = 0; i <= n; i++) {
            double f = n == 0 ? 0 : (double) i / n;
            int px = (int) (x0 + (x1 - x0) * f), py = (int) (y0 + (y1 - y0) * f);
            ctx.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}

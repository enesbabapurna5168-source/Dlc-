package com.example.spcombat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

/**
 * Savas, hareket ve gorsel yardimcilar. Tek oyunculu dunyalarda her zaman calisir;
 * sunucuda sadece GUI'den "Bu sunucuda izin: ACIK" yaptigin adreslerde calisir
 * (bkz. Access). Izinsiz sunucuda gorsel/ses moduller (HUD, Ambience, ClientSound,
 * ArmorHUD, TargetHUD) disindakiler kapanir ve calismaz.
 *
 * Sag Shift: GUI (Kontroller menusunden degistirilebilir).
 * Modul tuslari GUI'deki "Bind" satirindan atanir; hicbir modulde varsayilan tus yok.
 */
public class SpCombatClient implements ClientModInitializer {

    /** Sunucunun saldiri icin kabul ettigi en fazla mesafe. */
    private static final double REACH_LIMIT = 4.0;
    private static final double HIT_REACH = 3.6;

    private static KeyBinding keyGui;

    /** HUD paneli icin: son hedef ve ne zaman vuruldugu. */
    public static int ticks;
    public static LivingEntity hudTarget;
    public static int hudTargetTick;

    private static boolean critNext;          // sonraki hit kritik olacak mi (sprint paketi icin)
    private static boolean autoJumpHeld;       // AttackAura Auto jump
    private static int noSprintTick = -10;    // AttackAura kritik icin sprint'i kapatti
    private static LivingEntity elyLock;       // ElytraTarget kilitli hedef
    private static double fbAcc;               // FastBreak
    private static final java.util.HashMap<Integer, double[]> trk = new java.util.HashMap<>();
    private static LivingEntity auraLock;      // AttackAura: kilitli hedef
    private static int auraLockTick;
    private static LivingEntity espGlowing;    // TargetESP: parlattigimiz entity
    private static int totemCd, potionCd, helperCd, boosterCd, pearlCd, armorCd, stealCd;
    private static Vec3d frozen;               // AirStuck (Freeze)
    private static boolean nightVisionApplied; // FullBright
    private static int toolPrev = -1;          // AutoTool: onceki slot
    private static boolean eating;             // AutoGApple durumu
    private static int eatTicks, eatPrevSlot = -1;
    private static boolean stealStarted;       // ChestStealer: bu sandikta bir sey alindi mi

    @Override
    public void onInitializeClient() {
        Config.captureDefaults();
        Config.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("spcombat", "main"));
        keyGui = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.spcombat.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));

        Hud.init();
        Ambience.init();
        HitSound.init();
        MenuStyle.init();
        Extras.init();
        TitleStyle.init();
        ClientTickEvents.END_CLIENT_TICK.register(SpCombatClient::tick);
        ClientLifecycleEvents.CLIENT_STARTED.register(AltScreen::applySaved);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
    }

    // ==================================================================
    //  Ana dongu
    // ==================================================================
    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            frozen = null;
            nightVisionApplied = false;
            eating = false;
            toolPrev = -1;
            stealStarted = false;
            espGlowing = null;
            auraLock = null;
            Rotations.reset();
            return;
        }
        ticks++;
        targetEsp();

        // FullBright kapandiysa efekti kaldir (her durumda)
        if (nightVisionApplied && !Modules.FULLBRIGHT.enabled) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            nightVisionApplied = false;
        }
        if (frozen != null && !Modules.AIRSTUCK.enabled) frozen = null;

        // GUI her yerde acilabilir
        while (keyGui.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        }

        // Tek oyunculu her zaman; sunucuda sadece GUI'den izin verilen adreslerde
        boolean sp = Access.allowed(mc);
        handleBinds(mc, p, sp);

        Extras.tick(mc, p, sp, !p.isAlive() || mc.currentScreen != null);

        // Izinsiz sunucuda: gorsel/ses/eylem disindaki her sey kapanir, modul kodu calismaz
        if (!sp) {
            for (Module m : Modules.ALL) {
                if (!m.cosmetic && !m.actionKey) m.enabled = false;
            }
            if (eating) { mc.options.useKey.setPressed(false); eating = false; }
            toolPrev = -1;
            return;
        }

        if (Modules.AUTORESPAWN.enabled && mc.currentScreen instanceof DeathScreen) {
            p.requestRespawn();
        }

        // Oyuncu oluyse ya da bir ekran (envanter, menu) aciksa savas/hareket modulleri durur
        boolean idle = !p.isAlive() || mc.currentScreen != null;

        // Ekran acikken de calisanlar
        if (Modules.GUIMOVE.enabled) guiMove(mc);
        if (Modules.AUTOARMOR.enabled) autoArmor(mc, p);
        if (Modules.TOTEM.enabled) autoTotem(mc, p);
        if (Modules.CHESTSTEALER.enabled) chestStealer(mc, p);

        // Eylem tuslari (bind tetikler)
        elytraHelper(mc, p);
        clickPearl(mc, p);

        if (eating && (idle || !Modules.AUTOGAPPLE.enabled)) stopEating(mc, p);

        if (!idle) {
            if (Modules.AURA.enabled) attackAura(mc, p);
            else Rotations.clearTarget();
            if (Modules.TRIGGER.enabled) triggerBot(mc, p);
            if (Modules.POTION.enabled) autoPotion(mc, p);
            if (Modules.AUTOGAPPLE.enabled) autoGApple(mc, p);
            if (Modules.ELYTRA.enabled) elytraTarget(mc, p);
            if (Modules.ELYBOOSTER.enabled) elytraBooster(mc, p);
            if (Modules.AUTOSPRINT.enabled) autoSprint(mc, p);
            if (Modules.SPEED.enabled) speed(mc, p);
            if (Modules.FASTBREAK.enabled) fastBreak(mc, p);
            if (Modules.AUTOTOOL.enabled) autoTool(mc, p);
            else if (toolPrev >= 0) toolPrev = -1;
            if (Modules.SCAFFOLD.enabled) scaffold(mc, p);
        }
        if (Modules.AIRSTUCK.enabled) airStuck(p);
        if (Modules.FULLBRIGHT.enabled) fullbright(p);
        // Velocity: mixin/VelocityMixin.java icinde
    }

    /** Entity'nin son tick'teki hareketi (blok/tick). Ayni tick'te tekrar sorulursa ayni degeri verir. */
    private static Vec3d velocity(LivingEntity e) {
        if (trk.size() > 64) trk.clear();
        double[] o = trk.get(e.getId());
        if (o != null && (int) o[3] == ticks) return new Vec3d(o[4], o[5], o[6]);
        double vx = 0, vy = 0, vz = 0;
        if (o != null && ticks - (int) o[3] == 1) {
            vx = e.getX() - o[0];
            vy = e.getY() - o[1];
            vz = e.getZ() - o[2];
        }
        trk.put(e.getId(), new double[]{e.getX(), e.getY(), e.getZ(), ticks, vx, vy, vz});
        return new Vec3d(vx, vy, vz);
    }

    // TargetESP: son hedefi oyunun parlama (glow) efektiyle isaretler
    private static void targetEsp() {
        Module esp = Modules.TARGETESP;
        LivingEntity want = null;
        if (esp.enabled) {
            LivingEntity t = hudTarget;
            if (t != null && t.isAlive() && ticks - hudTargetTick <= 60) want = t;
        }
        int mode = esp.index("Mode");                  // 0 Ring, 1 Glow, 2 Both, 3 Ghost
        LivingEntity glow = (mode == 1 || mode == 2) ? want : null;

        if (espGlowing != null && espGlowing != glow) {
            espGlowing.setGlowing(false);
            espGlowing = null;
        }
        if (glow != null) {
            glow.setGlowing(true);
            espGlowing = glow;
        }
        if (want != null && (mode == 0 || mode == 2)) espRing(want, esp.num("Ring size %") / 100.0);
        if (want != null && mode == 3) espGhost(want, esp.num("Ring size %") / 100.0);
    }

    /** Ghost: hedefin etrafinda donen, yukari asagi suzulen mavi hayalet isiklari (izli). */
    private static void espGhost(LivingEntity t, double size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double r = Math.max(0.6, t.getWidth() * 1.0) * size;
        int orbs = 3;
        for (int k = 0; k < orbs; k++) {
            for (int tr = 0; tr < 5; tr++) {
                double ang = ticks * 0.22 + k * (Math.PI * 2.0 / orbs) - tr * 0.14;
                double ph = ticks * 0.05 + k * 2.1 - tr * 0.03;
                double y = t.getY() + t.getHeight() * (0.5 + 0.45 * Math.sin(ph * Math.PI * 2.0));
                mc.particleManager.addParticle(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME,
                        t.getX() + Math.cos(ang) * r, y, t.getZ() + Math.sin(ang) * r, 0, 0, 0);
            }
        }
    }

    /** Hedefin etrafinda yukari asagi gezen donen halka (parcacik izi). */
    private static void espRing(LivingEntity t, double size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double r = Math.max(0.55, t.getWidth() * 0.95) * size;
        double phase = (ticks % 50) / 50.0;
        double y = t.getY() + t.getHeight() * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2.0));
        int n = 14;
        for (int i = 0; i < n; i++) {
            double ang = ticks * 0.3 + i * (Math.PI * 2.0 / n);
            mc.particleManager.addParticle(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT,
                    t.getX() + Math.cos(ang) * r, y, t.getZ() + Math.sin(ang) * r, 0, 0, 0);
        }
    }

    // ==================================================================
    //  Tus atamalari (GUI'deki Bind satiri)
    //  Aksiyon modulleri bind'a basilinca tetiklenir, digerleri ac/kapat olur.
    // ==================================================================
    private static void handleBinds(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        long win = mc.getWindow().getHandle();
        for (Module m : Modules.ALL) {
            if (m.bind < 0) { m.bindDown = false; continue; }

            boolean down = mc.currentScreen == null && GLFW.glfwGetKey(win, m.bind) == GLFW.GLFW_PRESS;
            boolean edge = down && !m.bindDown;
            m.bindDown = down;
            if (!edge) continue;

            if (m.actionKey) {
                if (sp) m.trigger();
                else p.sendMessage(Text.literal("Bu sunucuda izin yok (GUI'den izin ver)."), true);
                continue;
            }
            if (!sp && !m.cosmetic) {
                p.sendMessage(Text.literal("Bu sunucuda izin yok (GUI'den izin ver)."), true);
                continue;
            }
            m.enabled = !m.enabled;
            p.sendMessage(Text.literal(m.name + ": " + (m.enabled ? "ACIK" : "KAPALI")), true);
        }
    }

    // ==================================================================
    //  Ortak yardimcilar
    // ==================================================================

    /** Hedef turu: 0 = Monsters, 1 = Mobs, 2 = All. Oyuncular, pet'ler, koylu/tuccar ve golemler haric. */
    private static boolean valid(LivingEntity e, ClientPlayerEntity p, int mode) {
        if (e == p || !e.isAlive()) return false;
        if (e instanceof PlayerEntity || e instanceof ArmorStandEntity || e instanceof MerchantEntity) return false;
        if (e instanceof TameableEntity t && t.isTamed()) return false;
        if (e instanceof IronGolemEntity) return false;
        switch (mode) {
            case 0:  return e instanceof Monster;
            case 1:  return e instanceof MobEntity;
            default: return true;
        }
    }

    /** Gozden hedefin vurulabilir kutusuna en yakin noktaya mesafenin karesi (oyunun kendi olcusu). */
    private static double reachSq(ClientPlayerEntity p, LivingEntity e) {
        Vec3d eye = p.getEyePos();
        Box b = e.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.squaredDistanceTo(x, y, z);
    }

    /** Arada blok yok mu? (duvar arkasindaki hedefe vurmamak icin) */
    private static boolean visible(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t) {
        Vec3d from = p.getEyePos();
        Vec3d[] points = { t.getBoundingBox().getCenter(), t.getEyePos() };
        for (Vec3d to : points) {
            BlockHitResult r = mc.world.raycast(new RaycastContext(
                    from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (r.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    /**
     * Menzildeki en uygun hedef.
     * @param reach     true: gozden kutuya mesafe (saldiri icin), false: merkezden merkeze (uzak hedef icin)
     * @param lowHp     true: en dusuk cani olan, false: en yakin
     * @param needSight true: duvar arkasindakileri atla
     */
    private static LivingEntity pick(MinecraftClient mc, ClientPlayerEntity p, double range, int mode,
                                     boolean reach, boolean lowHp, boolean needSight) {
        Box box = p.getBoundingBox().expand(range + 2.0);
        double limit = range * range;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        float bestHp = Float.MAX_VALUE;

        for (LivingEntity t : mc.world.getEntitiesByClass(LivingEntity.class, box, e -> valid(e, p, mode))) {
            double d = reach ? reachSq(p, t) : p.squaredDistanceTo(t);
            if (d > limit) continue;

            boolean better;
            if (lowHp) {
                float hp = t.getHealth();
                better = hp < bestHp || (hp == bestHp && d < bestDist);
            } else {
                better = d < bestDist;
            }
            if (!better) continue;
            if (needSight && !visible(mc, p, t)) continue;

            best = t;
            bestDist = d;
            bestHp = t.getHealth();
        }
        return best;
    }

    /** {yaw, pitch} - oyuncunun gozunden hedef noktaya bakis acisi. */
    private static float[] angles(ClientPlayerEntity p, Vec3d to) {
        Vec3d from = p.getEyePos();
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    /** Havadaysa ve dusuyorsa vurus kritik olur. */
    private static boolean critReady(ClientPlayerEntity p) {
        return !p.isOnGround() && p.getVelocity().y < -0.08 && !p.isClimbing()
                && !p.isTouchingWater() && !p.hasVehicle()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    private static boolean cooldownReady(ClientPlayerEntity p, Module m) {
        return p.getAttackCooldownProgress(0f) >= (float) (m.num("Cooldown %") / 100.0);
    }

    private static void hit(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t) {
        // Oyun kosarken kritik saymaz. Sunucunun sprint durumu paketle gelir, bu yuzden vurmadan once
        // "kosmayi birak", vurduktan sonra "kosmaya basla" paketi gonderilir (kosma kesilmez).
        boolean resprint = critNext && p.isSprinting() && mc.getNetworkHandler() != null;
        if (resprint) {
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                    p, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        if (resprint) {
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                    p, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        critNext = false;
        hudTarget = t;
        hudTargetTick = ticks;
        Extras.onHit(mc, t);
    }

    /** Hotbar'dan (yoksa sol elden) esyayi kullanir, sonra eski slota doner. */
    private static boolean useFromHotbar(MinecraftClient mc, ClientPlayerEntity p, Item item) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (!inv.getStack(i).isOf(item)) continue;
            int prev = inv.getSelectedSlot();
            inv.setSelectedSlot(i);
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            p.swingHand(Hand.MAIN_HAND);
            inv.setSelectedSlot(prev);
            return true;
        }
        if (p.getOffHandStack().isOf(item)) {
            mc.interactionManager.interactItem(p, Hand.OFF_HAND);
            p.swingHand(Hand.OFF_HAND);
            return true;
        }
        return false;
    }

    /** Baska siniflar icin: hotbar'dan / sol elden havai fisek kullanir. */
    public static boolean rocket(MinecraftClient mc, ClientPlayerEntity p) {
        return useFromHotbar(mc, p, Items.FIREWORK_ROCKET);
    }

    private static int findHotbar(PlayerInventory inv, Item a, Item b) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(a) || s.isOf(b)) return i;
        }
        return -1;
    }

    // ==================================================================
    //  COMBAT
    // ==================================================================

    // ---- AttackAura: hedefi bul -> (istenirse) ona don -> cooldown/crit hazirsa vur.
    //      Aim range, vurus menzilinden (Range) genis: hedefe once doner, menzile girince vurur.
    private static void attackAura(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.AURA;
        critNext = false;
        if (m.bool("Pause on use") && p.isUsingItem()) return;
        if (m.bool("Stop in GUI") && mc.currentScreen != null) return;

        double range = Math.min(m.num("Range"), HIT_REACH);   // sunucu siniri ~4: kenarda lag yuzunden bos vurmasin
        double aim = Math.max(range, m.num("Aim range"));
        // Auto jump: hedef menzildeyken yerden kalkar (her ziplamada kritik firsati)
        if (!m.bool("Auto jump") && autoJumpHeld) {
            mc.options.jumpKey.setPressed(false);
            autoJumpHeld = false;
        }

        int hold = (int) m.num("Switch delay (tick)");
        LivingEntity t;
        if (hold > 0 && auraLock != null && auraLock.isAlive() && ticks - auraLockTick < hold
                && reachSq(p, auraLock) <= aim * aim) {
            t = auraLock;                                   // gecikme dolana kadar ayni hedefte kal
        } else {
            t = auraPick(mc, p, aim, m);
            if (t != auraLock) { auraLock = t; auraLockTick = ticks; }
        }
        if (t == null) {
            Rotations.clearTarget();
            if (autoJumpHeld) { mc.options.jumpKey.setPressed(false); autoJumpHeld = false; }
            return;
        }
        if (m.bool("Auto jump")) {
            boolean near = reachSq(p, t) <= (range + 0.5) * (range + 0.5);
            // cooldown ~%50'yi gectiyse zipla: dusmeye basladiginda cooldown dolmus olur, vurus kritik olur
            boolean ready = p.getAttackCooldownProgress(0f) >= 0.5f;
            if (near && ready && p.isOnGround()) { mc.options.jumpKey.setPressed(true); autoJumpHeld = true; }
            else if (autoJumpHeld) { mc.options.jumpKey.setPressed(false); autoJumpHeld = false; }
        }
        hudTarget = t;                                      // TargetHUD / TargetESP
        hudTargetTick = ticks;

        // Celestial rotation: hedef kutuda gorunen en yakin nokta + secili strateji.
        // Gorunur modlarda kamera cevrilir; Silent (5) sadece vurus aninda yazar.
        int rot = m.index("Rotate");
        Rotations.Aim rotAim = null;
        if (rot > Rotations.OFF) {
            rotAim = Rotations.aim(mc, p, t, m, rot);
            if (rotAim == null) return;
            if (rot != Rotations.SILENT) {
                p.setYaw(rotAim.yaw);
                p.setPitch(rotAim.pitch);
            }
            if (!rotAim.ready) return;                       // henuz hedefe bakmiyor
        }

        if (reachSq(p, t) > range * range) return;           // sadece hedefe donuyor, menzile girince vurur
        if (!cooldownReady(p, m)) return;

        // Kritik: havadaysan dusmeye baslayana kadar bekler. Sunucu sprint'teyken kritik saymaz,
        // bu yuzden havadayken sprint kapatilir (AutoSprint de o tick'te karismaz).
        boolean airborne = !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater() && !p.hasVehicle();
        if (airborne && (m.bool("Crit jump") || m.bool("Crits only"))) {
            if (!critReady(p)) return;                       // yukselirken bekle, dusmeye baslayinca vur
        }
        if (m.bool("Crits only") && !critReady(p)) return;
        critNext = airborne && critReady(p);
        if (m.bool("Space only") && !mc.options.jumpKey.isPressed()) return;
        if (m.bool("Weapon only")) {
            var hand = p.getMainHandStack();
            if (!(hand.isIn(ItemTags.SWORDS) || hand.isIn(ItemTags.AXES) || hand.isOf(Items.MACE) || hand.isOf(Items.TRIDENT))) return;
        }

        // Silent: sadece vurus aninda hedefe bakar, hemen eski bakisa doner (kamera / F5 serbest kalir)
        float savedYaw = p.getYaw(), savedPitch = p.getPitch();
        boolean silentRot = rot == Rotations.SILENT && rotAim != null;
        if (silentRot) {
            p.setYaw(rotAim.yaw);
            p.setPitch(rotAim.pitch);
        }

        int aw = m.index("Auto weapon");
        if (aw > 0) {
            PlayerInventory winv = p.getInventory();
            var cur = p.getMainHandStack();
            boolean ok = aw == 1 ? cur.isIn(ItemTags.SWORDS) : cur.isIn(ItemTags.AXES);
            if (!ok) {
                for (int i = 0; i < 9; i++) {
                    var st = winv.getStack(i);
                    if (aw == 1 ? st.isIn(ItemTags.SWORDS) : st.isIn(ItemTags.AXES)) { winv.setSelectedSlot(i); break; }
                }
            }
        }

        // Sunucu da hedefe bakildigini gorsun: vurmadan once gercek bakis acisini paketle bildir.
        // (Silent modda kamera oynamaz, ama sunucuya giden aci hedefe dogru olur.)
        if (rot != 0 && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(
                    p.getYaw(), p.getPitch(), p.isOnGround(), p.horizontalCollision));
        }

        // Hedef kalkan tutuyorsa hotbar'daki baltayla kirar, sonra eski slota doner
        boolean done = false;
        if (m.bool("Break shield") && t instanceof PlayerEntity pl && pl.isBlocking()) {
            PlayerInventory inv = p.getInventory();
            for (int i = 0; i < 9; i++) {
                if (inv.getStack(i).isIn(ItemTags.AXES)) {
                    int prev = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    hit(mc, p, t);
                    inv.setSelectedSlot(prev);
                    done = true;
                    break;
                }
            }
        }
        if (!done) hit(mc, p, t);

        if (silentRot) {
            p.setYaw(savedYaw);
            p.setPitch(savedPitch);
        }
    }

    /** AttackAura hedef filtresi: oyuncular, hayvanlar, koylular ve "Ignore ..." ayarlari. */
    private static boolean auraOk(MinecraftClient mc, ClientPlayerEntity p, LivingEntity e, Module m,
                                  boolean players, int mode) {
        if (e == p || !e.isAlive()) return false;
        if (m.bool("Ignore invisible") && e.isInvisible()) return false;
        if (e instanceof PlayerEntity pl) {
            if (!players || pl.isSpectator()) return false;
            if (m.bool("Ignore creative") && pl.isCreative()) return false;
            if (m.bool("Ignore bots") && mc.getNetworkHandler() != null
                    && mc.getNetworkHandler().getPlayerListEntry(pl.getUuid()) == null) return false;
            if (m.bool("Ignore naked")) {
                boolean any = false;
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    if (!pl.getEquippedStack(s).isEmpty()) any = true;
                }
                if (!any) return false;
            }
            return true;
        }
        if (e instanceof MerchantEntity) return m.bool("Villagers");
        if (e instanceof net.minecraft.entity.passive.AnimalEntity && m.bool("Animals")) return true;
        return valid(e, p, mode);
    }

    /** AttackAura hedef secimi: Targets + Players, Sorting, FOV ve duvar arkasi kontrolu. */
    private static LivingEntity auraPick(MinecraftClient mc, ClientPlayerEntity p, double range, Module m) {
        boolean players = m.bool("Players");
        int mode = m.index("Targets");
        int sort = m.index("Sorting");
        double fov = m.num("FOV");
        boolean needSight = !m.bool("Through walls");

        Box box = p.getBoundingBox().expand(range + 2.0);
        double limit = range * range;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity t : mc.world.getEntitiesByClass(LivingEntity.class, box, e -> {
            return auraOk(mc, p, e, m, players, mode);
        })) {
            double d = reachSq(p, t);
            if (d > limit) continue;

            float[] a = angles(p, t.getBoundingBox().getCenter());
            float off = Math.abs(MathHelper.wrapDegrees(a[0] - p.getYaw()));
            if (fov < 360 && off > fov / 2.0) continue;

            double score;
            if (sort == 1) score = t.getHealth() * 1000.0 + d;
            else if (sort == 2) score = off * 1000.0 + d;
            else score = d;
            if (score >= bestScore) continue;
            if (needSight && !visible(mc, p, t)) continue;

            best = t;
            bestScore = score;
        }
        return best;
    }

    // ---- TriggerBot: nisangah bir hedefin ustundeyken vurur.
    private static void triggerBot(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.TRIGGER;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !valid(le, p, m.index("Targets"))) return;
        if (reachSq(p, le) > REACH_LIMIT * REACH_LIMIT) return;
        if (!cooldownReady(p, m)) return;
        if (m.bool("Crits only") && !critReady(p)) return;

        hit(mc, p, le);
    }

    // ---- AutoTotem: sol elde totem yoksa envanterden koyar. Envanter acikken de calisir.
    private static void autoTotem(MinecraftClient mc, ClientPlayerEntity p) {
        if (totemCd > 0) { totemCd--; return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        Module m = Modules.TOTEM;
        if (m.index("Mode") == 1 && p.getHealth() > (float) m.num("Health (hp)")) return;

        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int screenSlot = i < 9 ? i + 36 : i;   // hotbar 0-8 => 36-44
                // SWAP + buton 40 = sol el ile degistir
                mc.interactionManager.clickSlot(
                        p.playerScreenHandler.syncId, screenSlot, 40, SlotActionType.SWAP, p);
                totemCd = (int) m.num("Delay (tick)");
                return;
            }
        }
    }

    // ---- AutoPotion: can esiginin altinda healing splash atar (once sol el, sonra hotbar).
    private static void autoPotion(MinecraftClient mc, ClientPlayerEntity p) {
        if (potionCd > 0) { potionCd--; return; }
        if (p.getHealth() > (float) Modules.POTION.num("Health (hp)")) return;

        if (isHealingSplash(p.getOffHandStack())) {
            throwDown(mc, p, Hand.OFF_HAND, -1);
            potionCd = (int) Modules.POTION.num("Delay (tick)");
            return;
        }

        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (isHealingSplash(inv.getStack(i))) {
                throwDown(mc, p, Hand.MAIN_HAND, i);
                potionCd = (int) Modules.POTION.num("Delay (tick)");
                return;
            }
        }
    }

    /** hotbarSlot < 0 ise slot degistirmez (sol el). */
    private static void throwDown(MinecraftClient mc, ClientPlayerEntity p, Hand hand, int hotbarSlot) {
        PlayerInventory inv = p.getInventory();
        int prevSlot = inv.getSelectedSlot();
        float prevPitch = p.getPitch();

        if (hotbarSlot >= 0) inv.setSelectedSlot(hotbarSlot);
        p.setPitch(90f);                                  // asagi bak: potion ayaklarina dussun
        mc.interactionManager.interactItem(p, hand);
        p.swingHand(hand);
        if (hotbarSlot >= 0) inv.setSelectedSlot(prevSlot);
        p.setPitch(prevPitch);
    }

    private static boolean isHealingSplash(ItemStack s) {
        if (!s.isOf(Items.SPLASH_POTION)) return false;
        PotionContentsComponent c = s.get(DataComponentTypes.POTION_CONTENTS);
        if (c == null) return false;
        for (StatusEffectInstance e : c.getEffects()) {
            if (e.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) return true;
        }
        return false;
    }

    // ---- AutoGApple: can esigin altinda hotbar'daki altin elmayi yer, bitince eski slota doner.
    private static void autoGApple(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.AUTOGAPPLE;
        boolean low = p.getHealth() <= (float) m.num("Health (hp)");
        PlayerInventory inv = p.getInventory();

        if (!eating) {
            if (!low) return;
            int slot = findHotbar(inv, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
            if (slot < 0) return;
            eatPrevSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(slot);
            mc.options.useKey.setPressed(true);
            eating = true;
            eatTicks = 0;
            return;
        }

        eatTicks++;
        boolean finished = eatTicks > 3 && !p.isUsingItem(); // yeme bitti
        if (finished || eatTicks > 80) stopEating(mc, p);
    }

    private static void stopEating(MinecraftClient mc, ClientPlayerEntity p) {
        mc.options.useKey.setPressed(false);
        if (eatPrevSlot >= 0) p.getInventory().setSelectedSlot(eatPrevSlot);
        eatPrevSlot = -1;
        eating = false;
    }

    // ---- ClickPearl: bind tusuna basinca ender incisi atar.
    private static void clickPearl(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.CLICKPEARL;
        if (pearlCd > 0) pearlCd--;
        boolean pressed = m.consume();
        if (!pressed || !m.enabled || pearlCd > 0 || mc.currentScreen != null) return;

        if (useFromHotbar(mc, p, Items.ENDER_PEARL)) {
            pearlCd = (int) m.num("Cooldown (tick)");
        } else {
            p.sendMessage(Text.literal("Ender incisi bulunamadi (hotbar veya sol el)."), true);
        }
    }

    // ==================================================================
    //  MOVEMENT
    // ==================================================================

    // ---- AutoSprint
    private static void autoSprint(MinecraftClient mc, ClientPlayerEntity p) {
        if (ticks - noSprintTick <= 1) return; // aura kritik icin sprint'i kapatti
        if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                && !p.isUsingItem()
                && (p.getHungerManager().getFoodLevel() > 6 || p.getAbilities().allowFlying)) {
            p.setSprinting(true);
        }
    }

    // ---- Speed: yerdeki hizi kademeli artirir, sprint hizinin Multiplier kati ile sinirlar.
    private static void speed(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.SPEED;
        if (p.isGliding() || p.hasVehicle() || p.isClimbing() || p.isTouchingWater() || p.isInLava()) return;
        if (m.bool("Only on ground") && !p.isOnGround()) return;

        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (!moving) return;

        Vec3d v = p.getVelocity();
        double h = Math.hypot(v.x, v.z);
        if (h < 0.01) return;

        double cap = 0.2873 * m.num("Multiplier");
        double nh = Math.min(h * 1.15, cap);
        if (nh <= h) return;

        double f = nh / h;
        p.setVelocity(v.x * f, v.y, v.z * f);
    }

    // ---- ElytraTarget: ucarken en yakin hedefe oransal (yumusak) sekilde doner.
    //      Min distance'in icine girince birakir ki AttackAura isini yapsin.
    private static void elytraTarget(MinecraftClient mc, ClientPlayerEntity p) {
        if (!p.isGliding()) { elyLock = null; return; }
        Module m = Modules.ELYTRA;
        double range = m.num("Range");
        boolean walls = m.bool("Through walls");

        // Kilitli hedef menzildeyse onda kal (hedef degistirip titrememek icin)
        LivingEntity t = null;
        if (elyLock != null && elyLock.isAlive() && !elyLock.isRemoved()
                && p.squaredDistanceTo(elyLock) <= range * range && elyTargetOk(p, elyLock, m)) {
            t = elyLock;
        } else {
            int sort = m.index("Sorting");
            double bestScore = Double.MAX_VALUE;
            Box box = p.getBoundingBox().expand(range);
            for (PlayerEntity e : mc.world.getEntitiesByClass(PlayerEntity.class, box, x -> elyTargetOk(p, x, m))) {
                double d = p.squaredDistanceTo(e);
                if (d > range * range) continue;
                float[] a0 = angles(p, e.getBoundingBox().getCenter());
                double off = Math.abs(MathHelper.wrapDegrees(a0[0] - p.getYaw()));
                double score = sort == 1 ? e.getHealth() * 10000.0 + d : sort == 2 ? off * 10000.0 + d : d;
                if (score >= bestScore) continue;
                if (!walls && !visible(mc, p, e)) continue;
                bestScore = score;
                t = e;
            }
            elyLock = t;
        }
        if (t == null) return;
        hudTarget = t;                                         // TargetHUD / TargetESP
        hudTargetTick = ticks;

        double dist = Math.sqrt(p.squaredDistanceTo(t));
        if (dist < m.num("Min distance")) return;

        Vec3d aim = t.getBoundingBox().getCenter();
        if (m.bool("Lead target")) {                          // hedefin gidecegi yere dogru uc
            Vec3d v = velocity(t);
            double mySpeed = Math.max(0.6, p.getVelocity().length());
            double ahead = Math.min(dist / mySpeed, 12.0);
            Vec3d rel = v.subtract(p.getVelocity().multiply(0.0));
            aim = aim.add(rel.x * ahead * 0.85, rel.y * ahead * 0.5, rel.z * ahead * 0.85);
        }

        float[] a = angles(p, aim);
        float speed = (float) m.num("Speed (deg/tick)");
        float dYaw = MathHelper.wrapDegrees(a[0] - p.getYaw());
        float dPitch = a[1] - p.getPitch();

        // Fark buyukken hizli, kuculdukce yavas; cok kucukse hic oynatma (titremez)
        float k = dist < 8 ? 0.22f : 0.38f;                   // yakinda daha yumusak (hedefi gecip sallanmasin)
        float stepYaw = Math.abs(dYaw) < 0.5f ? 0f : MathHelper.clamp(dYaw * k, -speed, speed);
        float stepPitch = Math.abs(dPitch) < 0.5f ? 0f : MathHelper.clamp(dPitch * k, -speed, speed);
        p.setYaw(p.getYaw() + stepYaw);
        p.setPitch(MathHelper.clamp(p.getPitch() + stepPitch, -90f, 90f));
    }

    /** ElytraTarget sadece oyunculari hedefler. */
    private static boolean elyTargetOk(ClientPlayerEntity p, PlayerEntity e, Module m) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (m.bool("Ignore creative") && e.isCreative()) return false;
        return true;
    }

    private static boolean elyTargetOk(ClientPlayerEntity p, LivingEntity e, Module m) {
        return e instanceof PlayerEntity pl && elyTargetOk(p, pl, m);
    }

    // ---- FastBreak: bloklar arasi beklemeyi kaldirir, kirma ilerlemesini hizlandirir.
    //      Sunucu kirmayi gercek sureye gore dogrular (~%70 ilerleme ister), bu yuzden Speed en fazla 1.4.
    private static void fastBreak(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.FASTBREAK;
        if (!mc.options.attackKey.isPressed()) { fbAcc = 0; return; }
        if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        var im = mc.interactionManager;
        if (m.bool("No delay")) {
            ((com.example.spcombat.mixin.ClientPlayerInteractionManagerAccessor) im).spcSetBlockBreakingCooldown(0);
        }
        if (im.isBreakingBlock()) {
            fbAcc += m.num("Speed") - 1.0;
            while (fbAcc >= 1.0) {
                im.updateBlockBreakingProgress(bhr.getBlockPos(), bhr.getSide());
                fbAcc -= 1.0;
            }
        } else {
            fbAcc = 0;
        }
    }

    // ---- ElytraHelper: bind tusuna basinca (kilic elindeyken) fisek atar.
    //      Hold to repeat: tus basili kaldikca tekrarlar.
    private static void elytraHelper(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.ELYHELPER;
        if (helperCd > 0) helperCd--;

        boolean pressed = m.consume();
        if (m.bool("Hold to repeat") && m.bindDown) pressed = true;
        if (!pressed || !m.enabled || helperCd > 0 || mc.currentScreen != null) return;

        if (m.bool("Only while gliding") && !p.isGliding()) return;
        if (m.bool("Require sword") && !p.getMainHandStack().isIn(ItemTags.SWORDS)) return;

        if (useFromHotbar(mc, p, Items.FIREWORK_ROCKET)) {
            helperCd = (int) m.num("Cooldown (tick)");
        } else {
            p.sendMessage(Text.literal("Fisek bulunamadi (hotbar veya sol el)."), true);
        }
    }

    // ---- ElytraBooster: hiz esigin altina dusunce kendisi fisek atar.
    private static void elytraBooster(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.ELYBOOSTER;
        if (boosterCd > 0) { boosterCd--; return; }
        if (!p.isGliding()) return;
        if (p.getVelocity().length() * 20.0 >= m.num("Min speed (b/s)")) return;

        if (m.bool("Only with target")) {
            // sadece ElytraTarget bir oyuncuya kilitliyken ve hedef yeterince uzaktayken
            if (elyLock == null || !elyLock.isAlive()) return;
            if (Math.sqrt(p.squaredDistanceTo(elyLock)) < m.num("Min target dist")) return;
        }

        if (useFromHotbar(mc, p, Items.FIREWORK_ROCKET)) {
            boosterCd = (int) m.num("Cooldown (tick)");
        } else {
            boosterCd = 40; // fisek yok: surekli deneme
            p.sendMessage(Text.literal("Fisek bulunamadi (hotbar veya sol el)."), true);
        }
    }

    // ---- AirStuck
    //      Freeze: acildigi konumda dondurur. Hover: dusmeyi durdurur, yatay hareket serbest.
    private static void airStuck(ClientPlayerEntity p) {
        Module m = Modules.AIRSTUCK;
        boolean onlyAir = m.bool("Only in air");

        if (m.index("Mode") == 1) {                       // Hover
            frozen = null;
            if (onlyAir && p.isOnGround()) return;
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.0, v.z);
            return;
        }

        if (frozen == null) {                             // Freeze
            if (onlyAir && p.isOnGround()) return;
            frozen = new Vec3d(p.getX(), p.getY(), p.getZ());
        }
        p.setPosition(frozen.x, frozen.y, frozen.z);
        p.setVelocity(Vec3d.ZERO);
    }

    // ---- GuiMove: envanter/sandik acikken W A S D + Space ile yurumeye devam et.
    //      (Varsayilan tuslar okunur; yazi kutulu ekranlarda calismaz.)
    private static void guiMove(MinecraftClient mc) {
        Screen s = mc.currentScreen;
        if (s == null) return;
        if (!(s instanceof HandledScreen) || s instanceof AnvilScreen || s instanceof CreativeInventoryScreen) return;

        long w = mc.getWindow().getHandle();
        mc.options.forwardKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS);
        mc.options.backKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS);
        mc.options.leftKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS);
        mc.options.rightKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);
        mc.options.jumpKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
    }

    // ---- FullBright: istemci tarafinda gece gorusu efekti.
    private static void fullbright(ClientPlayerEntity p) {
        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || cur.getDuration() < 400) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        }
        nightVisionApplied = true;
    }

    // ==================================================================
    //  PLAYER
    // ==================================================================

    // ---- AutoTool: blok kirarken o blok icin en hizli aleti secer, birakinca eski slota doner.
    private static void autoTool(MinecraftClient mc, ClientPlayerEntity p) {
        PlayerInventory inv = p.getInventory();

        if (mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult b
                && b.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.world.getBlockState(b.getBlockPos());
            int cur = inv.getSelectedSlot();
            int best = cur;
            float bestSpeed = inv.getStack(cur).getMiningSpeedMultiplier(st);
            for (int i = 0; i < 9; i++) {
                float sp = inv.getStack(i).getMiningSpeedMultiplier(st);
                if (sp > bestSpeed + 0.01f) {
                    bestSpeed = sp;
                    best = i;
                }
            }
            if (best != cur) {
                if (toolPrev < 0) toolPrev = cur;
                inv.setSelectedSlot(best);
            }
        } else if (toolPrev >= 0) {
            inv.setSelectedSlot(toolPrev);
            toolPrev = -1;
        }
    }

    // ---- AutoArmor: envanterdeki daha iyi zirhi giyer (tier'e gore: netherite > elmas > demir ...).
    private static final String[] ARMOR_SUFFIX = {"_helmet", "_chestplate", "_leggings", "_boots"};
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static int armorRank(ItemStack s, int part) {
        if (s.isEmpty()) return 0;
        String path = Registries.ITEM.getId(s.getItem()).getPath();
        if (!path.endsWith(ARMOR_SUFFIX[part])) return -1;
        if (path.startsWith("netherite_")) return 6;
        if (path.startsWith("diamond_")) return 5;
        if (path.startsWith("iron_")) return 4;
        if (path.startsWith("chainmail_")) return 3;
        if (path.startsWith("golden_") || path.startsWith("turtle_")) return 2;
        if (path.startsWith("leather_")) return 1;
        return 1;
    }

    private static void autoArmor(MinecraftClient mc, ClientPlayerEntity p) {
        if (armorCd > 0) { armorCd--; return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        PlayerInventory inv = p.getInventory();
        for (int part = 0; part < 4; part++) {
            ItemStack worn = p.getEquippedStack(ARMOR_SLOTS[part]);
            int curRank = armorRank(worn, part);
            if (curRank < 0) continue; // zirh slotunda baska bir sey var (elytra, balkabagi): dokunma

            int bestInv = -1, bestRank = curRank;
            for (int i = 0; i < 36; i++) {
                int r = armorRank(inv.getStack(i), part);
                if (r > bestRank) {
                    bestRank = r;
                    bestInv = i;
                }
            }
            if (bestInv < 0) continue;

            int sync = p.playerScreenHandler.syncId;
            int slot = bestInv < 9 ? bestInv + 36 : bestInv;
            int armorSlot = 5 + part; // 5 basliik, 6 govde, 7 pantolon, 8 bot
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);      // yeni zirhi al
            mc.interactionManager.clickSlot(sync, armorSlot, 0, SlotActionType.PICKUP, p); // giy (eskisi imlece gelir)
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);      // eskisini yerine koy
            armorCd = (int) Modules.AUTOARMOR.num("Delay (tick)");
            return;
        }
    }

    // ---- ChestStealer: sandik acilinca icindekileri tek tek alir.
    private static void chestStealer(MinecraftClient mc, ClientPlayerEntity p) {
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            stealStarted = false;
            return;
        }
        if (stealCd > 0) { stealCd--; return; }

        Module m = Modules.CHESTSTEALER;
        GenericContainerScreenHandler h = gcs.getScreenHandler();
        int n = h.getRows() * 9;
        for (int i = 0; i < n; i++) {
            if (!h.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                stealStarted = true;
                stealCd = (int) m.num("Delay (tick)");
                return;
            }
        }
        // sandik bosaldi
        if (stealStarted && m.bool("Auto close")) {
            p.closeHandledScreen();
            stealStarted = false;
        }
    }

    // ==================================================================
    //  MISC
    // ==================================================================

    // ---- Scaffold: ayagin altindaki bos yere hotbar'daki bir blok koyar.
    private static void scaffold(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isGliding() || p.hasVehicle() || p.getAbilities().flying) return;

        BlockPos target = BlockPos.ofFloored(p.getX(), p.getY() - 1.0, p.getZ());
        if (!mc.world.getBlockState(target).isAir()) return;

        // Yerlestirmek icin yaninda/altinda dolu bir komsu blok bul
        Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        BlockPos support = null;
        Direction face = null;
        for (Direction d : dirs) {
            BlockPos n = target.offset(d);
            BlockState st = mc.world.getBlockState(n);
            if (!st.isAir() && st.getFluidState().isEmpty()) {
                support = n;
                face = d.getOpposite();
                break;
            }
        }
        if (support == null) return;

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() instanceof BlockItem bi
                    && bi.getBlock().getDefaultState().isSolidBlock(mc.world, target)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;

        int prev = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        Vec3d hitPos = Vec3d.ofCenter(support).add(
                face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, support, false));
        p.swingHand(Hand.MAIN_HAND);
        if (Modules.SCAFFOLD.bool("Switch back")) inv.setSelectedSlot(prev);
    }
}

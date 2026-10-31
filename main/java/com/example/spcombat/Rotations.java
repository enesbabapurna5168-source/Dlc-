package com.example.spcombat;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Celestial AttackAura rotation portu.
 *
 * <p>Zip icindeki dort strategy'nin davranisi:
 * <ul>
 *   <li><b>Matrix</b> (C4734_rrCr): yaw'a zaman tabanli sinus (~3 derece), pitch'e kosinus
 *       (~0.5 derece) ve kucuk random offset. Yaw adimi 52, pitch adimi 5 derece.</li>
 *   <li><b>Snap</b> (C4713_rrCP + C4701_rrCB): anlik donus, kamera acisini korur ve
 *       kucuk (+-0.5 derece) noise ekler.</li>
 *   <li><b>Funtime</b> (C6106_rrna + C6112_rrni): hedef kutusunda pitch'e en yakin noktayi
 *       secer, hareketten turetilen jitter ekler ve delta buyuklugune gore yumusatir.</li>
 *   <li><b>SpookyTime</b> (C4735_rrCs): ease curve + interpolasyon, yumusak insanimsı
 *       donus ve soft-block (snow, sign, slab, fence...) ray filter'i.</li>
 * </ul>
 *
 * <p>Ayrica Celestial'in PRE/POST mantigi gibi calisan <b>Silent</b> modu vardir: kamera
 * hic oynamaz, aci yalnizca vurus aninda yazilip hemen geri alinir. Silent mod Funtime
 * algoritmasini kullanir (Celestial'in varsayilan saldiri modu).
 */
public final class Rotations {
    private Rotations() {}

    // Rotate listesindeki indeksler (Modules.AURA)
    public static final int OFF = 0;
    public static final int MATRIX = 1;
    public static final int SNAP = 2;
    public static final int FUNTIME = 3;
    public static final int SPOOKYTIME = 4;
    public static final int SILENT = 5;

    /** SpookyTime'in ray trace'te engel saymadigi "ince" bloklar (C4735_rrCs). */
    private static final Pattern SOFT_BLOCK = Pattern.compile(
            ".*(snow|banner|sign|step|frame|conduit|bamboo|anvil|grindstone|stonecutter|lantern|"
            + "campfire|daylight_detector|lectern|brewing_stand|cauldron|table|slab|stair|chest|"
            + "ladder|vine|waterlily|wall|chain|carpet|pane|fence|gate|rod|bed|skull|pot|hopper|"
            + "door|bars|piston|lily|plate|farm|path).*");

    /** Bir tick'te en fazla bu kadar hedef noktasi denenir (ray trace maliyeti icin). */
    private static final int MAX_POINTS = 96;

    private static final Random RNG = new Random();

    // ---- hedefe ait interpolation state ----
    private static int targetId = -1;
    private static int mode = OFF;
    private static float yaw;
    private static float pitch;
    private static int aimTicks;
    private static float yawNoise;
    private static float pitchNoise;

    /** Hesap sonucu: uygulanacak aci ve hedefe yeterince bakiliyor mu? */
    public static final class Aim {
        public final float yaw;
        public final float pitch;
        public final boolean ready;

        Aim(float yaw, float pitch, boolean ready) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.ready = ready;
        }
    }

    /** Hedef/mod degisti ya da modul kapandi: interpolation state'ini sifirla. */
    public static void reset() {
        targetId = -1;
        mode = OFF;
        yaw = 0f;
        pitch = 0f;
        aimTicks = 0;
        yawNoise = 0f;
        pitchNoise = 0f;
    }

    /** Hedef kayboldu: ayni hedefe tekrar kilitlenince state temiz baslasin. */
    public static void clearTarget() {
        targetId = -1;
        aimTicks = 0;
        yawNoise = 0f;
        pitchNoise = 0f;
    }

    /**
     * Hedef icin bu tick'in rotation acisini hesaplar.
     * Donen aci oyuncuya uygulanmaz; cagiran taraf gorunur/silent moda gore yazar.
     */
    public static Aim aim(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t, Module m, int modeIndex) {
        if (t == null || modeIndex <= OFF) return null;

        if (t.getId() != targetId || modeIndex != mode) {
            targetId = t.getId();
            mode = modeIndex;
            yaw = p.getYaw();
            pitch = p.getPitch();
            aimTicks = 0;
            yawNoise = 0f;
            pitchNoise = 0f;
        }

        Vec3d point = pickPoint(mc, p, t, m, modeIndex);

        // Predict: hedefin hareket yonune 2 tick onden bak
        if (m.bool("Predict")) {
            Vec3d v = t.getVelocity();
            point = point.add(v.x * 2.0, 0.0, v.z * 2.0);
        }

        float[] a = angles(p, point);
        float baseYaw = a[0];
        float basePitch = a[1];
        float speed = (float) m.num("Smooth speed");
        boolean jitter = m.bool("Jitter");

        switch (modeIndex) {
            case MATRIX: {
                // C4734_rrCr: sin/cos salinimi + kucuk random, 52 / 5 derece adim
                float t0 = (SpCombatClient.ticks + aimTicks) * 0.55f;
                float jY = (float) Math.sin(t0) * 3.0f;
                float jP = (float) Math.cos(t0) * 0.5f;
                if (jitter) {
                    jY += rnd(-0.5f, 0.5f);
                    jP += rnd(-0.5f, 0.5f);
                }
                yaw += clamp(MathHelper.wrapDegrees(baseYaw + jY - yaw), -52f, 52f);
                pitch += clamp(basePitch + jP - pitch, -5f, 5f);
                break;
            }
            case SNAP: {
                // C4713_rrCP + C4701_rrCB: anlik donus, +-0.5 derece noise
                float nY = jitter ? rnd(-0.5f, 0.5f) : 0f;
                float nP = jitter ? rnd(-0.5f, 0.5f) : 0f;
                yaw = baseYaw + nY;
                pitch = basePitch + nP;
                break;
            }
            case FUNTIME:
            case SILENT: {
                // C6106_rrna + C6112_rrni: hareketten jitter, delta'ya bagli yumusatma
                float dY = MathHelper.wrapDegrees(baseYaw - yaw);
                float dP = basePitch - pitch;
                if (jitter) {
                    Vec3d tv = t.getVelocity();
                    Vec3d pv = p.getVelocity();
                    float mY = (float) ((tv.x - pv.x) * 7.0 + (tv.z - pv.z) * 7.0);
                    float mP = (float) ((tv.y - pv.y) * 10.0);
                    float wantY = clamp(mY, -1.2f, 1.2f) + rnd(-0.35f, 0.35f);
                    float wantP = clamp(mP, -0.8f, 0.8f) + rnd(-0.2f, 0.2f);
                    yawNoise += (wantY - yawNoise) * 0.35f;
                    pitchNoise += (wantP - pitchNoise) * 0.35f;
                    dY += yawNoise;
                    dP += pitchNoise;
                }
                float ease = clamp(speed / 50f, 0.15f, 1.0f);
                float maxY = Math.max(2.0f, speed);
                float stepY = clamp(Math.abs(dY) * 0.30f * ease + 1.2f, 1.2f, maxY);
                float stepP = clamp(Math.abs(dP) * 0.30f * ease + 0.8f, 0.8f, Math.max(2.0f, speed * 0.5f));
                yaw += clamp(dY, -stepY, stepY);
                pitch = clamp(pitch + clamp(dP, -stepP, stepP), -90f, 90f);
                break;
            }
            case SPOOKYTIME: {
                // C4735_rrCs: ease curve + interpolasyon + random varyans
                float dY = MathHelper.wrapDegrees(baseYaw - yaw);
                float dP = basePitch - pitch;
                float duration = clamp(18f - speed * 0.2f, 5f, 17f);
                float progress = clamp(aimTicks / duration, 0f, 1f);
                float curve = easeOutBack(progress);
                float baseFrac = clamp(speed / 60f, 0.10f, 0.85f);
                float frac = clamp(baseFrac * (0.45f + 0.55f * curve), 0.06f, 0.90f);
                if (jitter) {
                    dY += rnd(-0.25f, 0.25f);
                    dP += rnd(-0.15f, 0.15f);
                }
                yaw += dY * frac;
                pitch = clamp(pitch + dP * frac, -90f, 90f);
                break;
            }
            default:
                break;
        }

        float dY = Math.abs(MathHelper.wrapDegrees(baseYaw - yaw));
        float dP = Math.abs(basePitch - pitch);
        boolean ready = dY < 5.0f && dP < 5.0f;

        aimTicks++;
        return new Aim(yaw, pitch, ready);
    }

    /**
     * Hedefin AABB'sini grid'e boler (genislik/8, yukseklik/12), gorunen ve mevcut
     * bakisa en yakin noktayi secer. Hicbir nokta gorunmuyorsa tum adaylar arasindan
     * en yakinini (Celestial fallback) dondurur.
     */
    private static Vec3d pickPoint(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t,
                                   Module m, int modeIndex) {
        Box b = t.getBoundingBox();
        Vec3d center = b.getCenter();
        if (!m.bool("Aim at hitbox")) return center;

        boolean needSight = !m.bool("Through walls");
        boolean soft = modeIndex == SPOOKYTIME;

        double w = b.maxX - b.minX;
        double h = b.maxY - b.minY;
        double d = b.maxZ - b.minZ;
        double stepXZ = Math.max(0.06, Math.min(w, d) / 8.0);
        double stepY = Math.max(0.06, h / 12.0);

        List<Vec3d> pts = new ArrayList<>();
        double eye = p.getEyeY();
        for (double yy = b.minY; yy <= b.maxY + 1e-6; yy += stepY) {
            // goz seviyesinin cok altindaki noktalari ele (Celestial: kucuk tolerans)
            if (yy < eye - 0.35) continue;
            for (double xx = b.minX; xx <= b.maxX + 1e-6; xx += stepXZ) {
                pts.add(new Vec3d(xx, yy, b.minZ));
                pts.add(new Vec3d(xx, yy, b.maxZ));
            }
            for (double zz = b.minZ; zz <= b.maxZ + 1e-6; zz += stepXZ) {
                pts.add(new Vec3d(b.minX, yy, zz));
                pts.add(new Vec3d(b.maxX, yy, zz));
            }
        }
        if (pts.isEmpty()) return center;

        List<Vec3d> cand = pts.size() <= MAX_POINTS ? pts : stride(pts, MAX_POINTS);

        // Skor: mevcut aim acisina birlesik uzaklik; Funtime/Silent sadece pitch'e bakar.
        final boolean pitchOnly = modeIndex == FUNTIME || modeIndex == SILENT;
        final float curYaw = yaw;
        final float curPitch = pitch;
        cand.sort(Comparator.comparingDouble(pt -> {
            float[] a = angles(p, pt);
            if (pitchOnly) return Math.abs(a[1] - curPitch);
            return Math.abs(MathHelper.wrapDegrees(a[0] - curYaw)) + Math.abs(a[1] - curPitch);
        }));

        if (needSight) {
            for (Vec3d pt : cand) {
                if (clearLine(mc, p, pt, soft)) return pt;
            }
        }
        return cand.get(0); // gorunen yoksa fallback
    }

    /** Esit araliklarla en fazla n nokta sec. */
    private static List<Vec3d> stride(List<Vec3d> pts, int n) {
        List<Vec3d> out = new ArrayList<>(n);
        double step = (double) pts.size() / n;
        for (int i = 0; i < n; i++) out.add(pts.get((int) (i * step)));
        return out;
    }

    /** Gozden noktaya blok engeli var mi? SpookyTime soft bloklari engel saymaz. */
    private static boolean clearLine(MinecraftClient mc, ClientPlayerEntity p, Vec3d to, boolean soft) {
        if (mc.world == null) return true;
        BlockHitResult r = mc.world.raycast(new RaycastContext(
                p.getEyePos(), to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        if (r.getType() == HitResult.Type.MISS) return true;
        if (soft) {
            BlockState st = mc.world.getBlockState(r.getBlockPos());
            String id = Registries.BLOCK.getId(st.getBlock()).getPath();
            if (SOFT_BLOCK.matcher(id).matches()) return true;
        }
        return false;
    }

    /** {yaw, pitch} - oyuncunun gozunden hedef noktaya bakis acisi (C6092_rrnK). */
    private static float[] angles(ClientPlayerEntity p, Vec3d to) {
        Vec3d from = p.getEyePos();
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float y = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float px = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{y, MathHelper.clamp(px, -90f, 90f)};
    }

    /** C4735_rrCs'teki cubic ease-out-back egrisi. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private static float rnd(float min, float max) {
        return min + RNG.nextFloat() * (max - min);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }
}

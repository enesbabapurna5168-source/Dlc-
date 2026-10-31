package com.example.spcombat;

import com.example.spcombat.Module.Category;

import java.util.List;

public final class Modules {
    private Modules() {}

    // =========================== COMBAT ===========================
    // Menzil: sunucu etkilesim siniri yaklasik 4 blok, bu yuzden max 4.
    public static final Module AURA = new Module("AttackAura", Category.COMBAT,
            Setting.num("Range", 3.4, 1, 4.0, 0.1),
            Setting.num("Aim range", 4.0, 1, 8.0, 0.5),
            Setting.list("Targets", 0, "Monsters", "Mobs", "All"),
            Setting.bool("Players", true),
            Setting.list("Sorting", 0, "Closest", "Lowest HP", "FOV"),
            Setting.num("FOV", 360, 30, 360, 10),
            // Celestial rotation stratejileri: Matrix, Snap, Funtime, SpookyTime + Silent
            Setting.list("Rotate", 3, "Off", "Matrix", "Snap", "Funtime", "SpookyTime", "Silent"),
            Setting.num("Smooth speed", 25, 5, 60, 5),
            Setting.bool("Jitter", true),
            Setting.bool("Aim at hitbox", true),
            Setting.bool("Predict", true),
            Setting.num("Switch delay (tick)", 2, 0, 20, 1),
            Setting.bool("Through walls", false),
            Setting.bool("Break shield", true),
            Setting.num("Cooldown %", 100, 50, 100, 5),
            Setting.bool("Crit jump", true),
            Setting.bool("Auto jump", true),
            Setting.bool("Crits only", false),
            Setting.bool("Space only", false),
            Setting.bool("Weapon only", false),
            Setting.list("Auto weapon", 0, "Off", "Sword", "Axe"),
            Setting.bool("Animals", false),
            Setting.bool("Villagers", false),
            Setting.bool("Ignore invisible", false),
            Setting.bool("Ignore naked", false),
            Setting.bool("Ignore bots", true),
            Setting.bool("Ignore creative", true),
            Setting.bool("Pause on use", true),
            Setting.bool("Stop in GUI", true));

    public static final Module TRIGGER = new Module("TriggerBot", Category.COMBAT,
            Setting.list("Targets", 0, "Monsters", "Mobs", "All"),
            Setting.num("Cooldown %", 100, 50, 100, 5),
            Setting.bool("Crits only", false));

    public static final Module TOTEM = new Module("AutoTotem", Category.COMBAT,
            Setting.list("Mode", 0, "Always", "Below HP"),
            Setting.num("Health (hp)", 10, 2, 20, 1),
            Setting.num("Delay (tick)", 1, 0, 20, 1));

    public static final Module POTION = new Module("AutoPotion", Category.COMBAT,
            Setting.num("Health (hp)", 8, 2, 18, 1),
            Setting.num("Delay (tick)", 10, 0, 40, 1));

    /** Can azalinca hotbar'daki altin elmayi yer, bitince eski slota doner. */
    public static final Module AUTOGAPPLE = new Module("AutoGApple", Category.COMBAT,
            Setting.num("Health (hp)", 10, 2, 19, 1));

    /** Bind tusuna basinca hotbar'daki ender incisini atar. */
    public static final Module CLICKPEARL = new Module("ClickPearl", Category.COMBAT,
            Setting.num("Cooldown (tick)", 20, 0, 100, 1)).on().action();

    // =========================== MOVEMENT ===========================
    public static final Module AUTOSPRINT = new Module("AutoSprint", Category.MOVEMENT);

    public static final Module SPEED = new Module("Speed", Category.MOVEMENT,
            Setting.num("Multiplier", 1.3, 1.0, 3.0, 0.1),
            Setting.bool("Only on ground", true));

    /** Ucarken en yakin OYUNCUYA dogru doner (sadece oyuncular hedeflenir). */
    public static final Module ELYTRA = new Module("ElytraTarget", Category.MOVEMENT,
            Setting.num("Range", 60, 10, 120, 5),
            Setting.list("Sorting", 0, "Closest", "Lowest HP", "FOV"),
            Setting.bool("Lead target", true),
            Setting.num("Speed (deg/tick)", 20, 2, 45, 1),
            Setting.num("Min distance", 2, 0, 10, 1),
            Setting.bool("Ignore creative", true),
            Setting.bool("Through walls", true));

    /** TUSA BASINCA FISEK ATAR: Bind satirindan bir tus ata, ucarken o tusa bas. */
    public static final Module ELYHELPER = new Module("FireworkKey", Category.MOVEMENT,
            Setting.bool("Require sword", false),
            Setting.bool("Only while gliding", true),
            Setting.bool("Hold to repeat", true),
            Setting.num("Cooldown (tick)", 8, 0, 40, 1)).on().action();

    /** Hiz esigin altina dusunce kendisi fisek atar. */
    public static final Module ELYBOOSTER = new Module("ElytraBooster", Category.MOVEMENT,
            Setting.num("Min speed (b/s)", 25, 5, 60, 5),
            Setting.num("Cooldown (tick)", 25, 5, 60, 1),
            Setting.bool("Only with target", false),
            Setting.num("Min target dist", 10, 0, 60, 5));

    /** Freeze: oldugun yerde dondurur. Hover: sadece dusmeyi durdurur. */
    public static final Module AIRSTUCK = new Module("AirStuck", Category.MOVEMENT,
            Setting.list("Mode", 0, "Freeze", "Hover"),
            Setting.bool("Only in air", true));

    /** Knockback'i azaltir ya da tamamen iptal eder (0% = hic geri tepme). */
    public static final Module VELOCITY = new Module("Velocity", Category.MOVEMENT,
            Setting.num("Horizontal %", 0, 0, 100, 10),
            Setting.num("Vertical %", 0, 0, 100, 10));

    /** Envanter/sandik acikken yurumeye devam et (W A S D + Space). */
    public static final Module GUIMOVE = new Module("GuiMove", Category.MOVEMENT);

    // =========================== RENDER ===========================
    public static final Module FULLBRIGHT = new Module("FullBright", Category.RENDER);

    /** Gokyuzune yumusak renk tonu verir. */
    public static final Module AMBIENCE = new Module("Ambience", Category.RENDER,
            Setting.list("Style", 0, "Sunset", "Twilight", "Pastel"),
            Setting.num("Intensity %", 40, 10, 100, 10)).on().cosmetic();

    /** Vurdugunda guzel bir ses calar. */
    public static final Module CLIENTSOUND = new Module("ClientSound", Category.RENDER,
            Setting.list("Sound", 0, "Ding", "Orb", "Amethyst", "Level"),
            Setting.num("Volume %", 80, 10, 100, 10),
            Setting.num("Pitch %", 100, 50, 200, 10)).on().cosmetic();

    public static final Module HUD = new Module("HUD", Category.RENDER,
            Setting.bool("Watermark", true),
            Setting.bool("Module list", false),
            Setting.bool("Coords", true)).on().cosmetic();

    /** Ust ortada zirh dayaniklilik paneli. */
    public static final Module ARMORHUD = new Module("ArmorHUD", Category.RENDER).on().cosmetic();

    /** Saldirdigin hedefin can gostergesi. */
    public static final Module TARGETHUD = new Module("TargetHUD", Category.RENDER).on().cosmetic();

    /** Saldirdigin / aura'nin sectigi hedefi parlatir (oyunun kendi parlama efekti, FPS'e etkisi yok). */
    public static final Module TARGETESP = new Module("TargetESP", Category.RENDER,
            Setting.list("Mode", 0, "Ring", "Glow", "Both", "Ghost"),
            Setting.num("Ring size %", 100, 50, 200, 10)).on().cosmetic();

    /** Karaktere gercek elytra kanatlari cizer (sadece gorsel, envanter degismez). */
    public static final Module WINGS = new Module("Wings", Category.RENDER).cosmetic();

    /** Acik modullerin adini ve tuslarini ekranin kosesinde kucuk bir panelde gosterir. */
    public static final Module KEYBINDS = new Module("Keybinds", Category.RENDER,
            Setting.list("Side", 1, "Left", "Right"),
            Setting.bool("Show keys", true)).on().cosmetic();

    /** Vurus (el sallama) animasyonunu yavaslatip akici yapar. Sadece gorsel. */
    public static final Module SWING = new Module("SwingAnimation", Category.RENDER,
            Setting.num("Duration (tick)", 12, 6, 24, 1)).cosmetic();

    /** Keybinds panelinin altinda sevgi yazisi gosterir. */
    public static final Module BIRTANEM = new Module("Birtanem", Category.RENDER).cosmetic();

    /** Acikken GUI'nin altinda resimler gorunur. */
    public static final Module PHOTO = new Module("Photo", Category.RENDER,
            Setting.num("Size", 90, 40, 160, 10)).cosmetic();

    // =========================== PLAYER ===========================
    /** Blok kirmayi hizlandirir: bloklar arasi beklemeyi kaldirir, kirma ilerlemesini hizlandirir. */
    public static final Module FASTBREAK = new Module("FastBreak", Category.PLAYER,
            Setting.num("Speed", 1.4, 1.0, 1.4, 0.1),
            Setting.bool("No delay", true));

    /** Blok kirarken o blok icin en hizli aleti hotbar'dan secer. */
    public static final Module AUTOTOOL = new Module("AutoTool", Category.PLAYER);

    /** Envanterdeki daha iyi zirhi otomatik giyer. */
    public static final Module AUTOARMOR = new Module("AutoArmor", Category.PLAYER,
            Setting.num("Delay (tick)", 2, 0, 20, 1));

    /** Sandik acilinca icindekileri otomatik alir. */
    public static final Module CHESTSTEALER = new Module("ChestStealer", Category.PLAYER,
            Setting.num("Delay (tick)", 1, 0, 10, 1),
            Setting.bool("Auto close", true));

    // =========================== MISC ===========================
    /** Ayagin altindaki bos yere hotbar'daki blogu koyar. */
    public static final Module SCAFFOLD = new Module("Scaffold", Category.MISC,
            Setting.bool("Switch back", true));

    public static final Module AUTORESPAWN = new Module("AutoRespawn", Category.MISC);


    // =========================== EKLENENLER (ThunderHack'ten esinlenen) ===========================
    public static final Module ESP = new Module("ESP", Category.RENDER,
            Setting.list("Mode", 0, "Box", "Glow", "Both"),
            Setting.num("Range", 64, 10, 200, 10));
    public static final Module NAMETAGS = new Module("NameTags", Category.RENDER,
            Setting.num("Range", 64, 10, 200, 10),
            Setting.bool("Show HP", true),
            Setting.bool("Show distance", true));
    public static final Module TRACERS = new Module("Tracers", Category.RENDER,
            Setting.num("Range", 100, 10, 250, 10));
    public static final Module ITEMESP = new Module("ItemESP", Category.RENDER,
            Setting.num("Range", 32, 5, 100, 5));
    public static final Module HITPARTICLES = new Module("HitParticles", Category.RENDER,
            Setting.list("Type", 0, "Crit", "Heart", "Totem"),
            Setting.num("Amount", 12, 2, 40, 2)).cosmetic();
    public static final Module JUMPCIRCLE = new Module("JumpCircle", Category.RENDER,
            Setting.list("Type", 0, "End rod", "Cloud", "Enchant")).cosmetic();
    /** Birinci sahis el / esya konumu, boyutu ve acisi. */
    public static final Module VIEWMODEL = new Module("ViewModel", Category.RENDER,
            Setting.list("Hand", 0, "Both", "Main", "Off"),
            Setting.num("X", 0.0, -1.0, 1.0, 0.05),
            Setting.num("Y", 0.0, -1.0, 1.0, 0.05),
            Setting.num("Z", 0.0, -1.5, 0.5, 0.05),
            Setting.num("Scale %", 100, 30, 200, 5),
            Setting.num("Rotate X", 0, -180, 180, 5),
            Setting.num("Rotate Y", 0, -180, 180, 5),
            Setting.num("Rotate Z", 0, -180, 180, 5)).cosmetic();
    public static final Module KILLEFFECT = new Module("KillEffect", Category.RENDER).cosmetic();

    public static final Module AUTOEAT = new Module("AutoEat", Category.PLAYER,
            Setting.num("Hunger", 14, 1, 19, 1));
    public static final Module FASTUSE = new Module("FastUse", Category.PLAYER);
    public static final Module NOJUMPDELAY = new Module("NoJumpDelay", Category.PLAYER);

    public static final Module FLIGHT = new Module("Flight", Category.MOVEMENT,
            Setting.num("Speed", 1.0, 0.2, 5.0, 0.1));
    public static final Module STEP = new Module("Step", Category.MOVEMENT,
            Setting.num("Height", 1.5, 1.0, 2.5, 0.5));
    public static final Module NOFALL = new Module("NoFall", Category.MOVEMENT,
            Setting.num("Distance", 3, 2, 10, 1));
    public static final Module JESUS = new Module("Jesus", Category.MOVEMENT);
    public static final Module PARKOUR = new Module("Parkour", Category.MOVEMENT);
    public static final Module SAFEWALK = new Module("SafeWalk", Category.MOVEMENT);
    public static final Module SPIDER = new Module("Spider", Category.MOVEMENT);
    public static final Module ELYRECAST = new Module("ElytraRecast", Category.MOVEMENT);

    public static final Module VISUALRANGE = new Module("VisualRange", Category.MISC,
            Setting.num("Range", 64, 10, 200, 10));
    public static final Module AUTOEZ = new Module("AutoEZ", Category.MISC,
            Setting.list("Message", 0, "ez", "gg", "Menemen DLC"));


    // =========================== ONETAP'TEN ESINLENENLER ===========================
    public static final Module BLOCKOVERLAY = new Module("BlockOverlay", Category.RENDER).cosmetic();
    public static final Module FIREFLY = new Module("FireFly", Category.RENDER,
            Setting.list("Type", 0, "End rod", "Enchant", "Glow"),
            Setting.num("Amount", 2, 1, 8, 1)).cosmetic();
    public static final Module TRAILS = new Module("Trails", Category.RENDER,
            Setting.list("Type", 0, "Flame", "Cloud", "Soul"),
            Setting.num("Amount", 2, 1, 8, 1)).cosmetic();
    public static final Module FASTEXP = new Module("FastExp", Category.PLAYER);
    public static final Module TAPEMOUSE = new Module("TapeMouse", Category.PLAYER,
            Setting.list("Button", 0, "Left", "Right"),
            Setting.num("Delay (tick)", 10, 2, 40, 1));
    public static final Module ELYTAKEOFF = new Module("ElytraTakeoff", Category.MOVEMENT,
            Setting.bool("Auto firework", true),
            Setting.num("Firework delay (tick)", 15, 5, 60, 1));
    public static final Module PANIC = new Module("Panic", Category.MISC).on().action();
    public static final Module AUTOLEAVE = new Module("AutoLeave", Category.MISC,
            Setting.num("Health (hp)", 6, 1, 19, 1),
            Setting.bool("Player nearby", false),
            Setting.num("Range", 30, 5, 100, 5));
    public static final Module AUTOTPACCEPT = new Module("AutoTpaccept", Category.MISC);
    public static final Module DEATHCOORDS = new Module("DeathCoords", Category.MISC).on().cosmetic();
    public static final Module ANTIAFK = new Module("AntiAFK", Category.MISC,
            Setting.num("Interval (sec)", 30, 5, 120, 5));

    public static final List<Module> ALL = List.of(
            AURA, TRIGGER, TOTEM, POTION, AUTOGAPPLE, CLICKPEARL,
            AUTOSPRINT, SPEED, ELYTRA, ELYHELPER, ELYBOOSTER, AIRSTUCK, VELOCITY, GUIMOVE,
            FULLBRIGHT, AMBIENCE, CLIENTSOUND, HUD, ARMORHUD, TARGETHUD, KEYBINDS, PHOTO, TARGETESP, WINGS, SWING, BIRTANEM,
            AUTOTOOL, FASTBREAK, AUTOARMOR, CHESTSTEALER,
            SCAFFOLD, AUTORESPAWN,
            ESP, NAMETAGS, TRACERS, ITEMESP, HITPARTICLES, JUMPCIRCLE, KILLEFFECT, VIEWMODEL,
            AUTOEAT, FASTUSE, NOJUMPDELAY,
            FLIGHT, STEP, NOFALL, JESUS, PARKOUR, SAFEWALK, SPIDER, ELYRECAST,
            VISUALRANGE, AUTOEZ,
            BLOCKOVERLAY, FIREFLY, TRAILS, FASTEXP, TAPEMOUSE, ELYTAKEOFF,
            PANIC, AUTOLEAVE, AUTOTPACCEPT, DEATHCOORDS, ANTIAFK);

    public static List<Module> byCategory(Category c) {
        return ALL.stream().filter(m -> m.category == c).toList();
    }
}

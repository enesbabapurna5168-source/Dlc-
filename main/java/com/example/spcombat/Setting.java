package com.example.spcombat;

/** Bir modulun ayari: sayi (slider), acik/kapali veya secenek listesi. */
public class Setting {
    public enum Type { NUM, BOOL, ENUM }

    public final String name;
    public final Type type;

    public double num, min, max, step;
    public boolean bool;
    public String[] options;
    public int index;

    private Setting(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public static Setting num(String name, double value, double min, double max, double step) {
        Setting s = new Setting(name, Type.NUM);
        s.num = value; s.min = min; s.max = max; s.step = step;
        return s;
    }

    public static Setting bool(String name, boolean value) {
        Setting s = new Setting(name, Type.BOOL);
        s.bool = value;
        return s;
    }

    public static Setting list(String name, int index, String... options) {
        Setting s = new Setting(name, Type.ENUM);
        s.index = index;
        s.options = options;
        return s;
    }

    public String display() {
        switch (type) {
            case NUM:
                return step % 1 == 0 ? String.valueOf((int) Math.round(num)) : String.format("%.1f", num);
            case BOOL:
                return bool ? "ON" : "OFF";
            default:
                return options[index];
        }
    }

    /** Sol tik: sonraki / ac-kapat. Sag tik: onceki / ac-kapat. (NUM icin slider kullanilir.) */
    public void click(boolean left) {
        switch (type) {
            case NUM:
                num = left ? Math.min(max, num + step) : Math.max(min, num - step);
                break;
            case BOOL:
                bool = !bool;
                break;
            default:
                int n = options.length;
                index = left ? (index + 1) % n : (index - 1 + n) % n;
        }
    }

    // ---- slider ----
    public double fraction() {
        return max == min ? 0 : (num - min) / (max - min);
    }

    public void setFraction(double f) {
        f = Math.max(0.0, Math.min(1.0, f));
        double v = min + f * (max - min);
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        num = Math.max(min, Math.min(max, v));
    }

    // ---- kayit ----
    public String serialize() {
        switch (type) {
            case NUM:  return String.valueOf(num);
            case BOOL: return String.valueOf(bool);
            default:   return String.valueOf(index);
        }
    }

    public void deserialize(String s) {
        try {
            switch (type) {
                case NUM:
                    num = Math.max(min, Math.min(max, Double.parseDouble(s)));
                    break;
                case BOOL:
                    bool = Boolean.parseBoolean(s);
                    break;
                default:
                    int i = Integer.parseInt(s);
                    if (i >= 0 && i < options.length) index = i;
            }
        } catch (NumberFormatException ignored) {
        }
    }
}

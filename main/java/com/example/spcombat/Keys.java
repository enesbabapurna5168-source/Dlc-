package com.example.spcombat;

/** GLFW tus kodlari icin isim ve gecerlilik yardimcilari. */
public final class Keys {
    private Keys() {}

    /** glfwGetKey'in kabul ettigi gecerli tus araligi (gecersiz kod GLFW hatasi verir). */
    public static final int[][] RANGES = {
            {32, 32}, {39, 39}, {44, 57}, {59, 59}, {61, 61}, {65, 93}, {96, 96},
            {256, 269}, {280, 284}, {290, 314}, {320, 336}, {340, 348}
    };

    public static boolean valid(int k) {
        for (int[] r : RANGES) if (k >= r[0] && k <= r[1]) return true;
        return false;
    }

    public static String name(int k) {
        if (k < 0) return "None";
        if ((k >= 65 && k <= 90) || (k >= 48 && k <= 57)) return String.valueOf((char) k);
        if (k >= 290 && k <= 314) return "F" + (k - 289);
        if (k >= 320 && k <= 329) return "NUM" + (k - 320);
        switch (k) {
            case 32:  return "SPACE";
            case 39:  return "'";
            case 44:  return ",";
            case 45:  return "-";
            case 46:  return ".";
            case 47:  return "/";
            case 59:  return ";";
            case 61:  return "=";
            case 91:  return "[";
            case 92:  return "\\";
            case 93:  return "]";
            case 96:  return "`";
            case 256: return "ESC";
            case 257: return "ENTER";
            case 258: return "TAB";
            case 259: return "BACKSPACE";
            case 260: return "INSERT";
            case 261: return "DELETE";
            case 262: return "RIGHT";
            case 263: return "LEFT";
            case 264: return "DOWN";
            case 265: return "UP";
            case 266: return "PGUP";
            case 267: return "PGDN";
            case 268: return "HOME";
            case 269: return "END";
            case 280: return "CAPS";
            case 340: return "LSHIFT";
            case 341: return "LCTRL";
            case 342: return "LALT";
            case 343: return "LWIN";
            case 344: return "RSHIFT";
            case 345: return "RCTRL";
            case 346: return "RALT";
            case 347: return "RWIN";
            case 348: return "MENU";
            default:  return "KEY" + k;
        }
    }
}

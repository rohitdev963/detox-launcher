package com.ansub.tvhome;

/** Opt-in Home UI surface presets. Index zero retains all existing user styling. */
final class HomePremiumThemes {
    static final String[] IDS = {"classic", "frosted_pearl", "velvet_silk", "moonstone", "ivory_signature", "jade_essence", "diamond_aura"};
    static final String[] NAMES = {"Classic (existing)", "Frosted Pearl", "Velvet Silk", "Moonstone", "Ivory Signature", "Jade Essence", "Diamond Aura"};
    static final int[] ACCENTS = {0, 0xFFBCEBF0, 0xFFF2B9AE, 0xFFA4B7FF, 0xFFF0D9B3, 0xFFA0E9CB, 0xFFD8CFFF};
    static final int[] SURFACES = {0, 0x88334149, 0x99493B43, 0x992B3553, 0x99454039, 0x992A4440, 0x99343448};
    static final int[] BORDERS = {0, 0xB4C1F3F2, 0xB4F6BFB7, 0xB4A4B7FF, 0xB4F0D9B3, 0xB4A0E9CB, 0xB4D8CFFF};
    static final int[] RADII = {0, 22, 28, 20, 16, 22, 28};
    // These palettes affect only Home UI chrome, never the wallpaper or saved clock colour.
    static final int[] CLOCK_PRIMARY = {0, 0xFFEAFBFA, 0xFFFFE4DD, 0xFFE8EDFF, 0xFFFFF0D9, 0xFFDBFFED, 0xFFF4EEFF};
    static final int[] CLOCK_SECONDARY = {0, 0xFFC5E7E9, 0xFFE8BFC1, 0xFFBAC9F4, 0xFFE4CFAC, 0xFFACDFC7, 0xFFD4C9F5};
    static final String[] CLOCK_FONTS = {"sans-serif-light", "sans-serif-light", "sans-serif", "sans-serif-thin", "serif", "sans-serif-light", "sans-serif-light"};
    static final float[] CLOCK_SPACING = {0f, -0.025f, -0.015f, 0.015f, 0.005f, -0.025f, 0.025f};

    static int[] tileGradient(int index) {
        int i = Math.max(0, Math.min(index, IDS.length - 1));
        if (i == 0) return new int[] {0, 0};
        int base = SURFACES[i];
        int rgb = base & 0x00FFFFFF;
        return new int[] {0xB0000000 | rgb, (base & 0xF0000000) | rgb};
    }

    static int[] focusGradient(int index) {
        int i = Math.max(0, Math.min(index, IDS.length - 1));
        int rgb = ACCENTS[i] & 0x00FFFFFF;
        return new int[] {0x44000000 | rgb, 0x22000000 | rgb, 0x11000000 | rgb};
    }

    static int index(String id) {
        if (id != null) for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }
    private HomePremiumThemes() {}
}

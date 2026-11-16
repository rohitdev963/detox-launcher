package com.ansub.tvhome;

/** Pure layout and focus rules shared by Home and its appearance previews. */
final class HomeUiPolicy {
    static int bounded(int value, int low, int high) { return Math.max(low, Math.min(high, value)); }
    static int columns(int mode) { return mode == 1 ? 4 : mode == 2 ? 8 : 6; }
    static float focusScale(int animationMode, boolean contrast) {
        return animationMode == 2 ? 1f : contrast ? 1.08f : 1.045f;
    }
    static float safeFocusScale(int animationMode, boolean contrast, int tileWidth, int inset, int gap) {
        float room = Math.max(0, Math.min(inset - 6f, gap * .45f));
        // Leave space for the optional spring preset's overshoot as well as the steady scale.
        return Math.min(focusScale(animationMode, contrast), 1f + room * 2 / Math.max(1,tileWidth) / 1.2f);
    }
    static int shadeAlpha(int strength) { return Math.round(bounded(strength, 0, 60) * 2.55f); }
    static int brightnessAlpha(int brightness) { return Math.round((100 - bounded(brightness, 40, 100)) * 2.55f); }
    static int downIndex(int index, int count, int columns) {
        int nextRow = (index / columns + 1) * columns;
        return nextRow >= count ? -1 : Math.min(count - 1, index + columns);
    }
    static final class Layout {
        int columns, rows, tileWidth, tileHeight, step, top, contentHeight, sectionGap;
        int y(int index) { int row = index / columns; return top + row * step + (row > 0 ? sectionGap : 0); }
    }
    /** Include captions above cards, not the retired footer beneath them. */
    static int focusTop(int labelMode,float scale){return -Math.max(1,Math.round((labelMode==2?5:58)*scale));}
    static int focusBottom(int height,float scale){return height+Math.max(1,Math.round(5*scale));}
    static Layout layout(int width, int height, float scale, int mode, int count, int inset,
                         int sideReserve, int minimumTop, int utilityReserve, boolean headings) {
        if (width <= 0 || height <= 0 || scale <= 0 || Float.isNaN(scale) || Float.isInfinite(scale))
            throw new IllegalArgumentException("Invalid home viewport");
        Layout out = new Layout();
        out.columns = columns(mode); out.rows = Math.max(1, (Math.max(0, count) + out.columns - 1) / out.columns);
        int gap = Math.round(32 * scale);
        out.tileWidth = Math.max(1, (width - inset * 2 - sideReserve - gap * (out.columns - 1)) / out.columns);
        out.tileHeight = Math.max(1, out.tileWidth * 9 / 16);
        out.step = Math.max(Math.round(200 * scale), out.tileHeight + Math.round(92 * scale));
        out.sectionGap = headings && out.rows > 1 ? Math.round(34 * scale) : 0;
        int visibleRows = Math.min(2, out.rows);
        // Captions sit above the cards. Only a small focus-safe edge remains beneath the row.
        int bottomInset = Math.max(1, Math.round(8 * scale));
        int proposed = height - utilityReserve - bottomInset - out.tileHeight
                - (visibleRows - 1) * out.step - (visibleRows > 1 ? out.sectionGap : 0);
        out.top = Math.max(minimumTop, proposed);
        out.contentHeight = Math.max(height, out.y((out.rows - 1) * out.columns) + out.tileHeight + utilityReserve + bottomInset);
        return out;
    }
}

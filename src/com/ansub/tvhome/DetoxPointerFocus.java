package com.ansub.tvhome;

import android.view.MotionEvent;
import android.view.View;

/** Mouse/air-mouse focus bridge; preserves D-pad, click and long-press behavior. */
final class DetoxPointerFocus {
    private DetoxPointerFocus() {}

    static void install(View target) {
        if (target == null) return;
        target.setOnHoverListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER
                    && view.isShown() && view.isFocusable() && view.isEnabled()) {
                view.requestFocus();
            }
            // Do not consume hover: preserve platform tooltip and accessibility handling.
            return false;
        });
    }
}

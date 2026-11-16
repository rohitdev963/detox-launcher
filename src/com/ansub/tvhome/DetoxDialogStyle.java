package com.ansub.tvhome;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

/** Shared glass surface for standard Detox dialogs across Home and Gaming. */
public final class DetoxDialogStyle {
    private DetoxDialogStyle() {}

    /** Standard auxiliary dialog builder; never replaces a caller's custom on-show listener. */
    public static AlertDialog.Builder builder(Activity host) {
        return new AlertDialog.Builder(host, android.R.style.Theme_Material_Dialog_Alert) {
            @Override public AlertDialog create() {
                AlertDialog dialog = super.create();
                dialog.setOnShowListener(ignored -> apply(dialog,
                        host.getSharedPreferences("config", 0).getBoolean("menuBlur", false),
                        Math.max(1, Math.round(host.getResources().getDisplayMetrics().density))));
                return dialog;
            }
        };
    }

    /** Optional immediate-parent route for Settings-owned standard dialogs. */
    static AlertDialog.Builder builder(Activity host, Runnable parent) {
        return new AlertDialog.Builder(host, android.R.style.Theme_Material_Dialog_Alert) {
            @Override public AlertDialog create() {
                AlertDialog dialog=super.create();
                dialog.setOnShowListener(ignored->apply(dialog,host.getSharedPreferences("config",0).getBoolean("menuBlur",false),Math.max(1,Math.round(host.getResources().getDisplayMetrics().density))));
                parentOnBack(dialog,host,parent);
                return dialog;
            }
        };
    }

    static void parentOnBack(Dialog dialog,Activity host,Runnable parent) {
        final SettingsNavigationPolicy.BackGate gate=new SettingsNavigationPolicy.BackGate();
        dialog.setOnKeyListener((d,key,event)->{
            if(host instanceof MainActivity&&((MainActivity)host).routeDialogShortcut(dialog,event))return true;
            if(parent==null||key!=android.view.KeyEvent.KEYCODE_BACK)return false;
            if(gate.accept(key,event.getAction(),event.getRepeatCount())){
                // Run the caller's cancel restoration before opening its parent.
                dialog.cancel();
                if(!host.isFinishing()&&!host.isDestroyed())parent.run();
            }
            return true;
        });
    }

    static void apply(AlertDialog dialog, boolean blurEnabled, int borderPx) {
        if (dialog == null) return;
        CompanionInput.dialog(dialog);
        Window window = dialog.getWindow();
        if (window != null) {
            MenuTypography.tree(window.getDecorView());
            GradientDrawable surface = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0x304A5668, 0x48202C3D, 0x38131F30});
            surface.setCornerRadius(Math.max(1, borderPx) * 28f);
            surface.setStroke(Math.max(1, borderPx), 0x80FFFFFF);
            surface.setDither(true);
            window.setBackgroundDrawable(surface);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.20f;
            window.setAttributes(attributes);
            float density = dialog.getContext().getResources().getDisplayMetrics().density;
            int screenWidth = dialog.getContext().getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min(Math.round(560 * density), screenWidth - Math.round(48 * density)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            // Native TV themes paint opaque blue panels above the window background.
            // Clear only framework panel containers, leaving custom content surfaces intact.
            for (String name : new String[]{"parentPanel", "topPanel", "contentPanel", "buttonPanel",
                    "customPanel", "title_template", "scrollView"}) {
                android.view.View panel = frameworkView(dialog, name);
                if (panel != null) panel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            android.view.View title = frameworkView(dialog, "alertTitle");
            if (title instanceof android.widget.TextView) {
                android.widget.TextView text = (android.widget.TextView) title;
                MenuTypography.set(text,true,MenuTextPolicy.headingSize(18)*text.getResources().getDisplayMetrics().scaledDensity);
                text.setSingleLine(false);text.setMaxLines(2);text.setEllipsize(android.text.TextUtils.TruncateAt.END);
                text.setPadding(text.getPaddingLeft(),text.getPaddingTop(),text.getPaddingRight(),Math.round(10*density));
                text.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            android.widget.TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) styleText(message, 16, density);
            android.widget.ListView list = dialog.getListView();
            if (list != null) {
                list.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                list.setCacheColorHint(android.graphics.Color.TRANSPARENT);
                list.setDivider(null);
                list.setVerticalScrollBarEnabled(false);
                list.setSelector(rowSurface(true, density));
                list.setOnHierarchyChangeListener(new android.view.ViewGroup.OnHierarchyChangeListener() {
                    @Override public void onChildViewAdded(android.view.View parent, android.view.View child) { styleRow(child, density); }
                    @Override public void onChildViewRemoved(android.view.View parent, android.view.View child) { }
                });
                for (int i=0;i<list.getChildCount();i++) styleRow(list.getChildAt(i), density);
                MenuNavigationPolicy pace = new MenuNavigationPolicy();
                list.setOnKeyListener((view,key,event)->{
                    if(key!=android.view.KeyEvent.KEYCODE_DPAD_UP && key!=android.view.KeyEvent.KEYCODE_DPAD_DOWN)return false;
                    if(event.getAction()==android.view.KeyEvent.ACTION_UP){pace.release();return true;}
                    if(event.getAction()!=android.view.KeyEvent.ACTION_DOWN || !list.isEnabled())return true;
                    if(!pace.move(key,event.getRepeatCount()==0,android.os.SystemClock.uptimeMillis(),event.getDownTime()))return true;
                    int current=list.getSelectedItemPosition();
                    int next=current<0?0:Math.max(0,Math.min(list.getCount()-1,current+(key==android.view.KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));
                    if(list.getCount()>0)list.setSelection(next);
                    return true;
                });
            }
            if (Build.VERSION.SDK_INT >= 31) {
                window.setBackgroundBlurRadius(blurEnabled ? Math.max(1, borderPx) * 28 : 0);
            }
        }
        for (int id : new int[]{AlertDialog.BUTTON_POSITIVE,
                AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            android.widget.Button button = dialog.getButton(id);
            if (button != null) {
                button.setAllCaps(false);
                float density=dialog.getContext().getResources().getDisplayMetrics().density;
                styleText(button, 15, density);
                android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
                states.addState(new int[]{android.R.attr.state_focused},rowSurface(true,density));
                states.addState(new int[]{android.R.attr.state_pressed},rowSurface(true,density));
                states.addState(new int[]{},rowSurface(false,density));
                button.setBackground(states);
            }
        }
    }

    private static android.view.View frameworkView(AlertDialog dialog,String name) {
        int id=dialog.getContext().getResources().getIdentifier(name,"id","android");
        return id==0?null:dialog.findViewById(id);
    }
    private static void styleText(android.widget.TextView text,int size,float density) {
        MenuTypography.set(text,false,size*text.getResources().getDisplayMetrics().scaledDensity);
    }
    private static void styleRow(android.view.View view,float density) {
        if(!(view instanceof android.widget.TextView))return;
        android.widget.TextView row=(android.widget.TextView)view;styleText(row,18,density);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinHeight(Math.round(56*density));
        row.setPadding(Math.round(22*density),Math.round(6*density),Math.round(22*density),Math.round(6*density));
        row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }
    private static GradientDrawable rowSurface(boolean focused,float density) {
        GradientDrawable row=new GradientDrawable();row.setCornerRadius(8*density);
        row.setColor(focused?0x48FFFFFF:android.graphics.Color.TRANSPARENT);return row;
    }
}

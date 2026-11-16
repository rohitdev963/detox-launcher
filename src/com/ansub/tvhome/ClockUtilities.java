package com.ansub.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Remote-first tools; the installed system Clock owns alarms and timer delivery. */
final class ClockUtilities {
    interface Host {
        void enter(View view);
        void dismiss(Dialog dialog);
        void focus(View view, boolean focused);
        boolean shortcut(Dialog dialog, KeyEvent event);
        void ambientSettings();
    }
    private static final String[] TOOLS = {"Alarms", "Timers", "Stopwatch", "World clocks"};
    private static final String[] CITIES = {"London", "New York", "Tokyo", "Sydney", "Dubai"};
    private static final String[] ZONES = {"Europe/London", "America/New_York", "Asia/Tokyo", "Australia/Sydney", "Asia/Dubai"};
    private final Activity activity;
    private final Host host;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HomeUiFlow.Stopwatch watch = new HomeUiFlow.Stopwatch();
    private final ArrayList<Long> laps = new ArrayList<>();
    private final ArrayList<TextView> toolButtons = new ArrayList<>();
    private final ArrayList<TextView> actions = new ArrayList<>();
    private final float scale;
    private Dialog dialog, childDialog;
    private LinearLayout content;
    private ScrollView scroller;
    private TextView live, status, closeButton, watchButton;
    private int tab, alarmHour, alarmMinute, timerMinutes = 10;

    ClockUtilities(Activity activity, Host host) {
        this.activity = activity; this.host = host;
        prefs = activity.getSharedPreferences("clock_utilities", 0);
        scale = activity.getResources().getDisplayMetrics().widthPixels / 1920f;
        Calendar now = Calendar.getInstance();
        alarmHour = prefs.getInt("alarmHour", now.get(Calendar.HOUR_OF_DAY));
        alarmMinute = prefs.getInt("alarmMinute", now.get(Calendar.MINUTE));
        alarmHour = Math.max(0, Math.min(23, alarmHour));
        alarmMinute = Math.max(0, Math.min(59, alarmMinute));
        long elapsed = SystemClock.elapsedRealtime(), epoch = System.currentTimeMillis() - elapsed;
        if (Math.abs(epoch - prefs.getLong("bootEpoch", epoch)) < 120000) {
            watch.accumulated = Math.max(0, prefs.getLong("accumulated", 0));
            watch.started = prefs.getLong("started", -1);
            if (watch.started > elapsed) watch.reset();
            for (String value : prefs.getString("laps", "").split(",")) {
                try { if (laps.size() < 5 && !value.isEmpty()) laps.add(Long.parseLong(value)); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    private int px(float value) { return Math.max(1, Math.round(value * scale)); }
    private GradientDrawable surface() {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xE6233040, 0xED111C2B});
        shape.setCornerRadius(px(24)); shape.setStroke(px(1), 0x66D9E6F4);
        return shape;
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(activity); view.setText(value);
        MenuTypography.set(view,false,px(size));
        view.setIncludeFontPadding(false); return view;
    }
    private void styleButton(TextView view, boolean focused, boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? 0x48FFFFFF : selected ? 0x403C7299 : 0x182D4055);
        shape.setCornerRadius(px(8)); shape.setStroke(px(focused ? 2 : 1), focused ? Color.WHITE : 0x405E7A93);
        view.setBackground(shape); MenuTypography.refresh(view);
    }
    private TextView button(String label, Runnable action) {
        TextView view = text(label, 25, Color.WHITE);
        view.setId(View.generateViewId()); view.setFocusable(true); view.setClickable(true);
        view.setMinHeight(px(62)); view.setGravity(Gravity.CENTER);
        view.setPadding(px(18), px(12), px(18), px(12));
        view.setContentDescription(label); styleButton(view, false, false);
        view.setOnClickListener(v -> action.run());
        view.setOnFocusChangeListener((v, focused) -> styleButton(view, focused, false));
        return view;
    }

    void show(){show(null);}
    void show(Runnable parentRoute) {
        if (dialog != null && dialog.isShowing()) return;
        Dialog owner = new Dialog(activity, android.R.style.Theme_Material_Dialog_NoActionBar);
        dialog = owner;
        LinearLayout shell = new LinearLayout(activity); shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(px(36), px(30), px(36), px(28)); shell.setBackground(surface());
        LinearLayout heading = new LinearLayout(activity); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Clock & time", 36, Color.WHITE);
        MenuTypography.set(title,true,px(36));
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        closeButton = button("Close", () -> closeToParent(owner,parentRoute)); heading.addView(closeButton);
        shell.addView(heading, new LinearLayout.LayoutParams(-1, px(64)));
        TextView hint = text("Choose a tool. Use the arrows to move and OK to select.", 22, 0xFFBFCDDC);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = px(12); hintParams.bottomMargin = px(26); shell.addView(hint, hintParams);
        LinearLayout body = new LinearLayout(activity);
        shell.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout navigation = new LinearLayout(activity); navigation.setOrientation(LinearLayout.VERTICAL);
        body.addView(navigation, new LinearLayout.LayoutParams(px(260), -1)); toolButtons.clear();
        for (int i = 0; i < TOOLS.length; i++) {
            final int index = i;
            TextView tool = button(TOOLS[i], () -> { tab = index; render(); focusFirst(); });
            tool.setGravity(Gravity.CENTER_VERTICAL);
            tool.setOnFocusChangeListener((v, focused) -> styleButton(tool, focused, index == tab));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(68)); params.bottomMargin = px(10);
            navigation.addView(tool, params); toolButtons.add(tool);
        }
        View divider = new View(activity); divider.setBackgroundColor(0x305E7A93);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(px(1), -1);
        dividerParams.leftMargin = px(24); dividerParams.rightMargin = px(30); body.addView(divider, dividerParams);
        scroller = new ScrollView(activity); scroller.setFillViewport(true); scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false); scroller.setPadding(px(4), px(4), px(4), px(8));
        content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(content, new ScrollView.LayoutParams(-1, -2)); body.addView(scroller, new LinearLayout.LayoutParams(0, -1, 1));
        owner.setContentView(shell);
        owner.setOnDismissListener(d -> {
            handler.removeCallbacks(tick); persist();
            if (dialog == owner) dialog = null;
            if (childDialog != null) { childDialog.dismiss(); childDialog = null; }
            CompanionInput.clearDialog(owner);
        });
        owner.setOnKeyListener((d, key, event) -> {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_BUTTON_B) {
                if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount()==0) closeToParent(owner,parentRoute);
                return true;
            }
            return host.shortcut(owner, event);
        });
        owner.show(); CompanionInput.dialog(owner);
        Window window = owner.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int width = Math.min(px(1440), activity.getResources().getDisplayMetrics().widthPixels - px(96));
            int height = Math.min(px(860), activity.getResources().getDisplayMetrics().heightPixels - px(80));
            window.setLayout(width, height); window.setGravity(Gravity.CENTER);
            window.setDimAmount(.28f); host.enter(window.getDecorView());
        }
        render(); toolButtons.get(tab).requestFocus(); handler.removeCallbacks(tick); handler.post(tick);
    }

    private void closeToParent(Dialog owner,Runnable parent){
        if(owner!=dialog||!owner.isShowing()||closing)return;
        closing=true;
        owner.setOnDismissListener(d->{handler.removeCallbacks(tick);persist();if(dialog==owner)dialog=null;if(childDialog!=null){childDialog.dismiss();childDialog=null;}CompanionInput.clearDialog(owner);closing=false;if(parent!=null&&!activity.isFinishing()&&!activity.isDestroyed())parent.run();});
        host.dismiss(owner);
    }
    private boolean closing;

    private void heading(String title, String description) {
        TextView name = text(title, 32, Color.WHITE);
        MenuTypography.set(name,true,px(32)); content.addView(name);
        TextView detail = text(description, 22, 0xFFBFCDDC);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = px(10); params.bottomMargin = px(24); content.addView(detail, params);
    }
    private void display() {
        live = text("", 72, 0xFFF3F7FC); live.setFontFeatureSettings("tnum");
        live.setContentDescription(TOOLS[tab]);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = px(22);
        content.addView(live, params);
    }
    private TextView row(String label, Runnable action) {
        TextView view = button(label, action); actions.add(view);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(64)); params.bottomMargin = px(12);
        content.addView(view, params); return view;
    }
    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(64)); params.bottomMargin = px(16);
        content.addView(row, params); return row;
    }
    private TextView action(LinearLayout row, String label, Runnable task) {
        TextView view = button(label, task); actions.add(view);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) params.leftMargin = px(10); row.addView(view, params); return view;
    }
    private void render() {
        content.removeAllViews(); actions.clear(); live = null; status = null; watchButton = null;
        for (int i = 0; i < toolButtons.size(); i++) styleButton(toolButtons.get(i), toolButtons.get(i).hasFocus(), i == tab);
        if (tab == 0) {
            heading("Set an alarm", "Adjust the time with your remote. Your TV's Clock app delivers the alarm."); display();
            LinearLayout adjust = actionRow();
            action(adjust, "Hour −", () -> adjustAlarm(-1, 0)); action(adjust, "Hour +", () -> adjustAlarm(1, 0));
            action(adjust, "Minute −", () -> adjustAlarm(0, -1)); action(adjust, "Minute +", () -> adjustAlarm(0, 1));
            row("Set alarm", () -> {
                prefs.edit().putInt("alarmHour", alarmHour).putInt("alarmMinute", alarmMinute).apply();
                send(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, alarmHour)
                        .putExtra(AlarmClock.EXTRA_MINUTES, alarmMinute).putExtra(AlarmClock.EXTRA_MESSAGE, "Detox alarm")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true), "Alarm request sent to Clock.");
            });
            row("View saved alarms", () -> send(new Intent(AlarmClock.ACTION_SHOW_ALARMS), "Opening Clock alarms…"));
        } else if (tab == 1) {
            heading("Start a timer", "Choose a preset or adjust the duration. Your TV's Clock app delivers the alert.");
            LinearLayout presets = actionRow();
            action(presets, "5 minutes", () -> timer(5)); action(presets, "10 minutes", () -> timer(10)); action(presets, "25 minutes", () -> timer(25));
            display(); LinearLayout adjust = actionRow();
            action(adjust, "−5 min", () -> adjustTimer(-5)); action(adjust, "−1 min", () -> adjustTimer(-1));
            action(adjust, "+1 min", () -> adjustTimer(1)); action(adjust, "+5 min", () -> adjustTimer(5));
            row("Start custom timer", () -> timer(timerMinutes));
        } else if (tab == 2) {
            heading("Stopwatch", "Keeps counting when you close this menu. Save up to five laps."); display();
            LinearLayout controls = actionRow();
            watchButton = action(controls, watch.running() ? "Pause" : "Start", () -> {
                watch.toggle(SystemClock.elapsedRealtime()); persist();
                watchButton.setText(watch.running() ? "Pause" : "Start");
                watchButton.setContentDescription(watchButton.getText()); refresh();
            });
            action(controls, "Lap", () -> {
                if (!watch.running()) { message("Start the stopwatch before saving a lap."); return; }
                if (laps.size() == 5) laps.remove(0); laps.add(watch.elapsed(SystemClock.elapsedRealtime()));
                persist(); render(); if (actions.size() > 1) actions.get(1).requestFocus();
            });
            action(controls, "Reset", () -> { watch.reset(); laps.clear(); persist(); render(); actions.get(2).requestFocus(); });
            for (int i = 0; i < laps.size(); i++) {
                TextView lap = text("Lap " + (i + 1) + "     " + HomeUiFlow.Stopwatch.format(laps.get(i)), 24, 0xFFCDDAE7);
                lap.setPadding(0, px(8), 0, px(8)); content.addView(lap);
            }
        } else {
            heading("World clocks", "Compare your local time with another city and UTC.");
            live = text("", 29, 0xFFF3F7FC); live.setLineSpacing(px(14), 1); content.addView(live);
            TextView gap = text("", 8, Color.TRANSPARENT); content.addView(gap);
            row("Change comparison city", this::chooseCity);
        }
        status = text("", 22, 0xFFCEE2F2); status.setPadding(0, px(12), 0, 0); content.addView(status);
        wireFocus(); refresh(); scroller.scrollTo(0, 0);
    }

    private void wireFocus() {
        for (int i = 0; i < toolButtons.size(); i++) {
            TextView tool = toolButtons.get(i);
            tool.setNextFocusUpId(i == 0 ? closeButton.getId() : toolButtons.get(i - 1).getId());
            tool.setNextFocusDownId(toolButtons.get(Math.min(i + 1, toolButtons.size() - 1)).getId());
            tool.setNextFocusLeftId(tool.getId());
            tool.setNextFocusRightId(actions.isEmpty() ? closeButton.getId() : actions.get(0).getId());
            // Right selects a tool and enters its controls; OK does the same.
            final int index = i;
            tool.setOnKeyListener((v, key, event) -> {
                if (key != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) { tab = index; render(); focusFirst(); }
                return true;
            });
        }
        for (TextView action : actions) action.setNextFocusLeftId(toolButtons.get(tab).getId());
        ArrayList<ArrayList<View>> rows = new ArrayList<>();
        // Horizontal groups keep their own left/right neighbors; edge-left returns to the tools.
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child.isFocusable()) {
                ArrayList<View> controls = new ArrayList<>(); controls.add(child); rows.add(controls);
            }
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            ArrayList<View> controls = new ArrayList<>();
            for (int j = 0; j < row.getChildCount(); j++) {
                View control = row.getChildAt(j);
                if (!control.isFocusable()) continue;
                controls.add(control);
                control.setNextFocusLeftId(j == 0 ? toolButtons.get(tab).getId() : row.getChildAt(j - 1).getId());
                control.setNextFocusRightId(row.getChildAt(Math.min(j + 1, row.getChildCount() - 1)).getId());
            }
            if (!controls.isEmpty()) rows.add(controls);
        }
        for (int i = 0; i < rows.size(); i++) {
            ArrayList<View> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                View control = row.get(j);
                control.setNextFocusUpId(i == 0 ? closeButton.getId()
                        : rows.get(i - 1).get(Math.min(j, rows.get(i - 1).size() - 1)).getId());
                control.setNextFocusDownId(i == rows.size() - 1 ? control.getId()
                        : rows.get(i + 1).get(Math.min(j, rows.get(i + 1).size() - 1)).getId());
            }
        }
        closeButton.setNextFocusDownId(toolButtons.get(tab).getId());
    }
    private void focusFirst() { if (!actions.isEmpty()) actions.get(0).requestFocus(); }
    private void adjustAlarm(int hours, int minutes) {
        alarmHour = (alarmHour + hours + 24) % 24; alarmMinute = (alarmMinute + minutes + 60) % 60; refresh();
    }
    private void adjustTimer(int change) { timerMinutes = Math.max(1, Math.min(1440, timerMinutes + change)); refresh(); }
    private void timer(int minutes) {
        send(new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Detox timer").putExtra(AlarmClock.EXTRA_SKIP_UI, true),
                minutes + " minute timer request sent to Clock.");
    }
    private void message(String value) { if (status != null) { status.setText(value); status.announceForAccessibility(value); } }
    private void send(Intent request, String success) {
        try {
            activity.startActivity(request); message(success);
            android.widget.Toast.makeText(activity, success, android.widget.Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException absent) {
            message("No app on this TV supports this action. Install a compatible Clock app and try again.");
            DetoxDiagnostics.recordFailure("DetoxClock", "Clock action", request.getAction(), absent);
        } catch (SecurityException denied) {
            message("Android blocked this Clock request. Check the Clock app's permissions in TV settings.");
            DetoxDiagnostics.recordFailure("DetoxClock", "Clock action", request.getAction(), denied);
        } catch (RuntimeException error) {
            message("Clock could not open this action. Try again or check the Clock app in TV settings.");
            DetoxDiagnostics.recordFailure("DetoxClock", "Clock action", request.getAction(), error);
        }
    }
    private void chooseCity() {
        AlertDialog picker = DetoxDialogStyle.builder(activity).setTitle("Comparison city")
                .setSingleChoiceItems(CITIES, city(), (d, index) -> {
                    prefs.edit().putInt("city", index).apply(); d.dismiss(); refresh();
                }).setNegativeButton("Back", null).create();
        picker.setOnDismissListener(d -> {
            if (childDialog == picker) childDialog = null;
            if (dialog != null && dialog.isShowing()) CompanionInput.dialog(dialog);
            else CompanionInput.clearDialog(picker);
        });
        childDialog = picker;
        picker.show(); CompanionInput.dialog(picker);
        if (picker.getWindow() != null) picker.getWindow().setBackgroundDrawable(surface());
    }
    private int city() { return Math.max(0, Math.min(CITIES.length - 1, prefs.getInt("city", 0))); }
    private String format(Date now, String zone) {
        SimpleDateFormat format = new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(activity)
                ? "EEE, d MMM   HH:mm" : "EEE, d MMM   h:mm a", Locale.getDefault());
        if (zone != null) format.setTimeZone(TimeZone.getTimeZone(zone)); return format.format(now);
    }
    private void refresh() {
        if (live == null) return;
        if (tab == 0) {
            Calendar time = Calendar.getInstance(); time.set(Calendar.HOUR_OF_DAY, alarmHour); time.set(Calendar.MINUTE, alarmMinute);
            live.setText(android.text.format.DateFormat.format(android.text.format.DateFormat.is24HourFormat(activity) ? "HH:mm" : "h:mm a", time));
        } else if (tab == 1) live.setText(timerMinutes + (timerMinutes == 1 ? " minute" : " minutes"));
        else if (tab == 2) {
            long elapsed = watch.elapsed(SystemClock.elapsedRealtime());
            live.setText(HomeUiFlow.Stopwatch.format(elapsed) + "." + (elapsed / 100 % 10));
        } else {
            Date now = new Date();
            live.setText("Home\n" + format(now, null) + "\n\n" + CITIES[city()] + "\n" + format(now, ZONES[city()]) + "\n\nUTC\n" + format(now, "UTC"));
        }
    }
    private void persist() {
        StringBuilder saved = new StringBuilder(); for (Long lap : laps) saved.append(lap).append(',');
        prefs.edit().putLong("accumulated", watch.accumulated).putLong("started", watch.started)
                .putLong("bootEpoch", System.currentTimeMillis() - SystemClock.elapsedRealtime()).putString("laps", saved.toString()).apply();
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (dialog == null || !dialog.isShowing()) return;
            refresh(); handler.postDelayed(this, tab == 2 && watch.running() ? 100 : 1000);
        }
    };
    void close() {
        handler.removeCallbacks(tick); persist();
        if (dialog != null) { Dialog old = dialog; dialog = null; old.dismiss(); }
    }
}

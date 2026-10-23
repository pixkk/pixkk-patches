package app.pixkk.extension.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The bundle's settings. They open with a press and hold of two fingers anywhere in the app,
 * which no screen of TikTok's uses, so nothing of the original app or of another bundle is touched.
 */
public final class SettingsScreen {
    private static final long HOLD_MS = 700;
    private static final Map<Window, Boolean> WRAPPED = new WeakHashMap<>();

    private SettingsScreen() {
    }

    /** Called from the patched {@code MainActivity.onCreate}. */
    public static void install(Activity activity) {
        if (activity == null) return;
        try {
            Window window = activity.getWindow();
            if (window == null || WRAPPED.containsKey(window)) return;
            Window.Callback inner = window.getCallback();
            if (inner == null) return;
            Hold hold = new Hold(activity);
            InvocationHandler handler = (proxy, method, args) -> {
                try {
                    if (args != null && args.length == 1 && args[0] instanceof MotionEvent
                            && "dispatchTouchEvent".equals(method.getName())) {
                        hold.watch((MotionEvent) args[0]);
                    }
                    return method.invoke(inner, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getCause() != null ? ex.getCause() : ex;
                }
            };
            window.setCallback((Window.Callback) Proxy.newProxyInstance(
                    Window.Callback.class.getClassLoader(), new Class<?>[]{Window.Callback.class}, handler));
            WRAPPED.put(window, Boolean.TRUE);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not set up the settings gesture", ex);
        }
    }

    private static final class Hold {
        private final Activity activity;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable open;

        Hold(Activity activity) {
            this.activity = activity;
            this.open = () -> show(activity);
        }

        void watch(MotionEvent event) {
            int action = event.getActionMasked();
            if ((action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN)
                    && event.getPointerCount() == 2) {
                handler.removeCallbacks(open);
                handler.postDelayed(open, HOLD_MS);
            } else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL || event.getPointerCount() != 2) {
                handler.removeCallbacks(open);
            }
        }
    }

    public static void show(Context context) {
        try {
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            int pad = Dim.dp(16);
            list.setPadding(pad, pad / 2, pad, pad / 2);

            toggle(list, "Expand description in place", Settings.EXPAND_DESCRIPTION_IN_PLACE);
            toggle(list, "Sound name under description", Settings.SOUND_UNDER_DESCRIPTION);
            toggle(list, "Scroll sound name", Settings.SOUND_NAME_SCROLL);
            number(list, "Sound scroll speed", Settings.SOUND_NAME_SCROLL_SPEED);
            number(list, "Right column: gap between buttons, dp", Settings.RAIL_SPACING_DP);
            number(list, "Right column: gap under avatar, dp", Settings.RAIL_AVATAR_GAP_DP);
            number(list, "Right column: gap above disc, dp", Settings.RAIL_DISC_GAP_DP);
            toggle(list, "Don't open comments when swiping over icons", Settings.RAIL_SWIPE_PASSTHROUGH);
            toggle(list, "Centered comment menu", Settings.COMMENT_MENU_CENTERED);
            Button rows = new Button(context);
            rows.setText("Comment menu rows...");
            rows.setOnClickListener(v -> CommentMenuRowsPreference.show(context));
            list.addView(rows);
            toggle(list, "Animated profile covers", Settings.ANIMATED_PROFILE_COVERS);
            toggle(list, "Old-style seeking", Settings.SEEK_FOLLOWS_FINGER);
            toggle(list, "Old-style counts (12.3K)", Settings.OLD_STYLE_COUNTS);
            toggle(list, "Discover tab instead of Friends", Settings.DISCOVER_TAB);

            TextView note = new TextView(context);
            note.setText("Changes apply after the app is restarted.");
            note.setPadding(0, pad, 0, 0);
            list.addView(note);

            ScrollView scroll = new ScrollView(context);
            scroll.addView(list, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            new AlertDialog.Builder(context)
                    .setTitle("pixkk")
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not show the settings", ex);
        }
    }

    private static void toggle(LinearLayout list, String label, Settings.BooleanSetting setting) {
        Switch row = new Switch(list.getContext());
        row.setText(label);
        row.setChecked(setting.get());
        row.setPadding(0, Dim.dp(8), 0, Dim.dp(8));
        row.setOnCheckedChangeListener((button, checked) -> setting.save(checked));
        list.addView(row);
    }

    private static void number(LinearLayout list, String label, Settings.IntegerSetting setting) {
        Context context = list.getContext();
        TextView text = new TextView(context);
        text.setText(label + " (" + setting.min + " to " + setting.max + ")");
        list.addView(text);
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.valueOf(setting.get()));
        input.setOnFocusChangeListener((view, focused) -> {
            if (focused) return;
            try {
                setting.save(Integer.parseInt(input.getText().toString().trim()));
            } catch (NumberFormatException ignored) {
            }
            input.setText(String.valueOf(setting.get()));
        });
        list.addView(input);
    }
}

package app.pixkk.extension.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * The bundle's settings as a page of their own, shown by a TikTok activity the patch lends it
 * (the one Hushfeed lends too, told apart by the intent's action).
 */
public final class SettingsPage {
    private static final String ACTION = "pixkk_settings";

    private static int BACKGROUND;
    private static int SURFACE;
    private static int BORDER;
    private static int DIVIDER;
    private static int TEXT;
    private static int TEXT_MUTED;
    private static int ACCENT;

    private SettingsPage() {
    }

    /** The page follows the theme the activity runs in, which can differ from the system's. */
    private static void applyTheme(Activity activity) {
        boolean dark = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (dark) {
            BACKGROUND = Color.BLACK;
            SURFACE = Color.rgb(17, 17, 21);
            BORDER = Color.rgb(53, 53, 62);
            DIVIDER = Color.rgb(41, 41, 48);
            TEXT = Color.rgb(245, 245, 247);
            TEXT_MUTED = Color.rgb(168, 168, 179);
            ACCENT = Color.rgb(255, 79, 135);
        } else {
            BACKGROUND = Color.rgb(245, 245, 248);
            SURFACE = Color.WHITE;
            BORDER = Color.rgb(210, 210, 210);
            DIVIDER = Color.rgb(224, 224, 224);
            TEXT = Color.rgb(22, 22, 28);
            TEXT_MUTED = Color.rgb(87, 87, 98);
            ACCENT = Color.rgb(184, 22, 77);
        }
    }

    /** Starts the activity that shows the page. */
    public static void open(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent();
            intent.setClassName(context.getPackageName(),
                    "com.bytedance.ies.ugc.aweme.commercialize.compliance.personalization.AdPersonalizationActivity");
            intent.setAction(ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not open the settings page", ex);
        }
    }

    /**
     * Called from the lent activity's onCreate right after super.onCreate.
     *
     * @return whether the page took the activity, so the rest of its onCreate is skipped
     */
    public static boolean initialize(Activity activity) {
        try {
            Intent intent = activity.getIntent();
            if (intent == null || !ACTION.equals(intent.getAction())) return false;
            activity.setContentView(build(activity));
            registerBack(activity);
            return true;
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not build the settings page", ex);
            return false;
        }
    }

    /** Called from the lent activity's onBackPressed; true when the page answered. */
    public static boolean handleBackPressed(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null || !ACTION.equals(intent.getAction())) return false;
        activity.finish();
        return true;
    }

    private static View build(Activity activity) {
        applyTheme(activity);
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(BACKGROUND);
        root.setFitsSystemWindows(true);

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = Dim.dp(16);
        page.setPadding(side, Dim.dp(8), side, Dim.dp(32));

        LinearLayout top = new LinearLayout(activity);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(activity);
        back.setText("←");
        back.setTextColor(TEXT);
        back.setTextSize(22);
        back.setGravity(Gravity.CENTER);
        back.setBackground(circle(SURFACE));
        back.setOnClickListener(v -> activity.finish());
        top.addView(back, new LinearLayout.LayoutParams(Dim.dp(48), Dim.dp(48)));
        TextView brand = label(activity, "PIXKK", 12, ACCENT, true);
        brand.setPadding(Dim.dp(12), 0, 0, 0);
        brand.setLetterSpacing(0.1f);
        top.addView(brand);
        page.addView(top);

        TextView title = label(activity, "Pixkk Patches", 32, TEXT, true);
        title.setPadding(Dim.dp(8), Dim.dp(24), 0, 0);
        page.addView(title);
        TextView subtitle = label(activity, "Changes apply after the app is restarted", 15, TEXT_MUTED, false);
        subtitle.setPadding(Dim.dp(8), Dim.dp(8), 0, Dim.dp(8));
        page.addView(subtitle);

        LinearLayout feed = group(page, "FEED");
        toggle(feed, "Expand description in place", Settings.EXPAND_DESCRIPTION_IN_PLACE);
        toggle(feed, "Sound name under description", Settings.SOUND_UNDER_DESCRIPTION);
        toggle(feed, "Scroll sound name", Settings.SOUND_NAME_SCROLL);
        number(feed, "Sound scroll speed", Settings.SOUND_NAME_SCROLL_SPEED);
        toggle(feed, "Old-style seeking", Settings.SEEK_FOLLOWS_FINGER);
        toggle(feed, "Old-style counts (12.3K)", Settings.OLD_STYLE_COUNTS);
        toggle(feed, "Animated profile covers", Settings.ANIMATED_PROFILE_COVERS);

        LinearLayout rail = group(page, "RIGHT COLUMN");
        number(rail, "Gap between buttons, dp", Settings.RAIL_SPACING_DP);
        number(rail, "Gap under avatar, dp", Settings.RAIL_AVATAR_GAP_DP);
        number(rail, "Gap above disc, dp", Settings.RAIL_DISC_GAP_DP);
        toggle(rail, "Don't open comments when swiping over icons", Settings.RAIL_SWIPE_PASSTHROUGH);

        LinearLayout comments = group(page, "COMMENTS");
        toggle(comments, "Centered comment menu", Settings.COMMENT_MENU_CENTERED);
        action(comments, "Comment menu rows", "Choose which rows the menu shows",
                () -> CommentMenuRowsPreference.show(activity));

        LinearLayout navigation = group(page, "NAVIGATION");
        toggle(navigation, "Discover tab instead of Friends", Settings.DISCOVER_TAB);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private static LinearLayout group(LinearLayout page, String heading) {
        Context context = page.getContext();
        TextView header = label(context, heading, 12, TEXT_MUTED, true);
        header.setLetterSpacing(0.1f);
        header.setPadding(Dim.dp(8), Dim.dp(24), 0, Dim.dp(8));
        page.addView(header);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SURFACE);
        shape.setStroke(Dim.dp(1), BORDER);
        shape.setCornerRadius(Dim.dp(20));
        card.setBackground(shape);
        card.setPadding(Dim.dp(16), 0, Dim.dp(16), 0);
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    /** A row in a card; every row after the first gets a divider above it. */
    private static LinearLayout row(LinearLayout card) {
        Context context = card.getContext();
        if (card.getChildCount() > 0) {
            View divider = new View(context);
            divider.setBackgroundColor(DIVIDER);
            card.addView(divider, new LinearLayout.LayoutParams(-1, Dim.dp(1)));
        }
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Dim.dp(14), 0, Dim.dp(14));
        card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private static void toggle(LinearLayout card, String title, Settings.BooleanSetting setting) {
        LinearLayout row = row(card);
        row.addView(label(card.getContext(), title, 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(card.getContext());
        control.setChecked(setting.get());
        control.setOnCheckedChangeListener((button, checked) -> setting.save(checked));
        row.addView(control);
        row.setOnClickListener(v -> control.toggle());
    }

    private static void number(LinearLayout card, String title, Settings.IntegerSetting setting) {
        LinearLayout row = row(card);
        LinearLayout text = new LinearLayout(card.getContext());
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(card.getContext(), title, 16, TEXT, true));
        text.addView(label(card.getContext(), setting.min + " to " + setting.max, 13, TEXT_MUTED, false));
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

        EditText input = new EditText(card.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.valueOf(setting.get()));
        input.setTextColor(TEXT);
        input.setGravity(Gravity.END);
        input.setMinWidth(Dim.dp(64));
        input.setOnFocusChangeListener((view, focused) -> {
            if (focused) return;
            try {
                setting.save(Integer.parseInt(input.getText().toString().trim()));
            } catch (NumberFormatException ignored) {
            }
            input.setText(String.valueOf(setting.get()));
        });
        row.addView(input);
    }

    private static void action(LinearLayout card, String title, String summary, Runnable run) {
        LinearLayout row = row(card);
        LinearLayout text = new LinearLayout(card.getContext());
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(card.getContext(), title, 16, TEXT, true));
        text.addView(label(card.getContext(), summary, 13, TEXT_MUTED, false));
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(label(card.getContext(), ">", 18, TEXT_MUTED, false));
        row.setOnClickListener(v -> run.run());
    }

    private static TextView label(Context context, String text, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private static GradientDrawable circle(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(Dim.dp(1), BORDER);
        return shape;
    }

    /**
     * Back through the dispatcher, for the day TikTok's manifest stops routing it to onBackPressed.
     * Reflective, because the extension builds against an older API; ignored below Android 13.
     */
    private static void registerBack(Activity activity) {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            Class<?> callbackClass = Class.forName("android.window.OnBackInvokedCallback");
            Class<?> dispatcherClass = Class.forName("android.window.OnBackInvokedDispatcher");
            Object dispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher").invoke(activity);
            Object callback = Proxy.newProxyInstance(callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "onBackInvoked":
                                activity.finish();
                                return null;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            case "toString":
                                return "PixkkSettingsBack";
                            default:
                                return null;
                        }
                    });
            Method register = dispatcherClass.getMethod("registerOnBackInvokedCallback", int.class, callbackClass);
            register.invoke(dispatcher, 1_000_000, callback);
        } catch (Throwable ignored) {
        }
    }
}

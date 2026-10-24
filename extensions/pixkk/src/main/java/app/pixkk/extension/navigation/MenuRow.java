package app.pixkk.extension.navigation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import app.pixkk.extension.core.Logger;
import app.pixkk.extension.core.SettingsScreen;

/**
 * The Pixkk Patches row in the profile's side menu, right under "Settings and privacy".
 *
 * The menu's rows are built one per server-listed component, so a row of our own has no component
 * to come from. Instead the row TikTok builds for "Settings and privacy" is handed back inside a
 * wrapper, with a second row made from the same view class beside it. Everything about that class
 * is looked up by what it is (a view with a title, an icon and an accessory), not by its name.
 */
public final class MenuRow {
    private static final String SETTINGS_CELL = "content_section_cell_settings_and_privacy";
    private static final String TITLE = "Pixkk Patches";
    /** What the cell's constructor is given as its third argument everywhere TikTok builds one. */
    private static final int CELL_STYLE = 6;

    private MenuRow() {
    }

    /**
     * Called with the row TikTok just built.
     *
     * @param cell the built row
     * @param protocolName the name of the protocol of the component that built it
     * @param icon the resource id of the icon the new row shows
     * @return the view to put in the menu in its place
     */
    public static View wrap(View cell, String protocolName, int icon) {
        try {
            if (cell == null || !SETTINGS_CELL.equals(protocolName)) return cell;
            View mine = build(cell, icon);
            if (mine == null) return cell;

            LinearLayout column = new LinearLayout(cell.getContext());
            column.setOrientation(LinearLayout.VERTICAL);
            ViewGroup.LayoutParams own = cell.getLayoutParams();
            column.setLayoutParams(own);
            column.addView(cell, new LinearLayout.LayoutParams(-1, -2));
            column.addView(mine, new LinearLayout.LayoutParams(-1, -2));
            return column;
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not add the menu row", ex);
            return cell;
        }
    }

    private static View build(View original, int icon) throws Exception {
        Context context = original.getContext();
        Class<?> cellClass = original.getClass();
        Constructor<?> constructor = cellClass.getConstructor(Context.class, AttributeSet.class, int.class);
        View cell = (View) constructor.newInstance(context, null, CELL_STYLE);

        method(cellClass, "setTitle", CharSequence.class).invoke(cell, TITLE);

        // The icon: the cell's icon holder has one int-wrapping field, the resource id.
        Method setIcon = null;
        for (Method method : cellClass.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (method.getName().equals("setIcon") && types.length == 1
                    && !types[0].isInterface() && types[0].getConstructors().length > 0) {
                setIcon = method;
            }
        }
        if (setIcon != null) {
            Object holder = setIcon.getParameterTypes()[0].getConstructor().newInstance();
            for (Field field : holder.getClass().getFields()) {
                if (field.getType() == Integer.class) field.set(holder, icon);
            }
            setIcon.invoke(cell, holder);
        }

        // The arrow: a second accessory of the class the original carries.
        Object accessory = cellClass.getMethod("getAccessory").invoke(original);
        if (accessory != null) {
            Object arrow = accessory.getClass().getConstructor(Context.class, AttributeSet.class)
                    .newInstance(context, null);
            for (Method method : arrow.getClass().getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 1 && types[0] == View.OnClickListener.class) {
                    method.invoke(arrow, (View.OnClickListener) v -> SettingsScreen.show(context));
                    break;
                }
            }
            for (Method method : cellClass.getMethods()) {
                if (method.getName().equals("setAccessory") && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isInstance(arrow)) {
                    method.invoke(cell, arrow);
                    break;
                }
            }
        }
        cell.setOnClickListener(v -> SettingsScreen.show(context));
        return cell;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        return type.getMethod(name, parameters);
    }
}

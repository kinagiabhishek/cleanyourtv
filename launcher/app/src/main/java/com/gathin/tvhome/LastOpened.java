package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import java.util.Locale;
import java.util.Map;

/**
 * When an app was last opened from Home, for the cinematic shelf's "Last opened" line and for the 10-minute idle RAM cleaner.
 */
final class LastOpened {
    private static final String PREFS = "last_opened";

    private LastOpened() {
    }

    /** Notes that Home just opened this app. */
    static void record(Context ctx, String pkg) {
        if (ctx == null || pkg == null) return;
        prefs(ctx).edit().putLong(pkg, System.currentTimeMillis()).apply();
    }

    /** Milliseconds since the epoch, or 0 when Home never opened the app. */
    static long get(Context ctx, String pkg) {
        return pkg == null ? 0L : prefs(ctx).getLong(pkg, 0L);
    }

    /**
     * Kills background processes of any app not opened in the last maxIdleMs (e.g. 10 minutes)
     * to save RAM and prevent frozen background state bugs.
     */
    static void cleanupIdleApps(Context ctx, long maxIdleMs) {
        if (ctx == null) return;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;
        SharedPreferences sp = prefs(ctx);
        long now = System.currentTimeMillis();
        String myPkg = ctx.getPackageName();
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            String pkg = entry.getKey();
            if (pkg == null || pkg.equals(myPkg)) continue;
            Object val = entry.getValue();
            if (val instanceof Long) {
                long last = (Long) val;
                if (now - last >= maxIdleMs) {
                    try {
                        am.killBackgroundProcesses(pkg);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * "5 minutes ago" within the hour, then the day and the time: "today, 11:40 am", "yesterday, 9:40 pm",
     * "3 days ago, 9:40 pm", "Oct 12, 9:40 pm". The parts are Android's own wording in the TV's language.
     */
    static String describe(Context ctx, long when) {
        long now = System.currentTimeMillis();
        // Under a minute Android says "0 minutes ago"; one minute reads better.
        when = Math.min(when, now - DateUtils.MINUTE_IN_MILLIS);
        if (now - when < DateUtils.HOUR_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(when, now, DateUtils.MINUTE_IN_MILLIS).toString();
        }
        String day = DateUtils.getRelativeTimeSpanString(when, now, DateUtils.DAY_IN_MILLIS).toString();
        // It follows "Last opened" mid-sentence: "yesterday", not "Yesterday". Month names keep their capital.
        if (Locale.getDefault().getLanguage().equals("en") && (day.startsWith("Yesterday") || day.startsWith("Today"))) {
            day = Character.toLowerCase(day.charAt(0)) + day.substring(1);
        }
        return day + ", " + DateUtils.formatDateTime(ctx, when, DateUtils.FORMAT_SHOW_TIME);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

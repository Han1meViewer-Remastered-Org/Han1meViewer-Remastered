/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.Window;
import cn.jzvd.Jzvd;

public class JZUtils {
    public static final String TAG = "JZVD";
    public static int SYSTEM_UI = 0;

    public static String stringForTime(long timeMs) {
        if (timeMs <= 0L || timeMs >= 86400000L) {
            return "00:00";
        }
        long totalSeconds = timeMs / 1000L;
        int seconds = (int)(totalSeconds % 60L);
        int minutes = (int)(totalSeconds / 60L % 60L);
        int hours = (int)(totalSeconds / 3600L);
        if (hours > 0) {
            return hours + ":" + twoDigits(minutes) + ":" + twoDigits(seconds);
        }
        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private static String twoDigits(int value) {
        if (value < 10) {
            return "0" + value;
        }
        return Integer.toString(value);
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService("connectivity");
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == 1;
    }

    public static Activity scanForActivity(Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity)context;
        }
        if (context instanceof ContextWrapper) {
            return JZUtils.scanForActivity(((ContextWrapper)context).getBaseContext());
        }
        return null;
    }

    public static void setRequestedOrientation(Context context, int orientation) {
        if (JZUtils.scanForActivity(context) != null) {
            JZUtils.scanForActivity(context).setRequestedOrientation(orientation);
        } else {
            JZUtils.scanForActivity(context).setRequestedOrientation(orientation);
        }
    }

    public static Window getWindow(Context context) {
        if (JZUtils.scanForActivity(context) != null) {
            return JZUtils.scanForActivity(context).getWindow();
        }
        return JZUtils.scanForActivity(context).getWindow();
    }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

    public static void saveProgress(Context context, Object url, long progress) {
        if (!Jzvd.SAVE_PROGRESS) {
            return;
        }
        Log.i((String)TAG, (String)("saveProgress: " + progress));
        if (progress < 5000L) {
            progress = 0L;
        }
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS", 0);
        SharedPreferences.Editor editor = spn.edit();
        editor.putLong("newVersion:" + url.toString(), progress).apply();
    }

    public static long getSavedProgress(Context context, Object url) {
        if (!Jzvd.SAVE_PROGRESS) {
            return 0L;
        }
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS", 0);
        return spn.getLong("newVersion:" + url.toString(), 0L);
    }

    public static void clearSavedProgress(Context context, Object url) {
        if (url == null) {
            SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS", 0);
            spn.edit().clear().apply();
        } else {
            SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS", 0);
            spn.edit().putLong("newVersion:" + url.toString(), 0L).apply();
        }
    }

    @SuppressLint(value={"RestrictedApi"})
    public static void showStatusBar(Context context) {
        if (Jzvd.TOOL_BAR_EXIST) {
            JZUtils.getWindow(context).clearFlags(1024);
        }
    }

    @SuppressLint(value={"RestrictedApi"})
    public static void hideStatusBar(Context context) {
        if (Jzvd.TOOL_BAR_EXIST) {
            JZUtils.getWindow(context).setFlags(1024, 1024);
        }
    }

    @SuppressLint(value={"NewApi"})
    public static void hideSystemUI(Context context) {
        int uiOptions = 1542;
        if (Build.VERSION.SDK_INT >= 19) {
            uiOptions |= 0x1000;
        }
        SYSTEM_UI = JZUtils.getWindow(context).getDecorView().getSystemUiVisibility();
        JZUtils.getWindow(context).getDecorView().setSystemUiVisibility(uiOptions);
    }

    @SuppressLint(value={"NewApi"})
    public static void showSystemUI(Context context) {
        boolean uiOptions = false;
        JZUtils.getWindow(context).getDecorView().setSystemUiVisibility(SYSTEM_UI);
    }

    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    public static int getNavigationBarHeight(Context context) {
        boolean var1 = ViewConfiguration.get((Context)context).hasPermanentMenuKey();
        int var2 = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return var2 > 0 && !var1 ? context.getResources().getDimensionPixelSize(var2) : 0;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }
}


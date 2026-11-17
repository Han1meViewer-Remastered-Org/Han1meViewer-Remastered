@file:Suppress("DEPRECATION")

package com.yenaly.han1meviewer.ui.view.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import cn.jzvd.JZUtils
import cn.jzvd.Jzvd

fun playerActivity(context: Context?): Activity? {
    if (context == null) return null
    if (context is Activity) return context
    if (context is ContextWrapper) return playerActivity(context.baseContext)
    return null
}

fun playerDip(context: Context, dp: Float): Int {
    return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

fun playerScreenWidth(context: Context): Int {
    return context.resources.displayMetrics.widthPixels
}

fun playerStatusBarHeight(context: Context): Int {
    val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return context.resources.getDimensionPixelSize(id)
}

fun playerWindow(context: Context): Window? {
    return playerActivity(context)?.window
}

fun playerSetRequestedOrientation(context: Context, orientation: Int) {
    playerActivity(context)!!.requestedOrientation = orientation
}

fun savedPlaybackPosition(context: Context, url: Any?): Long {
    if (!Jzvd.SAVE_PROGRESS) return 0L
    val prefs = context.getSharedPreferences("JZVD_PROGRESS", Context.MODE_PRIVATE)
    return prefs.getLong("newVersion:$url", 0L)
}

fun showPlayerStatusBar(context: Context) {
    if (!Jzvd.TOOL_BAR_EXIST) return
    playerWindow(context)!!.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
}

fun hidePlayerStatusBar(context: Context) {
    if (!Jzvd.TOOL_BAR_EXIST) return
    playerWindow(context)!!.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
    )
}

private const val PLAYER_HIDE_SYSTEM_UI = 1542

fun hidePlayerSystemUi(context: Context) {
    var flags = PLAYER_HIDE_SYSTEM_UI
    if (Build.VERSION.SDK_INT >= 19) {
        flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
    val decor = playerWindow(context)!!.decorView
    JZUtils.SYSTEM_UI = decor.systemUiVisibility
    decor.systemUiVisibility = flags
}

fun showPlayerSystemUi(context: Context) {
    playerWindow(context)!!.decorView.systemUiVisibility = JZUtils.SYSTEM_UI
}

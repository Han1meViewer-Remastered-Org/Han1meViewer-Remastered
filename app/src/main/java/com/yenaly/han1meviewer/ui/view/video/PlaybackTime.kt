package com.yenaly.han1meviewer.ui.view.video

import java.util.Formatter
import java.util.Locale

fun playbackTimeText(timeMs: Long): String {
    if (timeMs <= 0L || timeMs >= 24 * 60 * 60 * 1000L) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()
    val formatter = Formatter(StringBuilder(), Locale.getDefault())
    return if (hours > 0) {
        formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
    } else {
        formatter.format("%02d:%02d", minutes, seconds).toString()
    }
}

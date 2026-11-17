package com.yenaly.han1meviewer.ui.view.video

fun playbackTimeText(timeMs: Long): String {
    if (timeMs <= 0L || timeMs >= 24 * 60 * 60 * 1000L) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()
    return if (hours > 0) {
        hours.toString() + ":" + twoDigits(minutes) + ":" + twoDigits(seconds)
    } else {
        twoDigits(minutes) + ":" + twoDigits(seconds)
    }
}

private fun twoDigits(value: Int): String {
    return if (value < 10) "0$value" else value.toString()
}

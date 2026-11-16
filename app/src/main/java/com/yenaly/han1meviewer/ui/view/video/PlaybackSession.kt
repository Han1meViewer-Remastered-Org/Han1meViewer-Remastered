package com.yenaly.han1meviewer.ui.view.video

import cn.jzvd.JZMediaInterface

/**
 * 页面和画中画使用的播放操作。内核仍由 JZVideo 持有，这里只转发已经实现的播放能力。
 */
interface PlaybackSession {
    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
    val videoWidth: Int
    val videoHeight: Int

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)

    fun togglePlayback() {
        if (isPlaying) pause() else play()
    }
}

internal class JzPlaybackSession(
    private val media: JZMediaInterface,
) : PlaybackSession {
    override val isPlaying: Boolean
        get() = media.isPlaying

    override val positionMs: Long
        get() = media.currentPosition

    override val durationMs: Long
        get() = media.duration

    override val videoWidth: Int
        get() = (media as? HMediaKernel)?.videoRealWidth ?: 0

    override val videoHeight: Int
        get() = (media as? HMediaKernel)?.videoRealHeight ?: 0

    override fun play() = media.start()

    override fun pause() = media.pause()

    override fun seekTo(positionMs: Long) = media.seekTo(positionMs)

    override fun setSpeed(speed: Float) = media.setSpeed(speed)
}

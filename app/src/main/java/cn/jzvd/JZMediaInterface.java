/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import cn.jzvd.Jzvd;

public abstract class JZMediaInterface
implements TextureView.SurfaceTextureListener {
    public static SurfaceTexture SAVED_SURFACE;
    public HandlerThread mMediaHandlerThread;
    public Handler mMediaHandler;
    public Handler handler;
    public Jzvd jzvd;

    public JZMediaInterface(Jzvd jzvd) {
        this.jzvd = jzvd;
    }

    public abstract void start();

    public abstract void prepare();

    public abstract void pause();

    public abstract boolean isPlaying();

    public abstract void seekTo(long var1);

    public abstract void release();

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract void setVolume(float var1, float var2);

    public abstract void setSpeed(float var1);

    public abstract void setSurface(Surface var1);
}


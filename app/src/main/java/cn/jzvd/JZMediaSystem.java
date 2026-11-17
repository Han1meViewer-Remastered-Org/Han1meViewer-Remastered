/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import cn.jzvd.JZMediaInterface;
import cn.jzvd.Jzvd;
import java.lang.reflect.Method;
import java.util.Map;

public class JZMediaSystem
extends JZMediaInterface
implements MediaPlayer.OnPreparedListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnSeekCompleteListener,
MediaPlayer.OnErrorListener,
MediaPlayer.OnInfoListener,
MediaPlayer.OnVideoSizeChangedListener {
    public MediaPlayer mediaPlayer;

    public JZMediaSystem(Jzvd jzvd) {
        super(jzvd);
    }

    @Override
    public void prepare() {
        this.release();
        this.mMediaHandlerThread = new HandlerThread("JZVD");
        this.mMediaHandlerThread.start();
        this.mMediaHandler = new Handler(this.mMediaHandlerThread.getLooper());
        this.handler = new Handler();
        this.mMediaHandler.post(() -> {
            try {
                this.mediaPlayer = new MediaPlayer();
                this.mediaPlayer.setAudioStreamType(3);
                this.mediaPlayer.setLooping(this.jzvd.jzDataSource.looping);
                this.mediaPlayer.setOnPreparedListener((MediaPlayer.OnPreparedListener)this);
                this.mediaPlayer.setOnCompletionListener((MediaPlayer.OnCompletionListener)this);
                this.mediaPlayer.setOnBufferingUpdateListener((MediaPlayer.OnBufferingUpdateListener)this);
                this.mediaPlayer.setScreenOnWhilePlaying(true);
                this.mediaPlayer.setOnSeekCompleteListener((MediaPlayer.OnSeekCompleteListener)this);
                this.mediaPlayer.setOnErrorListener((MediaPlayer.OnErrorListener)this);
                this.mediaPlayer.setOnInfoListener((MediaPlayer.OnInfoListener)this);
                this.mediaPlayer.setOnVideoSizeChangedListener((MediaPlayer.OnVideoSizeChangedListener)this);
                Class<MediaPlayer> clazz = MediaPlayer.class;
                Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
                method.invoke(this.mediaPlayer, this.jzvd.jzDataSource.getCurrentUrl().toString(), this.jzvd.jzDataSource.headerMap);
                this.mediaPlayer.prepareAsync();
                this.mediaPlayer.setSurface(new Surface(SAVED_SURFACE));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void start() {
        this.mMediaHandler.post(() -> this.mediaPlayer.start());
    }

    @Override
    public void pause() {
        this.mMediaHandler.post(() -> this.mediaPlayer.pause());
    }

    @Override
    public boolean isPlaying() {
        return this.mediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(long time) {
        this.mMediaHandler.post(() -> {
            try {
                this.mediaPlayer.seekTo((int)time);
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void release() {
        if (this.mMediaHandler != null && this.mMediaHandlerThread != null && this.mediaPlayer != null) {
            HandlerThread tmpHandlerThread = this.mMediaHandlerThread;
            MediaPlayer tmpMediaPlayer = this.mediaPlayer;
            JZMediaInterface.SAVED_SURFACE = null;
            this.mMediaHandler.post(() -> {
                tmpMediaPlayer.setSurface(null);
                tmpMediaPlayer.release();
                tmpHandlerThread.quit();
            });
            this.mediaPlayer = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (this.mediaPlayer != null) {
            return this.mediaPlayer.getCurrentPosition();
        }
        return 0L;
    }

    @Override
    public long getDuration() {
        if (this.mediaPlayer != null) {
            return this.mediaPlayer.getDuration();
        }
        return 0L;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (this.mMediaHandler == null) {
            return;
        }
        this.mMediaHandler.post(() -> {
            if (this.mediaPlayer != null) {
                this.mediaPlayer.setVolume(leftVolume, rightVolume);
            }
        });
    }

    @Override
    public void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT >= 23) {
            PlaybackParams pp = this.mediaPlayer.getPlaybackParams();
            pp.setSpeed(speed);
            this.mediaPlayer.setPlaybackParams(pp);
        }
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        this.handler.post(() -> this.jzvd.onPrepared());
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
        this.handler.post(() -> this.jzvd.onCompletion());
    }

    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        this.handler.post(() -> this.jzvd.setBufferProgress(percent));
    }

    public void onSeekComplete(MediaPlayer mediaPlayer) {
        this.handler.post(() -> this.jzvd.onSeekComplete());
    }

    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        this.handler.post(() -> this.jzvd.onError(what, extra));
        return true;
    }

    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
        this.handler.post(() -> this.jzvd.onInfo(what, extra));
        return false;
    }

    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        this.handler.post(() -> this.jzvd.onVideoSizeChanged(width, height));
    }

    @Override
    public void setSurface(Surface surface) {
        this.mediaPlayer.setSurface(surface);
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface;
            this.prepare();
        } else {
            this.jzvd.textureView.setSurfaceTexture(SAVED_SURFACE);
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}


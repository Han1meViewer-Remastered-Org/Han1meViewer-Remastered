/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import cn.jzvd.JZDataSource;
import cn.jzvd.JZMediaInterface;
import cn.jzvd.JZMediaSystem;
import cn.jzvd.JZTextureView;
import cn.jzvd.JZUtils;
import com.yenaly.han1meviewer.R;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Jzvd
extends FrameLayout
implements View.OnClickListener,
SeekBar.OnSeekBarChangeListener,
View.OnTouchListener {
    public static final String TAG = "JZVD";
    public static final int SCREEN_NORMAL = 0;
    public static final int SCREEN_FULLSCREEN = 1;
    public static final int SCREEN_TINY = 2;
    public static final int STATE_IDLE = -1;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARING_CHANGE_URL = 2;
    public static final int STATE_PREPARING_PLAYING = 3;
    public static final int STATE_PREPARED = 4;
    public static final int STATE_PLAYING = 5;
    public static final int STATE_PAUSE = 6;
    public static final int STATE_AUTO_COMPLETE = 7;
    public static final int STATE_ERROR = 8;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static final int THRESHOLD = 80;
    public static Jzvd CURRENT_JZVD;
    public static LinkedList<ViewGroup> CONTAINER_LIST;
    public static boolean TOOL_BAR_EXIST;
    public static int FULLSCREEN_ORIENTATION;
    public static int NORMAL_ORIENTATION;
    public static boolean SAVE_PROGRESS;
    public static boolean WIFI_TIP_DIALOG_SHOWED;
    public static int VIDEO_IMAGE_DISPLAY_TYPE;
    public static long lastAutoFullscreenTime;
    public static int ON_PLAY_PAUSE_TMP_STATE;
    public static int backUpBufferState;
    public static float PROGRESS_DRAG_RATE;
    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;
    public int state = -1;
    public int screen = -1;
    public JZDataSource jzDataSource;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public Class mediaInterfaceClass;
    public JZMediaInterface mediaInterface;
    public int positionInList = -1;
    public int videoRotation = 0;
    public int seekToManulPosition = -1;
    public long seekToInAdvance = 0L;
    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView;
    public TextView totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer;
    public ViewGroup bottomContainer;
    public JZTextureView textureView;
    public boolean preloading = false;
    protected long gobakFullscreenTime = 0L;
    protected long gotoFullscreenTime = 0L;
    protected Timer UPDATE_PROGRESS_TIMER;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;
    protected Context jzvdContext;
    protected long mCurrentPosition;
    protected ViewGroup.LayoutParams blockLayoutParams;
    protected int blockIndex;
    protected int blockWidth;
    protected int blockHeight;

    public Jzvd(Context context) {
        super(context);
        this.init(context);
    }

    public Jzvd(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public static void goOnPlayOnResume() {
        if (CURRENT_JZVD != null) {
            if (Jzvd.CURRENT_JZVD.state == 6) {
                if (ON_PLAY_PAUSE_TMP_STATE == 6) {
                    CURRENT_JZVD.onStatePause();
                    Jzvd.CURRENT_JZVD.mediaInterface.pause();
                } else {
                    CURRENT_JZVD.onStatePlaying();
                    Jzvd.CURRENT_JZVD.mediaInterface.start();
                }
                ON_PLAY_PAUSE_TMP_STATE = 0;
            } else if (Jzvd.CURRENT_JZVD.state == 1) {
                CURRENT_JZVD.startVideo();
            }
            if (Jzvd.CURRENT_JZVD.screen == 1) {
                JZUtils.hideStatusBar(Jzvd.CURRENT_JZVD.jzvdContext);
                JZUtils.hideSystemUI(Jzvd.CURRENT_JZVD.jzvdContext);
            }
        }
    }

    public static void goOnPlayOnPause() {
        if (CURRENT_JZVD != null) {
            if (Jzvd.CURRENT_JZVD.state == 7 || Jzvd.CURRENT_JZVD.state == 0 || Jzvd.CURRENT_JZVD.state == 8) {
                Jzvd.releaseAllVideos();
            } else if (Jzvd.CURRENT_JZVD.state == 1) {
                Jzvd.setCurrentJzvd(CURRENT_JZVD);
                Jzvd.CURRENT_JZVD.state = 1;
            } else {
                ON_PLAY_PAUSE_TMP_STATE = Jzvd.CURRENT_JZVD.state;
                CURRENT_JZVD.onStatePause();
                Jzvd.CURRENT_JZVD.mediaInterface.pause();
            }
        }
    }

    public static void startFullscreenDirectly(Context context, Class _class, String url, String title) {
        Jzvd.startFullscreenDirectly(context, _class, new JZDataSource(url, title));
    }

    public static void startFullscreenDirectly(Context context, Class _class, JZDataSource jzDataSource) {
        JZUtils.hideStatusBar(context);
        JZUtils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(context);
        ViewGroup vp = (ViewGroup)JZUtils.scanForActivity(context).getWindow().getDecorView();
        try {
            Constructor constructor = _class.getConstructor(Context.class);
            Jzvd jzvd = (Jzvd)((Object)constructor.newInstance(context));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
            vp.addView((View)jzvd, (ViewGroup.LayoutParams)lp);
            jzvd.setUp(jzDataSource, 1);
            jzvd.startVideo();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void releaseAllVideos() {
        Log.d((String)TAG, (String)"releaseAllVideos");
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.reset();
            CURRENT_JZVD = null;
        }
        CONTAINER_LIST.clear();
    }

    public static boolean backPress() {
        Log.i((String)TAG, (String)"backPress");
        if (CONTAINER_LIST.size() != 0 && CURRENT_JZVD != null) {
            CURRENT_JZVD.gotoNormalScreen();
            return true;
        }
        if (CONTAINER_LIST.size() == 0 && CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.screen != 0) {
            CURRENT_JZVD.clearFloatScreen();
            return true;
        }
        return false;
    }

    public static void setCurrentJzvd(Jzvd jzvd) {
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.reset();
        }
        CURRENT_JZVD = jzvd;
    }

    public static void setTextureViewRotation(int rotation) {
        if (CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.textureView != null) {
            Jzvd.CURRENT_JZVD.textureView.setRotation(rotation);
        }
    }

    public static void setVideoImageDisplayType(int type) {
        VIDEO_IMAGE_DISPLAY_TYPE = type;
        if (CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.textureView != null) {
            Jzvd.CURRENT_JZVD.textureView.requestLayout();
        }
    }

    public abstract int getLayoutId();

    public void init(Context context) {
        View.inflate((Context)context, (int)this.getLayoutId(), (ViewGroup)this);
        this.jzvdContext = context;
        this.startButton = (ImageView)this.findViewById(R.id.start);
        this.fullscreenButton = (ImageView)this.findViewById(R.id.fullscreen);
        this.progressBar = (SeekBar)this.findViewById(R.id.bottom_seek_progress);
        this.currentTimeTextView = (TextView)this.findViewById(R.id.current);
        this.totalTimeTextView = (TextView)this.findViewById(R.id.total);
        this.bottomContainer = (ViewGroup)this.findViewById(R.id.layout_bottom);
        this.textureViewContainer = (ViewGroup)this.findViewById(R.id.surface_container);
        this.topContainer = (ViewGroup)this.findViewById(R.id.layout_top);
        if (this.startButton == null) {
            this.startButton = new ImageView(context);
        }
        if (this.fullscreenButton == null) {
            this.fullscreenButton = new ImageView(context);
        }
        if (this.progressBar == null) {
            this.progressBar = new SeekBar(context);
        }
        if (this.currentTimeTextView == null) {
            this.currentTimeTextView = new TextView(context);
        }
        if (this.totalTimeTextView == null) {
            this.totalTimeTextView = new TextView(context);
        }
        if (this.bottomContainer == null) {
            this.bottomContainer = new LinearLayout(context);
        }
        if (this.textureViewContainer == null) {
            this.textureViewContainer = new FrameLayout(context);
        }
        if (this.topContainer == null) {
            this.topContainer = new RelativeLayout(context);
        }
        this.startButton.setOnClickListener((View.OnClickListener)this);
        this.fullscreenButton.setOnClickListener((View.OnClickListener)this);
        this.progressBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.bottomContainer.setOnClickListener((View.OnClickListener)this);
        this.textureViewContainer.setOnClickListener((View.OnClickListener)this);
        this.textureViewContainer.setOnTouchListener((View.OnTouchListener)this);
        this.mScreenWidth = this.getContext().getResources().getDisplayMetrics().widthPixels;
        this.mScreenHeight = this.getContext().getResources().getDisplayMetrics().heightPixels;
        this.state = -1;
    }

    public void setUp(String url, String title) {
        this.setUp(new JZDataSource(url, title), 0);
    }

    public void setUp(String url, String title, int screen) {
        this.setUp(new JZDataSource(url, title), screen);
    }

    public void setUp(JZDataSource jzDataSource, int screen) {
        this.setUp(jzDataSource, screen, JZMediaSystem.class);
    }

    public void setUp(String url, String title, int screen, Class mediaInterfaceClass) {
        this.setUp(new JZDataSource(url, title), screen, mediaInterfaceClass);
    }

    public void setUp(JZDataSource jzDataSource, int screen, Class mediaInterfaceClass) {
        this.jzDataSource = jzDataSource;
        this.screen = screen;
        this.onStateNormal();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    public void setMediaInterface(Class mediaInterfaceClass) {
        this.reset();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            this.clickStart();
        } else if (i == R.id.fullscreen) {
            this.clickFullscreen();
        }
    }

    protected void clickFullscreen() {
        Log.i((String)TAG, (String)("onClick fullscreen [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.state == 7) {
            return;
        }
        if (this.screen == 1) {
            Jzvd.backPress();
        } else {
            Log.d((String)TAG, (String)("toFullscreenActivity [" + ((Object)((Object)this)).hashCode() + "] "));
            this.gotoFullscreen();
        }
    }

    protected void clickStart() {
        Log.i((String)TAG, (String)("onClick start [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.jzDataSource == null || this.jzDataSource.urlsMap.isEmpty() || this.jzDataSource.getCurrentUrl() == null) {
            Toast.makeText((Context)this.getContext(), (CharSequence)this.getResources().getString(R.string.no_url), (int)0).show();
            return;
        }
        if (this.state == 0) {
            if (!(this.jzDataSource.getCurrentUrl().toString().startsWith("file") || this.jzDataSource.getCurrentUrl().toString().startsWith("/") || JZUtils.isWifiConnected(this.getContext()) || WIFI_TIP_DIALOG_SHOWED)) {
                this.showWifiDialog();
                return;
            }
            this.startVideo();
        } else if (this.state == 5) {
            Log.d((String)TAG, (String)("pauseVideo [" + ((Object)((Object)this)).hashCode() + "] "));
            this.mediaInterface.pause();
            this.onStatePause();
        } else if (this.state == 6) {
            this.mediaInterface.start();
            this.onStatePlaying();
        } else if (this.state == 7) {
            this.startVideo();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id2 = v.getId();
        if (id2 == R.id.surface_container) {
            switch (event.getAction()) {
                case 0: {
                    this.touchActionDown(x, y);
                    break;
                }
                case 2: {
                    this.touchActionMove(x, y);
                    break;
                }
                case 1: {
                    this.touchActionUp();
                }
            }
        }
        return false;
    }

    protected void touchActionUp() {
        Log.i((String)TAG, (String)("onTouch surfaceContainer actionUp [" + ((Object)((Object)this)).hashCode() + "] "));
        this.mTouchingProgressBar = false;
        this.dismissProgressDialog();
        this.dismissVolumeDialog();
        this.dismissBrightnessDialog();
        if (this.mChangePosition) {
            this.mediaInterface.seekTo(this.mSeekTimePosition);
            long duration = this.getDuration();
            int progress = (int)(this.mSeekTimePosition * 100L / (duration == 0L ? 1L : duration));
            this.progressBar.setProgress(progress);
        }
        if (this.mChangeVolume) {
            // empty if block
        }
        this.startProgressTimer();
    }

    protected void touchActionMove(float x, float y) {
        Log.i((String)TAG, (String)("onTouch surfaceContainer actionMove [" + ((Object)((Object)this)).hashCode() + "] "));
        float deltaX = x - this.mDownX;
        float deltaY = y - this.mDownY;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        if (this.screen == 1) {
            if (this.mDownX > (float)JZUtils.getScreenWidth(this.getContext()) || this.mDownY < (float)JZUtils.getStatusBarHeight(this.getContext())) {
                return;
            }
            if (!(this.mChangePosition || this.mChangeVolume || this.mChangeBrightness || !(absDeltaX > 80.0f) && !(absDeltaY > 80.0f))) {
                this.cancelProgressTimer();
                if (absDeltaX >= 80.0f) {
                    if (this.state != 8) {
                        this.mChangePosition = true;
                        this.mGestureDownPosition = this.getCurrentPositionWhenPlaying();
                    }
                } else if (this.mDownX < (float)this.mScreenHeight * 0.5f) {
                    this.mChangeBrightness = true;
                    WindowManager.LayoutParams lp = JZUtils.getWindow(this.getContext()).getAttributes();
                    if (lp.screenBrightness < 0.0f) {
                        try {
                            this.mGestureDownBrightness = Settings.System.getInt((ContentResolver)this.getContext().getContentResolver(), (String)"screen_brightness");
                            Log.i((String)TAG, (String)("current system brightness: " + this.mGestureDownBrightness));
                        }
                        catch (Settings.SettingNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        this.mGestureDownBrightness = lp.screenBrightness * 255.0f;
                        Log.i((String)TAG, (String)("current activity brightness: " + this.mGestureDownBrightness));
                    }
                } else {
                    this.mChangeVolume = true;
                    this.mGestureDownVolume = this.mAudioManager.getStreamVolume(3);
                }
            }
        }
        if (this.mChangePosition) {
            long totalTimeDuration = this.getDuration();
            if (PROGRESS_DRAG_RATE <= 0.0f) {
                Log.d((String)TAG, (String)"error PROGRESS_DRAG_RATE value");
                PROGRESS_DRAG_RATE = 1.0f;
            }
            this.mSeekTimePosition = (int)((float)this.mGestureDownPosition + deltaX * (float)totalTimeDuration / ((float)this.mScreenWidth * PROGRESS_DRAG_RATE));
            if (this.mSeekTimePosition > totalTimeDuration) {
                this.mSeekTimePosition = totalTimeDuration;
            }
            String seekTime = JZUtils.stringForTime(this.mSeekTimePosition);
            String totalTime = JZUtils.stringForTime(totalTimeDuration);
            this.showProgressDialog(deltaX, seekTime, this.mSeekTimePosition, totalTime, totalTimeDuration);
        }
        if (this.mChangeVolume) {
            deltaY = -deltaY;
            int max = this.mAudioManager.getStreamMaxVolume(3);
            int deltaV = (int)((float)max * deltaY * 3.0f / (float)this.mScreenHeight);
            this.mAudioManager.setStreamVolume(3, this.mGestureDownVolume + deltaV, 0);
            int volumePercent = (int)((float)(this.mGestureDownVolume * 100 / max) + deltaY * 3.0f * 100.0f / (float)this.mScreenHeight);
            this.showVolumeDialog(-deltaY, volumePercent);
        }
        if (this.mChangeBrightness) {
            deltaY = -deltaY;
            int deltaV = (int)(255.0f * deltaY * 3.0f / (float)this.mScreenHeight);
            WindowManager.LayoutParams params = JZUtils.getWindow(this.getContext()).getAttributes();
            params.screenBrightness = (this.mGestureDownBrightness + (float)deltaV) / 255.0f >= 1.0f ? 1.0f : ((this.mGestureDownBrightness + (float)deltaV) / 255.0f <= 0.0f ? 0.01f : (this.mGestureDownBrightness + (float)deltaV) / 255.0f);
            JZUtils.getWindow(this.getContext()).setAttributes(params);
            int brightnessPercent = (int)(this.mGestureDownBrightness * 100.0f / 255.0f + deltaY * 3.0f * 100.0f / (float)this.mScreenHeight);
            this.showBrightnessDialog(brightnessPercent);
        }
    }

    protected void touchActionDown(float x, float y) {
        Log.i((String)TAG, (String)("onTouch surfaceContainer actionDown [" + ((Object)((Object)this)).hashCode() + "] "));
        this.mTouchingProgressBar = true;
        this.mDownX = x;
        this.mDownY = y;
        this.mChangeVolume = false;
        this.mChangePosition = false;
        this.mChangeBrightness = false;
    }

    public void onStateNormal() {
        Log.i((String)TAG, (String)("onStateNormal  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 0;
        this.cancelProgressTimer();
        if (this.mediaInterface != null) {
            this.mediaInterface.release();
        }
    }

    public void onStatePreparing() {
        Log.i((String)TAG, (String)("onStatePreparing  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 1;
        this.resetProgressAndTime();
    }

    public void onStatePreparingPlaying() {
        Log.i((String)TAG, (String)("onStatePreparingPlaying  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 3;
    }

    public void onStatePreparingChangeUrl() {
        Log.i((String)TAG, (String)("onStatePreparingChangeUrl  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 2;
        Jzvd.releaseAllVideos();
        this.startVideo();
    }

    public void changeUrl(JZDataSource jzDataSource, long seekToInAdvance) {
        this.jzDataSource = jzDataSource;
        this.seekToInAdvance = seekToInAdvance;
        this.onStatePreparingChangeUrl();
    }

    public void onPrepared() {
        Log.i((String)TAG, (String)("onPrepared  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 4;
        if (!this.preloading) {
            this.mediaInterface.start();
            this.preloading = false;
        }
        if (this.jzDataSource.getCurrentUrl().toString().toLowerCase().contains("mp3") || this.jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wma") || this.jzDataSource.getCurrentUrl().toString().toLowerCase().contains("aac") || this.jzDataSource.getCurrentUrl().toString().toLowerCase().contains("m4a") || this.jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wav")) {
            this.onStatePlaying();
        }
    }

    public void startPreloading() {
        this.preloading = true;
        this.startVideo();
    }

    public void startVideoAfterPreloading() {
        if (this.state == 4) {
            this.mediaInterface.start();
        } else {
            this.preloading = false;
            this.startVideo();
        }
    }

    public void onStatePlaying() {
        Log.i((String)TAG, (String)("onStatePlaying  [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.state == 4) {
            Log.d((String)TAG, (String)"onStatePlaying:STATE_PREPARED ");
            this.mAudioManager = (AudioManager)this.getApplicationContext().getSystemService("audio");
            this.mAudioManager.requestAudioFocus(onAudioFocusChangeListener, 3, 2);
            if (this.seekToInAdvance != 0L) {
                this.mediaInterface.seekTo(this.seekToInAdvance);
                this.seekToInAdvance = 0L;
            } else {
                long position = JZUtils.getSavedProgress(this.getContext(), this.jzDataSource.getCurrentUrl());
                if (position != 0L) {
                    this.mediaInterface.seekTo(position);
                }
            }
        }
        this.state = 5;
        this.startProgressTimer();
    }

    public void onStatePause() {
        Log.i((String)TAG, (String)("onStatePause  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 6;
        this.startProgressTimer();
    }

    public void onStateError() {
        Log.i((String)TAG, (String)("onStateError  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 8;
        this.cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Log.i((String)TAG, (String)("onStateAutoComplete  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.state = 7;
        this.cancelProgressTimer();
        this.progressBar.setProgress(100);
        this.currentTimeTextView.setText(this.totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        Log.d((String)TAG, (String)("onInfo what - " + what + " extra - " + extra));
        if (what == 3) {
            Log.d((String)TAG, (String)"MEDIA_INFO_VIDEO_RENDERING_START");
            if (this.state == 4 || this.state == 2 || this.state == 3) {
                this.onStatePlaying();
            }
        } else if (what == 701) {
            Log.d((String)TAG, (String)"MEDIA_INFO_BUFFERING_START");
            backUpBufferState = this.state;
            this.setState(3);
        } else if (what == 702) {
            Log.d((String)TAG, (String)"MEDIA_INFO_BUFFERING_END");
            if (backUpBufferState != -1) {
                this.setState(backUpBufferState);
                backUpBufferState = -1;
            }
        }
    }

    public void onError(int what, int extra) {
        Log.e((String)TAG, (String)("onError " + what + " - " + extra + " [" + ((Object)((Object)this)).hashCode() + "] "));
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            this.onStateError();
            this.mediaInterface.release();
        }
    }

    public void onCompletion() {
        Runtime.getRuntime().gc();
        Log.i((String)TAG, (String)("onAutoCompletion  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.cancelProgressTimer();
        this.dismissBrightnessDialog();
        this.dismissProgressDialog();
        this.dismissVolumeDialog();
        this.onStateAutoComplete();
        this.mediaInterface.release();
        JZUtils.scanForActivity(this.getContext()).getWindow().clearFlags(128);
        JZUtils.saveProgress(this.getContext(), this.jzDataSource.getCurrentUrl(), 0L);
        if (this.screen == 1) {
            if (CONTAINER_LIST.size() == 0) {
                this.clearFloatScreen();
            } else {
                this.gotoNormalCompletion();
            }
        }
    }

    public void gotoNormalCompletion() {
        this.gobakFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup)JZUtils.scanForActivity(this.jzvdContext).getWindow().getDecorView();
        vg.removeView((View)this);
        this.textureViewContainer.removeView((View)this.textureView);
        CONTAINER_LIST.getLast().removeViewAt(this.blockIndex);
        CONTAINER_LIST.getLast().addView((View)this, this.blockIndex, this.blockLayoutParams);
        CONTAINER_LIST.pop();
        this.setScreenNormal();
        JZUtils.showStatusBar(this.jzvdContext);
        JZUtils.setRequestedOrientation(this.jzvdContext, NORMAL_ORIENTATION);
        JZUtils.showSystemUI(this.jzvdContext);
    }

    public void reset() {
        Log.i((String)TAG, (String)("reset  [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.state == 5 || this.state == 6) {
            long position = this.getCurrentPositionWhenPlaying();
            JZUtils.saveProgress(this.getContext(), this.jzDataSource.getCurrentUrl(), position);
        }
        this.cancelProgressTimer();
        this.dismissBrightnessDialog();
        this.dismissProgressDialog();
        this.dismissVolumeDialog();
        this.onStateNormal();
        this.textureViewContainer.removeAllViews();
        AudioManager mAudioManager = (AudioManager)this.getApplicationContext().getSystemService("audio");
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        JZUtils.scanForActivity(this.getContext()).getWindow().clearFlags(128);
        if (this.mediaInterface != null) {
            this.mediaInterface.release();
        }
    }

    public void setState(int state) {
        switch (state) {
            case 0: {
                this.onStateNormal();
                break;
            }
            case 1: {
                this.onStatePreparing();
                break;
            }
            case 3: {
                this.onStatePreparingPlaying();
                break;
            }
            case 2: {
                this.onStatePreparingChangeUrl();
                break;
            }
            case 5: {
                this.onStatePlaying();
                break;
            }
            case 6: {
                this.onStatePause();
                break;
            }
            case 8: {
                this.onStateError();
                break;
            }
            case 7: {
                this.onStateAutoComplete();
            }
        }
    }

    public void setScreen(int screen) {
        switch (screen) {
            case 0: {
                this.setScreenNormal();
                break;
            }
            case 1: {
                this.setScreenFullscreen();
                break;
            }
            case 2: {
                this.setScreenTiny();
            }
        }
    }

    public void startVideo() {
        Log.d((String)TAG, (String)("startVideo [" + ((Object)((Object)this)).hashCode() + "] "));
        Jzvd.setCurrentJzvd(this);
        try {
            Constructor constructor = this.mediaInterfaceClass.getConstructor(Jzvd.class);
            this.mediaInterface = (JZMediaInterface)constructor.newInstance(new Object[]{this});
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        this.addTextureView();
        JZUtils.scanForActivity(this.getContext()).getWindow().addFlags(128);
        this.onStatePreparing();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.screen == 1 || this.screen == 2) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (this.widthRatio != 0 && this.heightRatio != 0) {
            int specWidth = View.MeasureSpec.getSize((int)widthMeasureSpec);
            int specHeight = (int)((float)specWidth * (float)this.heightRatio / (float)this.widthRatio);
            this.setMeasuredDimension(specWidth, specHeight);
            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((int)specWidth, (int)0x40000000);
            int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec((int)specHeight, (int)0x40000000);
            this.getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void addTextureView() {
        Log.d((String)TAG, (String)("addTextureView [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.textureView != null) {
            this.textureViewContainer.removeView((View)this.textureView);
        }
        this.textureView = new JZTextureView(this.getContext().getApplicationContext());
        this.textureView.setSurfaceTextureListener(this.mediaInterface);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1, 17);
        this.textureViewContainer.addView((View)this.textureView, (ViewGroup.LayoutParams)layoutParams);
    }

    public void clearFloatScreen() {
        JZUtils.showStatusBar(this.getContext());
        JZUtils.setRequestedOrientation(this.getContext(), NORMAL_ORIENTATION);
        JZUtils.showSystemUI(this.getContext());
        ViewGroup vg = (ViewGroup)JZUtils.scanForActivity(this.getContext()).getWindow().getDecorView();
        vg.removeView((View)this);
        if (this.mediaInterface != null) {
            this.mediaInterface.release();
        }
        CURRENT_JZVD = null;
    }

    public void onVideoSizeChanged(int width, int height) {
        Log.i((String)TAG, (String)("onVideoSizeChanged  [" + ((Object)((Object)this)).hashCode() + "] "));
        if (this.textureView != null) {
            if (this.videoRotation != 0) {
                this.textureView.setRotation(this.videoRotation);
            }
            this.textureView.setVideoSize(width, height);
        }
    }

    public void startProgressTimer() {
        Log.i((String)TAG, (String)("startProgressTimer:  [" + ((Object)((Object)this)).hashCode() + "] "));
        this.cancelProgressTimer();
        this.UPDATE_PROGRESS_TIMER = new Timer();
        this.mProgressTimerTask = new ProgressTimerTask();
        this.UPDATE_PROGRESS_TIMER.schedule((TimerTask)this.mProgressTimerTask, 0L, 300L);
    }

    public void cancelProgressTimer() {
        if (this.UPDATE_PROGRESS_TIMER != null) {
            this.UPDATE_PROGRESS_TIMER.cancel();
        }
        if (this.mProgressTimerTask != null) {
            this.mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
        this.mCurrentPosition = position;
        if (!this.mTouchingProgressBar) {
            if (this.seekToManulPosition != -1) {
                if (this.seekToManulPosition > progress) {
                    return;
                }
                this.seekToManulPosition = -1;
            } else {
                this.progressBar.setProgress(progress);
            }
        }
        if (position != 0L) {
            this.currentTimeTextView.setText((CharSequence)JZUtils.stringForTime(position));
        }
        this.totalTimeTextView.setText((CharSequence)JZUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        this.progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        this.mCurrentPosition = 0L;
        this.progressBar.setProgress(0);
        this.progressBar.setSecondaryProgress(0);
        this.currentTimeTextView.setText((CharSequence)JZUtils.stringForTime(0L));
        this.totalTimeTextView.setText((CharSequence)JZUtils.stringForTime(0L));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0L;
        if (this.state == 5 || this.state == 6 || this.state == 3) {
            try {
                position = this.mediaInterface.getCurrentPosition();
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0L;
        try {
            duration = this.mediaInterface.getDuration();
        }
        catch (Exception e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i((String)TAG, (String)("bottomProgress onStartTrackingTouch [" + ((Object)((Object)this)).hashCode() + "] "));
        this.cancelProgressTimer();
        for (ViewParent vpdown = this.getParent(); vpdown != null; vpdown = vpdown.getParent()) {
            vpdown.requestDisallowInterceptTouchEvent(true);
        }
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i((String)TAG, (String)("bottomProgress onStopTrackingTouch [" + ((Object)((Object)this)).hashCode() + "] "));
        this.startProgressTimer();
        for (ViewParent vpup = this.getParent(); vpup != null; vpup = vpup.getParent()) {
            vpup.requestDisallowInterceptTouchEvent(false);
        }
        if (this.state != 5 && this.state != 6) {
            return;
        }
        long time = (long)seekBar.getProgress() * this.getDuration() / 100L;
        this.seekToManulPosition = seekBar.getProgress();
        this.mediaInterface.seekTo(time);
        Log.i((String)TAG, (String)("seekTo " + time + " [" + ((Object)((Object)this)).hashCode() + "] "));
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            long duration = this.getDuration();
            this.currentTimeTextView.setText((CharSequence)JZUtils.stringForTime((long)progress * duration / 100L));
        }
    }

    public void cloneAJzvd(ViewGroup vg) {
        try {
            Constructor<?> constructor = ((Object)((Object)this)).getClass().getConstructor(Context.class);
            Jzvd jzvd = (Jzvd)((Object)constructor.newInstance(this.getContext()));
            jzvd.setId(this.getId());
            jzvd.setMinimumWidth(this.blockWidth);
            jzvd.setMinimumHeight(this.blockHeight);
            vg.addView((View)jzvd, this.blockIndex, this.blockLayoutParams);
            jzvd.setUp(this.jzDataSource.cloneMe(), 0, this.mediaInterfaceClass);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void gotoFullscreen() {
        this.gotoFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup)this.getParent();
        this.jzvdContext = vg.getContext();
        this.blockLayoutParams = this.getLayoutParams();
        this.blockIndex = vg.indexOfChild((View)this);
        this.blockWidth = this.getWidth();
        this.blockHeight = this.getHeight();
        vg.removeView((View)this);
        this.cloneAJzvd(vg);
        CONTAINER_LIST.add(vg);
        vg = (ViewGroup)JZUtils.scanForActivity(this.jzvdContext).getWindow().getDecorView();
        FrameLayout.LayoutParams fullLayout = new FrameLayout.LayoutParams(-1, -1);
        vg.addView((View)this, (ViewGroup.LayoutParams)fullLayout);
        this.setScreenFullscreen();
        JZUtils.hideStatusBar(this.jzvdContext);
        JZUtils.setRequestedOrientation(this.jzvdContext, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(this.jzvdContext);
    }

    public void gotoNormalScreen() {
        this.gobakFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup)JZUtils.scanForActivity(this.jzvdContext).getWindow().getDecorView();
        vg.removeView((View)this);
        CONTAINER_LIST.getLast().removeViewAt(this.blockIndex);
        CONTAINER_LIST.getLast().addView((View)this, this.blockIndex, this.blockLayoutParams);
        CONTAINER_LIST.pop();
        this.setScreenNormal();
        JZUtils.showStatusBar(this.jzvdContext);
        JZUtils.setRequestedOrientation(this.jzvdContext, NORMAL_ORIENTATION);
        JZUtils.showSystemUI(this.jzvdContext);
    }

    public void setScreenNormal() {
        this.screen = 0;
    }

    public void setScreenFullscreen() {
        this.screen = 1;
    }

    public void setScreenTiny() {
        this.screen = 2;
    }

    public void autoFullscreen(float x) {
        if (CURRENT_JZVD != null && (this.state == 5 || this.state == 6) && this.screen != 1 && this.screen != 2) {
            if (x > 0.0f) {
                JZUtils.setRequestedOrientation(this.getContext(), 0);
            } else {
                JZUtils.setRequestedOrientation(this.getContext(), 8);
            }
            this.gotoFullscreen();
        }
    }

    public void autoQuitFullscreen() {
        if (System.currentTimeMillis() - lastAutoFullscreenTime > 2000L && this.state == 5 && this.screen == 1) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            Jzvd.backPress();
        }
    }

    public void onSeekComplete() {
    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {
    }

    public void showVolumeDialog(float deltaY, int volumePercent) {
    }

    public void dismissVolumeDialog() {
    }

    public void showBrightnessDialog(int brightnessPercent) {
    }

    public void dismissBrightnessDialog() {
    }

    public Context getApplicationContext() {
        Context applicationContext;
        Context context = this.getContext();
        if (context != null && (applicationContext = context.getApplicationContext()) != null) {
            return applicationContext;
        }
        return context;
    }

    static {
        CONTAINER_LIST = new LinkedList();
        TOOL_BAR_EXIST = true;
        FULLSCREEN_ORIENTATION = 6;
        NORMAL_ORIENTATION = 1;
        SAVE_PROGRESS = true;
        WIFI_TIP_DIALOG_SHOWED = false;
        VIDEO_IMAGE_DISPLAY_TYPE = 0;
        lastAutoFullscreenTime = 0L;
        ON_PLAY_PAUSE_TMP_STATE = 0;
        backUpBufferState = -1;
        PROGRESS_DRAG_RATE = 1.0f;
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener(){

            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case 1: {
                        break;
                    }
                    case -1: {
                        Jzvd.releaseAllVideos();
                        Log.d((String)Jzvd.TAG, (String)("AUDIOFOCUS_LOSS [" + this.hashCode() + "]"));
                        break;
                    }
                    case -2: {
                        try {
                            Jzvd player = CURRENT_JZVD;
                            if (player != null && player.state == 5) {
                                player.startButton.performClick();
                            }
                        }
                        catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        Log.d((String)Jzvd.TAG, (String)("AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]"));
                        break;
                    }
                }
            }
        };
    }

    public class ProgressTimerTask
    extends TimerTask {
        @Override
        public void run() {
            if (Jzvd.this.state == 5 || Jzvd.this.state == 6 || Jzvd.this.state == 3) {
                Jzvd.this.post(() -> {
                    long position = Jzvd.this.getCurrentPositionWhenPlaying();
                    long duration = Jzvd.this.getDuration();
                    int progress = (int)(position * 100L / (duration == 0L ? 1L : duration));
                    Jzvd.this.onProgress(progress, position, duration);
                });
            }
        }
    }

    public static class JZAutoFullscreenListener
    implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if ((x < -12.0f || x > 12.0f) && System.currentTimeMillis() - lastAutoFullscreenTime > 2000L) {
                if (CURRENT_JZVD != null) {
                    CURRENT_JZVD.autoFullscreen(x);
                }
                lastAutoFullscreenTime = System.currentTimeMillis();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}


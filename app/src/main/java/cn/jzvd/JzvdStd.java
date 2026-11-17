/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import cn.jzvd.JZDataSource;
import cn.jzvd.JZUtils;
import cn.jzvd.Jzvd;
import com.yenaly.han1meviewer.R;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class JzvdStd
extends Jzvd {
    public static long LAST_GET_BATTERYLEVEL_TIME = 0L;
    public static int LAST_GET_BATTERYLEVEL_PERCENT = 70;
    protected static Timer DISMISS_CONTROL_VIEW_TIMER;
    public ImageView backButton;
    public ProgressBar bottomProgressBar;
    public ProgressBar loadingProgressBar;
    public TextView titleTextView;
    public ImageView posterImageView;
    public ImageView tinyBackImageView;
    public LinearLayout batteryTimeLayout;
    public ImageView batteryLevel;
    public TextView videoCurrentTime;
    public TextView replayTextView;
    public TextView clarity;
    public PopupWindow clarityPopWindow;
    public TextView mRetryBtn;
    public LinearLayout mRetryLayout;
    public BroadcastReceiver battertReceiver = new BroadcastReceiver(){

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int percent;
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                LAST_GET_BATTERYLEVEL_PERCENT = percent = level * 100 / scale;
                JzvdStd.this.setBatteryLevel();
                try {
                    JzvdStd.this.jzvdContext.unregisterReceiver(JzvdStd.this.battertReceiver);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    protected DismissControlViewTimerTask mDismissControlViewTimerTask;
    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;
    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    protected TextView mDialogVolumeTextView;
    protected ImageView mDialogVolumeImageView;
    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;
    protected boolean mIsWifi;
    public BroadcastReceiver wifiReceiver = new BroadcastReceiver(){

        public void onReceive(Context context, Intent intent) {
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                boolean isWifi = JZUtils.isWifiConnected(context);
                if (JzvdStd.this.mIsWifi == isWifi) {
                    return;
                }
                JzvdStd.this.mIsWifi = isWifi;
                if (!JzvdStd.this.mIsWifi && !Jzvd.WIFI_TIP_DIALOG_SHOWED && JzvdStd.this.state == 5) {
                    JzvdStd.this.startButton.performClick();
                    JzvdStd.this.showWifiDialog();
                }
            }
        }
    };
    protected ArrayDeque<Runnable> delayTask = new ArrayDeque();
    protected GestureDetector gestureDetector = new GestureDetector(this.getContext().getApplicationContext(), (GestureDetector.OnGestureListener)new GestureDetector.SimpleOnGestureListener(){

        public boolean onDoubleTap(MotionEvent e) {
            if (JzvdStd.this.state == 5 || JzvdStd.this.state == 6) {
                Log.d((String)"JZVD", (String)("doublClick [" + ((Object)((Object)this)).hashCode() + "] "));
                JzvdStd.this.startButton.performClick();
            }
            return super.onDoubleTap(e);
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!JzvdStd.this.mChangePosition && !JzvdStd.this.mChangeVolume) {
                JzvdStd.this.onClickUiToggle();
            }
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }
    });

    public JzvdStd(Context context) {
        super(context);
    }

    public JzvdStd(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        this.batteryTimeLayout = (LinearLayout)this.findViewById(R.id.battery_time_layout);
        this.bottomProgressBar = (ProgressBar)this.findViewById(R.id.bottom_progress);
        this.titleTextView = (TextView)this.findViewById(R.id.title);
        this.backButton = (ImageView)this.findViewById(R.id.back);
        this.posterImageView = (ImageView)this.findViewById(R.id.poster);
        this.loadingProgressBar = (ProgressBar)this.findViewById(R.id.loading);
        this.tinyBackImageView = (ImageView)this.findViewById(R.id.back_tiny);
        this.batteryLevel = (ImageView)this.findViewById(R.id.battery_level);
        this.videoCurrentTime = (TextView)this.findViewById(R.id.video_current_time);
        this.replayTextView = (TextView)this.findViewById(R.id.replay_text);
        this.clarity = (TextView)this.findViewById(R.id.clarity);
        this.mRetryBtn = (TextView)this.findViewById(R.id.retry_btn);
        this.mRetryLayout = (LinearLayout)this.findViewById(R.id.retry_layout);
        if (this.batteryTimeLayout == null) {
            this.batteryTimeLayout = new LinearLayout(context);
        }
        if (this.bottomProgressBar == null) {
            this.bottomProgressBar = new ProgressBar(context);
        }
        if (this.titleTextView == null) {
            this.titleTextView = new TextView(context);
        }
        if (this.backButton == null) {
            this.backButton = new ImageView(context);
        }
        if (this.posterImageView == null) {
            this.posterImageView = new ImageView(context);
        }
        if (this.loadingProgressBar == null) {
            this.loadingProgressBar = new ProgressBar(context);
        }
        if (this.tinyBackImageView == null) {
            this.tinyBackImageView = new ImageView(context);
        }
        if (this.batteryLevel == null) {
            this.batteryLevel = new ImageView(context);
        }
        if (this.videoCurrentTime == null) {
            this.videoCurrentTime = new TextView(context);
        }
        if (this.replayTextView == null) {
            this.replayTextView = new TextView(context);
        }
        if (this.clarity == null) {
            this.clarity = new TextView(context);
        }
        if (this.mRetryBtn == null) {
            this.mRetryBtn = new TextView(context);
        }
        if (this.mRetryLayout == null) {
            this.mRetryLayout = new LinearLayout(context);
        }
        this.posterImageView.setOnClickListener((View.OnClickListener)this);
        this.backButton.setOnClickListener((View.OnClickListener)this);
        this.tinyBackImageView.setOnClickListener((View.OnClickListener)this);
        this.clarity.setOnClickListener((View.OnClickListener)this);
        this.mRetryBtn.setOnClickListener((View.OnClickListener)this);
    }

    @Override
    public void setUp(JZDataSource jzDataSource, int screen, Class mediaInterfaceClass) {
        if (System.currentTimeMillis() - this.gobakFullscreenTime < 200L) {
            return;
        }
        if (System.currentTimeMillis() - this.gotoFullscreenTime < 200L) {
            return;
        }
        super.setUp(jzDataSource, screen, mediaInterfaceClass);
        this.titleTextView.setText((CharSequence)jzDataSource.title);
        this.setScreen(screen);
    }

    @Override
    public void changeUrl(JZDataSource jzDataSource, long seekToInAdvance) {
        super.changeUrl(jzDataSource, seekToInAdvance);
        this.titleTextView.setText((CharSequence)jzDataSource.title);
    }

    public void changeStartButtonSize(int size) {
        ViewGroup.LayoutParams lp = this.startButton.getLayoutParams();
        lp.height = size;
        lp.width = size;
        lp = this.loadingProgressBar.getLayoutParams();
        lp.height = size;
        lp.width = size;
    }

    @Override
    public int getLayoutId() {
        return R.layout.jz_layout_std;
    }

    @Override
    public void onStateNormal() {
        super.onStateNormal();
        this.changeUiToNormal();
    }

    @Override
    public void onStatePreparing() {
        super.onStatePreparing();
        this.changeUiToPreparing();
    }

    @Override
    public void onStatePreparingPlaying() {
        super.onStatePreparingPlaying();
        this.changeUIToPreparingPlaying();
    }

    @Override
    public void onStatePreparingChangeUrl() {
        super.onStatePreparingChangeUrl();
        this.changeUIToPreparingChangeUrl();
    }

    @Override
    public void onStatePlaying() {
        super.onStatePlaying();
        this.changeUiToPlayingClear();
    }

    @Override
    public void onStatePause() {
        super.onStatePause();
        this.changeUiToPauseShow();
        this.cancelDismissControlViewTimer();
    }

    @Override
    public void onStateError() {
        super.onStateError();
        this.changeUiToError();
    }

    @Override
    public void onStateAutoComplete() {
        super.onStateAutoComplete();
        this.changeUiToComplete();
        this.cancelDismissControlViewTimer();
        this.bottomProgressBar.setProgress(100);
    }

    @Override
    public void startVideo() {
        super.startVideo();
        this.registerWifiListener(this.getApplicationContext());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id2 = v.getId();
        if (id2 == R.id.surface_container) {
            switch (event.getAction()) {
                case 0: 
                case 2: {
                    break;
                }
                case 1: {
                    this.startDismissControlViewTimer();
                    if (!this.mChangePosition) break;
                    long duration = this.getDuration();
                    int progress = (int)(this.mSeekTimePosition * 100L / (duration == 0L ? 1L : duration));
                    this.bottomProgressBar.setProgress(progress);
                }
            }
            this.gestureDetector.onTouchEvent(event);
        } else if (id2 == R.id.bottom_seek_progress) {
            switch (event.getAction()) {
                case 0: {
                    this.cancelDismissControlViewTimer();
                    break;
                }
                case 1: {
                    this.startDismissControlViewTimer();
                }
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.poster) {
            this.clickPoster();
        } else if (i == R.id.surface_container) {
            this.clickSurfaceContainer();
            if (this.clarityPopWindow != null) {
                this.clarityPopWindow.dismiss();
            }
        } else if (i == R.id.back) {
            this.clickBack();
        } else if (i == R.id.back_tiny) {
            this.clickBackTiny();
        } else if (i == R.id.clarity) {
            this.clickClarity();
        } else if (i == R.id.retry_btn) {
            this.clickRetryBtn();
        }
    }

    protected void clickRetryBtn() {
        if (this.jzDataSource.urlsMap.isEmpty() || this.jzDataSource.getCurrentUrl() == null) {
            Toast.makeText((Context)this.jzvdContext, (CharSequence)this.getResources().getString(R.string.no_url), (int)0).show();
            return;
        }
        if (!(this.jzDataSource.getCurrentUrl().toString().startsWith("file") || this.jzDataSource.getCurrentUrl().toString().startsWith("/") || JZUtils.isWifiConnected(this.jzvdContext) || WIFI_TIP_DIALOG_SHOWED)) {
            this.showWifiDialog();
            return;
        }
        this.seekToInAdvance = this.mCurrentPosition;
        this.startVideo();
    }

    protected void clickClarity() {
        this.onCLickUiToggleToClear();
        LayoutInflater inflater = (LayoutInflater)this.jzvdContext.getSystemService("layout_inflater");
        LinearLayout layout2 = (LinearLayout)inflater.inflate(R.layout.jz_layout_clarity, null);
        View.OnClickListener mQualityListener = v1 -> {
            int index;
            this.jzDataSource.currentUrlIndex = index = ((Integer)v1.getTag()).intValue();
            this.changeUrl(this.jzDataSource, this.getCurrentPositionWhenPlaying());
            this.clarity.setText((CharSequence)this.jzDataSource.getCurrentKey().toString());
            for (int j = 0; j < layout2.getChildCount(); ++j) {
                if (j == this.jzDataSource.currentUrlIndex) {
                    ((TextView)layout2.getChildAt(j)).setTextColor(Color.parseColor((String)"#fff85959"));
                    continue;
                }
                ((TextView)layout2.getChildAt(j)).setTextColor(Color.parseColor((String)"#ffffff"));
            }
            if (this.clarityPopWindow != null) {
                this.clarityPopWindow.dismiss();
            }
        };
        for (int j = 0; j < this.jzDataSource.urlsMap.size(); ++j) {
            String key = this.jzDataSource.getKeyFromDataSource(j);
            TextView clarityItem = (TextView)View.inflate((Context)this.jzvdContext, (int)R.layout.jz_layout_clarity_item, null);
            clarityItem.setText((CharSequence)key);
            clarityItem.setTag((Object)j);
            layout2.addView((View)clarityItem, j);
            clarityItem.setOnClickListener(mQualityListener);
            if (j != this.jzDataSource.currentUrlIndex) continue;
            clarityItem.setTextColor(Color.parseColor((String)"#fff85959"));
        }
        this.clarityPopWindow = new PopupWindow((View)layout2, JZUtils.dip2px(this.jzvdContext, 240.0f), -1, true);
        this.clarityPopWindow.setContentView((View)layout2);
        this.clarityPopWindow.setAnimationStyle(R.style.pop_animation);
        this.clarityPopWindow.showAtLocation((View)this.textureViewContainer, 0x800005, 0, 0);
    }

    protected void clickBackTiny() {
        this.clearFloatScreen();
    }

    protected void clickBack() {
        JzvdStd.backPress();
    }

    protected void clickSurfaceContainer() {
        this.startDismissControlViewTimer();
    }

    protected void clickPoster() {
        if (this.jzDataSource == null || this.jzDataSource.urlsMap.isEmpty() || this.jzDataSource.getCurrentUrl() == null) {
            Toast.makeText((Context)this.jzvdContext, (CharSequence)this.getResources().getString(R.string.no_url), (int)0).show();
            return;
        }
        if (this.state == 0) {
            if (!(this.jzDataSource.getCurrentUrl().toString().startsWith("file") || this.jzDataSource.getCurrentUrl().toString().startsWith("/") || JZUtils.isWifiConnected(this.jzvdContext) || WIFI_TIP_DIALOG_SHOWED)) {
                this.showWifiDialog();
                return;
            }
            this.startVideo();
        } else if (this.state == 7) {
            this.onClickUiToggle();
        }
    }

    @Override
    public void setScreenNormal() {
        super.setScreenNormal();
        this.fullscreenButton.setImageResource(R.drawable.jz_enlarge);
        this.backButton.setVisibility(8);
        this.tinyBackImageView.setVisibility(4);
        this.changeStartButtonSize((int)this.getResources().getDimension(R.dimen.jz_start_button_w_h_normal));
        this.batteryTimeLayout.setVisibility(8);
        this.clarity.setVisibility(8);
    }

    @Override
    public void setScreenFullscreen() {
        super.setScreenFullscreen();
        this.fullscreenButton.setImageResource(R.drawable.jz_shrink);
        this.backButton.setVisibility(0);
        this.tinyBackImageView.setVisibility(4);
        this.batteryTimeLayout.setVisibility(0);
        if (this.jzDataSource.urlsMap.size() == 1) {
            this.clarity.setVisibility(8);
        } else {
            this.clarity.setText((CharSequence)this.jzDataSource.getCurrentKey().toString());
            this.clarity.setVisibility(0);
        }
        this.changeStartButtonSize((int)this.getResources().getDimension(R.dimen.jz_start_button_w_h_fullscreen));
        this.setSystemTimeAndBattery();
    }

    @Override
    public void setScreenTiny() {
        super.setScreenTiny();
        this.tinyBackImageView.setVisibility(0);
        this.setAllControlsVisiblity(4, 4, 4, 4, 4, 4, 4);
        this.batteryTimeLayout.setVisibility(8);
        this.clarity.setVisibility(8);
    }

    @Override
    public void showWifiDialog() {
        super.showWifiDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this.jzvdContext);
        builder.setMessage((CharSequence)this.getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton((CharSequence)this.getResources().getString(R.string.tips_not_wifi_confirm), (dialog, which) -> {
            dialog.dismiss();
            WIFI_TIP_DIALOG_SHOWED = true;
            if (this.state == 6) {
                this.startButton.performClick();
            } else {
                this.startVideo();
            }
        });
        builder.setNegativeButton((CharSequence)this.getResources().getString(R.string.tips_not_wifi_cancel), (dialog, which) -> {
            dialog.dismiss();
            JzvdStd.releaseAllVideos();
            this.clearFloatScreen();
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener(){

            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                Jzvd.releaseAllVideos();
                JzvdStd.this.clearFloatScreen();
            }
        });
        builder.create().show();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        this.cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        this.startDismissControlViewTimer();
    }

    public void onClickUiToggle() {
        if (this.bottomContainer.getVisibility() != 0) {
            this.setSystemTimeAndBattery();
            this.clarity.setText((CharSequence)this.jzDataSource.getCurrentKey().toString());
        }
        if (this.state == 1) {
            this.changeUiToPreparing();
            if (this.bottomContainer.getVisibility() != 0) {
                this.setSystemTimeAndBattery();
            }
        } else if (this.state == 5) {
            if (this.bottomContainer.getVisibility() == 0) {
                this.changeUiToPlayingClear();
            } else {
                this.changeUiToPlayingShow();
            }
        } else if (this.state == 6) {
            if (this.bottomContainer.getVisibility() == 0) {
                this.changeUiToPauseClear();
            } else {
                this.changeUiToPauseShow();
            }
        }
    }

    public void setSystemTimeAndBattery() {
        SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        this.videoCurrentTime.setText((CharSequence)dateFormater.format(date));
        if (System.currentTimeMillis() - LAST_GET_BATTERYLEVEL_TIME > 30000L) {
            LAST_GET_BATTERYLEVEL_TIME = System.currentTimeMillis();
            this.jzvdContext.registerReceiver(this.battertReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        } else {
            this.setBatteryLevel();
        }
    }

    public void setBatteryLevel() {
        int percent = LAST_GET_BATTERYLEVEL_PERCENT;
        if (percent < 15) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_10);
        } else if (percent >= 15 && percent < 40) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_30);
        } else if (percent >= 40 && percent < 60) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_50);
        } else if (percent >= 60 && percent < 80) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_70);
        } else if (percent >= 80 && percent < 95) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_90);
        } else if (percent >= 95 && percent <= 100) {
            this.batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_100);
        }
    }

    public void onCLickUiToggleToClear() {
        if (this.state == 1) {
            if (this.bottomContainer.getVisibility() == 0) {
                this.changeUiToPreparing();
            }
        } else if (this.state == 5) {
            if (this.bottomContainer.getVisibility() == 0) {
                this.changeUiToPlayingClear();
            }
        } else if (this.state == 6) {
            if (this.bottomContainer.getVisibility() == 0) {
                this.changeUiToPauseClear();
            }
        } else if (this.state == 7 && this.bottomContainer.getVisibility() == 0) {
            this.changeUiToComplete();
        }
    }

    @Override
    public void onProgress(int progress, long position, long duration) {
        super.onProgress(progress, position, duration);
        this.bottomProgressBar.setProgress(progress);
    }

    @Override
    public void setBufferProgress(int bufferProgress) {
        super.setBufferProgress(bufferProgress);
        this.bottomProgressBar.setSecondaryProgress(bufferProgress);
    }

    @Override
    public void resetProgressAndTime() {
        super.resetProgressAndTime();
        this.bottomProgressBar.setProgress(0);
        this.bottomProgressBar.setSecondaryProgress(0);
    }

    public void changeUiToNormal() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(0, 4, 0, 4, 0, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUiToPreparing() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(4, 4, 4, 0, 0, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUIToPreparingPlaying() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(0, 0, 4, 0, 4, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUIToPreparingChangeUrl() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(4, 4, 4, 0, 0, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUiToPlayingShow() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(0, 0, 0, 4, 4, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUiToPlayingClear() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(4, 4, 4, 4, 4, 0, 4);
                break;
            }
        }
    }

    public void changeUiToPauseShow() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(0, 0, 0, 4, 4, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUiToPauseClear() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(4, 4, 4, 4, 4, 0, 4);
                break;
            }
        }
    }

    public void changeUiToComplete() {
        switch (this.screen) {
            case 0: 
            case 1: {
                this.setAllControlsVisiblity(0, 4, 0, 4, 0, 4, 4);
                this.updateStartImage();
                break;
            }
        }
    }

    public void changeUiToError() {
        switch (this.screen) {
            case 0: {
                this.setAllControlsVisiblity(4, 4, 0, 4, 4, 4, 0);
                this.updateStartImage();
                break;
            }
            case 1: {
                this.setAllControlsVisiblity(0, 4, 0, 4, 4, 4, 0);
                this.updateStartImage();
                break;
            }
        }
    }

    public void setAllControlsVisiblity(int topCon, int bottomCon, int startBtn, int loadingPro, int posterImg, int bottomPro, int retryLayout) {
        this.topContainer.setVisibility(topCon);
        this.bottomContainer.setVisibility(bottomCon);
        this.startButton.setVisibility(startBtn);
        this.loadingProgressBar.setVisibility(loadingPro);
        this.posterImageView.setVisibility(posterImg);
        this.bottomProgressBar.setVisibility(bottomPro);
        this.mRetryLayout.setVisibility(retryLayout);
    }

    public void updateStartImage() {
        if (this.state == 5) {
            this.startButton.setVisibility(0);
            this.startButton.setImageResource(R.drawable.jz_click_pause_selector);
            this.replayTextView.setVisibility(8);
        } else if (this.state == 8) {
            this.startButton.setVisibility(4);
            this.replayTextView.setVisibility(8);
        } else if (this.state == 7) {
            this.startButton.setVisibility(0);
            this.startButton.setImageResource(R.drawable.jz_click_replay_selector);
            this.replayTextView.setVisibility(0);
        } else {
            this.startButton.setImageResource(R.drawable.jz_click_play_selector);
            this.replayTextView.setVisibility(8);
        }
    }

    @Override
    public void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (this.mProgressDialog == null) {
            View localView = LayoutInflater.from((Context)this.jzvdContext).inflate(R.layout.jz_dialog_progress, null);
            this.mDialogProgressBar = (ProgressBar)localView.findViewById(R.id.duration_progressbar);
            this.mDialogSeekTime = (TextView)localView.findViewById(R.id.tv_current);
            this.mDialogTotalTime = (TextView)localView.findViewById(R.id.tv_duration);
            this.mDialogIcon = (ImageView)localView.findViewById(R.id.duration_image_tip);
            this.mProgressDialog = this.createDialogWithView(localView);
        }
        if (!this.mProgressDialog.isShowing()) {
            this.mProgressDialog.show();
        }
        this.mDialogSeekTime.setText((CharSequence)seekTime);
        this.mDialogTotalTime.setText((CharSequence)(" / " + totalTime));
        this.mDialogProgressBar.setProgress(totalTimeDuration <= 0L ? 0 : (int)(seekTimePosition * 100L / totalTimeDuration));
        if (deltaX > 0.0f) {
            this.mDialogIcon.setBackgroundResource(R.drawable.jz_forward_icon);
        } else {
            this.mDialogIcon.setBackgroundResource(R.drawable.jz_backward_icon);
        }
        this.onCLickUiToggleToClear();
    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
        }
    }

    @Override
    public void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (this.mVolumeDialog == null) {
            View localView = LayoutInflater.from((Context)this.jzvdContext).inflate(R.layout.jz_dialog_volume, null);
            this.mDialogVolumeImageView = (ImageView)localView.findViewById(R.id.volume_image_tip);
            this.mDialogVolumeTextView = (TextView)localView.findViewById(R.id.tv_volume);
            this.mDialogVolumeProgressBar = (ProgressBar)localView.findViewById(R.id.volume_progressbar);
            this.mVolumeDialog = this.createDialogWithView(localView);
        }
        if (!this.mVolumeDialog.isShowing()) {
            this.mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            this.mDialogVolumeImageView.setBackgroundResource(R.drawable.jz_close_volume);
        } else {
            this.mDialogVolumeImageView.setBackgroundResource(R.drawable.jz_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
        }
        this.mDialogVolumeTextView.setText((CharSequence)(volumePercent + "%"));
        this.mDialogVolumeProgressBar.setProgress(volumePercent);
        this.onCLickUiToggleToClear();
    }

    @Override
    public void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (this.mVolumeDialog != null) {
            this.mVolumeDialog.dismiss();
        }
    }

    @Override
    public void showBrightnessDialog(int brightnessPercent) {
        super.showBrightnessDialog(brightnessPercent);
        if (this.mBrightnessDialog == null) {
            View localView = LayoutInflater.from((Context)this.jzvdContext).inflate(R.layout.jz_dialog_brightness, null);
            this.mDialogBrightnessTextView = (TextView)localView.findViewById(R.id.tv_brightness);
            this.mDialogBrightnessProgressBar = (ProgressBar)localView.findViewById(R.id.brightness_progressbar);
            this.mBrightnessDialog = this.createDialogWithView(localView);
        }
        if (!this.mBrightnessDialog.isShowing()) {
            this.mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }
        this.mDialogBrightnessTextView.setText((CharSequence)(brightnessPercent + "%"));
        this.mDialogBrightnessProgressBar.setProgress(brightnessPercent);
        this.onCLickUiToggleToClear();
    }

    @Override
    public void dismissBrightnessDialog() {
        super.dismissBrightnessDialog();
        if (this.mBrightnessDialog != null) {
            this.mBrightnessDialog.dismiss();
        }
    }

    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(this.jzvdContext, R.style.jz_style_dialog_progress);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        window.addFlags(8);
        window.addFlags(32);
        window.addFlags(16);
        window.setLayout(-2, -2);
        WindowManager.LayoutParams localLayoutParams = window.getAttributes();
        localLayoutParams.gravity = 17;
        window.setAttributes(localLayoutParams);
        return dialog;
    }

    public void startDismissControlViewTimer() {
        this.cancelDismissControlViewTimer();
        DISMISS_CONTROL_VIEW_TIMER = new Timer();
        this.mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISMISS_CONTROL_VIEW_TIMER.schedule((TimerTask)this.mDismissControlViewTimerTask, 2500L);
    }

    public void cancelDismissControlViewTimer() {
        if (DISMISS_CONTROL_VIEW_TIMER != null) {
            DISMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (this.mDismissControlViewTimerTask != null) {
            this.mDismissControlViewTimerTask.cancel();
        }
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        this.cancelDismissControlViewTimer();
    }

    @Override
    public void reset() {
        super.reset();
        this.cancelDismissControlViewTimer();
        this.unregisterWifiListener(this.getApplicationContext());
    }

    public void dissmissControlView() {
        if (this.state != 0 && this.state != 8 && this.state != 7) {
            this.post(() -> {
                this.bottomContainer.setVisibility(4);
                this.topContainer.setVisibility(4);
                this.startButton.setVisibility(4);
                if (this.screen != 2) {
                    this.bottomProgressBar.setVisibility(0);
                }
            });
        }
    }

    public void registerWifiListener(Context context) {
        if (context == null) {
            return;
        }
        this.mIsWifi = JZUtils.isWifiConnected(context);
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(this.wifiReceiver, intentFilter);
    }

    public void unregisterWifiListener(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.unregisterReceiver(this.wifiReceiver);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public class DismissControlViewTimerTask
    extends TimerTask {
        @Override
        public void run() {
            JzvdStd.this.dissmissControlView();
        }
    }
}


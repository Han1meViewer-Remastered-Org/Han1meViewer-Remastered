/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import cn.jzvd.Jzvd;

public class JZTextureView
extends TextureView {
    protected static final String TAG = "JZResizeTextureView";
    public int currentVideoWidth = 0;
    public int currentVideoHeight = 0;

    public JZTextureView(Context context) {
        super(context);
    }

    public JZTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setVideoSize(int currentVideoWidth, int currentVideoHeight) {
        if (this.currentVideoWidth != currentVideoWidth || this.currentVideoHeight != currentVideoHeight) {
            this.currentVideoWidth = currentVideoWidth;
            this.currentVideoHeight = currentVideoHeight;
            this.requestLayout();
        }
    }

    public void setRotation(float rotation) {
        if (rotation != this.getRotation()) {
            super.setRotation(rotation);
            this.requestLayout();
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i((String)TAG, (String)("onMeasure  [" + ((Object)((Object)this)).hashCode() + "] "));
        int viewRotation = (int)this.getRotation();
        int videoWidth = this.currentVideoWidth;
        int videoHeight = this.currentVideoHeight;
        int parentHeight = ((View)this.getParent()).getMeasuredHeight();
        int parentWidth = ((View)this.getParent()).getMeasuredWidth();
        if (parentWidth != 0 && parentHeight != 0 && videoWidth != 0 && videoHeight != 0 && Jzvd.VIDEO_IMAGE_DISPLAY_TYPE == 1) {
            if (viewRotation == 90 || viewRotation == 270) {
                int tempSize = parentWidth;
                parentWidth = parentHeight;
                parentHeight = tempSize;
            }
            videoHeight = videoWidth * parentHeight / parentWidth;
        }
        if (viewRotation == 90 || viewRotation == 270) {
            int tempMeasureSpec = widthMeasureSpec;
            widthMeasureSpec = heightMeasureSpec;
            heightMeasureSpec = tempMeasureSpec;
        }
        int width = JZTextureView.getDefaultSize((int)videoWidth, (int)widthMeasureSpec);
        int height = JZTextureView.getDefaultSize((int)videoHeight, (int)heightMeasureSpec);
        if (videoWidth > 0 && videoHeight > 0) {
            int widthSpecMode = View.MeasureSpec.getMode((int)widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize((int)widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode((int)heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize((int)heightMeasureSpec);
            Log.i((String)TAG, (String)("widthMeasureSpec  [" + View.MeasureSpec.toString((int)widthMeasureSpec) + "]"));
            Log.i((String)TAG, (String)("heightMeasureSpec [" + View.MeasureSpec.toString((int)heightMeasureSpec) + "]"));
            if (widthSpecMode == 0x40000000 && heightSpecMode == 0x40000000) {
                height = heightSpecSize;
                width = widthSpecSize;
                if (videoWidth * height < width * videoHeight) {
                    width = height * videoWidth / videoHeight;
                } else if (videoWidth * height > width * videoHeight) {
                    height = width * videoHeight / videoWidth;
                }
            } else if (widthSpecMode == 0x40000000) {
                width = widthSpecSize;
                height = width * videoHeight / videoWidth;
                if (heightSpecMode == Integer.MIN_VALUE && height > heightSpecSize) {
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
            } else if (heightSpecMode == 0x40000000) {
                height = heightSpecSize;
                width = height * videoWidth / videoHeight;
                if (widthSpecMode == Integer.MIN_VALUE && width > widthSpecSize) {
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            } else {
                width = videoWidth;
                height = videoHeight;
                if (heightSpecMode == Integer.MIN_VALUE && height > heightSpecSize) {
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
                if (widthSpecMode == Integer.MIN_VALUE && width > widthSpecSize) {
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            }
        }
        if (parentWidth != 0 && parentHeight != 0 && videoWidth != 0 && videoHeight != 0) {
            if (Jzvd.VIDEO_IMAGE_DISPLAY_TYPE == 3) {
                height = videoHeight;
                width = videoWidth;
            } else if (Jzvd.VIDEO_IMAGE_DISPLAY_TYPE == 2) {
                if (viewRotation == 90 || viewRotation == 270) {
                    int tempSize = parentWidth;
                    parentWidth = parentHeight;
                    parentHeight = tempSize;
                }
                if ((double)videoHeight / (double)videoWidth > (double)parentHeight / (double)parentWidth) {
                    height = (int)((double)parentWidth / (double)width * (double)height);
                    width = parentWidth;
                } else if ((double)videoHeight / (double)videoWidth < (double)parentHeight / (double)parentWidth) {
                    width = (int)((double)parentHeight / (double)height * (double)width);
                    height = parentHeight;
                }
            }
        }
        this.setMeasuredDimension(width, height);
    }
}


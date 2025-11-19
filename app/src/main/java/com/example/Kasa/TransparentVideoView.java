package com.example.Kasa;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.widget.VideoView;

public class TransparentVideoView extends VideoView {
    public TransparentVideoView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
    }
}

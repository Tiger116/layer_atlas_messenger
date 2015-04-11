package com.layer.quick_start_android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.content.Context;

public class RoundedCorners extends ImageView {

    private int RADIUS = 0;

    public RoundedCorners(Context context) {
        super(context);
    }

    public RoundedCorners(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedCorners(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {


        Drawable maiDrawable = getDrawable();

        if (maiDrawable!=null && maiDrawable instanceof BitmapDrawable && RADIUS > 0)
        {

            Paint paint = ((BitmapDrawable) maiDrawable).getPaint();
            final int color = 0xff000000;
            Rect bitmapBounds = maiDrawable.getBounds();
            final RectF rectF = new RectF(bitmapBounds);
            // Create an off-screen bitmap to the PorterDuff alpha blending to work right
            int saveCount = canvas.saveLayer(rectF, null,
                    Canvas.MATRIX_SAVE_FLAG |
                            Canvas.CLIP_SAVE_FLAG |
                            Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                            Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                            Canvas.CLIP_TO_LAYER_SAVE_FLAG);
            // Resize the rounded rect we'll clip by this view's current bounds
            // (super.onDraw() will do something similar with the drawable to draw)

            getImageMatrix().mapRect(rectF);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, RADIUS, RADIUS, paint);

            Xfermode oldMode = paint.getXfermode();
            // This is the paint already associated with the BitmapDrawable that super draws
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            super.onDraw(canvas);
            paint.setXfermode(oldMode);
            canvas.restoreToCount(saveCount);
        } else {
            super.onDraw(canvas);
        }
    }

    public void setRadius(int radius){
        this.RADIUS = radius;
    }
}
package com.layer.messenger;

import android.graphics.Bitmap;

public class ImageParams {
    private int width;
    private int orientation;
    private int height;

    public ImageParams(Bitmap image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        if (width > height)
            this.orientation = 0;
        else
            this.orientation = 1;

    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }
}

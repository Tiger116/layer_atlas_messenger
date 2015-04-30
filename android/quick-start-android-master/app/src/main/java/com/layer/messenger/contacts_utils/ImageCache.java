package com.layer.messenger.contacts_utils;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.layer.messenger.BuildConfig;

public class ImageCache {
    private static final float CACHE_PERCENTAGE = 0.1f;
    private static final String TAG = "ImageCache";
    private LruCache<String, Bitmap> mMemoryCache;
    public static final ImageCache INSTANCE = new ImageCache(CACHE_PERCENTAGE);

    private ImageCache(float memCacheSizePercent) {
        init(memCacheSizePercent);
    }

    private void init(float memCacheSizePercent) {
        int memCacheSize = calculateMemCacheSize(memCacheSizePercent);
        // Set up memory cache
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Memory cache created (size = " + memCacheSize + ")");
        mMemoryCache = new LruCache<String, Bitmap>(memCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                final int bitmapSize = getBitmapSize(bitmap) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };
    }

    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null)
            return;
        // Add to memory cache
        if (mMemoryCache != null && mMemoryCache.get(data) == null)
            mMemoryCache.put(data, bitmap);
    }

    public Bitmap getBitmapFromMemCache(String data) {
        if (mMemoryCache != null) {
            final Bitmap memBitmap = mMemoryCache.get(data);
            if (memBitmap != null) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Memory cache hit");
                return memBitmap;
            }
        }
        return null;
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
            return bitmap.getByteCount();
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static int calculateMemCacheSize(float percent) {
        if (percent < 0.05f || percent > 0.8f) {
            throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                    + "between 0.05 and 0.8 (inclusive)");
        }
        return Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
    }

}

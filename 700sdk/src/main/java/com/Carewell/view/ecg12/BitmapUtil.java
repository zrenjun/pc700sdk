package com.Carewell.view.ecg12;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {

    public static Bitmap bytesToBimap(byte[] b) {
        if (b == null || b.length == 0) return null;

        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    public static Bitmap scaleTo(Bitmap bitmap, int width, int height) {

        if (bitmap == null) return null;
        int w, h;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            if (bitmap.getHeight() <= height) return bitmap;
            h = height;
            w = (int) ((100f / bitmap.getHeight()) * bitmap.getWidth());
        } else {
            if (bitmap.getWidth() <= width) return bitmap;
            w = width;
            h = (int) ((100f / bitmap.getWidth()) * bitmap.getHeight());
        }
        return Bitmap.createScaledBitmap(bitmap, w, h, true);
    }
}

package com.framgia.gifcreator.effect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Created by VULAN on 7/1/2016.
 */
public class ContrastEffect extends EditingEffect {
    private float mValue;

    public ContrastEffect(float mValue) {
        this.mValue = mValue;
    }

    public Bitmap applyContrast(Bitmap baseBitmap) {
        int width = baseBitmap.getWidth();
        int height = baseBitmap.getHeight();
        float[] contrastMatrix = {
                1f, 0, 0, 0, mValue,
                0, 1f, 0, 0, mValue,
                0, 0, 1f, 0, mValue,
                0, 0, 0, 1f, 0
        };
        ColorMatrix colorMatrix = new ColorMatrix(contrastMatrix);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        Bitmap contrastBitmap = Bitmap.createBitmap(width, height, baseBitmap.getConfig());
        Canvas canvas = new Canvas(contrastBitmap);
        canvas.drawBitmap(baseBitmap, 0, 0, paint);
        return contrastBitmap;
    }

    @Override
    public Bitmap applyEffect(Bitmap baseBitmap) {
        return applyContrast(baseBitmap);
    }
}

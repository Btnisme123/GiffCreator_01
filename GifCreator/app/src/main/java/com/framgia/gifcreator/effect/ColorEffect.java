package com.framgia.gifcreator.effect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Created by VULAN on 6/13/2016.
 */
public class ColorEffect extends EditingEffect {

    private float mValue;
    private Type mType;

    public ColorEffect(float mValue, Type mType) {
        this.mValue = mValue;
        this.mType = mType;
    }

    public Bitmap setColor(Bitmap baseBitmap) {
        int width = baseBitmap.getWidth();
        int height = baseBitmap.getHeight();
        float[] colorTransform = new float[]{
                1f, 0, 0, 0, 0,
                0, 1f, 0, 0, 0,
                0, 0, 1f, 0, 0,
                0, 0, 0, 1f, 0};
        switch (mType) {
            case RED:
                colorTransform = new float[]{
                        1f, 0, 0, 0, mValue,
                        0, 1f, 0, 0, 0,
                        0, 0, 1f, 0, 0,
                        0, 0, 0, 1f, 0};
                break;
            case GREEN:
                colorTransform = new float[]{
                        1f, 0, 0, 0, 0,
                        0, 1f, 0, 0, mValue,
                        0, 0, 1f, 0, 0,
                        0, 0, 0, 1f, 0};
                break;
            case BLUE:
                colorTransform = new float[]{
                        1f, 0, 0, 0, 0,
                        0, 1f, 0, 0, 0,
                        0, 0, 1f, 0, mValue,
                        0, 0, 0, 1f, 0};
                break;
            case ALPHA:
                colorTransform = new float[]{
                        1f, 0, 0, 0, 0,
                        0, 1f, 0, 0, 0,
                        0, 0, 1f, 0, 0,
                        0, 0, 0, 1f, mValue};
                break;
        }
        ColorMatrix colorMatrix = new ColorMatrix(colorTransform);
        Bitmap colorBitmap = Bitmap.createBitmap(width, height, baseBitmap.getConfig());
        Canvas canvas = new Canvas(colorBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(baseBitmap, 0, 0, paint);
        return colorBitmap;
    }

    @Override
    public Bitmap applyEffect(Bitmap baseBitmap) {
        return setColor(baseBitmap);
    }

    public enum Type {
        RED,
        GREEN,
        BLUE,
        ALPHA
    }
}

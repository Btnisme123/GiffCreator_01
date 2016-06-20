package com.framgia.gifcreator.effect;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class RotationEffect extends EditingEffect {

    public Bitmap rotate(Bitmap mBitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
    }

    @Override
    public Bitmap applyEffect(Bitmap baseBitmap) {
        return rotate(baseBitmap);
    }
}

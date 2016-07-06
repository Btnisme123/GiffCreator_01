package com.framgia.gifcreator.effect;

import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicConvolve3x3;

import com.framgia.gifcreator.ui.activity.AdjustImageActivity;

/**
 * Created by yue on 04/07/2016.
 */
public class EdgeDetect extends EditingEffect {

    @Override
    public Bitmap applyEffect(Bitmap baseBitmap) {
        float[] sharp =
                {
                        -1.0f, -1.0f, -1.0f,
                        -1.0f, 8.05f, -1.0f,
                        -1.0f, -1.0f, -1.0f
                };

        Bitmap bitmap = Bitmap.createBitmap(
                baseBitmap.getWidth(), baseBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(AdjustImageActivity.sContext);

        Allocation allocIn = Allocation.createFromBitmap(rs, baseBitmap);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicConvolve3x3 convolution
                = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolution.setInput(allocIn);
        convolution.setCoefficients(sharp);
        convolution.forEach(allocOut);

        allocOut.copyTo(bitmap);
        rs.destroy();

        return bitmap;
    }
}

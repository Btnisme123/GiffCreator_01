package com.framgia.gifcreator.effect.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by HungNT on 5/2/16.
 */
public class CropImageView extends ImageView {

    private HighlightView mHighlightView;
    private RotateBitmap mDisplayBitmap = new RotateBitmap(null, 0);

    private Matrix mSuppMatrix = new Matrix();

    private int mViewWidth;
    private int mViewHeight;

    private Runnable mLayoutRunnable;

    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mViewWidth = right - left;
        mViewHeight = bottom - top;
        Runnable r = mLayoutRunnable;

        if (r != null) {
            mLayoutRunnable = null;
            r.run();
        }

        if (mDisplayBitmap.getBitmap() != null) {
            getProperBaseMatrix(mDisplayBitmap, mBaseMatrix, true);
            setImageMatrix(getImageViewMatrix());
        }
    }

    public void setDefault() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        final int imgW = mDisplayBitmap.getWidth();
        final int imgH = mDisplayBitmap.getHeight();

        // set the crop width & height = 4/5 of image size
        int cropW = Math.min(imgW, imgH) * 4 / 5;
        int cropH = cropW;

        int x = (imgW - cropW) / 2;
        int y = (imgH - cropH) / 2;

        RectF cropRect = new RectF(x, y, x + cropW, y + cropH);
        RectF imageRect = new RectF(0, 0, imgW, imgH);

        mHighlightView.setup(getUnrotatedMatrix(), cropRect, imageRect);

        // Center the highlight view
        center();

        mHighlightView.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mHighlightView != null) {
            return mHighlightView.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mHighlightView != null)
            mHighlightView.onDraw(canvas);
    }

    private void init() {
        mHighlightView = new HighlightView(this);
        setScaleType(ScaleType.MATRIX);
    }

    public void center() {
        final Bitmap bitmap = mDisplayBitmap.getBitmap();
        if (bitmap == null) {
            return;
        }

        Matrix m = getImageMatrix();

        RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        m.mapRect(rectF);

        float width = rectF.width();
        float height = rectF.height();

        float deltaX = 0, deltaY = 0;

        deltaX = centerHorizontal(rectF, width, deltaX);
        deltaY = centerVertical(rectF, height, deltaY);

        postTranslate(deltaX, deltaY);
        setImageMatrix(getImageMatrix());
    }

    private float centerVertical(RectF rect, float height, float deltaY) {
        int viewHeight = getHeight();
        if (height < viewHeight) {
            deltaY = (viewHeight - height) / 2 - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = getHeight() - rect.bottom;
        }
        return deltaY;
    }

    private float centerHorizontal(RectF rect, float width, float deltaX) {
        int viewWidth = getWidth();
        if (width < viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }
        return deltaX;
    }

    private void postTranslate(float dx, float dy) {
        mSuppMatrix.postTranslate(dx, dy);
    }

    /**
     * Setup the base matrix
     */
    private void getProperBaseMatrix(RotateBitmap bitmap, Matrix matrix, boolean includeRotation) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float w = bitmap.getWidth();
        float h = bitmap.getHeight();
        matrix.reset();

        float widthScale = Math.min(viewWidth / w, 3.0f);
        float heightScale = Math.min(viewHeight / h, 3.0f);
        float scale = Math.min(widthScale, heightScale);

        if (includeRotation) {
            matrix.postConcat(bitmap.getRotateMatrix());
        }

        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - w * scale) / 2F, (viewHeight - h * scale) / 2F);
    }

    /**
     * Combine the base matrix and the supp matrix to make the final matrix
     */
    protected Matrix getImageViewMatrix() {
        mDisplayMatrix.set(mBaseMatrix);
        mDisplayMatrix.postConcat(mSuppMatrix);
        return mDisplayMatrix;
    }

    // Get the matrix if rotate image, will use in last update
    public Matrix getUnrotatedMatrix() {
        Matrix unrotated = new Matrix();
        getProperBaseMatrix(mDisplayBitmap, unrotated, false);
        unrotated.postConcat(mSuppMatrix);
        return unrotated;
    }

    public void setImageBitmap(final RotateBitmap bitmap) {

        // If this method run before onLayout, wait it complete
        if (mViewWidth <= 0) {
            mLayoutRunnable = new Runnable() {
                public void run() {
                    setImageBitmap(bitmap);
                }
            };
            return;
        }

        if (bitmap.getBitmap() != null) {
            getProperBaseMatrix(bitmap, mBaseMatrix, true);
            setBitmapDisplayed(bitmap.getBitmap(), bitmap.getRotation());
        } else {
            mBaseMatrix.reset();
            setBitmapDisplayed(null, 0);
        }

        mSuppMatrix.reset();
        setImageMatrix(getImageViewMatrix());

        setDefault();
    }

    public void setBitmapDisplayed(Bitmap bitmap, int rotation) {
        super.setImageBitmap(bitmap);
        Drawable d = getDrawable();
        if (d != null) {
            d.setDither(true);
        }

        mDisplayBitmap.setBitmap(bitmap);
        mDisplayBitmap.setRotation(rotation);
    }

    public HighlightView getHighlightView() {
        return mHighlightView;
    }


    public void clearHighlight() {
        mHighlightView = null;
    }
}

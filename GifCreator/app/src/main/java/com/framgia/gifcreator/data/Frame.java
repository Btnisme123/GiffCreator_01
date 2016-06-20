package com.framgia.gifcreator.data;

import android.graphics.Bitmap;

/**
 * Created by yue on 14/06/2016.
 */
public class Frame {

    private String mPhotoPath;
    private Bitmap mFrame;
    private boolean mIsChosen;

    public Frame() {
    }

    public Frame(String photoPath) {
        mPhotoPath = photoPath;
    }

    public Frame(String photoPath, Bitmap frame) {
        mPhotoPath = photoPath;
        mFrame = frame;
    }

    public void setPhotoPath(String photoPath) {
        mPhotoPath = photoPath;
    }

    public String getPhotoPath() {
        return mPhotoPath;
    }

    public void setFrame(Bitmap frame) {
        mFrame = frame;
    }

    public Bitmap getFrame() {
        return mFrame;
    }

    public void destroy() {
        if (mFrame != null) {
            mFrame.recycle();
            mFrame = null;
        }
    }

    public void setStatus(boolean isChosen) {
        this.mIsChosen = isChosen;
    }

    public boolean isChosen() {
        return mIsChosen;
    }
}
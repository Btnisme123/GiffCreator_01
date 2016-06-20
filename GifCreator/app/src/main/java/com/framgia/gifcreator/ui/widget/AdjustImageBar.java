package com.framgia.gifcreator.ui.widget;

import android.content.Context;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.framgia.gifcreator.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yue on 13/06/2016.
 */
public class AdjustImageBar extends RelativeLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    public final static int BUTTON_CANCEL = 0;
    public final static int BUTTON_COMPLETE = 1;
    private SeekBar mAdjustSeekBar;
    private OnAdjustImageBarItemInteractListener mOnAdjustImageBarItemInteractListener;

    public AdjustImageBar(Context context) {
        super(context);
        init(context);
    }

    public AdjustImageBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setOnAdjustImageBarItemInteractListener(
            OnAdjustImageBarItemInteractListener onAdjustImageBarItemInteractListener) {
        mOnAdjustImageBarItemInteractListener = onAdjustImageBarItemInteractListener;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_cancel:
                if (mOnAdjustImageBarItemInteractListener != null) {
                    mOnAdjustImageBarItemInteractListener.onButtonClickListener(BUTTON_CANCEL);
                }
                break;
            case R.id.button_complete:
                if (mOnAdjustImageBarItemInteractListener != null) {
                    mOnAdjustImageBarItemInteractListener.onButtonClickListener(BUTTON_COMPLETE);
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mOnAdjustImageBarItemInteractListener != null) {
            mOnAdjustImageBarItemInteractListener.onSeekBarValueChange(seekBar.getProgress());
        }
    }

    public void setMaxValue(int max) {
        mAdjustSeekBar.setMax(max);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.adjust_image_bar, this);
        mAdjustSeekBar = (SeekBar) findViewById(R.id.adjust_seek_bar);
        mAdjustSeekBar.setOnSeekBarChangeListener(this);
        findViewById(R.id.button_cancel).setOnClickListener(this);
        findViewById(R.id.button_complete).setOnClickListener(this);
    }

    public void setProgress(int progress) {
        if(mAdjustSeekBar!=null)mAdjustSeekBar.setProgress(progress);
    }

    public interface OnAdjustImageBarItemInteractListener {
        void onButtonClickListener(@AdjustImageBarButtonDef int button);

        void onSeekBarValueChange(int progress);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BUTTON_CANCEL, BUTTON_COMPLETE
    })
    public @interface AdjustImageBarButtonDef {
    }
}

package com.framgia.gifcreator.ui.decoration;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.util.AppHelper;

/**
 * Created by yue on 28/06/2016.
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {

    private Context mContext;

    public GridItemDecoration(Context context) {
        mContext = context;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (parent.getChildLayoutPosition(view) == 0 || parent.getChildLayoutPosition(view) == 1) {
            outRect.top = AppHelper.getDimen(mContext, R.dimen.common_size_6);
        } else {
            outRect.top = 0;
        }
        if (parent.getChildLayoutPosition(view) % 2 == 0) {
            outRect.left = AppHelper.getDimen(mContext, R.dimen.common_size_6);
            outRect.right = 0;
        } else {
            outRect.right = AppHelper.getDimen(mContext, R.dimen.common_size_6);
            outRect.left = AppHelper.getDimen(mContext, R.dimen.common_size_6);
        }
        outRect.bottom = AppHelper.getDimen(mContext, R.dimen.common_size_6);
    }
}

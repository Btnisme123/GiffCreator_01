package com.framgia.gifcreator.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.util.BitmapWorkerTask;
import com.framgia.gifcreator.util.listener.OnListItemInteractListener;

import java.util.List;

/**
 * Created by yue on 27/06/2016.
 */
public class ThumbnailPagerAdapter extends PagerAdapter {
    private final String TAG_PREFIX = "IMG_";
    private Context mContext;
    private ViewPager mThumbnailPager;
    private List<Frame> mFrames;
    private OnListItemInteractListener mListener;

    public ThumbnailPagerAdapter(Context context, ViewPager thumbnailPager, List<Frame> frames) {
        mContext = context;
        mThumbnailPager = thumbnailPager;
        mFrames = frames;
    }

    public void setOnListItemInteractListener(OnListItemInteractListener listener) {
        mListener = listener;
    }

    @Override
    public float getPageWidth(int position) {
        return 0.485f;
    }

    @Override
    public int getCount() {
        return mFrames.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final Frame frame = mFrames.get(position);
        View view = View.inflate(mContext, R.layout.item_thumbnail, null);
        ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
        thumbnail.setTag(TAG_PREFIX + position);
        BitmapWorkerTask worker = new BitmapWorkerTask(thumbnail, frame,
                mContext.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                mContext.getResources().getDimensionPixelSize(R.dimen.thumbnail_height),
                true);
        worker.execute(BitmapWorkerTask.TASK_DECODE_FILE, frame.getPhotoPath());
        View thumbnailContainer = view.findViewById(R.id.thumbnail_container);
        thumbnailContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    int p = (int) v.getTag();
                    mListener.onListItemLongClick(p);
                }
                return false;
            }
        });
        thumbnailContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    int p = (int) v.getTag();
                    mListener.onListItemClick(p);
                }
            }
        });
        thumbnailContainer.setTag(position);
        container.addView(view);
        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public void refresh(int position) {
        ImageView imageView = (ImageView) mThumbnailPager.findViewWithTag(TAG_PREFIX + position);
        final Frame frame = mFrames.get(position);
        BitmapWorkerTask worker = new BitmapWorkerTask(imageView, frame,
                mContext.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                mContext.getResources().getDimensionPixelSize(R.dimen.thumbnail_height),
                true);
        worker.execute(BitmapWorkerTask.TASK_DECODE_FILE, frame.getPhotoPath());
    }
}

package com.framgia.gifcreator.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.framgia.gifcreator.R;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.ui.activity.ShowListChosenImageActivity;
import com.framgia.gifcreator.util.AppHelper;

import java.util.List;

/**
 * Created by VULAN on 6/6/2016.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<Frame> mFrames;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    public ImageAdapter(Context context, List<Frame> frames) {
        mContext = context;
        mFrames = frames;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Frame frame = mFrames.get(position);
        Glide.with(mContext).load(frame.getPhotoPath()).listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                holder.mProgressBar.setVisibility(View.INVISIBLE);
                return false;
            }
        }).centerCrop().into(holder.mImageView);
        holder.mCheckbox.setChecked(frame.isChosen());
        holder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(position);
                }
            }
        });
        holder.mCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShowListChosenImageActivity.sCanAdjustFrame) {
                    if (frame.isChosen()) {
                        frame.setChecked(false);
                        ShowListChosenImageActivity.sNumberOfFrames--;
                    } else {
                        if (ShowListChosenImageActivity.sNumberOfFrames < Constants.MAXIMUM_FRAMES) {
                            frame.setChecked(true);
                            ShowListChosenImageActivity.sNumberOfFrames++;
                        } else {
                            AppHelper.showSnackbar(ShowListChosenImageActivity.sCoordinatorLayout,
                                    R.string.out_of_limit);
                            frame.setChecked(false);
                        }
                    }
                    notifyItemChanged(position);
                } else {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onRemoveItem(position);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFrames == null ? 0 : mFrames.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onItemClick(int position);

        void onRemoveItem(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public CheckBox mCheckbox;
        public ProgressBar mProgressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.image_choosing);
            mCheckbox = (CheckBox) itemView.findViewById(R.id.checkbox_item);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
        }
    }
}

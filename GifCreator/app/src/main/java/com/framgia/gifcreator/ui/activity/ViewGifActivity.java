package com.framgia.gifcreator.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.framgia.gifcreator.R;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Gif;
import com.framgia.gifcreator.ui.base.BaseActivity;

public class ViewGifActivity extends BaseActivity {

    private ImageView mImageGif;
    private Gif mGif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        getData();
        Glide.with(this).load(mGif.getGifPath()).asGif().into(mImageGif);
        enableBackButton();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_view_gif;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void findViews() {
        mImageGif = (ImageView) findViewById(R.id.image_gif);
    }

    private void getData() {
        Intent intent = getIntent();
        if (intent != null) {
            mGif = new Gif(intent.getStringExtra(Constants.EXTRA_GIF_PATH));
        }
    }
}

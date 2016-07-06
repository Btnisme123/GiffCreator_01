package com.framgia.gifcreator.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.util.LoadFrameTask;
import com.framgia.gifcreator.util.MakingGifTask;
import com.framgia.gifcreator.util.PermissionUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class PreviewGifActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, LoadFrameTask.OnLoadCompleteListener, MakingGifTask.OnFinishMakingGif {

    private final int MAX_FPS = 9;
    private final int DEFAULT_FPS = 4;
    private ImageView mImagePreviewGif;
    private TextView mTextFps;
    private AppCompatSeekBar mSeekBarAdjustFps;
    private List<Frame> mFrames;
    private CountDownTimer mCountDownTimer;
    private int mInterval;
    private int mCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        getData();
        mSeekBarAdjustFps.setMax(MAX_FPS);
        mSeekBarAdjustFps.setProgress(DEFAULT_FPS);
        mSeekBarAdjustFps.setOnSeekBarChangeListener(this);
        mTextFps.setText(MessageFormat.format(getString(R.string.fps), DEFAULT_FPS + 1));
        mInterval = 1000 / DEFAULT_FPS;
        enableToolbar();
        enableBackButton();
        loadFrames();
        mToolbar.setTitle(R.string.title_preview_gif_activity);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_preview_gif;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_preview_gif, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mTextFps.setText(MessageFormat.format(getString(R.string.fps), progress + 1));
        mInterval = 1000 / (progress + 1);
        if (mCountDownTimer != null) mCountDownTimer.cancel();
        startPreview();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onBackPressed() {
        mCountDownTimer.cancel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        ShowListChosenImageActivity.sCanAdjustFrame = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_gif:
                Intent intent = new Intent(this, ShowGifDetailActivity.class);
                int size = mFrames.size();
                String[] paths = new String[size];
                for (int i = 0; i < size; i++) {
                    paths[i] = mFrames.get(i).getPhotoPath();
                }
                intent.putExtra(Constants.EXTRA_PATHS_LIST, paths);
                startActivityForResult(intent, Constants.REQUEST_ADJUST);
                mCountDownTimer.cancel();
                mImagePreviewGif.setImageBitmap(null);
                break;
            case R.id.action_make_gif:
                if (PermissionUtil.isStoragePermissionGranted(this)) {
                    MakingGifTask makingGifTask =
                            new MakingGifTask(this, mSeekBarAdjustFps.getProgress() + 1);
                    int numberOfFrames = mFrames.size();
                    String[] photoPaths = new String[numberOfFrames];
                    for (int i = 0; i < numberOfFrames; i++) {
                        photoPaths[i] = mFrames.get(i).getPhotoPath();
                    }
                    makingGifTask.setOnFinishMakingGif(this);
                    makingGifTask.execute(photoPaths);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_export_gif).show();
                }
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == Constants.REQUEST_ADJUST) {
                String[] photoPaths = data.getStringArrayExtra(Constants.EXTRA_PATHS_LIST);
                int size = photoPaths.length;
                if (size > 0) {
                    mFrames.clear();
                    for (int i = 0; i < size; i++) {
                        mFrames.add(new Frame(photoPaths[i]));
                    }
                    loadFrames();
                }
            }
        } else {
            startPreview();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLoadComplete() {
        startPreview();
    }

    @Override
    public void onFinishMakingGif(String gifPath) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void findViews() {
        mImagePreviewGif = (ImageView) findViewById(R.id.image_preview_gif);
        mTextFps = (TextView) findViewById(R.id.text_fps);
        mSeekBarAdjustFps = (AppCompatSeekBar) findViewById(R.id.seek_bar_adjust_fps);
    }

    private void getData() {
        mFrames = new ArrayList<>();
        Intent intent = getIntent();
        if (intent != null) {
            String[] paths = intent.getStringArrayExtra(Constants.EXTRA_PATHS_LIST);
            for (String path : paths) {
                mFrames.add(new Frame(path));
            }
        }
    }

    private void loadFrames() {
        LoadFrameTask loadFrameTask = new LoadFrameTask(this, mFrames);
        loadFrameTask.setOnLoadCompleteListener(this);
        loadFrameTask.execute();
    }

    private void startPreview() {
        mCountDownTimer = new CountDownTimer(Long.MAX_VALUE, mInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                mImagePreviewGif.setImageBitmap(mFrames.get(mCount).getFrame());
                mCount++;
                if (mCount == mFrames.size()) mCount = 0;
            }

            @Override
            public void onFinish() {

            }
        };
        mCountDownTimer.start();
    }
}


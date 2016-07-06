package com.framgia.gifcreator.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.adapter.ThumbnailPagerAdapter;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.ui.widget.GetPhotoDialog;
import com.framgia.gifcreator.util.AppHelper;
import com.framgia.gifcreator.util.BitmapWorkerTask;
import com.framgia.gifcreator.util.FileUtil;
import com.framgia.gifcreator.util.listener.OnListItemInteractListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yue on 09/06/2016.
 */
public class ShowGifDetailActivity extends BaseActivity implements
        GetPhotoDialog.OnDialogItemChooseListener, OnListItemInteractListener, View.OnClickListener {

    private final String IMAGE_EXTENSION = ".jpg";
    private final String PICK_IMAGE_TYPE = "image/*";
    private final String PICK_IMAGE_TITLE = "Select Picture";
    private final int OFF_SCREEN_PAGE_LIMIT = 3;
    private ImageView mLargeImage;
    private ViewPager mThumbnailPager;
    private CoordinatorLayout mCoordinatorLayout;
    private ThumbnailPagerAdapter mPagerAdapter;
    private List<Frame> mFrames;
    private String mCurrentPhotoPath;
    private int mCurrentPosition;
    private int mLongClickPosition;
    private boolean mIsFirst = true;
    private boolean mIsFrameChanged;
    private boolean mIsListChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        getData();
        mPagerAdapter = new ThumbnailPagerAdapter(this, mThumbnailPager, mFrames);
        mPagerAdapter.setOnListItemInteractListener(this);
        mThumbnailPager.setClipToPadding(false);
        mThumbnailPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.common_size_10));
        mThumbnailPager.setOffscreenPageLimit(OFF_SCREEN_PAGE_LIMIT);
        mThumbnailPager.setAdapter(mPagerAdapter);
        mLargeImage.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mIsFirst) {
                            mIsFirst = false;
                            Frame currentFrame = mFrames.get(mCurrentPosition);
                            BitmapWorkerTask task = new BitmapWorkerTask(mLargeImage, currentFrame,
                                    mLargeImage.getWidth(), mLargeImage.getHeight(), false);
                            task.execute(
                                    BitmapWorkerTask.TASK_DECODE_FILE, currentFrame.getPhotoPath());
                        }
                    }
                });
        enableBackButton();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_show_gif_detail;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_show_gif_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_image:
                moveToNextActivity();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.REQUEST_ADJUST:
                    if (resultCode == RESULT_OK) {
                        String photoPath = data.getStringExtra(Constants.EXTRA_PHOTO_PATH);
                        mCurrentPosition = data.getIntExtra(Constants.EXTRA_POSITION, mCurrentPosition);
                        if (!TextUtils.isEmpty(photoPath)) {
                            mIsFrameChanged = true;
                            Frame currentFrame = mFrames.get(mCurrentPosition);
                            currentFrame.setPhotoPath(photoPath);
                            currentFrame.setFrame(null);
                            mPagerAdapter.refresh(mCurrentPosition);
                            BitmapWorkerTask task = new BitmapWorkerTask(mLargeImage, currentFrame,
                                    mLargeImage.getWidth(), mLargeImage.getHeight(), false);
                            task.execute(BitmapWorkerTask.TASK_DECODE_FILE, currentFrame.getPhotoPath());
                        }
                    }
                    break;
                case Constants.REQUEST_GALLERY:
                    mIsListChanged = true;
                    Uri uri = data.getData();
                    String photoPath = FileUtil.getGalleryPhotoPath(this, uri);
                    if (!TextUtils.isEmpty(photoPath)) {
                        mFrames.add(mLongClickPosition + 1, new Frame(photoPath));
                        mPagerAdapter.notifyDataSetChanged();
                    }
                    break;
                case Constants.REQUEST_CAMERA:
                    mIsListChanged = true;
                    mFrames.add(mLongClickPosition + 1, new Frame(mCurrentPhotoPath));
                    mPagerAdapter.notifyDataSetChanged();
                    galleryAddPic();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mIsFrameChanged || mIsListChanged) {
            Intent intent = new Intent();
            int size = mFrames.size();
            String[] photoPaths = new String[size];
            for (int i = 0; i < size; i++) {
                photoPaths[i] = mFrames.get(i).getPhotoPath();
            }
            intent.putExtra(Constants.EXTRA_PATHS_LIST, photoPaths);
            setResult(RESULT_OK, intent);
        } else setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    public void onDialogItemChoose(int type) {
        int size = mFrames.size();
        switch (type) {
            case GetPhotoDialog.TYPE_GALLERY:
                if (size == Constants.MAXIMUM_FRAMES) {
                    AppHelper.showSnackbar(mCoordinatorLayout, R.string.out_of_limit);
                } else {
                    handlePickPhotoClick();
                }
                break;
            case GetPhotoDialog.TYPE_CAMERA:
                if (size == Constants.MAXIMUM_FRAMES) {
                    AppHelper.showSnackbar(mCoordinatorLayout, R.string.out_of_limit);
                } else {
                    Intent getPhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (getPhotoIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (photoFile != null) {
                            getPhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            startActivityForResult(getPhotoIntent, Constants.REQUEST_CAMERA);
                        }
                    }
                }
                break;
            case GetPhotoDialog.TYPE_REMOVE:
                mIsListChanged = true;
                mFrames.remove(mLongClickPosition);
                mPagerAdapter.notifyDataSetChanged();
                if (mLongClickPosition == mCurrentPosition) {
                    if (mFrames.size() > 0) {
                        if (mCurrentPosition > mFrames.size() - 1) {
                            mCurrentPosition--;
                        }
                        mLargeImage.setImageBitmap(mFrames.get(mCurrentPosition).getFrame());
                    }
                }
                break;
        }
    }

    @Override
    public void onListItemClick(int position) {
        mCurrentPosition = position;
        Frame frame = mFrames.get(position);
        mLargeImage.setImageBitmap(frame.getFrame());
    }

    @Override
    public void onListItemLongClick(int position) {
        mLongClickPosition = position;
        GetPhotoDialog dialog = mFrames.size() > 2 ?
                new GetPhotoDialog(this, true) : new GetPhotoDialog(this);
        dialog.setOnDialogItemChooseListener(this);
        dialog.showDialog();
    }

    @Override
    public void onClick(View v) {
        moveToNextActivity();
    }

    private void findViews() {
        mLargeImage = (ImageView) findViewById(R.id.large_image);
        mThumbnailPager = (ViewPager) findViewById(R.id.thumbnail_pager);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        // Set listener
        mLargeImage.setOnClickListener(this);
    }

    private void getData() {
        mFrames = new ArrayList<>();
        Intent intent = getIntent();
        if (intent != null) {
            String[] paths = intent.getStringArrayExtra(Constants.EXTRA_PATHS_LIST);
            if (paths.length > 0) {
                for (String path : paths) {
                    mFrames.add(new Frame(path));
                }
            }
        }
    }

    private void handlePickPhotoClick() {
        Intent intent = new Intent();
        intent.setType(PICK_IMAGE_TYPE);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, PICK_IMAGE_TITLE),
                Constants.REQUEST_GALLERY);
    }

    private File createImageFile() throws IOException {
        String imageFileName = FileUtil.getImageName();
        File storageDir = Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, IMAGE_EXTENSION, storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void moveToNextActivity() {
        Intent intent = new Intent(this, AdjustImageActivity.class);
        intent.putExtra(Constants.EXTRA_PHOTO_PATH,
                mFrames.get(mCurrentPosition).getPhotoPath());
        intent.putExtra(Constants.EXTRA_POSITION, mCurrentPosition);
        startActivityForResult(intent, Constants.REQUEST_ADJUST);
    }
}

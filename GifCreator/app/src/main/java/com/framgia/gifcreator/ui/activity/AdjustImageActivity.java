package com.framgia.gifcreator.ui.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.effect.ColorEffect;
import com.framgia.gifcreator.effect.ContrastEffect;
import com.framgia.gifcreator.effect.EdgeDetect;
import com.framgia.gifcreator.effect.EditingEffect;
import com.framgia.gifcreator.effect.GrayScaleEffect;
import com.framgia.gifcreator.effect.NegativeEffect;
import com.framgia.gifcreator.effect.RotationEffect;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.ui.widget.AdjustImageBar;
import com.framgia.gifcreator.ui.widget.BottomNavigationItem;
import com.framgia.gifcreator.util.BitmapHelper;
import com.framgia.gifcreator.util.BitmapWorkerTask;
import com.framgia.gifcreator.util.FileUtil;
import com.framgia.gifcreator.util.HandlingImageAsyncTask;
import com.framgia.gifcreator.util.ImageProcessing;
import com.framgia.gifcreator.util.PermissionUtil;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AdjustImageActivity extends BaseActivity implements
        BottomNavigationItem.OnBottomNavigationItemClickListener,
        HandlingImageAsyncTask.OnProgressListener,
        AdjustImageBar.OnAdjustImageBarItemInteractListener {

    public static Context sContext;
    private final int BOTTOM_NAVIGATION_LEVEL_1 = 1;
    private final int BOTTOM_NAVIGATION_LEVEL_2 = 2;
    private final float IMAGE_VALUE = 127;
    private final int RED = 0;
    private final int GREEN = 1;
    private final int BLUE = 2;
    private final int ALPHA = 3;
    private final int CONTRAST = 4;
    private final int START_PROGRESS = 127;
    private final int MAX_PROGRESS = 255;
    private ImageView mAdjustImage;
    private LinearLayout mBottomNavigationContainer;
    private Frame mFrame;
    private Bitmap mOriginImage;
    private Bitmap mProcessedImage;
    private MenuItem mItemApplyEffect;
    private List<BottomNavigationItem> mBottomNavigationMainItems;
    private List<BottomNavigationItem> mBottomNavigationEffectItems;
    private List<BottomNavigationItem> mBottomNavigationColorItems;
    private AdjustImageBar mAdjustmentImageBar;
    private HandlingImageAsyncTask mHandlingImageAsyncTask;
    private ProgressDialog mProgressDialog;
    private RotationEffect mRotationEffect;
    private GrayScaleEffect mGrayScaleEffect;
    private NegativeEffect mNegativeEffect;
    private ColorEffect mColorEffect;
    private ContrastEffect mContrastEffect;
    private EdgeDetect mEdgeDetectEffect;
    private Uri mImageUri;
    private HorizontalScrollView mHorizontalScrollView;
    private int mBottomNavigationLevel;
    private int mPosition;
    private int mCurrentProgress;
    private int mOriginRedProgress;
    private int mOriginGreenProgress;
    private int mOriginBlueProgress;
    private int mOriginAlphaProgress;
    private int mOriginContrastProgress;
    private boolean mIsFirst;
    private boolean mIsProcessing;
    private boolean mIsImageEdited;
    private int mEffectType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sContext = this;
        findViews();
        getData();
        mIsFirst = true;
        init();
        initBottomNavigation();
        mAdjustImage.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mIsFirst) {
                            BitmapWorkerTask worker = new BitmapWorkerTask(mAdjustImage, mFrame, true);
                            worker.execute(BitmapWorkerTask.TASK_DECODE_FILE, mFrame.getPhotoPath());
                            mIsFirst = false;
                            mAdjustmentImageBar.getLayoutParams().height = mHorizontalScrollView.getHeight();
                        }
                    }
                }
        );
        enableBackButton();
        mAdjustmentImageBar.setOnAdjustImageBarItemInteractListener(this);
        mAdjustmentImageBar.setMaxValue(MAX_PROGRESS);
        mToolbar.setTitle(R.string.title_adjust_image_activity);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_adjust_image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_adjust_image, menu);
        mItemApplyEffect = menu.findItem(R.id.action_apply_effect);
        mItemApplyEffect.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.title_adjust_image_activity);
    }

    @Override
    public void onItemClick(BottomNavigationItem item) {
        if (mOriginImage == null && mFrame.getFrame() != null) {
            mOriginImage = BitmapHelper.copy(mFrame.getFrame());
        }
        String title = item.getTitle();
        mAdjustImage.setImageBitmap(mOriginImage);
        if (title.equals(getString(R.string.bottom_navigation_main_item_effect))) {
            makeBottomNavigation(mBottomNavigationEffectItems);
            mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_2;
            mIsProcessing = false;
        } else if (title.equals(getString(R.string.bottom_navigation_adjust_item_blur))) {
            showItemApplyEffect();
            mProcessedImage = ImageProcessing.blurImage(this, mOriginImage);
            mAdjustImage.setImageBitmap(mProcessedImage);
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_adjust_item_gray_scale))) {
            showItemApplyEffect();
            setEffect(mGrayScaleEffect);
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_adjust_item_negative))) {
            showItemApplyEffect();
            setEffect(mNegativeEffect);
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_main_item_orientation))) {
            showItemApplyEffect();
            setEffect(mRotationEffect);
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_main_item_color))) {
            makeBottomNavigation(mBottomNavigationColorItems);
            mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_2;
            mIsProcessing = false;
        } else if (title.equals(getString(R.string.bottom_navigation_blue_color))) {
            showItemApplyEffect();
            openSeekbar();
            mEffectType = BLUE;
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_red_color))) {
            showItemApplyEffect();
            openSeekbar();
            mEffectType = RED;
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_green_color))) {
            showItemApplyEffect();
            openSeekbar();
            mEffectType = GREEN;
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_alpha_color))) {
            showItemApplyEffect();
            openSeekbar();
            mEffectType = ALPHA;
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_adjust_item_contrast))) {
            showItemApplyEffect();
            openSeekbar();
            mEffectType = CONTRAST;
            mIsProcessing = true;
        } else if (title.equals(getString(R.string.bottom_navigation_main_item_crop))) {
            if (PermissionUtil.isStoragePermissionGranted(this)) {
                if (mProcessedImage != null) {
                    mOriginImage = null;
                    mOriginImage = BitmapHelper.copy(mProcessedImage);
                }
                saveProcessedImage();
                startCrop();
                mIsProcessing = true;
            }
        } else if (title.equals(getString(R.string.bottom_navigation_edge_detect))) {
            showItemApplyEffect();
            setEffect(mEdgeDetectEffect);
            mIsProcessing = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_apply_effect:
                if (mIsProcessing) {
                    mFrame.setFrame(null);
                    mFrame.setFrame(BitmapHelper.copy(mProcessedImage));
                    mOriginImage = null;
                    mOriginImage = BitmapHelper.copy(mProcessedImage);
                    mProcessedImage = null;
                    makeBottomNavigation(mBottomNavigationMainItems);
                    closeSeekbar();
                    mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_1;
                    mIsProcessing = false;
                    mIsImageEdited = true;
                    switch (mEffectType) {
                        case RED:
                            mOriginRedProgress = mCurrentProgress;
                            break;
                        case GREEN:
                            mOriginGreenProgress = mCurrentProgress;
                            break;
                        case BLUE:
                            mOriginBlueProgress = mCurrentProgress;
                            break;
                        case ALPHA:
                            mOriginAlphaProgress = mCurrentProgress;
                            break;
                        case CONTRAST:
                            mOriginContrastProgress = mCurrentProgress;
                            break;
                    }
                } else {
                    backToPreviousActivity();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mIsProcessing) {
            if (mItemApplyEffect != null && !mIsImageEdited) mItemApplyEffect.setVisible(false);
            if (mAdjustmentImageBar.getVisibility() == View.VISIBLE) closeSeekbar();
            mProcessedImage = mOriginImage = null;
            mOriginImage = BitmapHelper.copy(mFrame.getFrame());
            mAdjustImage.setImageBitmap(mOriginImage);
            makeBottomNavigation(mBottomNavigationMainItems);
            mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_1;
            mIsProcessing = false;
        } else {
            if (mBottomNavigationLevel == BOTTOM_NAVIGATION_LEVEL_2) {
                makeBottomNavigation(mBottomNavigationMainItems);
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setMessage(R.string.save_image).
                        setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                backToPreviousActivity();
                            }
                        }).
                        setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        });
                dialogBuilder.show();
                ShowListChosenImageActivity.sCanAdjustFrame = true;
            }
            closeSeekbar();
        }
    }

    @Override
    public void onButtonClickListener(@AdjustImageBar.AdjustImageBarButtonDef int button) {
        switch (button) {
            case AdjustImageBar.BUTTON_COMPLETE:
                mOriginImage = null;
                mOriginImage = BitmapHelper.copy(mProcessedImage);
                break;
            case AdjustImageBar.BUTTON_CANCEL:
                mAdjustImage.setImageBitmap(mOriginImage);
                resetSeekbarProgress();
                break;
        }
    }

    @Override
    public void onSeekBarValueChange(int progress) {
        float progressColorValue = progress - IMAGE_VALUE;
        switch (mEffectType) {
            case RED:
                mCurrentProgress = progress;
                mColorEffect = new ColorEffect(progressColorValue, ColorEffect.Type.RED);
                break;
            case BLUE:
                mCurrentProgress = progress;
                mColorEffect = new ColorEffect(progressColorValue, ColorEffect.Type.BLUE);
                break;
            case GREEN:
                mCurrentProgress = progress;
                mColorEffect = new ColorEffect(progressColorValue, ColorEffect.Type.GREEN);
                break;
            case ALPHA:
                mCurrentProgress = progress;
                mColorEffect = new ColorEffect(progressColorValue, ColorEffect.Type.ALPHA);
                break;
            case CONTRAST:
                mCurrentProgress = progress;
                mContrastEffect = new ContrastEffect(progress);
                break;
        }
        if (mEffectType == CONTRAST) {
            setEffect(mContrastEffect);
        } else {
            setEffect(mColorEffect);
        }
    }

    @Override
    public void onHandleFinish(Bitmap bitmap) {
        if (mProcessedImage != null) mProcessedImage = null;
        mProcessedImage = BitmapHelper.copy(bitmap);
        mAdjustImage.setImageBitmap(mProcessedImage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            mImageUri = result.getUri();
            try {
                mOriginImage = null;
                mOriginImage = getImageFromUri(getApplicationContext(), mImageUri);
                mFrame.setFrame(null);
                mFrame.setFrame(mOriginImage);
                mIsProcessing = false;
                showItemApplyEffect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAdjustImage.setImageBitmap(mOriginImage);
        } else {
            mAdjustImage.setImageBitmap(mOriginImage);
        }
    }

    private void backToPreviousActivity() {
        if (PermissionUtil.isStoragePermissionGranted(AdjustImageActivity.this)) {
            saveProcessedImage();
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_POSITION, mPosition);
            intent.putExtra(Constants.EXTRA_PHOTO_PATH, mFrame.getPhotoPath());
            setResult(RESULT_OK, intent);
            finish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(AdjustImageActivity.this);
            builder.setTitle(R.string.error).
                    setMessage(R.string.cannot_save).
                    setCancelable(false).
                    setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }).show();
        }
    }

    private void openSeekbar() {
        if (mAdjustmentImageBar.getVisibility() == View.GONE) {
            mAdjustmentImageBar.setVisibility(View.VISIBLE);
            mHorizontalScrollView.setVisibility(View.GONE);
        }
        resetSeekbarProgress();
    }

    private void closeSeekbar() {
        if (mAdjustmentImageBar.getVisibility() == View.VISIBLE) {
            mAdjustmentImageBar.setVisibility(View.GONE);
            mHorizontalScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void findViews() {
        mAdjustImage = (ImageView) findViewById(R.id.adjust_image);
        mBottomNavigationContainer = (LinearLayout) findViewById(R.id.bottom_navigation_container);
        mAdjustmentImageBar = (AdjustImageBar) findViewById(R.id.adjust_image_bar);
        mHorizontalScrollView = (HorizontalScrollView) findViewById(R.id.horizontal_scroll_view);
    }

    private void getData() {
        Intent intent = getIntent();
        mFrame = new Frame();
        if (intent != null) {
            mPosition = intent.getIntExtra(Constants.EXTRA_POSITION, 0);
            mFrame.setPhotoPath(intent.getStringExtra(Constants.EXTRA_PHOTO_PATH));
        }
    }

    private void initBottomNavigation() {
        mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_1;
        mIsProcessing = false;
        initBottomNavigationMainItems();
        initBottomNavigationAdjustItems();
        initBottomColorItems();
        makeBottomNavigation(mBottomNavigationMainItems);
    }

    private void initBottomNavigationMainItems() {
        if (mBottomNavigationMainItems == null || mBottomNavigationMainItems.size() == 0) {
            mBottomNavigationMainItems = new ArrayList<>();
            mBottomNavigationMainItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_main_item_effect), R.drawable.ic_effect));
            mBottomNavigationMainItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_main_item_color), R.drawable.ic_color_palette));
            mBottomNavigationMainItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_main_item_crop), R.drawable.ic_crop));
            mBottomNavigationMainItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_main_item_orientation), R.drawable.ic_orientation));
            mBottomNavigationMainItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_adjust_item_contrast), R.drawable.ic_contrast));
        }
    }

    private void initBottomNavigationAdjustItems() {
        if (mBottomNavigationEffectItems == null || mBottomNavigationEffectItems.size() == 0) {
            mBottomNavigationEffectItems = new ArrayList<>();
            mBottomNavigationEffectItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_adjust_item_blur), R.drawable.ic_blur));
            mBottomNavigationEffectItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_adjust_item_gray_scale), R.drawable.ic_blur));
            mBottomNavigationEffectItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_adjust_item_negative), R.drawable.ic_blur));
            mBottomNavigationEffectItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_edge_detect), R.drawable.ic_blur));
        }
    }

    private void initBottomColorItems() {
        if (mBottomNavigationColorItems == null || mBottomNavigationColorItems.size() == 0) {
            mBottomNavigationColorItems = new ArrayList<>();
            mBottomNavigationColorItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_green_color), R.drawable.ic_color_green));
            mBottomNavigationColorItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_blue_color), R.drawable.ic_color_blue));
            mBottomNavigationColorItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_red_color), R.drawable.ic_color_red));
            mBottomNavigationColorItems.add(new BottomNavigationItem(this,
                    getString(R.string.bottom_navigation_alpha_color), R.drawable.ic_color_alpha));
        }
    }

    private void makeBottomNavigation(List<BottomNavigationItem> bottomNavigationItems) {
        mBottomNavigationLevel = BOTTOM_NAVIGATION_LEVEL_1;
        mBottomNavigationContainer.removeAllViews();
        final int size = bottomNavigationItems.size();
        for (int i = 0; i < size; i++) {
            mBottomNavigationContainer.addView(bottomNavigationItems.get(i).getView());
            bottomNavigationItems.get(i).setOnBottomNavigationItemClickListener(this);
            if (i == size - 1) bottomNavigationItems.get(i).hideSeparator();
        }
    }

    private void showItemApplyEffect() {
        if (mItemApplyEffect != null && !mItemApplyEffect.isVisible()) {
            mItemApplyEffect.setVisible(true);
        }
    }

    private void saveProcessedImage() {
        if (mFrame.getPhotoPath().contains(FileUtil.getAppFolderPath(this))) {
            FileUtil.removeFile(mFrame.getPhotoPath());
        }
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        try {
            if (mFrame.getFrame() != null) {
                if (mOriginImage == null) mOriginImage = BitmapHelper.copy(mFrame.getFrame());
                Bitmap bitmap = BitmapHelper.resizeBitmap(mOriginImage,
                        screenWidth, screenHeight);
                mFrame.setPhotoPath(FileUtil.saveImage(AdjustImageActivity.this, bitmap));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setEffect(EditingEffect effect) {
        if (!(effect instanceof RotationEffect)) {
            mProcessedImage = null;
            mProcessedImage = BitmapHelper.copy(mOriginImage);
        } else {
            if (mProcessedImage == null) mProcessedImage = BitmapHelper.copy(mOriginImage);
        }
        mHandlingImageAsyncTask = new HandlingImageAsyncTask(effect, mProcessedImage, mProgressDialog);
        mHandlingImageAsyncTask.setOnProgressListener(this);
        mHandlingImageAsyncTask.execute();
    }

    private void init() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.loading_effect));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mRotationEffect = new RotationEffect();
        mGrayScaleEffect = new GrayScaleEffect();
        mNegativeEffect = new NegativeEffect();
        mEdgeDetectEffect = new EdgeDetect();
        mOriginRedProgress = mOriginContrastProgress = mOriginGreenProgress =
                mOriginBlueProgress = mOriginAlphaProgress = mCurrentProgress = START_PROGRESS;
    }

    private void startCrop() {
        mImageUri = Uri.fromFile(new File(mFrame.getPhotoPath()));
        CropImage.activity(mImageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    private Bitmap getImageFromUri(Context context, Uri uri) throws IOException {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int size = windowManager.getDefaultDisplay().getWidth();
        InputStream input = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;
        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ?
                onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
        double ratio = (originalSize > size) ? (originalSize / size) : 1.0;
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else return k;
    }

    private void resetSeekbarProgress() {
        switch (mEffectType) {
            case CONTRAST:
                mAdjustmentImageBar.setProgress(mOriginContrastProgress);
                break;
            case RED:
                mAdjustmentImageBar.setProgress(mOriginRedProgress);
                break;
            case GREEN:
                mAdjustmentImageBar.setProgress(mOriginGreenProgress);
                break;
            case BLUE:
                mAdjustmentImageBar.setProgress(mOriginBlueProgress);
                break;
            case ALPHA:
                mAdjustmentImageBar.setProgress(mOriginAlphaProgress);
                break;
        }
    }
}

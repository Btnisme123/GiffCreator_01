package com.framgia.gifcreator.ui.activity;

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import com.framgia.gifcreator.R;

import com.framgia.gifcreator.adapter.ImageAdapter;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShowListChosenImageActivity extends BaseActivity implements ImageAdapter.OnItemClickListener,
        View.OnClickListener {


    private final String IMAGE_EXTENSION = ".jpg";
    private ImageAdapter mImageAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayout mFloatingMenu;
    private CoordinatorLayout mCoordinatorLayout;
    private List<Frame> mFrames;
    private String mCurrentPhotoPath;
    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        // Setup recycler view
        mFrames = new ArrayList<>();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mFrames.addAll(getImageListGallery());
        mImageAdapter = new ImageAdapter(this, mFrames);
        mRecyclerView.setAdapter(mImageAdapter);
        mImageAdapter.setOnItemClickListener(this);
        // Call activity to get photo
        Intent intent = getIntent();
        if (intent != null) {
            switch (intent.getIntExtra(Constants.EXTRA_REQUEST, Constants.REQUEST_GALLERY)) {
                case Constants.REQUEST_CAMERA:
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
                    break;
                case Constants.REQUEST_GALLERY:
                    refresh(getImageListGallery());
                    break;
            }
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_choosing_image;
    }

    @Override
    protected int getMenuResId() {
        return R.menu.menu_chosen_image;
    }

    @Override

    protected String getActivityTitle() {
        return getString(R.string.title_show_chosen_images_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.REQUEST_CAMERA:
                    Frame frame = new Frame(mCurrentPhotoPath);
                    mFrames.add(frame);
                    mImageAdapter.notifyItemInserted(mFrames.indexOf(frame));
                    galleryAddPic();
                    break;
                case Constants.REQUEST_GALLERY:
                    Uri selectedImg = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                            selectedImg, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    String imgDecodeString = cursor.
                            getString(cursor.getColumnIndex(filePathColumn[0]));
                    cursor.close();
                    frame = new Frame(imgDecodeString);
                    mFrames.add(frame);
                    mImageAdapter.notifyItemInserted(mFrames.indexOf(frame));
                    break;
                case Constants.REQUEST_ADJUST:
                    int position = data.getIntExtra(Constants.EXTRA_POSITION, 0);
                    String photoPath = data.getStringExtra(Constants.EXTRA_PHOTO_PATH);
                    if (!TextUtils.isEmpty(photoPath)) {
                        frame = mFrames.get(position);
                        frame.destroy();
                        frame.setPhotoPath(photoPath);
                    }
                    mImageAdapter.notifyItemChanged(position);
                    break;
            }
        }
    }

    private List<Frame> getImageListGallery() {
        List<Frame> imageItems = new ArrayList<>();
        CursorLoader imageLoader = new CursorLoader(this,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA},
                null,
                null,
                MediaStore.Images.Media._ID);
        Cursor imageCursor = imageLoader.loadInBackground();
        if (imageCursor.moveToLast()) {
            do {
                String imagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Frame frame = new Frame(imagePath);
                imageItems.add(frame);
            } while (imageCursor.moveToPrevious());
        }
        imageCursor.close();
        return imageItems;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preview_gif:
                if (mFrames.size() < MIN_SIZE) {
                    Snackbar.make(mCoordinatorLayout,
                            getString(R.string.warning_make_gif), Snackbar.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, PreviewGifActivity.class);
                    int size = mFrames.size();
                    String[] paths = new String[size];
                    for (int i = 0; i < size; i++) {
                        paths[i] = mFrames.get(i).getPhotoPath();
                    }
                    intent.putExtra(Constants.EXTRA_PATHS_LIST, paths);
                    startActivity(intent);
                }
                break;
            case R.id.action_open_list_all:
                if (getImageListGallery().size() != mFrames.size()) {
                    refresh(getImageListGallery());
                } else {
                    mImageAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.action_open_list_chosen:
                if (getChosenList().size() > MAX_SIZE) {
                    Snackbar.make(mCoordinatorLayout,
                            getString(R.string.out_of_limit), Snackbar.LENGTH_SHORT).show();
                } else {
                    refresh(getChosenList());
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRemoveItem(int position) {
        mFrames.remove(position);
        mImageAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onPhotoChoose(int position) {
        Intent intent = new Intent(ShowListChosenImageActivity.this, AdjustImageActivity.class);
        intent.putExtra(Constants.EXTRA_PHOTO_PATH, mFrames.get(position).getPhotoPath());
        intent.putExtra(Constants.EXTRA_POSITION, position);
        startActivityForResult(intent, Constants.REQUEST_ADJUST);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_camera:
                if (mFrames.size() == Constants.MAXIMUM_FRAMES) {
                    Snackbar.make(mCoordinatorLayout,
                            getString(R.string.out_of_limit), Snackbar.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (photoFile != null) {
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            startActivityForResult(intent, Constants.REQUEST_CAMERA);
                        }
                    }
                }
                break;
            case R.id.fab_gallery:
                refresh(getImageListGallery());
                break;
            case R.id.main_floating_button:
                mFloatingMenu.setVisibility(
                        mFloatingMenu.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                break;
        }
    }

    private void findViews() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_choosing_image);
        mFloatingMenu = (LinearLayout) findViewById(R.id.floating_menu);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        findViewById(R.id.fab_camera).setOnClickListener(this);
        findViewById(R.id.fab_gallery).setOnClickListener(this);
        findViewById(R.id.main_floating_button).setOnClickListener(this);
    }

    private File createImageFile() throws IOException {
        String imageFileName = FileUtil.getImageName();
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, IMAGE_EXTENSION, storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private List<Frame> getChosenList() {
        List<Frame> chosenList = new ArrayList<>();
        int length = mFrames.size();
        for (int i = 0; i < length; i++) {
            if (mFrames.get(i).isChosen()) {
                chosenList.add(mFrames.get(i));
            }
        }
        return chosenList;
    }

    public void refresh(List<Frame> frames) {
        mFrames.clear();
        mFrames.addAll(frames);
        mImageAdapter.notifyDataSetChanged();
    }
}

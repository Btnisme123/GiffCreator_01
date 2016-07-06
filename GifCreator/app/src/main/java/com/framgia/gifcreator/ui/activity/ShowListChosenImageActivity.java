package com.framgia.gifcreator.ui.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.framgia.gifcreator.R;
import com.framgia.gifcreator.adapter.ImageAdapter;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Frame;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.ui.decoration.GridItemDecoration;
import com.framgia.gifcreator.ui.widget.GetPhotoDialog;
import com.framgia.gifcreator.util.AppHelper;
import com.framgia.gifcreator.util.FileUtil;
import com.framgia.gifcreator.util.PermissionUtil;
import com.framgia.gifcreator.util.UrlCacheUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowListChosenImageActivity extends BaseActivity implements
        ImageAdapter.OnItemClickListener, View.OnClickListener, GetPhotoDialog.OnDialogItemChooseListener {

    public static CoordinatorLayout sCoordinatorLayout;
    public static int sNumberOfFrames;
    public static boolean sCanAdjustFrame;
    private final int MIN_SIZE = 2;
    private final int MAX_SIZE = 10;
    private final String IMAGE_EXTENSION = ".jpg";
    private ImageAdapter mImageAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;
    private MenuItem mItemPreviewGif;
    private MenuItem mItemOpenListChosen;
    private List<Frame> mAllItemList;
    private List<Frame> mGalleryList;
    private List<Frame> mCameraList;
    private List<Frame> mFacebookList;
    private List<Frame> mChosenList;
    private String mCurrentPhotoPath;
    private int mRequestCode;
    private boolean mIsChosenList;
    private CallbackManager mCallbackManager;
    private AccessToken mAccessToken;
    String[] paths;
    int size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        mCallbackManager = CallbackManager.Factory.create();
        findViews();
        // Setup recycler view
        mAllItemList = new ArrayList<>();
        mCameraList = new ArrayList<>();
        mGalleryList = new ArrayList<>();
        mChosenList = new ArrayList<>();
        mFacebookList = new ArrayList<>();
        mImageAdapter = new ImageAdapter(this, mAllItemList);
        mImageAdapter.setOnItemClickListener(this);
        // Call activity to get photo
        Intent intent = getIntent();
        if (intent != null) {
            mRequestCode = intent.getIntExtra(Constants.EXTRA_REQUEST, Constants.REQUEST_FACEBOOK);
            switch (mRequestCode) {
                case Constants.REQUEST_CAMERA:
                    mIsChosenList = false;
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
                    sNumberOfFrames = 0;
                    sCanAdjustFrame = false;
                    mIsChosenList = false;
                    if (mGalleryList.size() == 0) {
                        mGalleryList = getImageListGallery();
                    }
                    refresh(mGalleryList);
                    break;
                case Constants.REQUEST_FACEBOOK:
                    mRequestCode = Constants.REQUEST_FACEBOOK;
                    loginFacebook();
                    if (mFacebookList.size() == 0) {
                        getFacebookAlbum();
                    }
                    break;
            }
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.addItemDecoration(new GridItemDecoration(this));
        mRecyclerView.setAdapter(mImageAdapter);
        enableBackButton();
        mToolbar.setTitle(R.string.title_show_chosen_images_activity);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_choosing_image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chosen_image, menu);
        mItemPreviewGif = menu.findItem(R.id.action_preview_gif);
        mItemOpenListChosen = menu.findItem(R.id.action_open_list_chosen);
        mItemPreviewGif.setVisible(mRequestCode != Constants.REQUEST_GALLERY);
        if (mRequestCode == Constants.REQUEST_GALLERY || mRequestCode == Constants.REQUEST_FACEBOOK) {
            mItemOpenListChosen.setVisible(true);
        } else {
            mItemOpenListChosen.setVisible(false);
        }
        //mItemOpenListChosen.setVisible(mRequestCode == Constants.REQUEST_GALLERY);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.title_show_chosen_images_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCallbackManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        sCanAdjustFrame = true;
        if (resultCode == RESULT_OK) {
            Frame frame;
            switch (requestCode) {
                case Constants.REQUEST_CAMERA:
                    mIsChosenList = true;
                    refreshToolbar();
                    frame = new Frame(mCurrentPhotoPath);
                    frame.setChecked(true);
                    mCameraList.add(frame);
                    refresh(mCameraList);
                    galleryAddPic();
                    break;
                case Constants.REQUEST_ADJUST:
                    int position = data.getIntExtra(Constants.EXTRA_POSITION, 0);
                    String photoPath = data.getStringExtra(Constants.EXTRA_PHOTO_PATH);
                    if (!TextUtils.isEmpty(photoPath)) {
                        frame = mAllItemList.get(position);
                        frame.setPhotoPath(photoPath);
                        frame.setFrame(null);
                        switch (mRequestCode) {
                            case Constants.REQUEST_CAMERA:
                                mCameraList.get(position).setPhotoPath(photoPath);
                                break;
                            case Constants.REQUEST_GALLERY:
                                mGalleryList.get(position).setPhotoPath(photoPath);
                                break;
                            case Constants.REQUEST_FACEBOOK:
                                mFacebookList.get(position).setPhotoPath(photoPath);
                                break;
                        }
                        mImageAdapter.notifyItemChanged(position);
                    }
                case Constants.REQUEST_FACEBOOK:
                    if (mFacebookList.size() == 0) {
                        getFacebookAlbum();
                    }
                    refresh(mFacebookList);
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preview_gif:
                size = mAllItemList.size();
                paths = new String[size];
                if (size < MIN_SIZE) {
                    AppHelper.showSnackbar(sCoordinatorLayout, R.string.warning_make_gif);

                } else {
                    if (mRequestCode == Constants.REQUEST_FACEBOOK) {
                        UrlCacheUtil.getInstance().init(this);
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute();
                    } else {
                        Intent intent = new Intent(ShowListChosenImageActivity.this, PreviewGifActivity.class);
                        for (int i = 0; i < size; i++) {
                            paths[i] = mAllItemList.get(i).getPhotoPath();
                        }
                        intent.putExtra(Constants.EXTRA_PATHS_LIST, paths);
                        startActivity(intent);
                    }
                }
                break;
            case R.id.action_open_list_chosen:
                if (getChosenList().size() > MAX_SIZE) {
                    AppHelper.showSnackbar(sCoordinatorLayout, R.string.out_of_limit);
                } else {
                    sCanAdjustFrame = true;
                    mIsChosenList = true;
                    mChosenList = getChosenList();
                    refresh(mChosenList);
                }
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mIsChosenList) {
            mIsChosenList = true;
            sCanAdjustFrame = true;
            int chosenListSize = mChosenList.size();
            int gallerySize = mGalleryList.size();
            if (gallerySize > 0) {
                for (int i = 0; i < gallerySize; i++) {
                    mGalleryList.get(i).setChecked(false);
                }
                if (chosenListSize > 0) {
                    for (int i = 0; i < chosenListSize; i++) {
                        for (int j = 0; j < gallerySize; j++) {
                            Frame frame = mGalleryList.get(j);
                            if (frame.getPhotoPath().equals(mChosenList.get(i).getPhotoPath())) {
                                frame.setChecked(true);
                                break;
                            }
                        }
                    }
                }
            }
            sNumberOfFrames = chosenListSize;
            refresh(mChosenList);
        } else {
            super.onBackPressed();
            sCanAdjustFrame = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floating_button:
                GetPhotoDialog dialog = new GetPhotoDialog(this);
                dialog.setOnDialogItemChooseListener(this);
                dialog.showDialog();
                break;
        }
    }

    @Override
    public void onDialogItemChoose(int type) {
        switch (type) {
            case GetPhotoDialog.TYPE_CAMERA:
                if (PermissionUtil.isCameraPermissionGranted(this)) {
                    if (mAllItemList.size() == Constants.MAXIMUM_FRAMES) {
                        AppHelper.showSnackbar(sCoordinatorLayout, R.string.out_of_limit);
                    } else {
                        mRequestCode = Constants.REQUEST_CAMERA;
                        mIsChosenList = false;
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
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).
                            setMessage(R.string.cannot_access_camera).show();
                }
                break;
            case GetPhotoDialog.TYPE_GALLERY:
                if (PermissionUtil.isStoragePermissionGranted(this)) {
                    if (mAllItemList.size() > Constants.MAXIMUM_FRAMES) {
                        AppHelper.showSnackbar(sCoordinatorLayout, R.string.out_of_limit);
                    } else {
                        sCanAdjustFrame = false;
                        mRequestCode = Constants.REQUEST_GALLERY;
                        mIsChosenList = false;
                        if (mGalleryList.size() == 0) {
                            mGalleryList = getImageListGallery();
                        }
                        refresh(mGalleryList);
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_access_gallery).show();
                }
                break;
            case GetPhotoDialog.TYPE_FACEBOOK:
                if (PermissionUtil.isNetworkPermissionGranted(this)) {
                    if (mAllItemList.size() > Constants.MAXIMUM_FRAMES) {
                        AppHelper.showSnackbar(sCoordinatorLayout, R.string.out_of_limit);
                    } else {
                        sCanAdjustFrame = false;
                        mRequestCode = Constants.REQUEST_FACEBOOK;
                        mIsChosenList = false;
                        if (mFacebookList.size() == 0) {
                            getFacebookAlbum();
                        }
                        refresh(mFacebookList);
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_access_network).show();
                }
                break;
        }
    }

    @Override
    public void onItemClick(int position) {
        if (sCanAdjustFrame) {
            Intent intent = new Intent(this, AdjustImageActivity.class);
            intent.putExtra(Constants.EXTRA_POSITION, position);
            intent.putExtra(Constants.EXTRA_PHOTO_PATH, mAllItemList.get(position).getPhotoPath());
            startActivityForResult(intent, Constants.REQUEST_ADJUST);
        } else {
            Frame frame = mAllItemList.get(position);
            if (frame.isChosen()) {
                sNumberOfFrames--;
                frame.setChecked(false);
            } else {
                if (sNumberOfFrames < Constants.MAXIMUM_FRAMES) {
                    frame.setChecked(true);
                    sNumberOfFrames++;
                } else {
                    AppHelper.showSnackbar(sCoordinatorLayout, R.string.out_of_limit);
                    frame.setChecked(false);
                }
            }
            mImageAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onRemoveItem(int position) {
        sNumberOfFrames--;
        if (mIsChosenList) {
            switch (mRequestCode) {
                case Constants.REQUEST_CAMERA:
                    mAllItemList.remove(position);
                    mCameraList.remove(position);
                    mImageAdapter.notifyItemRemoved(position);
                    mImageAdapter.notifyItemRangeChanged(position, mAllItemList.size());
                    break;
                case Constants.REQUEST_GALLERY:
                    UpdateStateFromList(mGalleryList, mChosenList.get(position));
                    mChosenList.remove(position);
                    refresh(mChosenList);
                    break;
                case Constants.REQUEST_FACEBOOK:
                    UpdateStateFromList(mFacebookList, mChosenList.get(position));
                    mChosenList.remove(position);
                    refresh(mChosenList);
                    break;
            }
        }
    }

    private void findViews() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_choosing_image);
        sCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mFab = (FloatingActionButton) findViewById(R.id.floating_button);
        mFab.setOnClickListener(this);
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
        int length = mAllItemList.size();
        for (int i = 0; i < length; i++) {
            Frame frame = mAllItemList.get(i);
            if (frame.isChosen()) {
                chosenList.add(frame);
            }
        }
        return chosenList;
    }

    public void refresh(List<Frame> frames) {
        if (mAllItemList.size() > 0) mAllItemList.clear();
        mAllItemList.addAll(frames);
        mImageAdapter.notifyDataSetChanged();
        mFab.setVisibility(mIsChosenList ? View.VISIBLE : View.GONE);
        refreshToolbar();
    }

    private void refreshToolbar() {
        if (mItemPreviewGif != null && mItemOpenListChosen != null) {
            mItemPreviewGif.setVisible(mIsChosenList);
            mItemOpenListChosen.setVisible(!mIsChosenList);
        }
    }

    private void UpdateStateFromList(List<Frame> frames, Frame frame) {
        int length = frames.size();
        for (int i = 0; i < length; i++) {
            if (frame.getPhotoPath().equals(frames.get(i).getPhotoPath())) {
                frames.get(i).setChecked(false);
            }
        }
    }

    private List<Frame> getImageListGallery() {
        List<Frame> imageItems = new ArrayList<>();
        CursorLoader imageLoader = new CursorLoader(this,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA},
                null, null, MediaStore.Images.Media._ID);
        Cursor imageCursor = imageLoader.loadInBackground();
        if (imageCursor.moveToLast()) {
            do {
                String imagePath = imageCursor.getString(
                        imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                if (isNormalImage(imagePath)) {
                    Frame frame = new Frame(imagePath);
                    imageItems.add(frame);
                }
            } while (imageCursor.moveToPrevious());
        }
        imageCursor.close();
        return imageItems;
    }

    private boolean isNormalImage(String filePath) {
        int position = filePath.lastIndexOf(Constants.DOT);
        return (position > 0 && (filePath.substring(position + 1).equals(Constants.PNG) ||
                filePath.substring(position + 1).equals(Constants.JPG)));
    }

    private void getFacebookAlbum() {
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mAccessToken = loginResult.getAccessToken();
                GraphRequest.Callback callback = new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse graphResponse) {
                        JSONObject jsonObject = graphResponse.getJSONObject();
                        try {
                            JSONArray array = jsonObject.getJSONArray(Constants.FACEBOOK_DATA);
                            int lengthAlbum = array.length();
                            for (int i = 0; i < lengthAlbum; i++) {
                                getImageLinkFromAlbum(array.getJSONObject(i).getString(Constants.FACEBOOK_ID));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                GraphRequest req = GraphRequest.newGraphPathRequest(mAccessToken, Constants.FACEBOOK_ALBUM, callback);
                Bundle parameters = new Bundle();
                parameters.putString(Constants.FACEBOOK_FIELD, Constants.FACEBOOK_ID);
                req.setParameters(parameters);
                req.executeAsync();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
            }
        });
    }

    private void getImageLinkFromAlbum(String albumId) {
        GraphRequest.Callback callbackLink = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                JSONObject jsonObject = response.getJSONObject();
                try {
                    JSONArray array = jsonObject.getJSONArray(Constants.FACEBOOK_DATA);
                    int lengthImage = array.length();
                    for (int i = 0; i < lengthImage; i++) {
                        Frame item = new Frame();
                        item.setPhotoPath(array.getJSONObject(i).getString(Constants.FACEBOOK_SOURCE));
                        mFacebookList.add(item);
                    }
                    refresh(mFacebookList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        String api = "/" + albumId + Constants.FACEBOOK_PHOTO_API;
        GraphRequest req = GraphRequest.newGraphPathRequest(mAccessToken, api, callbackLink);
        Bundle parameters = new Bundle();
        parameters.putString(Constants.FACEBOOK_FIELD, Constants.FACEBOOK_SOURCE);
        req.setParameters(parameters);
        req.executeAsync();
    }

    private void loginFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(Constants.FACEBOOK_PHOTO_PERMISSION));
    }

    private ProgressDialog mDialog;
    public static final int progress_bar_type = 0;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                mDialog = new ProgressDialog(this);
                mDialog.setMessage(getString(R.string.downloading));
                mDialog.setIndeterminate(false);
                mDialog.setMax(100);
                mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDialog.setCancelable(true);
                mDialog.show();
                return mDialog;
            default:
                return null;
        }
    }

    public class DownloadTask extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDialog.setProgress(Integer.parseInt(values[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dismissDialog(progress_bar_type);
            Intent intent = new Intent(ShowListChosenImageActivity.this, PreviewGifActivity.class);
            for (int i = 0; i < size; i++) {
                paths[i] = mChosenList.get(i).getPhotoPath();
            }
            intent.putExtra(Constants.EXTRA_PATHS_LIST, paths);
            startActivity(intent);
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String folderPath = "";
            int size = mChosenList.size();
            for (int i = 0; i < size; i++) {
                Frame frame = new Frame();
                try {
                    folderPath = UrlCacheUtil
                            .getInstance()
                            .cacheImage(mChosenList.get(i).getPhotoPath());
                    frame.setPhotoPath(folderPath);
                    mChosenList.get(i).setPhotoPath(folderPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}

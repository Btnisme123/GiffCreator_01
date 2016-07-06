package com.framgia.gifcreator.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.framgia.gifcreator.R;
import com.framgia.gifcreator.adapter.GifAdapter;
import com.framgia.gifcreator.data.Constants;
import com.framgia.gifcreator.data.Gif;
import com.framgia.gifcreator.data.GifItem;
import com.framgia.gifcreator.ui.base.BaseActivity;
import com.framgia.gifcreator.ui.decoration.GridItemDecoration;
import com.framgia.gifcreator.ui.decoration.LinearItemDecoration;
import com.framgia.gifcreator.ui.widget.GetPhotoDialog;
import com.framgia.gifcreator.util.FileUtil;
import com.framgia.gifcreator.util.PermissionUtil;
import com.framgia.gifcreator.util.listener.OnListItemInteractListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener,
        GetPhotoDialog.OnDialogItemChooseListener, OnListItemInteractListener {

    private final int TYPE_SELECT = 0;
    private final int TYPE_VIEW = 1;
    private final int TYPE_DELETE = 2;
    private final int NUMBER_OF_COLUMNS = 2;
    private final String DOT = ".";
    private RecyclerView mGifRecyclerView;
    private TextView mTextNotify;
    private FloatingActionButton mFab;
    private MenuItem mItemDelete;
    private MenuItem mItemSelectAll;
    private GifAdapter mGifAdapter;
    private List<GifItem> mGifs = new ArrayList<>();
    private boolean mIsPlayingGif;
    private boolean mIsAllSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        PermissionUtil.checkPermission(this);
        mGifAdapter = new GifAdapter(this, mGifs);
        mGifAdapter.setOnListItemInteractListener(this);
        mGifRecyclerView.setAdapter(mGifAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getGifs();
        int size = mGifs.size();
        for (int i = 0; i < size; i++) {
            mGifs.get(i).setState(false);
        }
        if (size == 0) {
            showTextNotify();
        } else {
            hideTextNotify();
            if (size > 1) {
                if (!(mGifRecyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                    mGifRecyclerView.setLayoutManager(new GridLayoutManager(this, NUMBER_OF_COLUMNS));
                    mGifRecyclerView.addItemDecoration(new GridItemDecoration(this));
                }
            } else {
                if (mGifRecyclerView.getLayoutManager() == null) {
                    mGifRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    mGifRecyclerView.addItemDecoration(new LinearItemDecoration(this));
                }
            }
        }
        mGifAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mItemDelete = menu.findItem(R.id.action_delete);
        mItemSelectAll = menu.findItem(R.id.action_select_all);
        mItemDelete.setVisible(false);
        mItemSelectAll.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_delete:
                int gifsListSize = mGifs.size();
                int numberOfChosenGifs = 0;
                for (int i = 0; i < gifsListSize; i++) {
                    if (mGifs.get(i).isChosen()) numberOfChosenGifs++;
                    else break;
                }
                mIsAllSelected = (numberOfChosenGifs == gifsListSize);
                if (mIsAllSelected) {
                    for (int i = 0; i < gifsListSize; i++) {
                        FileUtil.removeFile(mGifs.get(i).getGifPath());
                    }
                    mGifs.removeAll(mGifs);
                    mGifAdapter.notifyItemRangeRemoved(0, gifsListSize);
                    showTextNotify();
                    mGifAdapter.setState(false);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    mItemDelete.setVisible(false);
                    mItemSelectAll.setVisible(false);
                    mFab.setVisibility(View.VISIBLE);
                } else {
                    for (int i = 0; i < gifsListSize; i++) {
                        GifItem gifItem = mGifs.get(i);
                        if (gifItem.isChosen()) {
                            FileUtil.removeFile(gifItem.getGifPath());
                            mGifs.remove(i);
                            gifsListSize = mGifs.size();
                            mGifAdapter.notifyItemRemoved(i);
                            mGifAdapter.notifyItemRangeChanged(i, gifsListSize);
                            i--;
                        }
                    }
                }
                break;
            case R.id.action_select_all:
                int size = mGifs.size();
                int count = 0;
                for (int i = 0; i < size; i++) {
                    if (mGifs.get(i).isChosen()) count++;
                    else break;
                }
                mIsAllSelected = (count == size);
                for (int i = 0; i < size; i++) {
                    mGifs.get(i).setChosen(!mIsAllSelected);
                }
                mGifAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mGifAdapter.isSelecting()) {
            mGifAdapter.setState(false);
            int size = mGifs.size();
            for (int i = 0; i < size; i++) {
                GifItem gifItem = mGifs.get(i);
                gifItem.setChosen(false);
                gifItem.showCheckbox(false);
            }
            mGifAdapter.notifyDataSetChanged();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mItemDelete.setVisible(false);
            mItemSelectAll.setVisible(false);
            mFab.setVisibility(View.VISIBLE);
        } else super.onBackPressed();
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
        Intent intent = null;
        switch (type) {
            case GetPhotoDialog.TYPE_CAMERA:
                if (PermissionUtil.isCameraPermissionGranted(this)) {
                    intent = new Intent(MainActivity.this, ShowListChosenImageActivity.class);
                    intent.putExtra(Constants.EXTRA_REQUEST, Constants.REQUEST_CAMERA);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_access_camera).show();
                }
                break;
            case GetPhotoDialog.TYPE_GALLERY:
                if (PermissionUtil.isStoragePermissionGranted(this)) {
                    intent = new Intent(MainActivity.this, ShowListChosenImageActivity.class);
                    intent.putExtra(Constants.EXTRA_REQUEST, Constants.REQUEST_GALLERY);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_access_gallery).show();
                }
                break;
            case GetPhotoDialog.TYPE_FACEBOOK:
                if(PermissionUtil.isNetworkPermissionGranted(this)){
                    intent = new Intent(MainActivity.this, ShowListChosenImageActivity.class);
                    intent.putExtra(Constants.EXTRA_REQUEST, Constants.REQUEST_FACEBOOK);
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error).setMessage(R.string.cannot_access_network).show();
                }
                break;
        }
        if (intent != null) startActivity(intent);
    }

    @Override
    public void onListItemClick(int position) {
        GifItem gif = mGifs.get(position);
        if (mIsPlayingGif && !gif.isPlaying()) {
            resetGifState();
            mIsPlayingGif = false;
        }
        if (!mIsPlayingGif && !gif.isPlaying()) {
            setPlaying(gif, true);
        } else {
            setPlaying(gif, false);
        }
        mGifAdapter.notifyItemChanged(position);
    }

    @Override
    public void onListItemLongClick(final int position) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setItems(R.array.gif_long_click, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case TYPE_SELECT:
                        mGifAdapter.setState(true);
                        int size = mGifs.size();
                        for (int i = 0; i < size; i++) {
                            mGifs.get(i).showCheckbox(true);
                        }
                        mGifs.get(position).setChosen(true);
                        mGifAdapter.notifyDataSetChanged();
                        enableBackButton();
                        mItemDelete.setVisible(true);
                        mItemSelectAll.setVisible(true);
                        mFab.setVisibility(View.GONE);
                        break;
                    case TYPE_VIEW:
                        Intent intent = new Intent(MainActivity.this, ViewGifActivity.class);
                        intent.putExtra(Constants.EXTRA_GIF_PATH, mGifs.get(position).getGifPath());
                        startActivity(intent);
                        break;
                    case TYPE_DELETE:
                        mGifAdapter.setState(false);
                        FileUtil.removeFile(mGifs.get(position).getGifPath());
                        mGifs.remove(position);
                        mGifAdapter.notifyItemRemoved(position);
                        mGifAdapter.notifyItemRangeChanged(position, mGifs.size());
                        break;
                }
            }
        });
        dialogBuilder.show();
    }

    private void findViews() {
        mGifRecyclerView = (RecyclerView) findViewById(R.id.list_gif);
        mTextNotify = (TextView) findViewById(R.id.text_notify);
        mFab = (FloatingActionButton) findViewById(R.id.floating_button);
        mFab.setOnClickListener(this);
    }

    private void getGifs() {
        File folder = new File(FileUtil.getAppFolderPath(this));
        if (folder != null) {
            File[] files = folder.listFiles();
            if (files != null) {
                int length = files.length;
                if (mGifs.size() > 0) mGifs.clear();
                for (int i = 0; i < length; i++) {
                    String fileName = files[i].getName();
                    int indexOfLastDot = fileName.lastIndexOf(DOT);
                    if (indexOfLastDot > 0 &&
                            fileName.substring(indexOfLastDot).equals(Constants.GIF_EXTENSION)) {
                        mGifs.add(new GifItem(files[i].getPath()));
                    }
                }
            }
        }
    }

    private void resetGifState() {
        int size = mGifs.size();
        for (int i = 0; i < size; i++) {
            if (mGifs.get(i).isPlaying()) {
                mGifs.get(i).setState(false);
                mGifAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void setPlaying(Gif gif, boolean isPlaying) {
        gif.setState(isPlaying);
        mIsPlayingGif = isPlaying;
    }

    private void showTextNotify() {
        mGifRecyclerView.setVisibility(View.GONE);
        mTextNotify.setVisibility(View.VISIBLE);
    }

    private void hideTextNotify() {
        mGifRecyclerView.setVisibility(View.VISIBLE);
        mTextNotify.setVisibility(View.GONE);
    }
}

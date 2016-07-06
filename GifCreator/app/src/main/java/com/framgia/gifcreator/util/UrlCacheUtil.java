package com.framgia.gifcreator.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by VULAN on 6/12/2016.
 */
public class UrlCacheUtil {
    private static UrlCacheUtil sUrlCacheUtil;
    private Context mContext;

    public static UrlCacheUtil getInstance() {
        if (sUrlCacheUtil == null) {
            synchronized (UrlCacheUtil.class) {
                if (sUrlCacheUtil == null) {
                    sUrlCacheUtil = new UrlCacheUtil();
                }
            }
        }
        return sUrlCacheUtil;
    }

    public void init(Context context) {
        mContext = context;
    }

    public String cacheImage(String imgUrl) throws IOException {
        File imageFile = getImageFile(imgUrl);
        if (!imageFile.exists()) {
            downloadFile(imgUrl, imageFile);
        }
        HttpRequest.getInstance().disconnect(HttpRequest.getInstance().getConnection());
        return imageFile.getAbsolutePath();
    }

    public File getImageFile(String url) {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appFolder = new File(downloadFolder, mContext.getPackageName());
        if (!appFolder.exists()) {
            appFolder.mkdirs();
        }
        String fileName = getFileName(url);
        return new File(appFolder, fileName);
    }

    private void downloadFile(String imgUrl, File file) throws IOException {
        URL url = new URL(imgUrl);
        HttpRequest.getInstance().makeConnection(imgUrl);
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        InputStream inputStream = url.openStream();
        byte[] buffer = new byte[1024];
        int bufferLength = 0;
        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, bufferLength);
        }
        inputStream.close();
        fileOutputStream.close();
    }

    private String getFileName(String imageUrl) {
        String imageName = "";
        String modifyName = "";
        int first = 0;
        int last = 0;
        try {
            first = imageUrl.lastIndexOf("/");
            if (imageUrl.contains("?")) {
                last = imageUrl.indexOf("?");
                modifyName = imageUrl.substring(first, last);
            } else {
                modifyName = imageUrl.substring(first);
            }
            for (int i = modifyName.length() - 1; i >= 0; i--) {
                if (modifyName.charAt(i) != '/') {
                    imageName += modifyName.charAt(i);
                } else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new StringBuilder(imageName).reverse().toString().trim();
    }
}

package com.framgia.gifcreator.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by VULAN on 6/12/2016.
 */
public class HttpRequest {
    private static HttpRequest sHttpRequest;
    private HttpURLConnection mConnection;
    private static final String GET_METHOD = "GET";
    private static final int READING_TIMEOUT = 10000;
    private static final int CONNECTION_TIMEOUT = 12000;

    public static HttpRequest getInstance() {
        if (sHttpRequest == null) {
            synchronized (HttpRequest.class) {
                if (sHttpRequest == null) {
                    sHttpRequest = new HttpRequest();
                }
            }
        }
        return sHttpRequest;
    }

    public void makeConnection(String urlString) throws IOException {
        disconnect(mConnection);
        URL url = new URL(urlString);
        mConnection = (HttpURLConnection) url.openConnection();
        mConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        mConnection.setReadTimeout(READING_TIMEOUT);
        mConnection.setRequestMethod(GET_METHOD);
        mConnection.setDoInput(true);
        mConnection.setDoOutput(true);
        mConnection.connect();
    }

    public void disconnect(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    public HttpURLConnection getConnection() {
        return mConnection;
    }
}

package com.netease.vcloudnosupload.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by hzwangxiaoming on 2016/12/9.
 */
public class NOSHTTPPost implements Runnable {

    public interface PostCallback {
        void onResponse(HTTPResult result);
    }

    private Map<String, String> mHeader;
    private byte[] mPostEntity;
    private PostCallback mCallback;
    private String mUrl;

    public void setHeader(Map<String, String> header) {
        mHeader = header;
    }

    public void setPostEntity(byte[] entity) {
        mPostEntity = entity;
    }

    public void setCallback(PostCallback callback) {
        synchronized (NOSHTTPPost.class) {
            mCallback = callback;
        }
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void run() {
        try {
            URL url = new URL(mUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");

            for (Map.Entry<String, String> entry : mHeader.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());

            ds.write(mPostEntity);

            ds.flush();
            ds.close();
            int code = conn.getResponseCode();
            StringBuffer sb = new StringBuffer();
            String readLine;
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while ((readLine = responseReader.readLine()) != null) {
                sb.append(readLine).append("\n");
            }
            responseReader.close();

            HTTPResult result = new HTTPResult(code, sb.toString(), null);
            if (mCallback != null) {
                synchronized (NOSHTTPPost.class) {
                    if (mCallback != null) {
                        mCallback.onResponse(result);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

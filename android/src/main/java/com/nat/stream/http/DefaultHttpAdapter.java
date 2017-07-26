package com.nat.stream.http;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xuqinchao on 17/1/20.
 *  Copyright (c) 2017 Instapp. All rights reserved.
 */

public class DefaultHttpAdapter implements HttpAdapter {
    public static final String TAG = "DefaultHttpAdapter";
    private ExecutorService mExecutorService;

    private void execute(Runnable runnable){
        if(mExecutorService==null){
            mExecutorService = Executors.newFixedThreadPool(3);
        }
        mExecutorService.execute(runnable);
    }

    @Override
    public void sendRequest(final Request request, final HttpAdapter.OnHttpListener listener) {
        if (listener != null) {
            listener.onHttpStart();
        }
        execute(new Runnable() {
            @Override
            public void run() {
                Response response = new Response();
                try {
                    HttpURLConnection connection = openConnection(request, listener);
                    Map<String,List<String>> headers = connection.getHeaderFields();
                    int responseCode = connection.getResponseCode();
                    String responseMsg = "";
//                    String responseMsg = connection.getResponseMessage();
                    if(listener != null){
                        listener.onHeadersReceived(responseCode,headers);
                    }

                    response.statusCode = responseCode;
                    response.statusMessage = responseMsg;
                    if (responseCode >= 200 && responseCode<=299) {
                        response.originalData = readInputStreamAsBytes(connection.getInputStream(), listener);
                        response.errorCode = 0;
                    } else {
                        response.errorMsg = readInputStream(connection.getErrorStream(), listener);
                        response.errorCode = 1;
                    }
                    if (listener != null) {
                        listener.onHttpFinish(response);
                    }
                } catch (IOException|IllegalArgumentException e) {
                    Log.e(TAG, e.getMessage() + "");
                    e.printStackTrace();
                    response.statusCode = -1;
                    response.errorCode= -1;
                    response.errorMsg=e.getMessage();
                    if(listener!=null){
                        listener.onHttpFinish(response);
                    }
                }
            }
        });
    }


    /**
     * Opens an {@link HttpURLConnection} with parameters.
     *
     * @param request
     * @param listener
     * @return an open connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(Request request, HttpAdapter.OnHttpListener listener) throws IOException {
        URL url = new URL(request.url);
        HttpURLConnection connection = createConnection(url);
        connection.setConnectTimeout(request.timeoutMs);
        connection.setReadTimeout(request.timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        if (request.paramMap != null) {
            Set<String> keySets = request.paramMap.keySet();
            for (String key : keySets) {
                connection.addRequestProperty(key, request.paramMap.get(key));
            }
        }

        if ("POST".equals(request.method) || "PUT".equals(request.method) || "PATCH".equals(request.method)) {
            connection.setRequestMethod(request.method);
            if (request.body != null) {
                if (listener != null) {
                    listener.onHttpUploadProgress(0);
                }
                connection.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                //TODO big stream will cause OOM; Progress callback is meaningless
                out.write(request.body.getBytes());
                out.close();
                if (listener != null) {
                    listener.onHttpUploadProgress(100);
                }
            }
        } else if (!TextUtils.isEmpty(request.method)) {
            connection.setRequestMethod(request.method);
        } else {
            connection.setRequestMethod("GET");
        }

        return connection;
    }

    private byte[] readInputStreamAsBytes(InputStream inputStream, HttpAdapter.OnHttpListener listener) throws IOException{
        if(inputStream == null){
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        int readCount = 0;
        byte[] data = new byte[2048];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
            readCount += nRead;
            if (listener != null) {
                listener.onHttpResponseProgress(readCount);
            }
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private String readInputStream(InputStream inputStream, HttpAdapter.OnHttpListener listener) throws IOException {
        if(inputStream == null){
            return null;
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        char[] data = new char[2048];
        int len;
        while ((len = localBufferedReader.read(data)) != -1) {
            builder.append(data, 0, len);
            if (listener != null) {
                listener.onHttpResponseProgress(builder.length());
            }
        }
        localBufferedReader.close();
        return builder.toString();
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }
}

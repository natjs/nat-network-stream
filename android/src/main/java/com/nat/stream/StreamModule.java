package com.nat.stream;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import com.nat.stream.http.DefaultHttpAdapter;
import com.nat.stream.http.Request;
import com.nat.stream.http.Response;
import com.nat.stream.http.HttpAdapter;
import com.nat.stream.http.Options;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by xuqinchao on 17/1/23.
 *  Copyright (c) 2017 Instapp. All rights reserved.
 */

public class StreamModule {

    private Context mContext;
    private static volatile StreamModule instance = null;

    private StreamModule(Context context){
        mContext = context;
    }

    public static StreamModule getInstance(Context context) {
        if (instance == null) {
            synchronized (StreamModule.class) {
                if (instance == null) {
                    instance = new StreamModule(context);
                }
            }
        }

        return instance;
    }
    
    public void fetch(String optionsStr, final ModuleResultListener ModuleResultListener){
        if (ModuleResultListener == null)return;
        JSONObject optionsObj = null;
        try {
            optionsObj = JSON.parseObject(optionsStr);
        }catch (JSONException e){
            Log.e("", e.getMessage());
            ModuleResultListener.onResult(Util.getError(Constant.FETCH_INVALID_ARGUMENT, 1));
        }

        boolean invaildOption = optionsObj==null || optionsObj.getString("url")==null;
        if(invaildOption){
            if(ModuleResultListener != null) {
                ModuleResultListener.onResult(Util.getError(Constant.FETCH_INVALID_ARGUMENT, 1));
            }
            return;
        }

        String method = optionsObj.getString("method");
        String url = optionsObj.getString("url");
        JSONObject headers = optionsObj.getJSONObject("headers");
        String body = optionsObj.getString("body");
        final String type = optionsObj.getString("type");
        int timeout = optionsObj.getIntValue("timeout");

        Options.Builder builder = new Options.Builder()
                .setMethod(!"GET".equals(method)
                        &&!"POST".equals(method)
                        &&!"PUT".equals(method)
                        &&!"DELETE".equals(method)
                        &&!"HEAD".equals(method)
                        &&!"PATCH".equals(method)?"GET":method)
                .setUrl(url)
                .setBody(body)
                .setType(type)
                .setTimeout(timeout);

        extractHeaders(headers, builder, type);
        final Options options = builder.createOptions();
        sendRequest(options, new ResponseCallback() {
            @Override
            public void onResponse(Response response, Map<String, String> headers) {
                if (ModuleResultListener != null) {
                    if (response.errorCode < 0) {
                        ModuleResultListener.onResult(Util.getError(Constant.FETCH_NETWORK_ERROR, Constant.FETCH_NETWORK_ERROR_CODE));
                        return;
                    }
                    String ori_data = new String(response.originalData);

                    HashMap<String, Object> result = new HashMap<String, Object>();
                    result.put("status", response.statusCode);
                    result.put("statusText", response.statusMessage);

                    if (headers != null)
                        result.put("headers", headers);

                    if (response.errorCode > 0) {
                        result.put("ok", false);
                        if (!TextUtils.isEmpty(ori_data)) result.put("data", ori_data);
                        Log.d("network fetch false", result.toString());
                        ModuleResultListener.onResult(result);
                        return;
                    }

                    result.put("ok", true);
                    if (!TextUtils.isEmpty(ori_data)) result.put("data", ori_data);
                    Log.d("network fetch true", result.toString());
                    ModuleResultListener.onResult(result);
                }
            }
        });

    }

    private void extractHeaders(JSONObject headers, Options.Builder builder, String type){
        if(headers != null){
            for (String key : headers.keySet()) {
                builder.putHeader(key, headers.getString(key));
            }
        }
        builder.putHeader("platform", "android");
        switch (type) {
            case "text":
                builder.putHeader("Content-Type", "text/plain");
                break;
            case "json":
                builder.putHeader("Content-Type", "application/json");
                break;
            case "jsonp":
                builder.putHeader("Content-Type", "text/javascript");
                break;
        }
    }

    private void sendRequest(Options options, ResponseCallback callback){
        Request request = new Request();
        request.method = options.getMethod();
//        request.url = mWXSDKInstance.rewriteUri(Uri.parse(options.getUrl()), URIAdapter.REQUEST).toString();
        request.url = options.getUrl();
        request.body = options.getBody();
        request.timeoutMs = options.getTimeout();

        if(options.getHeaders()!=null)
            if (request.paramMap == null) {
                request.paramMap = options.getHeaders();
            }else{
                request.paramMap.putAll(options.getHeaders());
            }


        HttpAdapter adapter = new DefaultHttpAdapter();
        if (adapter != null) {
            adapter.sendRequest(request, new StreamHttpListener(callback));
        }else{
            Log.e("WXStreamModule","No HttpAdapter found,request failed.");
        }
    }

    private interface ResponseCallback{
        void onResponse(Response response, Map<String, String> headers);
    }

    private static class StreamHttpListener implements HttpAdapter.OnHttpListener {
        private ResponseCallback mCallback;
        private Map<String,Object> mResponse = new HashMap<>();
        private Map<String,String> mRespHeaders;

        private StreamHttpListener(ResponseCallback callback) {
            mCallback = callback;
        }


        @Override
        public void onHttpStart() {
        }

        @Override
        public void onHttpUploadProgress(int uploadProgress) {

        }

        @Override
        public void onHeadersReceived(int statusCode,Map<String,List<String>> headers) {
            mResponse.put("readyState",2);
            mResponse.put("status",statusCode);

            Iterator<Map.Entry<String,List<String>>> it = headers.entrySet().iterator();
            Map<String,String> simpleHeaders = new HashMap<>();
            while(it.hasNext()){
                Map.Entry<String,List<String>> entry = it.next();
                if(entry.getValue().size()>0)
                    simpleHeaders.put(entry.getKey()==null?"_":entry.getKey(),entry.getValue().get(0));
            }

            mResponse.put("headers",simpleHeaders);
            mRespHeaders = simpleHeaders;
        }

        @Override
        public void onHttpResponseProgress(int loadedLength) {
            mResponse.put("length",loadedLength);

        }

        @Override
        public void onHttpFinish(final Response response) {
            //compatible with old sendhttp
            if(mCallback!=null){
                mCallback.onResponse(response, mRespHeaders);
            }
        }
    }
}

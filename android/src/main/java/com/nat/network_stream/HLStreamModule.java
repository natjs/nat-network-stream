package com.nat.network_stream;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.nat.network_stream.http.DefaultHLHttpAdapter;
import com.nat.network_stream.http.HLRequest;
import com.nat.network_stream.http.HLResponse;
import com.nat.network_stream.http.IHLHttpAdapter;
import com.nat.network_stream.http.Options;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by xuqinchao on 17/1/23.
 *  Copyright (c) 2017 Nat. All rights reserved.
 */

public class HLStreamModule{

    private Context mContext;
    private static volatile HLStreamModule instance = null;

    private HLStreamModule(Context context){
        mContext = context;
    }

    public static HLStreamModule getInstance(Context context) {
        if (instance == null) {
            synchronized (HLStreamModule.class) {
                if (instance == null) {
                    instance = new HLStreamModule(context);
                }
            }
        }

        return instance;
    }
    
    public void fetch(String optionsStr, final HLModuleResultListener HLModuleResultListener){
        if (HLModuleResultListener == null)return;
        JSONObject optionsObj = null;
        try {
            optionsObj = JSON.parseObject(optionsStr);
        }catch (JSONException e){
            Log.e("", e.getMessage());
            HLModuleResultListener.onResult(HLUtil.getError(HLConstant.FETCH_INVALID_ARGUMENT, 1));
        }

        boolean invaildOption = optionsObj==null || optionsObj.getString("url")==null;
        if(invaildOption){
            if(HLModuleResultListener != null) {
                HLModuleResultListener.onResult(HLUtil.getError(HLConstant.FETCH_INVALID_ARGUMENT, 1));
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
            public void onResponse(HLResponse response, Map<String, String> headers) {
                if (HLModuleResultListener != null) {
                    if (response.errorCode < 0) {
                        HLModuleResultListener.onResult(HLUtil.getError(HLConstant.FETCH_NETWORK_ERROR, HLConstant.FETCH_NETWORK_ERROR_CODE));
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
                        HLModuleResultListener.onResult(result);
                        return;
                    }

                    result.put("ok", true);
                    if (!TextUtils.isEmpty(ori_data)) result.put("data", ori_data);
                    Log.d("network fetch true", result.toString());
                    HLModuleResultListener.onResult(result);
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
        HLRequest hlRequest = new HLRequest();
        hlRequest.method = options.getMethod();
//        hlRequest.url = mWXSDKInstance.rewriteUri(Uri.parse(options.getUrl()), URIAdapter.REQUEST).toString();
        hlRequest.url = options.getUrl();
        hlRequest.body = options.getBody();
        hlRequest.timeoutMs = options.getTimeout();

        if(options.getHeaders()!=null)
            if (hlRequest.paramMap == null) {
                hlRequest.paramMap = options.getHeaders();
            }else{
                hlRequest.paramMap.putAll(options.getHeaders());
            }


        IHLHttpAdapter adapter = new DefaultHLHttpAdapter();
        if (adapter != null) {
            adapter.sendRequest(hlRequest, new StreamHttpListener(callback));
        }else{
            Log.e("WXStreamModule","No HttpAdapter found,request failed.");
        }
    }

    private interface ResponseCallback{
        void onResponse(HLResponse response, Map<String, String> headers);
    }

    private static class StreamHttpListener implements IHLHttpAdapter.OnHttpListener {
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
        public void onHttpFinish(final HLResponse response) {
            //compatible with old sendhttp
            if(mCallback!=null){
                mCallback.onResponse(response, mRespHeaders);
            }
        }
    }
}

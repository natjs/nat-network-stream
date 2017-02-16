package com.nat.network_stream.http;

import java.util.List;
import java.util.Map;

/**
 * Created by xuqinchao on 17/1/20.
 *  Copyright (c) 2017 Nat. All rights reserved.
 */

public interface IHLHttpAdapter {

    /**
     * http request method
     *
     * @param request weex assemble request
     * @param listener http response notify
     */
    void sendRequest(HLRequest request, OnHttpListener listener);

    interface OnHttpListener {

        /**
         * start request
         */
        void onHttpStart();

        /**
         * headers received
         */
        void onHeadersReceived(int statusCode, Map<String, List<String>> headers);

        /**
         * post progress
         * @param uploadProgress
         */
        void onHttpUploadProgress(int uploadProgress);

        /**
         * response loaded length (bytes), full length should read from headers (content-length)
         * @param loadedLength
         */
        void onHttpResponseProgress(int loadedLength);

        /**
         * http response finish
         * @param response
         */
        void onHttpFinish(HLResponse response);
    }
}

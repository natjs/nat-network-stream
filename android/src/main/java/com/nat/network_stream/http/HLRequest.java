package com.nat.network_stream.http;

import com.nat.network_stream.HLConstant;

import java.util.Map;

/**
 * Created by xuqinchao on 17/1/20.
 *  Copyright (c) 2017 Nat. All rights reserved.
 */

public class HLRequest {

    /**
     * The request parameter
     */
    public Map<String, String> paramMap;

    /**
     * The request URL
     */
    public String url;
    /**
     * The request method
     */
    public String method;
    /**
     * The request body
     */
    public String body;

    /**
     * The request time out
     */
    public int timeoutMs = HLConstant.DEFAULT_TIMEOUT_MS;

    /**
     * The default timeout
     */
    public static final int DEFAULT_TIMEOUT_MS = 3000;
}

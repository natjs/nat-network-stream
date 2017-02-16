package com.nat.network_stream.http;

import java.util.Map;

/**
 * Created by xuqinchao on 17/1/20.
 *  Copyright (c) 2017 Nat. All rights reserved.
 */

public class HLResponse {
    /**
     * Status code
     */
    public int statusCode;

    public String statusMessage;

    /**
     * Byte stream fetched from the connection
     */
    public String data;

    public byte[] originalData;

    /**
     * Server internal error
     */
    public int errorCode;

    /**
     * Server error message
     */
    public String errorMsg;

    /**
     * Message for toast
     */
    public String toastMsg;

    /**
     * Parameter for further extension.
     */
    public Map<String, Object> extendParams;

}

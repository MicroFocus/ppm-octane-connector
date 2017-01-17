package com.ppm.integration.agilesdk.connector.octane.client;

public class OctaneClientException extends RuntimeException {

    private final String errorCode;

    private final String msgKey;

    private final String[] params;

    public OctaneClientException(String code, String msgKey, String... params) {
        this.errorCode = code;
        this.msgKey = msgKey;
        this.params = params;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public String[] getParams() {
        return params;
    }
}

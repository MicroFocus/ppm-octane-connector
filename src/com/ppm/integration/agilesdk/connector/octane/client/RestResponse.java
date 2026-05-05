package com.ppm.integration.agilesdk.connector.octane.client;

import org.springframework.http.HttpHeaders;

public class RestResponse {

    private final int statusCode;

    private final String data;

    private final byte[] binaryData;

    private final HttpHeaders headers;

    public RestResponse(int statusCode, String data) {
        this(statusCode, data, null, null);
    }

    public RestResponse(int statusCode, String data, HttpHeaders headers) {
        this(statusCode, data, null, headers);
    }

    public RestResponse(int statusCode, byte[] binaryData, HttpHeaders headers) {
        this(statusCode, null, binaryData, headers);
    }

    private RestResponse(int statusCode, String data, byte[] binaryData, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.data = data;
        this.binaryData = binaryData;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getData() {
        return data;
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

}

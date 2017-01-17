package com.ppm.integration.agilesdk.connector.octane.client;

public class RestResponse {

    private int statusCode;

    private String data;

    public RestResponse(int statusCode, String data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getData() {
        return data;
    }

}

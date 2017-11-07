package com.ppm.integration.agilesdk.connector.octane.client;

import com.ppm.integration.agilesdk.connector.octane.OctaneIntegrationConnector;
import com.ppm.integration.agilesdk.provider.Providers;

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

    @Override
    public String getMessage() {
        return Providers.getLocalizationProvider(OctaneIntegrationConnector.class).getConnectorText(msgKey, params);
    }
}

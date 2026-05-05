package com.ppm.integration.agilesdk.connector.octane.client;

import com.ppm.integration.IntegrationException;
import com.ppm.integration.agilesdk.connector.octane.OctaneIntegrationConnector;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import java.lang.Thread.UncaughtExceptionHandler;

public class OctaneConnectivityExceptionHandler implements UncaughtExceptionHandler {

    @Override public void uncaughtException(Thread t, Throwable e) {
        uncaughtException(t, e, OctaneIntegrationConnector.class);
    }

    public void uncaughtException(Thread t, Throwable e, Class cls) {
        if (e instanceof OctaneClientException) {
            handleAgmClientException((OctaneClientException)e, cls);
        } else if (isLegacyWinkClientRuntimeException(e)
                || extractException(e, java.net.UnknownHostException.class) != null
                || extractException(e, java.net.ConnectException.class) != null) {
            handleConnectivityException(e, cls);
        } else {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_OCTANE_ERR_202")
                    .setMessage("ERROR_UNKNOWN_ERROR", e.getMessage());
        }
    }

    private void handleAgmClientException(OctaneClientException e, Class cls) {
        throw IntegrationException.build(cls).setErrorCode(e.getErrorCode()).setMessage(e.getMsgKey(), e.getParams());
    }

    private void handleConnectivityException(Throwable e, Class cls) {
        java.net.UnknownHostException unknownHost = extractException(e, java.net.UnknownHostException.class);
        if (unknownHost != null) {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_OCTANE_ERR_202")
                    .setMessage("ERROR_UNKNOWN_HOST_ERROR", unknownHost.getMessage());
        }

        java.net.ConnectException connectException = extractException(e, java.net.ConnectException.class);
        if (connectException != null) {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_OCTANE_ERR_202")
                    .setMessage("ERROR_CONNECTIVITY_ERROR");
        }

        throw IntegrationException.build(cls).setErrorCode("PPM_INT_OCTANE_ERR_202")
                .setMessage("ERROR_CONNECTIVITY_ERROR");
    }

    private boolean isLegacyWinkClientRuntimeException(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if ("org.apache.wink.client.ClientRuntimeException".equals(t.getClass().getName())) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked") protected <T extends Throwable> T extractException(Throwable e,
            Class<T> clazz)
    {

        Throwable t = e;
        while (!clazz.isInstance(t) && t != null) {
            t = t.getCause();
        }

        return (T)t;
    }
}

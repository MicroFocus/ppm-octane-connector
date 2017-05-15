package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientException;
import org.json.JSONException;

import java.io.IOException;

/**
 * Created by luch on 5/10/2017.
 */
public class OnctaneIntegrationHelper {

    public static ClientPublicAPI getClient(ValueSet values) throws IOException, JSONException {
        ClientPublicAPI client = OctaneFunctionIntegration.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);
        if (!client.getAccessTokenWithFormFormat(clientId, clientSecret)) {
            throw new OctaneClientException("AGM_APP", "error in access token retrieve.");
        }
        return client;
    }
}

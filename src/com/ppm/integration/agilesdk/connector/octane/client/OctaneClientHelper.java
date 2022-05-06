package com.ppm.integration.agilesdk.connector.octane.client;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.OctaneIntegrationConnector;
import com.ppm.integration.agilesdk.connector.octane.OctaneWorkPlanIntegration;
import com.ppm.integration.agilesdk.connector.octane.client.UsernamePasswordClient;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.provider.Providers;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by lutian on 2016/11/9.
 */
public class OctaneClientHelper {

    public static ClientPublicAPI setupClientPublicAPI(ValueSet values) {

        ClientPublicAPI client = new ClientPublicAPI(values.get(OctaneConstants.KEY_BASE_URL));
        client.setSharedSpaceId(values.get(OctaneConstants.KEY_SHAREDSPACEID));
        client.setWorkSpaceId(values.get(OctaneConstants.KEY_WORKSPACEID));
        client.setClientId(values.get(OctaneConstants.APP_CLIENT_ID));
        String proxyHost = null, proxyPort = null;
        if (values.getBoolean(OctaneConstants.KEY_USE_GLOBAL_PROXY, false)) {
            String proxyURL = Providers.getServerConfigurationProvider(OctaneIntegrationConnector.class).getServerProperty("HTTP_PROXY_URL");
            if(proxyURL == null){
            	 Logger.getLogger(OctaneIntegrationConnector.class)
                 .debug(String.format("Please configure proxy in server.conf"));
            } else {
            	 Matcher m = Pattern.compile("^([^:]*)(:(\\d+))?$").matcher(proxyURL);
                 if(m.matches()){
                     proxyHost = m.group(1);
                     proxyPort = m.group(3);
                     proxyPort = proxyPort == null? "80":proxyPort;
                 }
            }
            
        } else {
            proxyHost = values.get(OctaneConstants.KEY_PROXY_HOST);
            proxyPort = values.get(OctaneConstants.KEY_PROXY_PORT);
        }

        if (!StringUtils.isEmpty(proxyHost) && !StringUtils.isEmpty(proxyPort) && StringUtils.isNumeric(proxyPort)) {
            Logger.getLogger(OctaneIntegrationConnector.class)
                    .debug(String.format("Use HTTP Proxy HOST=%s PORT=%s", proxyHost, proxyPort));
            client.setProxy(proxyHost, (int)Long.parseLong(proxyPort));
        } else {
            if (!StringUtils.isEmpty(proxyHost) || !StringUtils.isEmpty(proxyPort)) {
                Logger.getLogger(OctaneIntegrationConnector.class)
                        .error(String.format("Invalid HTTP Proxy HOST=%s PORT=%s", proxyHost, proxyPort));
            }
        }

        return client;
    }

    public static UsernamePasswordClient setupClient(UsernamePasswordClient client, List<String> cookies, ValueSet values) {
        String proxyHost = null, proxyPort = null;
        if (values.getBoolean(OctaneConstants.KEY_USE_GLOBAL_PROXY, false)) {

            String proxyURL = Providers.getServerConfigurationProvider(OctaneIntegrationConnector.class)
                    .getServerProperty("HTTP_PROXY_URL");
            Matcher m = Pattern.compile("^([^:]*)(:(\\d+))?$").matcher(proxyURL);
            if (m.matches()) {
                proxyHost = m.group(1);
                proxyPort = m.group(3);
                proxyPort = proxyPort == null ? "80" : proxyPort;
            }
        } else {
            proxyHost = values.get(OctaneConstants.KEY_PROXY_HOST);
            proxyPort = values.get(OctaneConstants.KEY_PROXY_PORT);
        }

        if (!StringUtils.isEmpty(proxyHost) && !StringUtils.isEmpty(proxyPort) && StringUtils.isNumeric(proxyPort)) {
            Logger.getLogger(OctaneWorkPlanIntegration.class)
                    .debug(String.format("Use HTTP Proxy HOST=%s PORT=%s", proxyHost, proxyPort));
            client.proxy(proxyHost, (int)Long.parseLong(proxyPort));
        } else {
            if (!StringUtils.isEmpty(proxyHost) || !StringUtils.isEmpty(proxyPort)) {
                Logger.getLogger(OctaneWorkPlanIntegration.class)
                        .error(String.format("Invalid HTTP Proxy HOST=%s PORT=%s", proxyHost, proxyPort));
            }
        }

        if (cookies != null && cookies.size() > 0) {
            client.auth(cookies);
        } else if ("true" == values.get(OctaneConstants.KEY_ENABLE_CSRF)) {
            client.auth(values.get(OctaneConstants.KEY_USERNAME), values.get(OctaneConstants.KEY_PASSWORD), true);
        } else {
            client.auth(values.get(OctaneConstants.KEY_USERNAME), values.get(OctaneConstants.KEY_PASSWORD));
        }
        return client;
    }

    /**
     * This call is needed for verifying user identity in Timesheet integration.
     */
    public static UsernamePasswordClient setupClient(UsernamePasswordClient client, ValueSet values) {
        return setupClient(client, null, values);
    }
}

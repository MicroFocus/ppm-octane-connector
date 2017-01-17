package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.Client;
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
public class OctaneFunctionIntegration {

    public static ClientPublicAPI setupClientPublicAPI(ValueSet values) {

        ClientPublicAPI client = new ClientPublicAPI(values.get(OctaneConstants.KEY_BASE_URL));
        String proxyHost = null, proxyPort = null;
        if (values.getBoolean(OctaneConstants.KEY_USE_GLOBAL_PROXY, false)) {

            //String proxyURL = Providers.getServerConfigurationProvider(OctaneIntegrationConnector.class).getServerProperty("HTTP_PROXY_URL");
            //            Matcher m = Pattern.compile("^([^:]*)(:(\\d+))?$").matcher(proxyURL);
            //            if(m.matches()){
            //                proxyHost = m.group(1);
            //                proxyPort = m.group(3);
            //                proxyPort = proxyPort == null? "80":proxyPort;
            //            }
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

    public static Client setupClient(Client client, List<String> cookies, ValueSet values) {
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

    public static Client setupClient(Client client, ValueSet values) {
        return setupClient(client, null, values);
    }
}

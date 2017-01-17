package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import java.util.Random;

/**
 * Created by lutian on 2016/11/9.
 */
public class CommonParameters {
    private static ValueSet values = new ValueSet();

    public static ValueSet getDefaultValueSet() {
        return initValueSet();
    }

    private static ValueSet initValueSet() {
        values.put(OctaneConstants.KEY_BASE_URL, "http://16.60.184.244:54869");
        values.put(OctaneConstants.KEY_USERNAME, "sa@nga");
        values.put(OctaneConstants.KEY_PASSWORD, "Welcome1");
        values.put(OctaneConstants.KEY_ENABLE_CSRF, "true");

        values.put(OctaneConstants.KEY_SHAREDSPACE, "default_shared_space");
        values.put(OctaneConstants.KEY_WORKSPACE, "default_workspace");
        values.put(OctaneConstants.KEY_RELEASE, "734.1");
        values.put(OctaneConstants.KEY_SHAREDSPACEID, "1001");
        values.put(OctaneConstants.KEY_WORKSPACEID, "1002");
        values.put(OctaneConstants.KEY_RELEASEID, "1001");

        values.put(OctaneConstants.KEY_PROXY_HOST, "web-proxy.sgp.hp.com");
        values.put(OctaneConstants.KEY_PROXY_PORT, "8080");

        values.put(OctaneConstants.APP_CLIENT_ID, "PPM1114_n4138ql6ywey3f1l64n9qvy0g");
        values.put(OctaneConstants.APP_CLIENT_SECRET, "-64f8ff3bbc3ce7C");
        return values;
    }

    public static String genRandomNumber(int digitNumber) {
        StringBuilder num = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < digitNumber; i++) {
            num.append(random.nextInt(10));
        }
        return num.toString();
    }
}

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
        values.put(OctaneConstants.KEY_BASE_URL, "***");
        values.put(OctaneConstants.KEY_USERNAME, "***");
        values.put(OctaneConstants.KEY_PASSWORD, "***");
        values.put(OctaneConstants.KEY_ENABLE_CSRF, "***");

        values.put(OctaneConstants.KEY_SHAREDSPACE, "***");
        values.put(OctaneConstants.KEY_WORKSPACE, "***");
        values.put(OctaneConstants.KEY_RELEASE, "***");
        values.put(OctaneConstants.KEY_SHAREDSPACEID, "***");
        values.put(OctaneConstants.KEY_WORKSPACEID, "***");
        values.put(OctaneConstants.KEY_RELEASEID, "***");

        values.put(OctaneConstants.KEY_PROXY_HOST, "***");
        values.put(OctaneConstants.KEY_PROXY_PORT, "***");

        values.put(OctaneConstants.APP_CLIENT_ID, "***");
        values.put(OctaneConstants.APP_CLIENT_SECRET, "***");
        
        values.put(OctaneConstants.KEY_EPIC_ENTITY_NAME, "***");
        values.put(OctaneConstants.KEY_EPIC_ENTITY_TYPE, "***");
        values.put(OctaneConstants.KEY_WORKITEM_PARENT_ID, "***");
        values.put(OctaneConstants.KEY_WORKITEM_PARENT_TYPE, "***");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME_ID, "***");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME_TYPE, "***");
        
        values.put(OctaneConstants.KEY_WORKITEM_SUBTYPE, "***");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME, "***");

        values.put(OctaneConstants.CREATE_EPIC_IN_WORKSPACE_RESPONSE_JSONDATA, "***");
        values.put(OctaneConstants.GET_EPIC_PHASE_JSONDATA, "***");
        values.put(OctaneConstants.GET_EPIC_PARENT_JSONDATA, "***");
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

package com.ppm.integration.agilesdk.connector.octane.client;

import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class CredentialJson {
    public static JSONObject obj = new JSONObject();

    public static JSONObject toJSONObject(String user, String password) {
        obj.put("user", user);
        obj.put("password", password);
        return obj;
    }

    public static JSONObject toJSONObject(String user, String password, boolean enable_csrf) {
        obj.put("user", user);
        obj.put("password", password);
        obj.put("enable_csrf", enable_csrf);
        return obj;
    }

}

package com.ppm.integration.agilesdk.connector.octane.model;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class JSonBackedObject {

    private JSONObject obj = null;

    public JSonBackedObject(JSONObject obj) {
        this.obj = obj;
    }

    protected String getString(String key, JSONObject obj) {

        if (obj == null) {
            return null;
        }

        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    protected String getString(String key) {
        return getString(key, obj);
    }


    protected JSONObject getObj(String key) {
        return getObj(key, obj);
    }

    protected JSONObject getObj(String key, JSONObject obj) {
        if (obj == null) {
            return null;
        }

        try {
            return obj.getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }

    protected int getInt(String key, int defaultValue) {
        try {
            return obj.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected Integer getInteger(String key) {
        try {
            return obj.getInt(key);
        } catch (Exception e) {
            return null;
        }
    }

    protected void updateNumericProperty(String prop, Number n) {
        obj.put(prop, n);
    }
}

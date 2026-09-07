package com.ppm.integration.agilesdk.connector.octane.model;

import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/14.
 */

public abstract class WorkItem {

    public String id;

    public String name;

    public String subType;

    static public String getSubObjectItem(String lableName, String subLableName, JSONObject rawItem) {
        JSONObject subObj = (JSONObject)rawItem.get(lableName);
        String subValue = "";
        if(subObj==null){
            return subValue;
        }
        try {
            subValue = subObj.getString(subLableName);
        } catch (net.sf.json.JSONException expected) {
            // the lable is null
        }
        return subValue;
    }

    protected static String getStringValue(JSONObject rawItem, String fieldName) {
        if (rawItem == null || fieldName == null || fieldName.isEmpty() || !rawItem.containsKey(fieldName)) {
            return "";
        }

        Object value = rawItem.get(fieldName);
        if (value == null || "null".equals(String.valueOf(value))) {
            return "";
        }

        return String.valueOf(value);
    }

    protected static int getIntValue(JSONObject rawItem, String fieldName) {
        if (rawItem == null || fieldName == null || fieldName.isEmpty() || !rawItem.containsKey(fieldName)) {
            return 0;
        }

        Object value = rawItem.get(fieldName);
        if (value == null || "null".equals(String.valueOf(value))) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException expected) {
            return 0;
        }
    }

    protected static JSONObject getObjectValue(JSONObject rawItem, String fieldName) {
        if (rawItem == null || fieldName == null || fieldName.isEmpty() || !rawItem.containsKey(fieldName)) {
            return null;
        }

        Object value = rawItem.get(fieldName);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }

        return null;
    }

    public abstract void ParseJsonData(JSONObject Obj);

}

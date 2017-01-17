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
        try {
            subValue = subObj.getString(subLableName);
        } catch (net.sf.json.JSONException expected) {
            // the lable is null
        }
        return subValue;
    }

    public abstract void ParseJsonData(JSONObject Obj);

}

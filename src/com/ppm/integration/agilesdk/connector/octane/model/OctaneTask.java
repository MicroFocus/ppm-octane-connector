package com.ppm.integration.agilesdk.connector.octane.model;

import net.sf.json.JSONObject;

public class OctaneTask extends BaseOctaneObject {

    public OctaneTask(JSONObject obj) {
        super(obj);
    }

    public String getStoryId() {
        return getString("id", getObj("story"));
    }

    public String getStoryType() {
        return getString("type", getObj("story"));
    }

}

package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class Releases extends SimpleEntityCollection<Release> {

    public List<Release> getCollection() {
        return super.getCollection();
    }

    public void SetCollection(String data) {
        JSONObject object = JSONObject.fromObject(data);
        JSONArray jsonarray = (JSONArray)(object.get("data"));
        for (int i = 0, length = jsonarray.size(); i < length; i++) {
            JSONObject tempObj = jsonarray.getJSONObject(i);
            Release release = new Release();
            release.ParseData(tempObj.toString());
            super.add(release);
        }
    }
}
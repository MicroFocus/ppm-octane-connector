package com.ppm.integration.agilesdk.connector.octane.model;

import com.ppm.integration.agilesdk.connector.octane.client.Client;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/28.
 */
public class Sprints extends SimpleEntityCollection<Sprint> {

    public List<Sprint> getCollection() {
        return super.getCollection();
    }

    public void SetCollection(String data) {

        JSONObject object = JSONObject.fromObject(data);
        JSONArray jsonarray = (JSONArray)(object.get("data"));
        if(jsonarray == null){
        	return;
        }
        for (int i = 0, length = jsonarray.size(); i < length; i++) {
            JSONObject tempObj = jsonarray.getJSONObject(i);
            Sprint tempSprint = new Sprint();
            tempSprint.ParseJsonData(tempObj);

            super.add(tempSprint);
        }
    }
}

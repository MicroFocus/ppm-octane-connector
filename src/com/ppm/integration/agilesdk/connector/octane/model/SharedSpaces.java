package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class SharedSpaces extends SimpleEntityCollection<SharedSpace> {

    public List<SharedSpace> getCollection() {
        return super.getCollection();
    }

    public void SetCollection(String data) {

        JSONObject object = JSONObject.fromObject(data);
        JSONArray jsonarray = (JSONArray)(object.get("data"));
        if(jsonarray == null){
        	return;
        }
        for (int i = 0, length = jsonarray.size(); i < length; i++) {
            JSONObject tempObj = (JSONObject)jsonarray.getJSONObject(i);
            SharedSpace tempSharedSpace = new SharedSpace();
            tempSharedSpace.id = (String)tempObj.get("id");
            tempSharedSpace.name = (String)tempObj.get("name");
            tempSharedSpace.type = (String)tempObj.get("type");
            tempSharedSpace.mode =  (String)tempObj.get("mode");
            super.add(tempSharedSpace);
        }
    }
}

package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/10.
 */
public class WorkSpaces extends SimpleEntityCollection<WorkSpace> {
    public List<WorkSpace> getCollection() {
        return super.getCollection();
    }

    public void SetCollection(String data) {

        JSONObject object = JSONObject.fromObject(data);
        JSONArray jsonarray = (JSONArray)(object.get("data"));
        if(jsonarray == null){
        	return;
        }
        int length = jsonarray.size();
        for (int i = 0; i < length; i++) {
            JSONObject tempObj = jsonarray.getJSONObject(i);
            WorkSpace tempSharedSpace = new WorkSpace();
            tempSharedSpace.id = (String)tempObj.get("id");
            tempSharedSpace.name = (String)tempObj.get("name");
            tempSharedSpace.type = (String)tempObj.get("type");
            tempSharedSpace.logicalName = (String)tempObj.get("logical_name");
            super.add(tempSharedSpace);
        }
    }
}

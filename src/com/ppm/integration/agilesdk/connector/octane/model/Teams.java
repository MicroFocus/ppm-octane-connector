package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/28.
 */
public class Teams extends SimpleEntityCollection<Team> {

    public String workSpaceId;

    public List<Team> getCollection() {
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
            Team tempTeam = new Team();
            tempTeam.id = (String)tempObj.get("id");
            tempTeam.name = (String)tempObj.get("name");
            tempTeam.teamLeader = WorkItem.getSubObjectItem("team_lead", "name", tempObj);

            tempTeam.numOfMembers = tempObj.getInt("number_of_members");
            tempTeam.estimatedVelocity = tempObj.getInt("estimated_velocity");
            super.add(tempTeam);
        }
    }
}

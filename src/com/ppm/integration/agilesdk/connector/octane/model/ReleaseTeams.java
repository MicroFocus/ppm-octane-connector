package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by lutian on 2016/11/28.
 */
public class ReleaseTeams extends SimpleEntityCollection<ReleaseTeam> {

    static int releaseTeamId = 0;

    public String releaseId;

    public String workSpaceId;

    public List<ReleaseTeam> getCollection() {
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
            ReleaseTeam tempReleaseTeam = new ReleaseTeam();
            tempReleaseTeam.teamId = (String)tempObj.get("id");
            tempReleaseTeam.releaseId = releaseId;
            tempReleaseTeam.workSpaceId = workSpaceId;
            tempReleaseTeam.releaseTeamId = String.valueOf(++releaseTeamId);
            super.add(tempReleaseTeam);
        }
    }
}

package com.ppm.integration.agilesdk.connector.octane;

import java.util.ArrayList;
import java.util.List;

import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.user.UserIntegration;

public class OctaneUserIntegration extends UserIntegration {

    @Override
    public List<AgileDataUser> getOctaneUsers(ValueSet octaneConfiguration) {
        ClientPublicAPI client = ClientPublicAPI.getClient(octaneConfiguration);
        String workSpaceId = octaneConfiguration.get(OctaneConstants.KEY_WORKSPACEID);
        String sharedSpaceId = octaneConfiguration.get(OctaneConstants.KEY_SHAREDSPACEID);
        JSONArray userArray = client.getUsersByIds(sharedSpaceId, workSpaceId, null);
        List<AgileDataUser> users = new ArrayList<>();

        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            AgileDataUser user = new AgileDataUser();
            user.setUserId(Long.valueOf(userObj.getString("id")));
            user.setUserName(userObj.getString("name"));
            user.setFirstName(userObj.getString("first_name"));
            user.setLastName(userObj.getString("last_name"));
            user.setEmail(userObj.getString("email"));
            user.setActivityLevel(userObj.getInt("activity_level"));
            users.add(user);
        }

        return users;
    }

}

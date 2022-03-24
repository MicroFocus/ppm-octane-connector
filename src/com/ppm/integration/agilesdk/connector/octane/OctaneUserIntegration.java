package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.TimeZone;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.user.UserIntegration;

public class OctaneUserIntegration extends UserIntegration {

    @Override
    public List<AgileDataUser> getAgileDataUsers(ValueSet octaneConfiguration, Date lastUpdateDate) {
        ClientPublicAPI client = ClientPublicAPI.getClient(octaneConfiguration);
        String workSpaceId = octaneConfiguration.get(OctaneConstants.KEY_WORKSPACEID);
        String sharedSpaceId = octaneConfiguration.get(OctaneConstants.KEY_SHAREDSPACEID);
        String formatDate = formatLastUpdateDate(lastUpdateDate);

        String filter = "";
        if (formatDate != null) {
            filter = "\"last_modified >= '" + formatDate + "'\"";
        }
        try {
            filter = URLEncoder.encode(filter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        JSONArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, filter);
        List<AgileDataUser> users = new ArrayList<>();

        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            AgileDataUser user = new AgileDataUser();
            user.setUserId(Long.valueOf(userObj.getString("id")));
            if (userObj.getInt("activity_level") == OctaneConstants.USER_DELETED_STATUS_CODE) {
                user.setUserName(userObj.getString("id") + "-DELETED");
            } else {
                user.setUserName(userObj.getString("name"));
            }

            user.setFirstName(userObj.getString("first_name"));
            user.setLastName(userObj.getString("last_name"));
            user.setEmail(userObj.getString("email"));
            if (userObj.getInt("activity_level") == OctaneConstants.USER_ACTIVITY_STATUS_CODE) {
                user.setEnabledFlag(true);
            } else {
                user.setEnabledFlag(false);
            }
            users.add(user);
        }

        return users;
    }

    private String formatLastUpdateDate(Date lastUpdateDate) {
        String formatDate = null;
        if (lastUpdateDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            formatDate = sdf.format(lastUpdateDate);

        }
        return formatDate;
    }

}

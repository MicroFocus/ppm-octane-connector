package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.user.UserIntegration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class OctaneUserIntegration extends UserIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private static final String DELETED = "-DELETED";

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public List<AgileDataUser> getAgileDataUsers(final ValueSet instanceConfigurationParameters,
            final String agileProjectValue, Date lastUpdateTime)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject workspaceJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        String filter = getFilterByDateQuery(lastUpdateTime);

        JSONArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, filter);
        List<AgileDataUser> users = new ArrayList<>();

        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            AgileDataUser user = new AgileDataUser();
            user.setUserId(Long.valueOf(userObj.getString("id")));
            if (userObj.getInt("activity_level") == OctaneConstants.USER_DELETED_STATUS_CODE) {
                user.setUserName(userObj.getString("id") + DELETED);
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

            try {
                Date date = sdf.parse(userObj.getString("last_modified"));
                user.setLastUpdateDate(date);

            } catch (final ParseException e) {
                logger.error(e.getMessage());
            }

            users.add(user);
        }

        return users;
    }

    private String getFilterByDateQuery(Date lastUpdateTime) {
        String filter = "";

        if (lastUpdateTime != null) {
            String formatDate = sdf.format(lastUpdateTime);
            filter = "\"last_modified > '" + formatDate + "'\"";
        }

        try {
            filter = URLEncoder.encode(filter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        return filter;
    }

}

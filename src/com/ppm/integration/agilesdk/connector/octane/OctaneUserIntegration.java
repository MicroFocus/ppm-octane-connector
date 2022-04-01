package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
            final String agileProjectValue, Map<String, Object> queryParams)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject agileProjectJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);
        JSONObject workspaceJson = agileProjectJson.getJSONObject("agileProjectValue");
        String workSpaceId = workspaceJson.getString(OctaneConstants.WORKSPACE_ID);
        String sharedSpaceId = workspaceJson.getString(OctaneConstants.SHARED_SPACE_ID);

        String filter = getFilterByDateQuery(queryParams);

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

            addSecurityGroupAndLicenseToUsers(user, agileProjectJson);

            users.add(user);
        }

        return users;
    }

    /**
     * TODO need classify users to add security and product.
     * @param user
     */
    private void addSecurityGroupAndLicenseToUsers(AgileDataUser user, JSONObject agileProjectJson) {
        // security and license
        JSONObject securityGroupsJson = agileProjectJson.getJSONObject("securityGroups");
        JSONArray adminSecurityGroups = securityGroupsJson.getJSONArray("admin");
        JSONArray userSecurityGroups = securityGroupsJson.getJSONArray("users");
        JSONObject productLicensesJson = agileProjectJson.getJSONObject("productLicenses");
        JSONArray adminLicenses = productLicensesJson.getJSONArray("admin");
        JSONArray userLicenses = productLicensesJson.getJSONArray("users");

        List<Long> userLicensesList = JSONArray.toList(userLicenses, Long.class);
        List<String> userSecurityList = JSONArray.toList(userSecurityGroups, String.class);

        user.setSecurityGroupCodes(userSecurityList);
        user.setProductIds(userLicensesList);

    }

    private String getFilterByDateQuery(Map<String, Object> queryParams) {
        String filter = "";
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
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

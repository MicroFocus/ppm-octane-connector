package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneSyncUserConfiguration;
import com.ppm.integration.agilesdk.connector.octane.model.UserSecurityConfiguration;
import com.ppm.integration.agilesdk.user.UserIntegration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class OctaneUserIntegration extends UserIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private static final String DELETED = "-DELETED";

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * @param instanceConfigurationParameters
     * @param agileProjectValue
     *            {"security":[{"role":"roleName","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"licenseType":"strategy","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
     * @param queryParams
     * @return
     * @see com.ppm.integration.agilesdk.user.UserIntegration#getAgileDataUsers(com.ppm.integration.agilesdk.ValueSet,
     *      java.lang.String, java.util.Map)
     */
    @Override
    public List<AgileDataUser> getAgileDataUsers(final ValueSet instanceConfigurationParameters,
            final String agileProjectValue, Map<String, Object> queryParams)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject agileProjectJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);

        Map<String, Class<?>> classMap = new HashMap<>();
        classMap.put("security", UserSecurityConfiguration.class);
        OctaneSyncUserConfiguration userConfiguration = (OctaneSyncUserConfiguration)agileProjectJson
                .toBean(agileProjectJson, OctaneSyncUserConfiguration.class, classMap);

        String workSpaceId = String.valueOf(userConfiguration.getAgileProjectValue().getWorkspaceId());
        String sharedSpaceId = String.valueOf(userConfiguration.getAgileProjectValue().getSharedSpaceId());

        String filter = getFilterByDateQuery(queryParams);
        Long offset = 0L;
        Long limit = 2000L;
        JSONArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, limit, offset, filter);

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
                addSecurityGroupAndLicenseToUsers(user, userObj.getJSONObject("roles"),
                        userConfiguration.getSecurity());
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

    /**
     * @param user
     * @param roles contain user role information.
     * @param agileProjectJson
     *            {"security":[{"role":"roleName","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"licenseType":"strategy","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
     */
    private void addSecurityGroupAndLicenseToUsers(AgileDataUser user, JSONObject roles,
            List<UserSecurityConfiguration> security)
    {
        if (!roles.isNullObject()) {
            JSONArray rolesArray = roles.getJSONArray("data");
            List<String> roleList = new ArrayList<>();
            for (int i = 0; i < rolesArray.size(); i++) {
                JSONObject jsonObject = rolesArray.getJSONObject(i);
                roleList.add(jsonObject.getString("name"));
            }

            for (UserSecurityConfiguration securityConf : security) {
                String role = securityConf.getRole();
                String licenseType = securityConf.getLicenseType();
                if (role != null) {
                    if (roleList.contains(role)) {
                        user.setSecurityGroupCodes(securityConf.getSecurityGroups());
                        user.setProductIds(securityConf.getProductLicenses());
                    } else {
                        // TODO add SPM user security group to SPM users
                        user.setSecurityGroupCodes(securityConf.getSecurityGroups());
                        user.setProductIds(securityConf.getProductLicenses());
                    }
                }

            }

        }


    }

    private String getFilterByDateQuery(Map<String, Object> queryParams) {
        String filter = "\"";
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
        if (lastUpdateTime != null) {
            String formatDate = sdf.format(lastUpdateTime);

            filter += "last_modified > '" + formatDate + "' ; ";
        }
        // filter out api access
        filter += "is_api_key=false\"";

        try {
            filter = URLEncoder.encode(filter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        return filter;
    }

}

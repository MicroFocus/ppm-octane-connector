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

import org.apache.commons.lang.StringUtils;
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
     *            {"security":[{"role":"role_logical_Name","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"licenseType":"licenseTypeId","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
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

        String filter = getFilterByDateQuery(queryParams, workSpaceId);
        Long offset = null;
        Long limit = null;
        try {
            if (queryParams.containsKey("offset") && queryParams.containsKey("limit")) {
                offset = Long.parseLong((String)queryParams.get("offset"));
                limit = Long.parseLong((String)queryParams.get("limit"));
            }

        } catch (NumberFormatException e) {
            logger.error("Exception when parsing query parameter", e);
        }

        JSONArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, limit, offset, filter);

        List<AgileDataUser> users = new ArrayList<>();

        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            AgileDataUser user = new AgileDataUser();
            user.setUserId(userObj.getString("id"));
            if (userObj.getInt("activity_level") == OctaneConstants.USER_DELETED_STATUS_CODE) {
                user.setUserName(userObj.getString("id") + DELETED);
            } else {
                user.setUserName(userObj.getString("name"));
            }

            user.setFirstName(userObj.getString("first_name"));
            user.setLastName(userObj.getString("last_name"));
            user.setEmail(userObj.getString("email"));

            List<String> roleList = getRoleListofUser(workSpaceId, userObj);

            JSONObject licenseType = userObj.getJSONObject("license_type");
            String licenseTypeId = licenseType.getString("id");
            if (userObj.getInt("activity_level") == OctaneConstants.USER_ACTIVITY_STATUS_CODE) {
                addSecurityGroupAndLicenseToUsers(user, roleList, licenseTypeId, userConfiguration.getSecurity());
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

    private List<String> getRoleListofUser(String workSpaceId, JSONObject userObj) {
        List<String> roleList = new ArrayList<>();
        JSONObject workspaceRoles = userObj.getJSONObject("workspace_roles");
        JSONArray rolesArray = workspaceRoles.getJSONArray("data");
        for (int j = 0; j < rolesArray.size(); j++) {
            JSONObject roleJson = rolesArray.getJSONObject(j);
            JSONObject workspace = roleJson.getJSONObject("workspace");
            if (!workspace.isNullObject() && workspace.getString("id").equals(workSpaceId)) {
                JSONObject role = roleJson.getJSONObject("role");
                roleList.add(role.getString("logical_name"));
            }
        }
        return roleList;
    }

    /**
     * @param user
     * @param roleList contain user role information.
     * @param agileProjectJson
     *            {"security":[{"role":"role_logical_Name","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"licenseType":"licenseTypeId","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
     */
    private void addSecurityGroupAndLicenseToUsers(AgileDataUser user, List<String> roleList, String licenseTypeId,
            List<UserSecurityConfiguration> security)
    {

        for (UserSecurityConfiguration securityConf : security) {
            String role = securityConf.getRole();
            String licenseType = securityConf.getLicenseType();
            if (role != null) {
                if (roleList.contains(role)) {
                    user.addAllSecurityGroupCodes(securityConf.getSecurityGroups());
                    user.addAllProductIds(securityConf.getProductLicenses());

                }
            }

            if (licenseType != null) {
                if (licenseType.equalsIgnoreCase(licenseTypeId)) {
                    user.addAllSecurityGroupCodes(securityConf.getSecurityGroups());
                    user.addAllProductIds(securityConf.getProductLicenses());
                }

            }

        }

    }

    private String getFilterByDateQuery(Map<String, Object> queryParams, String workSpaceId) {
        String filter = "\"";
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
        if (lastUpdateTime != null) {
            String formatDate = sdf.format(lastUpdateTime);

            filter += "last_modified > '" + formatDate + "' ; ";
        }
        // if workSpaceId is null, it will return sharedSpace users
        if (workSpaceId != null) {
            filter += "workspaces={id=" + workSpaceId + "};";
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
